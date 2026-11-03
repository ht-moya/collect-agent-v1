package com.luzhiwen.collectagent.memory.infrastructure.persistence.entity;

import com.luzhiwen.collectagent.memory.domain.AgentSummaryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * Agent 会话摘要表。
 *
 * <p>摘要只覆盖一段连续会话序号，不修改 agent_session_state，也不参与状态表乐观锁。</p>
 */
@Entity
@Table(
        name = "agent_summary",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_agent_summary_session_seq",
                columnNames = {"tenant_id", "session_id", "covers_seq_to"}
        )
)
public class AgentSummaryEntity {

    /** 数据库主键。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 租户唯一标识。 */
    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    /** 会话唯一标识。 */
    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    /** 摘要内容。 */
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "summary", nullable = false)
    private String summary;

    /** 摘要覆盖的起始会话序号。 */
    @Column(name = "covers_seq_from", nullable = false)
    private long coversSeqFrom;

    /** 摘要覆盖的结束会话序号。 */
    @Column(name = "covers_seq_to", nullable = false)
    private long coversSeqTo;

    /** 生成摘要使用的模型。 */
    @Column(name = "model", length = 64)
    private String model;

    /** 摘要提示词版本。 */
    @Column(name = "prompt_version", length = 32)
    private String promptVersion;

    /** 摘要估算 Token 数。 */
    @Column(name = "token_count")
    private Integer tokenCount;

    /** 摘要状态。 */
    @Column(name = "status", nullable = false, length = 16)
    private String status;

    /** 创建时间。 */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** 创建空实体，供 JPA 使用。 */
    public AgentSummaryEntity() {
    }

    /**
     * 创建摘要实体。
     *
     * @param tenantId 租户唯一标识
     * @param sessionId 会话唯一标识
     * @param summary 摘要内容
     * @param coversSeqFrom 起始序号
     * @param coversSeqTo 结束序号
     * @param model 模型名称
     * @param promptVersion 提示词版本
     * @param tokenCount 估算 Token 数
     */
    public AgentSummaryEntity(
            String tenantId,
            String sessionId,
            String summary,
            long coversSeqFrom,
            long coversSeqTo,
            String model,
            String promptVersion,
            Integer tokenCount
    ) {
        this.tenantId = tenantId;
        this.sessionId = sessionId;
        this.summary = summary;
        this.coversSeqFrom = coversSeqFrom;
        this.coversSeqTo = coversSeqTo;
        this.model = model;
        this.promptVersion = promptVersion;
        this.tokenCount = tokenCount;
        this.status = AgentSummaryStatus.ACTIVE;
    }

    /** 持久化前设置创建时间。 */
    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /** @return 数据库主键。 */
    public Long getId() { return id; }

    /** @return 租户唯一标识。 */
    public String getTenantId() { return tenantId; }

    /** @return 会话唯一标识。 */
    public String getSessionId() { return sessionId; }

    /** @return 摘要内容。 */
    public String getSummary() { return summary; }

    /** @return 摘要起始序号。 */
    public long getCoversSeqFrom() { return coversSeqFrom; }

    /** @return 摘要结束序号。 */
    public long getCoversSeqTo() { return coversSeqTo; }

    /** @return 模型名称。 */
    public String getModel() { return model; }

    /** @return 提示词版本。 */
    public String getPromptVersion() { return promptVersion; }

    /** @return 摘要估算 Token 数。 */
    public Integer getTokenCount() { return tokenCount; }

    /** @return 摘要状态。 */
    public String getStatus() { return status; }

    /** @param status 摘要状态。 */
    public void setStatus(String status) { this.status = status; }

    /** @return 创建时间。 */
    public LocalDateTime getCreatedAt() { return createdAt; }
}


