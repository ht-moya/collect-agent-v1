package com.luzhiwen.collectagent.model.application;

/**
 * DeepSeek 返回的单个工具调用。
 */
public class LlmToolCall {

    /** 工具调用唯一标识。 */
    private final String toolCallId;

    /** 工具名称。 */
    private final String toolName;

    /** 工具参数 JSON。 */
    private final String argumentsJson;

    /**
     * 创建工具调用对象。
     *
     * @param toolCallId 工具调用唯一标识
     * @param toolName 工具名称
     * @param argumentsJson 工具参数 JSON
     */
    public LlmToolCall(String toolCallId, String toolName, String argumentsJson) {
        this.toolCallId = toolCallId;
        this.toolName = toolName;
        this.argumentsJson = argumentsJson;
    }

    /** @return 工具调用唯一标识。 */
    public String getToolCallId() { return toolCallId; }

    /** @return 工具名称。 */
    public String getToolName() { return toolName; }

    /** @return 工具参数 JSON。 */
    public String getArgumentsJson() { return argumentsJson; }
}
