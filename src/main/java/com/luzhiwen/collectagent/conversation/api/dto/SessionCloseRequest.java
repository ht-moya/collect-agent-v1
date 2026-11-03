package com.luzhiwen.collectagent.conversation.api.dto;

/**
 * 结束会话请求。
 */
public class SessionCloseRequest {

    /** 关闭原因，可选。 */
    private String reason;

    /** @return 关闭原因。 */ public String getReason() { return reason; }
    /** @param reason 关闭原因。 */ public void setReason(String reason) { this.reason = reason; }
}
