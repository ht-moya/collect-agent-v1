package com.luzhiwen.collectagent.eventing.infrastructure.persistence.entity;

import com.luzhiwen.collectagent.eventing.domain.EventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 原始事件层实体。
 *
 * <p>event_id 用于唯一指认一条事件，session_seq 用于表示事件在单个会话内的处理顺序。</p>
 * <p>该表只允许追加，不允许更新或删除，是短期记忆、会话状态和摘要的事实源。</p>
 */
@Entity
@Immutable
@Table(
        name = "event_layer",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_event_layer_session_seq",
                columnNames = {"session_id", "session_seq"}
        )
)
public class EventLayerEntity {

    /** 数据库主键。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 对外事件唯一标识。 */
    @Column(name = "event_id", nullable = false, unique = true, length = 64)
    private String eventId;

    /** 请求链路标识。 */
    @Column(name = "trace_id", nullable = false, length = 64)
    private String traceId;

    /** 会话唯一标识。 */
    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    /** 客户唯一标识。 */
    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    /**
     * 会话内单调递增序号。
     *
     * <p>历史数据可能没有该字段，因此数据库列允许为空；新追加事件必须写入序号。</p>
     */
    @Column(name = "session_seq")
    private Long sessionSeq;

    /** 事件类型。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    private EventType eventType;

    /** 事件业务载荷 JSON。 */
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "payload_json", nullable = false)
    private String payloadJson;

    /** 业务发生时间。 */
    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    /** 数据库写入时间。 */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * 创建原始事件实体，JPA 使用无参构造器。
     */
    public EventLayerEntity() {
    }

    /**
     * 创建原始事件实体。
     *
     * @param eventId 事件唯一标识
     * @param traceId 请求链路标识
     * @param sessionId 会话唯一标识
     * @param customerId 客户唯一标识
     * @param sessionSeq 会话内事件序号
     * @param eventType 事件类型
     * @param payloadJson 事件载荷 JSON
     * @param occurredAt 业务发生时间
     */
    public EventLayerEntity(
            String eventId,
            String traceId,
            String sessionId,
            String customerId,
            Long sessionSeq,
            EventType eventType,
            String payloadJson,
            LocalDateTime occurredAt
    ) {
        this.eventId = eventId;
        this.traceId = traceId;
        this.sessionId = sessionId;
        this.customerId = customerId;
        this.sessionSeq = sessionSeq;
        this.eventType = eventType;
        this.payloadJson = payloadJson;
        this.occurredAt = occurredAt;
    }

    /**
     * 持久化前设置创建时间。
     */
    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /** @return 数据库主键。 */
    public Long getId() { return id; }

    /** @return 事件唯一标识。 */
    public String getEventId() { return eventId; }

    /** @return 请求链路标识。 */
    public String getTraceId() { return traceId; }

    /** @return 会话唯一标识。 */
    public String getSessionId() { return sessionId; }

    /** @return 客户唯一标识。 */
    public String getCustomerId() { return customerId; }

    /** @return 会话内事件序号。 */
    public Long getSessionSeq() { return sessionSeq; }

    /** @return 事件类型。 */
    public EventType getEventType() { return eventType; }

    /** @return 事件载荷 JSON。 */
    public String getPayloadJson() { return payloadJson; }

    /** @return 业务发生时间。 */
    public LocalDateTime getOccurredAt() { return occurredAt; }

    /** @return 数据库写入时间。 */
    public LocalDateTime getCreatedAt() { return createdAt; }
}

