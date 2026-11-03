package com.luzhiwen.collectagent.model.application;

import java.util.ArrayList;
import java.util.List;

/**
 * DeepSeek 单次聊天结果。
 */
public class LlmChatResult {

    /** 模型原始响应 JSON。 */
    private final String rawResponse;

    /** 普通文本回复。 */
    private final String content;

    /** 本次响应中的全部工具调用。 */
    private final List<LlmToolCall> toolCalls;

    /** 第一个工具调用名称，没有工具调用时为空。 */
    private final String toolName;

    /** 第一个工具调用参数 JSON。 */
    private final String argumentsJson;

    /** 第一个工具调用唯一标识。 */
    private final String toolCallId;

    /**
     * 创建包含多个工具调用的聊天结果。
     *
     * @param rawResponse 原始响应
     * @param content 文本回复
     * @param toolCalls 工具调用列表
     */
    public LlmChatResult(
            String rawResponse,
            String content,
            List<LlmToolCall> toolCalls
    ) {
        this.rawResponse = rawResponse;
        this.content = content;
        this.toolCalls = List.copyOf(new ArrayList<>(toolCalls));
        LlmToolCall first = this.toolCalls.isEmpty() ? null : this.toolCalls.get(0);
        this.toolName = first == null ? null : first.getToolName();
        this.argumentsJson = first == null ? null : first.getArgumentsJson();
        this.toolCallId = first == null ? null : first.getToolCallId();
    }

    /**
     * 创建只包含一个工具调用的聊天结果。
     *
     * @param rawResponse 原始响应
     * @param content 文本回复
     * @param toolName 工具名称
     * @param argumentsJson 工具参数 JSON
     * @param toolCallId 工具调用标识
     */
    public LlmChatResult(
            String rawResponse,
            String content,
            String toolName,
            String argumentsJson,
            String toolCallId
    ) {
        this.rawResponse = rawResponse;
        this.content = content;
        this.toolCalls = toolName == null || toolName.isBlank()
                ? List.of()
                : List.of(new LlmToolCall(toolCallId, toolName, argumentsJson));
        this.toolName = toolName;
        this.argumentsJson = argumentsJson;
        this.toolCallId = toolCallId;
    }

    /** @return 原始响应 JSON。 */
    public String getRawResponse() { return rawResponse; }

    /** @return 普通文本回复。 */
    public String getContent() { return content; }

    /** @return 全部工具调用。 */
    public List<LlmToolCall> getToolCalls() { return toolCalls; }

    /** @return 第一个工具名称。 */
    public String getToolName() { return toolName; }

    /** @return 第一个工具参数 JSON。 */
    public String getArgumentsJson() { return argumentsJson; }

    /** @return 第一个工具调用标识。 */
    public String getToolCallId() { return toolCallId; }

    /** @return 是否存在工具调用。 */
    public boolean hasToolCall() { return !toolCalls.isEmpty(); }
}
