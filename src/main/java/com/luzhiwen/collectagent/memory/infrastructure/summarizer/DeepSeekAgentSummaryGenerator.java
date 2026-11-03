package com.luzhiwen.collectagent.memory.infrastructure.summarizer;

import com.luzhiwen.collectagent.eventing.infrastructure.persistence.entity.EventLayerEntity;
import com.luzhiwen.collectagent.memory.domain.AgentSummaryResult;
import com.luzhiwen.collectagent.model.application.LlmChatResult;
import com.luzhiwen.collectagent.model.application.LlmMessage;
import com.luzhiwen.collectagent.model.infrastructure.deepseek.DeepSeekChatClient;
import com.luzhiwen.collectagent.shared.config.AgentProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * 基于 DeepSeek 的会话摘要生成器。
 */
@Component
@ConditionalOnProperty(prefix = "agent.llm", name = "enabled", havingValue = "true")
public class DeepSeekAgentSummaryGenerator implements AgentSummaryGenerator {

    /** DeepSeek 客户端。 */
    private final DeepSeekChatClient deepSeekChatClient;

    /** JSON 序列化器。 */
    private final ObjectMapper objectMapper;

    /** Agent 配置。 */
    private final AgentProperties agentProperties;

    /**
     * 创建 DeepSeek 摘要生成器。
     *
     * @param deepSeekChatClient DeepSeek 客户端
     * @param objectMapper JSON 序列化器
     * @param agentProperties Agent 配置
     */
    public DeepSeekAgentSummaryGenerator(
            DeepSeekChatClient deepSeekChatClient,
            ObjectMapper objectMapper,
            AgentProperties agentProperties
    ) {
        this.deepSeekChatClient = deepSeekChatClient;
        this.objectMapper = objectMapper;
        this.agentProperties = agentProperties;
    }

    /**
     * 使用 DeepSeek 生成结构化摘要。
     *
     * @param traceId 请求链路标识
     * @param sessionId 会话唯一标识
     * @param customerId 客户唯一标识
     * @param previousSummary 旧摘要
     * @param events 待压缩事件
     * @return 摘要结果
     */
    @Override
    public AgentSummaryResult summarize(
            String traceId,
            String sessionId,
            String customerId,
            String previousSummary,
            List<EventLayerEntity> events
    ) {
        LlmChatResult result = deepSeekChatClient.chat(
                traceId,
                sessionId,
                customerId,
                List.of(
                        new LlmMessage("system", buildSystemPrompt()),
                        new LlmMessage("user", buildUserPrompt(previousSummary, events))
                ),
                List.of()
        );
        String json = stripCodeFence(result.getContent());
        validate(json);
        return new AgentSummaryResult(
                json,
                agentProperties.getLlm().getChatModel(),
                Math.max(1, json.length() / 2)
        );
    }

    /**
     * 构建摘要系统提示词。
     *
     * @return 系统提示词
     */
    private String buildSystemPrompt() {
        return """
                你是智能催收会话摘要器。只能依据旧摘要和新增事件合并摘要。
                不得编造金额、日期、方案、承诺或投诉状态。
                输出必须是 JSON 对象，不要输出 Markdown 和额外说明。
                固定字段：
                customerGoal: string
                confirmedFacts: string[]
                discussedPlans: string[]
                objections: string[]
                commitments: string[]
                openIssues: string[]
                nextActions: string[]
                complianceFlags: string[]
                """;
    }

    /**
     * 构建摘要用户提示词。
     *
     * @param previousSummary 旧摘要
     * @param events 待压缩事件
     * @return 用户提示词
     */
    private String buildUserPrompt(String previousSummary, List<EventLayerEntity> events) {
        StringBuilder builder = new StringBuilder();
        builder.append("旧摘要：\n").append(previousSummary).append("\n\n新增事件：\n");
        for (EventLayerEntity event : events) {
            builder.append("seq=").append(event.getSessionSeq())
                    .append("，eventId=").append(event.getEventId())
                    .append("，type=").append(event.getEventType())
                    .append("，payload=").append(event.getPayloadJson())
                    .append('\n');
        }
        builder.append("\n合并旧摘要和新增事件，只输出对应 JSON。");
        return builder.toString();
    }

    /**
     * 校验摘要 JSON 的固定结构。
     *
     * @param json 摘要 JSON
     */
    private void validate(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isObject() || !root.path("customerGoal").isTextual()) {
                throw new IllegalStateException("摘要结构不合法");
            }
        } catch (JacksonException exception) {
            throw new IllegalStateException("摘要 JSON 解析失败", exception);
        }
    }

    /**
     * 去除 Markdown 代码围栏。
     *
     * @param content 模型回复
     * @return 纯 JSON
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
