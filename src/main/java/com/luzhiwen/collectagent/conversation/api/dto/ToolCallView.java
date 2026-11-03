package com.luzhiwen.collectagent.conversation.api.dto;

/**
 * 工具调用响应视图。
 */
public class ToolCallView {

    /** 工具名称。 */
    private final String toolName;

    /** 是否成功。 */
    private final boolean success;

    /** 工具返回文本。 */
    private final String result;

    /** 工具耗时。 */
    private final long latencyMs;

    /**
     * 创建工具调用视图。
     *
     * @param toolName 工具名称
     * @param success 是否成功
     * @param result 工具返回文本
     * @param latencyMs 工具耗时
     */
    public ToolCallView(String toolName, boolean success, String result, long latencyMs) {
        this.toolName = toolName;
        this.success = success;
        this.result = result;
        this.latencyMs = latencyMs;
    }

    /** @return 工具名称。 */
    public String getToolName() { return toolName; }

    /** @return 是否成功。 */
    public boolean isSuccess() { return success; }

    /** @return 工具返回文本。 */
    public String getResult() { return result; }

    /** @return 工具耗时。 */
    public long getLatencyMs() { return latencyMs; }
}
