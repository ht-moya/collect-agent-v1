package com.luzhiwen.collectagent.eventing.infrastructure.persistence.entity;

import com.luzhiwen.collectagent.eventing.domain.OutboxStatus;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 数据库 Outbox 事件实体。
 */
@Entity
@Table(name = "outbox_event")
public class OutboxEventEntity {

    /** 数据库主键。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 原始事件唯一标识。 */
    @Column(name = "event_id", nullable = false, unique = true, length = 64)
    private String eventId;

    /** 聚合类型。 */
    @Column(name = "aggregate_type", nullable = false, length = 64)
    private String aggregateType;

    /** 聚合标识。 */
    @Column(name = "aggregate_id", nullable = false, length = 64)
    private String aggregateId;

    /** 事件类型。 */
    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    /** 事件载荷 JSON。 */
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "payload_json", nullable = false)
    private String payloadJson;

    /** 处理状态。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OutboxStatus status;

    /** 已重试次数。 */
    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    /** 下一次重试时间。 */
    @Column(name = "next_retry_at", nullable = false)
    private LocalDateTime nextRetryAt;

    /** 创建时间。 */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** 处理完成时间。 */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /** 最近一次错误信息。 */
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "last_error")
    private String lastError;

    /**
     * 创建 Outbox 实体，JPA 使用无参构造器。
     */
    public OutboxEventEntity() {
    }

    /**
     * 创建 Outbox 实体。
     *
     * @param eventId 原始事件唯一标识
     * @param aggregateType 聚合类型
     * @param aggregateId 聚合标识
     * @param eventType 事件类型
     * @param payloadJson 事件载荷
     */
    public OutboxEventEntity(
            String eventId,
            String aggregateType,
            String aggregateId,
            String eventType,
            String payloadJson
    ) {
        this.eventId = eventId;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payloadJson = payloadJson;
        this.status = OutboxStatus.PENDING;
        this.retryCount = 0;
        this.nextRetryAt = LocalDateTime.now();
    }

    /** 持久化前设置创建时间。 */
    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /** @return 数据库主键。 */
    public Long getId() { return id; }

    /** @return 原始事件唯一标识。 */
    public String getEventId() { return eventId; }

    /** @return 聚合类型。 */
    public String getAggregateType() { return aggregateType; }

    /** @return 聚合标识。 */
    public String getAggregateId() { return aggregateId; }

    /** @return 事件类型。 */
    public String getEventType() { return eventType; }

    /** @return 事件载荷 JSON。 */
    public String getPayloadJson() { return payloadJson; }

    /** @return 处理状态。 */
    public OutboxStatus getStatus() { return status; }

    /** @param status 处理状态。 */
    public void setStatus(OutboxStatus status) { this.status = status; }

    /** @return 已重试次数。 */
    public int getRetryCount() { return retryCount; }

    /** @param retryCount 已重试次数。 */
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    /** @return 下一次重试时间。 */
    public LocalDateTime getNextRetryAt() { return nextRetryAt; }

    /** @param nextRetryAt 下一次重试时间。 */
    public void setNextRetryAt(LocalDateTime nextRetryAt) { this.nextRetryAt = nextRetryAt; }

    /** @return 创建时间。 */
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** @return 处理完成时间。 */
    public LocalDateTime getProcessedAt() { return processedAt; }

    /** @param processedAt 处理完成时间。 */
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }

    /** @return 最近一次错误信息。 */
    public String getLastError() { return lastError; }

    /** @param lastError 最近一次错误信息。 */
    public void setLastError(String lastError) { this.lastError = lastError; }
}

