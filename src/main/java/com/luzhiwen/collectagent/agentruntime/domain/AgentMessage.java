package com.luzhiwen.collectagent.agentruntime.domain;

/**
 * Agent 上下文中的一条聊天消息。
 */
public class AgentMessage {

    /**
     * 消息角色，例如 system、user、assistant、tool。
     */
    private final String role;

    /**
     * 消息完整文本内容。
     */
    private final String content;

    /**
     * 创建一条 Agent 消息。
     *
     * @param role 消息角色
     * @param content 消息内容
     */
    public AgentMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    /**
     * 获取消息角色。
     *
     * @return 消息角色
     */
    public String getRole() {
        return role;
    }

    /**
     * 获取消息内容。
     *
     * @return 消息内容
     */
    public String getContent() {
        return content;
    }
}

