package com.luzhiwen.collectagent.memory.infrastructure.summarizer;

import com.luzhiwen.collectagent.eventing.domain.EventType;
import com.luzhiwen.collectagent.eventing.infrastructure.persistence.entity.EventLayerEntity;
import com.luzhiwen.collectagent.memory.domain.AgentSummaryResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 无 DeepSeek 时使用的规则式会话摘要生成器。
 */
@Component
@ConditionalOnProperty(prefix = "agent.llm", name = "enabled", havingValue = "false", matchIfMissing = true)
public class RuleBasedAgentSummaryGenerator implements AgentSummaryGenerator {

    /** JSON 序列化器。 */
    private final ObjectMapper objectMapper;

    /**
     * 创建规则式摘要生成器。
     *
     * @param objectMapper JSON 序列化器
     */
    public RuleBasedAgentSummaryGenerator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 生成规则式摘要。
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
        List<String> facts = new ArrayList<>();
        String goal = "";
        for (EventLayerEntity event : events) {
            String content = extractContent(event);
            if (!content.isBlank()) {
                facts.add(event.getEventType().name() + "：" + content);
                if (event.getEventType() == EventType.USER_MESSAGE) {
                    goal = content;
                }
            }
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("customerGoal", goal);
        summary.put("confirmedFacts", facts);
        summary.put("discussedPlans", List.of());
        summary.put("objections", List.of());
        summary.put("commitments", List.of());
        summary.put("openIssues", List.of());
        summary.put("nextActions", List.of());
        summary.put("complianceFlags", List.of());
        try {
            String json = objectMapper.writeValueAsString(summary);
            return new AgentSummaryResult(json, "RULE", estimateTokens(json));
        } catch (JacksonException exception) {
            throw new IllegalStateException("规则摘要序列化失败", exception);
        }
    }

    /**
     * 从事件载荷提取可读内容。
     *
     * @param event 原始事件
     * @return 可读内容
     */
    private String extractContent(EventLayerEntity event) {
        try {
            var payload = objectMapper.readTree(event.getPayloadJson());
            String content = payload.path("content").asString("");
            if (!content.isBlank()) {
                return content;
            }
            String toolName = payload.path("toolName").asString("tool");
            String result = payload.path("result").asString("");
            return result.isBlank() ? event.getPayloadJson() : toolName + "：" + result;
        } catch (JacksonException exception) {
            return "";
        }
    }

    /**
     * 估算摘要 Token 数。
     *
     * @param text 摘要文本
     * @return 估算 Token 数
     */
    private int estimateTokens(String text) {
        return Math.max(1, text.length() / 2);
    }
}
