package com.luzhiwen.collectagent.agentruntime.domain;

/**
 * 一次 Tool 执行后的观察结果，作为 ReAct 下一步规划的输入。
 */
public class ToolObservation {

    /**
     * 对应的工具调用唯一标识。
     */
    private final String toolCallId;

    /**
     * 被调用的工具名称。
     */
    private final String toolName;

    /**
     * 工具是否执行成功。
     */
    private final boolean success;

    /**
     * 工具返回给 Agent 的文本结果。
     */
    private final String content;

    /**
     * 工具执行耗时，单位为毫秒。
     */
    private final long latencyMs;

    /**
     * 创建一条工具观察结果。
     *
     * @param toolName 工具名称
     * @param success 是否成功
     * @param content 工具返回文本
     * @param latencyMs 执行耗时毫秒数
     */
    public ToolObservation(String toolName, boolean success, String content, long latencyMs) {
        this(null, toolName, success, content, latencyMs);
    }

    /**
     * 创建带工具调用标识的观察结果。
     *
     * @param toolCallId 工具调用唯一标识
     * @param toolName 工具名称
     * @param success 是否成功
     * @param content 工具返回文本
     * @param latencyMs 执行耗时毫秒数
     */
    public ToolObservation(
            String toolCallId,
            String toolName,
            boolean success,
            String content,
            long latencyMs
    ) {
        this.toolCallId = toolCallId;
        this.toolName = toolName;
        this.success = success;
        this.content = content;
        this.latencyMs = latencyMs;
    }

    /** @return 工具调用唯一标识。 */
    public String getToolCallId() {
        return toolCallId;
    }

    /**
     * 获取工具名称。
     *
     * @return 工具名称
     */
    public String getToolName() {
        return toolName;
    }

    /**
     * 判断工具是否执行成功。
     *
     * @return true 表示成功
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * 获取工具返回文本。
     *
     * @return 工具返回文本
     */
    public String getContent() {
        return content;
    }

    /**
     * 获取工具执行耗时。
     *
     * @return 执行耗时毫秒数
     */
    public long getLatencyMs() {
        return latencyMs;
    }
}

