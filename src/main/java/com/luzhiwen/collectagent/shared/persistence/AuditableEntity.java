package com.luzhiwen.collectagent.shared.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.LocalDateTime;

/**
 * 带创建时间和更新时间的 JPA 实体基类。
 */
@MappedSuperclass
public abstract class AuditableEntity {

    /** 创建时间。 */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** 更新时间。 */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** 持久化前初始化时间。 */
    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** 更新前刷新时间。 */
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /** @return 创建时间。 */
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** @return 更新时间。 */
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
