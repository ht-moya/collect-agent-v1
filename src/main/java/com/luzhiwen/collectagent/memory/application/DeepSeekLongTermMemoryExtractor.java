package com.luzhiwen.collectagent.memory.application;

import com.luzhiwen.collectagent.eventing.infrastructure.persistence.entity.EventLayerEntity;
import com.luzhiwen.collectagent.memory.domain.LongTermMemoryCandidate;
import com.luzhiwen.collectagent.memory.domain.LongTermMemoryType;
import com.luzhiwen.collectagent.model.application.LlmChatResult;
import com.luzhiwen.collectagent.model.application.LlmMessage;
import com.luzhiwen.collectagent.model.infrastructure.deepseek.DeepSeekChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 DeepSeek 的长期记忆抽取器。
 */
@Component
@ConditionalOnProperty(prefix = "agent.llm", name = "enabled", havingValue = "true")
public class DeepSeekLongTermMemoryExtractor implements LongTermMemoryExtractor {

    /** DeepSeek 客户端。 */
    private final DeepSeekChatClient deepSeekChatClient;

    /** JSON 序列化器。 */
    private final ObjectMapper objectMapper;

    /**
     * 创建 DeepSeek 长期记忆抽取器。
     *
     * @param deepSeekChatClient DeepSeek 客户端
     * @param objectMapper JSON 序列化器
     */
    public DeepSeekLongTermMemoryExtractor(DeepSeekChatClient deepSeekChatClient, ObjectMapper objectMapper) {
        this.deepSeekChatClient = deepSeekChatClient;
        this.objectMapper = objectMapper;
    }

    /** {@inheritDoc} */
    @Override
    public List<LongTermMemoryCandidate> extract(
            String traceId,
            String sessionId,
            String userId,
            List<EventLayerEntity> events
    ) {
        LlmChatResult result = deepSeekChatClient.chat(
                traceId,
                sessionId,
                userId,
                List.of(
                        new LlmMessage("system", buildSystemPrompt()),
                        new LlmMessage("user", buildUserPrompt(events))
                ),
                List.of(),
                null,
                true
        );
        return parseCandidates(result.getContent());
    }

    /**
     * 构建抽取系统提示词。
     *
     * @return 系统提示词
     */
    private String buildSystemPrompt() {
        return """
                你是记忆抽取器。根据给定会话事件，抽取值得跨会话保留的信息。
                只抽取：
                - preference：联系方式、联系时间、沟通方式偏好
                - fact：失业、疾病、家庭情况等稳定事实
                - episode：还款承诺及是否履约的历史模式
                - procedure：可复用且明确的处理流程
                不要抽取一次性指令、模型猜测、欠款金额/逾期天数、身份证、银行卡完整号。
                structured 只允许：
                - preference: channel(sms|phone|wechat), time_window(morning|afternoon|evening)
                - fact: fact_key(employment_status|family_status|health_status|financial_status), fact_value(string)
                - episode: promise_date(YYYY-MM-DD), promise_amount(number), fulfilled(boolean)
                - procedure: 空对象
                事实字段映射要求：
                - 失业、待业、收入来源变化 -> employment_status
                - 家庭关系或家庭成员变化 -> family_status
                - 疾病、身体状况 -> health_status
                - 经济困难、还款压力、资金紧张 -> financial_status
                严格输出 JSON 对象，禁止 Markdown 和额外说明：
                {"memories":[{"type":"preference","content":"用户更接受短信联系","structured":{"channel":"sms"},"confidence":0.85,"source_event_ids":["event_id"]}]}
                每个候选的 source_event_ids 只能来自输入事件的 event_id。
                """;
    }

    /**
     * 构建抽取用户提示词。
     *
     * @param events 事件列表
     * @return 用户提示词
     */
    private String buildUserPrompt(List<EventLayerEntity> events) {
        StringBuilder builder = new StringBuilder("待抽取事件：\n");
        for (EventLayerEntity event : events) {
            builder.append("seq=").append(event.getSessionSeq())
                    .append("，event_id=").append(event.getEventId())
                    .append("，type=").append(event.getEventType())
                    .append("，payload=").append(event.getPayloadJson())
                    .append('\n');
        }
        return builder.toString();
    }

    /**
     * 解析模型返回的候选列表。
     *
     * @param content 模型文本
     * @return 候选列表
     */
    @SuppressWarnings("unchecked")
    private List<LongTermMemoryCandidate> parseCandidates(String content) {
        try {
            JsonNode root = objectMapper.readTree(stripCodeFence(content));
            JsonNode memories = root.path("memories");
            if (!memories.isArray()) {
                return List.of();
            }
            List<LongTermMemoryCandidate> candidates = new ArrayList<>();
            for (JsonNode item : memories) {
                Map<String, Object> structured = item.path("structured").isObject()
                        ? objectMapper.convertValue(item.path("structured"), Map.class)
                        : Map.of();
                List<String> sourceIds = new ArrayList<>();
                for (JsonNode sourceId : item.path("source_event_ids")) {
                    sourceIds.add(sourceId.asString());
                }
                candidates.add(new LongTermMemoryCandidate(
                        LongTermMemoryType.fromValue(item.path("type").asString()),
                        item.path("content").asString(),
                        structured,
                        sourceIds,
                        item.path("confidence").asDouble()
                ));
            }
            return candidates;
        } catch (JacksonException exception) {
            throw new IllegalStateException("长期记忆抽取 JSON 解析失败", exception);
        }
    }

    /**
     * 去除 Markdown 代码围栏。
     *
     * @param content 模型文本
     * @return JSON 文本
     */
    private String stripCodeFence(String content) {
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.startsWith("```")) {
            int firstLineEnd = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstLineEnd >= 0 && lastFence > firstLineEnd) {
                return trimmed.substring(firstLineEnd + 1, lastFence).trim();
            }
        }
        return trimmed;
    }
}
