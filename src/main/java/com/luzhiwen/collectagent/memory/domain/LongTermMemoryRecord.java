package com.luzhiwen.collectagent.memory.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 长期记忆持久化记录。
 */
public class LongTermMemoryRecord {

    /** 记忆ID。 */
    private String memoryId;
    /** 租户ID。 */
    private String tenantId;
    /** 用户ID。 */
    private String userId;
    /** 作用域。 */
    private String scope;
    /** 记忆类型。 */
    private LongTermMemoryType memoryType;
    /** 内容。 */
    private String content;
    /** 内容哈希。 */
    private String contentHash;
    /** 结构化字段。 */
    private Map<String, Object> structured;
    /** 来源事件ID。 */
    private List<String> sourceEventIds;
    /** 置信度。 */
    private double confidence;
    /** 状态。 */
    private String status;
    /** 生效时间。 */
    private LocalDateTime validFrom;
    /** 失效时间。 */
    private LocalDateTime validTo;
    /** 最后召回时间。 */
    private LocalDateTime lastAccessedAt;
    /** 召回次数。 */
    private int accessCount;
    /** 向量。 */
    private List<Float> embedding;
    /** 向量模型。 */
    private String embeddingModel;
    /** 创建时间。 */
    private LocalDateTime createdAt;
    /** 更新时间。 */
    private LocalDateTime updatedAt;
    /** 当前检索得分。 */
    private double score;

    /** @return 记忆ID。 */ public String getMemoryId() { return memoryId; }
    /** @param memoryId 记忆ID。 */ public void setMemoryId(String memoryId) { this.memoryId = memoryId; }
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
    /** @return 生效时间。 */ public LocalDateTime getValidFrom() { return validFrom; }
    /** @param validFrom 生效时间。 */ public void setValidFrom(LocalDateTime validFrom) { this.validFrom = validFrom; }
    /** @return 失效时间。 */ public LocalDateTime getValidTo() { return validTo; }
    /** @param validTo 失效时间。 */ public void setValidTo(LocalDateTime validTo) { this.validTo = validTo; }
    /** @return 最后召回时间。 */ public LocalDateTime getLastAccessedAt() { return lastAccessedAt; }
    /** @param lastAccessedAt 最后召回时间。 */ public void setLastAccessedAt(LocalDateTime lastAccessedAt) { this.lastAccessedAt = lastAccessedAt; }
    /** @return 召回次数。 */ public int getAccessCount() { return accessCount; }
    /** @param accessCount 召回次数。 */ public void setAccessCount(int accessCount) { this.accessCount = accessCount; }
    /** @return 向量。 */ public List<Float> getEmbedding() { return embedding; }
    /** @param embedding 向量。 */ public void setEmbedding(List<Float> embedding) { this.embedding = embedding; }
    /** @return 向量模型。 */ public String getEmbeddingModel() { return embeddingModel; }
    /** @param embeddingModel 向量模型。 */ public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }
    /** @return 创建时间。 */ public LocalDateTime getCreatedAt() { return createdAt; }
    /** @param createdAt 创建时间。 */ public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    /** @return 更新时间。 */ public LocalDateTime getUpdatedAt() { return updatedAt; }
    /** @param updatedAt 更新时间。 */ public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    /** @return 检索得分。 */ public double getScore() { return score; }
    /** @param score 检索得分。 */ public void setScore(double score) { this.score = score; }
}
