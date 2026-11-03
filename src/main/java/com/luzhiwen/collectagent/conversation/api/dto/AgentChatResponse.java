package com.luzhiwen.collectagent.conversation.api.dto;

import java.util.List;

/**
 * Agent 聊天响应。
 */
public class AgentChatResponse {

    /** 会话唯一标识。 */
    private final String sessionId;

    /** 请求链路标识。 */
    private final String traceId;

    /** 最终回复。 */
    private final String answer;

    /** 实际使用的规划器。 */
    private final String planner;

    /** 工具调用列表。 */
    private final List<ToolCallView> toolCalls;

    /** 请求总耗时。 */
    private final long latencyMs;

    /**
     * 创建聊天响应。
     *
     * @param sessionId 会话唯一标识
     * @param traceId 请求链路标识
     * @param answer 最终回复
     * @param planner 规划器
     * @param toolCalls 工具调用列表
     * @param latencyMs 请求总耗时
     */
    public AgentChatResponse(
            String sessionId,
            String traceId,
            String answer,
            String planner,
            List<ToolCallView> toolCalls,
            long latencyMs
    ) {
        this.sessionId = sessionId;
        this.traceId = traceId;
        this.answer = answer;
        this.planner = planner;
        this.toolCalls = List.copyOf(toolCalls);
        this.latencyMs = latencyMs;
    }

    /** @return 会话唯一标识。 */
    public String getSessionId() { return sessionId; }

    /** @return 请求链路标识。 */
    public String getTraceId() { return traceId; }

    /** @return 最终回复。 */
    public String getAnswer() { return answer; }

    /** @return 规划器。 */
    public String getPlanner() { return planner; }

    /** @return 工具调用列表。 */
    public List<ToolCallView> getToolCalls() { return toolCalls; }

    /** @return 请求总耗时。 */
    public long getLatencyMs() { return latencyMs; }
}
