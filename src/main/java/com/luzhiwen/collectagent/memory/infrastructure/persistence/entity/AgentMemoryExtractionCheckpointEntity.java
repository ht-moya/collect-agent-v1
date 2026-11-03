package com.luzhiwen.collectagent.memory.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 长期记忆抽取位点实体。
 */
@Entity
@Table(name = "agent_memory_extraction_checkpoint")
public class AgentMemoryExtractionCheckpointEntity {

    /** 会话ID。 */
    @Id
    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    /** 租户ID。 */
    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    /** 用户ID。 */
    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    /** 已抽取到的会话内序号。 */
    @Column(name = "extracted_seq", nullable = false)
    private long extractedSeq;

    /** 最后一次抽取时间。 */
    @Column(name = "last_extraction_at")
    private LocalDateTime lastExtractionAt;

    /** 创建时间。 */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** 更新时间。 */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** 创建空实体，供JPA使用。 */
    public AgentMemoryExtractionCheckpointEntity() {
    }

    /**
     * 创建抽取位点。
     *
     * @param sessionId 会话ID
     * @param tenantId 租户ID
     * @param userId 用户ID
     */
    public AgentMemoryExtractionCheckpointEntity(String sessionId, String tenantId, String userId) {
        this.sessionId = sessionId;
        this.tenantId = tenantId;
        this.userId = userId;
        this.extractedSeq = 0L;
    }

    /** 持久化前初始化时间。 */
    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** 更新前刷新时间。 */
    @PreUpdate
    public void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    /** @return 会话ID。 */ public String getSessionId() { return sessionId; }
    /** @return 租户ID。 */ public String getTenantId() { return tenantId; }
    /** @return 用户ID。 */ public String getUserId() { return userId; }
    /** @return 已抽取序号。 */ public long getExtractedSeq() { return extractedSeq; }
    /** @param extractedSeq 已抽取序号。 */ public void setExtractedSeq(long extractedSeq) { this.extractedSeq = extractedSeq; }
    /** @return 最后抽取时间。 */ public LocalDateTime getLastExtractionAt() { return lastExtractionAt; }
    /** @param lastExtractionAt 最后抽取时间。 */ public void setLastExtractionAt(LocalDateTime lastExtractionAt) { this.lastExtractionAt = lastExtractionAt; }
    /** @return 创建时间。 */ public LocalDateTime getCreatedAt() { return createdAt; }
    /** @return 更新时间。 */ public LocalDateTime getUpdatedAt() { return updatedAt; }
}
