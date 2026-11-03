package com.luzhiwen.collectagent.memory.domain;

import java.time.LocalDateTime;

/**
 * 短期记忆中的轻量事件。
 */
public class ShortTermMemoryEvent {

    /** 事件唯一标识。 */
    private String eventId;

    /** 会话内事件序号。 */
    private Long sessionSeq;

    /** 事件类型。 */
    private String eventType;

    /** 事件内容摘要。 */
    private String content;

    /** 事件发生时间。 */
    private LocalDateTime occurredAt;

    /** 创建空事件，供缓存反序列化使用。 */
    public ShortTermMemoryEvent() {
    }

    /**
     * 创建短期记忆事件。
     *
     * @param eventId 事件唯一标识
     * @param sessionSeq 会话内事件序号
     * @param eventType 事件类型
     * @param content 事件内容摘要
     * @param occurredAt 事件发生时间
     */
    public ShortTermMemoryEvent(
            String eventId,
            Long sessionSeq,
            String eventType,
            String content,
            LocalDateTime occurredAt
    ) {
        this.eventId = eventId;
        this.sessionSeq = sessionSeq;
        this.eventType = eventType;
        this.content = content;
        this.occurredAt = occurredAt;
    }

    /** @return 事件唯一标识。 */
    public String getEventId() { return eventId; }

    /** @param eventId 事件唯一标识。 */
    public void setEventId(String eventId) { this.eventId = eventId; }

    /** @return 会话内事件序号。 */
    public Long getSessionSeq() { return sessionSeq; }

    /** @param sessionSeq 会话内事件序号。 */
    public void setSessionSeq(Long sessionSeq) { this.sessionSeq = sessionSeq; }

    /** @return 事件类型。 */
    public String getEventType() { return eventType; }

    /** @param eventType 事件类型。 */
    public void setEventType(String eventType) { this.eventType = eventType; }

    /** @return 事件内容摘要。 */
    public String getContent() { return content; }

    /** @param content 事件内容摘要。 */
    public void setContent(String content) { this.content = content; }

    /** @return 事件发生时间。 */
    public LocalDateTime getOccurredAt() { return occurredAt; }

    /** @param occurredAt 事件发生时间。 */
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
}
