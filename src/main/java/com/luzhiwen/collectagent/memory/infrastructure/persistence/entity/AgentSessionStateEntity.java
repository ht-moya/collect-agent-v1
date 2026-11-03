package com.luzhiwen.collectagent.memory.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

/**
 * Agent 会话状态表。
 *
 * <p>按 session_id 组织，保存阶段、槽位、待办和标记位。MySQL 使用 LONGTEXT 保存 JSON，
 * 后续切换 PostgreSQL 时可把 state_json 迁移成 JSONB。</p>
 */
@Entity
@Table(name = "agent_session_state")
public class AgentSessionStateEntity {

    /** 会话唯一标识，同时作为数据库主键。 */
    @Id
    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    /** 租户唯一标识，用于多租户隔离。 */
    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    /** 用户或债务人唯一标识。 */
    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    /** 任务类型，当前固定为还款提醒。 */
    @Column(name = "task", nullable = false, length = 32)
    private String task;

    /** 会话阶段。 */
    @Column(name = "stage", nullable = false, length = 32)
    private String stage;

    /** 状态主体 JSON，包含 slots、pending 和 flags。 */
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "state_json", nullable = false)
    private String stateJson;

    /** 乐观锁版本号。 */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /** 状态已处理到的会话内最后序号。 */
    @Column(name = "last_seq", nullable = false)
    private long lastSeq;

    /** 状态 JSON 结构版本。 */
    @Column(name = "schema_version", nullable = false)
    private int schemaVersion;

    /** 会话过期时间，可为空。 */
    @Column(name = "expire_at")
    private LocalDateTime expireAt;

    /** 创建时间。 */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** 更新时间。 */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** 创建空实体，供 JPA 使用。 */
    public AgentSessionStateEntity() {
    }

    /**
     * 创建会话状态实体。
     *
     * @param sessionId 会话唯一标识
     * @param tenantId 租户唯一标识
     * @param userId 用户唯一标识
     * @param task 任务类型
     * @param stage 初始阶段
     * @param stateJson 初始状态 JSON
     */
    public AgentSessionStateEntity(
            String sessionId,
            String tenantId,
            String userId,
            String task,
            String stage,
            String stateJson
    ) {
        this.sessionId = sessionId;
        this.tenantId = tenantId;
        this.userId = userId;
        this.task = task;
        this.stage = stage;
        this.stateJson = stateJson;
        this.lastSeq = 0L;
        this.schemaVersion = 1;
    }

    /** 持久化前初始化时间。 */
    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** 更新前刷新更新时间。 */
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /** @return 会话唯一标识。 */
    public String getSessionId() { return sessionId; }

    /** @return 租户唯一标识。 */
    public String getTenantId() { return tenantId; }

    /** @return 用户唯一标识。 */
    public String getUserId() { return userId; }

    /** @return 任务类型。 */
    public String getTask() { return task; }

    /** @return 会话阶段。 */
    public String getStage() { return stage; }

    /** @param stage 会话阶段。 */
    public void setStage(String stage) { this.stage = stage; }

    /** @return 状态主体 JSON。 */
    public String getStateJson() { return stateJson; }

    /** @param stateJson 状态主体 JSON。 */
    public void setStateJson(String stateJson) { this.stateJson = stateJson; }

    /** @return 乐观锁版本号。 */
    public Long getVersion() { return version; }

    /** @return 已处理到的会话内最后序号。 */
    public long getLastSeq() { return lastSeq; }

    /** @param lastSeq 已处理到的会话内最后序号。 */
    public void setLastSeq(long lastSeq) { this.lastSeq = lastSeq; }

    /** @return 状态结构版本。 */
    public int getSchemaVersion() { return schemaVersion; }

    /** @return 会话过期时间。 */
    public LocalDateTime getExpireAt() { return expireAt; }

    /** @return 创建时间。 */
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** @return 更新时间。 */
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}



