package com.luzhiwen.collectagent.conversation.api.dto;

import java.time.LocalDateTime;

/**
 * 结束会话响应。
 */
public class SessionCloseResponse {

    /** 会话ID。 */ private String sessionId;
    /** 会话状态。 */ private String status;
    /** 关闭时间。 */ private LocalDateTime closedAt;
    /** 关闭事件ID，已关闭时可能为空。 */ private String eventId;
    /** 关闭事件会话内序号。 */ private Long sessionSeq;

    /** @return 会话ID。 */ public String getSessionId() { return sessionId; }
    /** @param sessionId 会话ID。 */ public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    /** @return 状态。 */ public String getStatus() { return status; }
    /** @param status 状态。 */ public void setStatus(String status) { this.status = status; }
    /** @return 关闭时间。 */ public LocalDateTime getClosedAt() { return closedAt; }
    /** @param closedAt 关闭时间。 */ public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
    /** @return 事件ID。 */ public String getEventId() { return eventId; }
    /** @param eventId 事件ID。 */ public void setEventId(String eventId) { this.eventId = eventId; }
    /** @return 会话内序号。 */ public Long getSessionSeq() { return sessionSeq; }
    /** @param sessionSeq 会话内序号。 */ public void setSessionSeq(Long sessionSeq) { this.sessionSeq = sessionSeq; }
}
