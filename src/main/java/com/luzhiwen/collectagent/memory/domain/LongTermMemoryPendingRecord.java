package com.luzhiwen.collectagent.memory.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 待人工确认的长期记忆记录。
 */
public class LongTermMemoryPendingRecord {

    /** 主键。 */ private Long id;
    /** 租户ID。 */ private String tenantId;
    /** 用户ID。 */ private String userId;
    /** 作用域。 */ private String scope;
    /** 记忆类型。 */ private LongTermMemoryType memoryType;
    /** 内容。 */ private String content;
    /** 内容哈希。 */ private String contentHash;
    /** 结构化字段。 */ private Map<String, Object> structured;
    /** 来源事件ID。 */ private List<String> sourceEventIds;
    /** 置信度。 */ private double confidence;
    /** 状态。 */ private String status;
    /** 审核时间。 */ private LocalDateTime reviewedAt;
    /** 审核人。 */ private String reviewer;
    /** 审核说明。 */ private String reviewNote;
    /** 创建时间。 */ private LocalDateTime createdAt;

    /** @return 主键。 */ public Long getId() { return id; }
    /** @param id 主键。 */ public void setId(Long id) { this.id = id; }
    /** @return 租户ID。 */ public String getTenantId() { return tenantId; }
    /** @param tenantId 租户ID。 */ public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    /** @return 用户ID。 */ public String getUserId() { return userId; }
    /** @param userId 用户ID。 */ public void setUserId(String userId) { this.userId = userId; }
    /** @return 作用域。 */ public String getScope() { return scope; }
    /** @param scope 作用域。 */ public void setScope(String scope) { this.scope = scope; }
    /** @return 记忆类型。 */ public LongTermMemoryType getMemoryType() { return memoryType; }
    /** @param memoryType 记忆类型。 */ public void setMemoryType(LongTermMemoryType memoryType) { this.memoryType = memoryType; }
    /** @return 内容。 */ public String getContent() { return content; }
    /** @param content 内容。 */ public void setContent(String content) { this.content = content; }
    /** @return 内容哈希。 */ public String getContentHash() { return contentHash; }
    /** @param contentHash 内容哈希。 */ public void setContentHash(String contentHash) { this.contentHash = contentHash; }
    /** @return 结构化字段。 */ public Map<String, Object> getStructured() { return structured; }
    /** @param structured 结构化字段。 */ public void setStructured(Map<String, Object> structured) { this.structured = structured; }
    /** @return 来源事件ID。 */ public List<String> getSourceEventIds() { return sourceEventIds; }
    /** @param sourceEventIds 来源事件ID。 */ public void setSourceEventIds(List<String> sourceEventIds) { this.sourceEventIds = sourceEventIds; }
    /** @return 置信度。 */ public double getConfidence() { return confidence; }
    /** @param confidence 置信度。 */ public void setConfidence(double confidence) { this.confidence = confidence; }
    /** @return 状态。 */ public String getStatus() { return status; }
    /** @param status 状态。 */ public void setStatus(String status) { this.status = status; }
    /** @return 审核时间。 */ public LocalDateTime getReviewedAt() { return reviewedAt; }
    /** @param reviewedAt 审核时间。 */ public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    /** @return 审核人。 */ public String getReviewer() { return reviewer; }
    /** @param reviewer 审核人。 */ public void setReviewer(String reviewer) { this.reviewer = reviewer; }
    /** @return 审核说明。 */ public String getReviewNote() { return reviewNote; }
    /** @param reviewNote 审核说明。 */ public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }
    /** @return 创建时间。 */ public LocalDateTime getCreatedAt() { return createdAt; }
    /** @param createdAt 创建时间。 */ public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
