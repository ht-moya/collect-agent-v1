package com.luzhiwen.collectagent.agentruntime.domain;

import com.luzhiwen.collectagent.memory.domain.ShortTermMemoryEvent;
import com.luzhiwen.collectagent.memory.domain.LongTermMemoryView;
import com.luzhiwen.collectagent.model.application.LlmMessage;

import java.util.List;

/**
 * Agent 单次规划上下文。
 */
public class AgentContext {

    /** 请求链路标识。 */
    private final String traceId;

    /** 会话唯一标识。 */
    private final String sessionId;

    /** 客户唯一标识。 */
    private final String customerId;

    /** 当前用户消息。 */
    private final String currentMessage;

    /** 当前用户消息对应的原始事件主键。 */
    private final Long sourceEventId;

    /** 短期记忆最近事件。 */
    private final List<ShortTermMemoryEvent> recentEvents;

    /** 提供给 LLM 的会话状态视图 JSON。 */
    private final String stateViewJson;

    /** 当前最新摘要文本。 */
    private final String summaryJson;

    /** 当前会话动态变量 JSON。 */
    private final String dynamicVariablesJson;

    /** 长期记忆卡片。 */
    private final List<LongTermMemoryView> longTermMemories;

    /** 已执行工具观察结果。 */
    private final List<ToolObservation> observations;

    /** 当前请求的标准 LLM 消息历史。 */
    private final List<LlmMessage> nativeMessages;

    /**
     * 创建 Agent 上下文。
     *
     * @param traceId 请求链路标识
     * @param sessionId 会话唯一标识
     * @param customerId 客户唯一标识
     * @param currentMessage 当前用户消息
     * @param sourceEventId 当前用户消息对应的原始事件主键
     * @param recentEvents 短期记忆事件
     * @param stateViewJson 会话状态视图 JSON
     * @param summaryJson 最新摘要文本
     * @param dynamicVariablesJson 动态变量 JSON
     * @param longTermMemories 长期记忆卡片
     * @param observations 工具观察结果
     * @param nativeMessages 标准 LLM 消息历史
     */
    public AgentContext(
            String traceId,
            String sessionId,
            String customerId,
            String currentMessage,
            Long sourceEventId,
            List<ShortTermMemoryEvent> recentEvents,
            String stateViewJson,
            String summaryJson,
            String dynamicVariablesJson,
            List<LongTermMemoryView> longTermMemories,
            List<ToolObservation> observations,
            List<LlmMessage> nativeMessages
    ) {
        this.traceId = traceId;
        this.sessionId = sessionId;
        this.customerId = customerId;
        this.currentMessage = currentMessage;
        this.sourceEventId = sourceEventId;
        this.recentEvents = List.copyOf(recentEvents);
        this.stateViewJson = stateViewJson;
        this.summaryJson = summaryJson;
        this.dynamicVariablesJson = dynamicVariablesJson;
        this.longTermMemories = List.copyOf(longTermMemories);
        this.observations = List.copyOf(observations);
        this.nativeMessages = nativeMessages;
    }

    /** @return 请求链路标识。 */
    public String getTraceId() { return traceId; }

    /** @return 会话唯一标识。 */
    public String getSessionId() { return sessionId; }

    /** @return 客户唯一标识。 */
    public String getCustomerId() { return customerId; }

    /** @return 当前用户消息。 */
    public String getCurrentMessage() { return currentMessage; }

    /** @return 当前用户消息对应的原始事件主键。 */
    public Long getSourceEventId() { return sourceEventId; }

    /** @return 短期记忆事件。 */
    public List<ShortTermMemoryEvent> getRecentEvents() { return recentEvents; }

    /** @return 会话状态视图 JSON。 */
    public String getStateViewJson() { return stateViewJson; }

    /** @return 最新摘要文本。 */
    public String getSummaryJson() { return summaryJson; }

    /** @return 动态变量 JSON。 */
    public String getDynamicVariablesJson() { return dynamicVariablesJson; }

    /** @return 长期记忆卡片。 */
    public List<LongTermMemoryView> getLongTermMemories() { return longTermMemories; }

    /** @return 工具观察结果。 */
    public List<ToolObservation> getObservations() { return observations; }

    /** @return 标准 LLM 消息历史。 */
    public List<LlmMessage> getNativeMessages() { return nativeMessages; }
}

