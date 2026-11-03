package com.luzhiwen.collectagent.agentruntime.application;

import com.luzhiwen.collectagent.agentruntime.domain.ToolObservation;
import com.luzhiwen.collectagent.conversation.api.dto.AgentChatRequest;
import com.luzhiwen.collectagent.eventing.infrastructure.persistence.entity.EventLayerEntity;
import com.luzhiwen.collectagent.memory.domain.LongTermMemoryView;
import com.luzhiwen.collectagent.memory.domain.ShortTermMemorySnapshot;
import com.luzhiwen.collectagent.model.application.LlmMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * 单次 Agent 请求的可变执行上下文。
 */
public class AgentTurnContext {

    /** 请求链路标识。 */
    private final String traceId;

    /** 会话唯一标识。 */
    private final String sessionId;

    /** 客户唯一标识。 */
    private final String customerId;

    /** 当前用户消息。 */
    private final String currentMessage;

    /** 请求开始时间纳秒值。 */
    private final long startNanos;

    /** 原始聊天请求。 */
    private final AgentChatRequest request;

    /** 标准 LLM 消息历史。 */
    private final List<LlmMessage> nativeMessages;

    /** 已执行工具结果。 */
    private final List<ToolObservation> observations = new ArrayList<>();

    /** 当前用户消息事件主键。 */
    private Long sourceEventId;

    /** 当前用户消息事件。 */
    private EventLayerEntity userEvent;

    /** 当前短期记忆快照。 */
    private ShortTermMemorySnapshot shortTermMemory;

    /** 本轮长期记忆召回结果。 */
    private List<LongTermMemoryView> longTermMemories = new ArrayList<>();

    /** 是否已经执行本轮长期记忆召回。 */
    private boolean longTermMemoryLoaded;

    /** 是否已经尝试上下文预算压缩。 */
    private boolean contextBudgetCompressionAttempted;

    /**
     * 创建单次 Agent 执行上下文。
     *
     * @param traceId 请求链路标识
     * @param sessionId 会话唯一标识
     * @param customerId 客户唯一标识
     * @param currentMessage 当前用户消息
     * @param startNanos 请求开始时间
     * @param request 原始聊天请求
     * @param nativeMessages 标准消息历史
     */
    public AgentTurnContext(
            String traceId,
            String sessionId,
            String customerId,
            String currentMessage,
            long startNanos,
            AgentChatRequest request,
            List<LlmMessage> nativeMessages
    ) {
        this.traceId = traceId;
        this.sessionId = sessionId;
        this.customerId = customerId;
        this.currentMessage = currentMessage;
        this.startNanos = startNanos;
        this.request = request;
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

    /** @return 请求开始时间纳秒值。 */
    public long getStartNanos() { return startNanos; }

    /** @return 原始聊天请求。 */
    public AgentChatRequest getRequest() { return request; }

    /** @return 标准消息历史。 */
    public List<LlmMessage> getNativeMessages() { return nativeMessages; }

    /** @return 工具观察结果。 */
    public List<ToolObservation> getObservations() { return observations; }

    /** @return 当前用户事件主键。 */
    public Long getSourceEventId() { return sourceEventId; }

    /** @param sourceEventId 当前用户事件主键。 */
    public void setSourceEventId(Long sourceEventId) { this.sourceEventId = sourceEventId; }

    /** @return 当前用户消息事件。 */
    public EventLayerEntity getUserEvent() { return userEvent; }

    /** @param userEvent 当前用户消息事件。 */
    public void setUserEvent(EventLayerEntity userEvent) { this.userEvent = userEvent; }

    /** @return 短期记忆快照。 */
    public ShortTermMemorySnapshot getShortTermMemory() { return shortTermMemory; }

    /** @param shortTermMemory 短期记忆快照。 */
    public void setShortTermMemory(ShortTermMemorySnapshot shortTermMemory) {
        this.shortTermMemory = shortTermMemory;
    }

    /** @return 本轮长期记忆召回结果。 */
    public List<LongTermMemoryView> getLongTermMemories() { return longTermMemories; }

    /** @param longTermMemories 本轮长期记忆召回结果。 */
    public void setLongTermMemories(List<LongTermMemoryView> longTermMemories) {
        this.longTermMemories = longTermMemories == null ? new ArrayList<>() : longTermMemories;
    }

    /** @return 是否已经执行长期记忆召回。 */
    public boolean isLongTermMemoryLoaded() { return longTermMemoryLoaded; }

    /** @param longTermMemoryLoaded 是否已经执行长期记忆召回。 */
    public void setLongTermMemoryLoaded(boolean longTermMemoryLoaded) { this.longTermMemoryLoaded = longTermMemoryLoaded; }

    /** @return 是否已尝试预算压缩。 */
    public boolean isContextBudgetCompressionAttempted() {
        return contextBudgetCompressionAttempted;
    }

    /** @param contextBudgetCompressionAttempted 是否已尝试预算压缩。 */
    public void setContextBudgetCompressionAttempted(boolean contextBudgetCompressionAttempted) {
        this.contextBudgetCompressionAttempted = contextBudgetCompressionAttempted;
    }
}

