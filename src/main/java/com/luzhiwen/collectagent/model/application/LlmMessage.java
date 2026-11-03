package com.luzhiwen.collectagent.model.application;

import java.util.List;

/**
 * 发送给大模型的聊天消息。
 */
public class LlmMessage {

    /** 消息角色，例如 system、user、assistant、tool。 */
    private final String role;

    /** 消息文本，assistant 工具调用消息可以为空。 */
    private final String content;

    /** tool 消息对应的工具调用唯一标识。 */
    private final String toolCallId;

    /** assistant 消息携带的工具调用列表。 */
    private final List<LlmToolCall> toolCalls;

    /**
     * 创建普通聊天消息。
     *
     * @param role 消息角色
     * @param content 消息文本
     */
    public LlmMessage(String role, String content) {
        this(role, content, null, List.of());
    }

    /**
     * 创建携带工具调用信息的消息。
     *
     * @param role 消息角色
     * @param content 消息文本
     * @param toolCallId tool 消息对应的工具调用标识
     * @param toolCalls assistant 消息携带的工具调用列表
     */
    public LlmMessage(
            String role,
            String content,
            String toolCallId,
            List<LlmToolCall> toolCalls
    ) {
        this.role = role;
        this.content = content;
        this.toolCallId = toolCallId;
        this.toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
    }

    /** @return 消息角色。 */
    public String getRole() { return role; }

    /** @return 消息文本。 */
    public String getContent() { return content; }

    /** @return 工具调用唯一标识。 */
    public String getToolCallId() { return toolCallId; }

    /** @return assistant 工具调用列表。 */
    public List<LlmToolCall> getToolCalls() { return toolCalls; }
}
