package com.luzhiwen.collectagent.conversation.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Agent 聊天请求。
 */
public class AgentChatRequest {

    /** 客户唯一标识。 */
    @NotBlank(message = "不能为空")
    private String customerId;

    /** 会话唯一标识，可为空。 */
    private String sessionId;

    /** 当前用户消息。 */
    @NotBlank(message = "不能为空")
    private String message;

    /** 创建空请求，供 JSON 反序列化使用。 */
    public AgentChatRequest() {
    }

    /** @return 客户唯一标识。 */
    public String getCustomerId() { return customerId; }

    /** @param customerId 客户唯一标识。 */
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    /** @return 会话唯一标识。 */
    public String getSessionId() { return sessionId; }

    /** @param sessionId 会话唯一标识。 */
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    /** @return 当前用户消息。 */
    public String getMessage() { return message; }

    /** @param message 当前用户消息。 */
    public void setMessage(String message) { this.message = message; }
}
