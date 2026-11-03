package com.luzhiwen.collectagent.conversation.api.dto;

import java.time.LocalDateTime;

/**
 * 动态变量定义响应。
 */
public class DynamicVariableDefinitionResponse {

    /** 数据库主键。 */
    private Long id;

    /** 变量键。 */
    private String variableKey;

    /** 变量展示名称。 */
    private String displayName;

    /** 变量用途说明。 */
    private String description;

    /** 给模型使用的抽取提示。 */
    private String extractionHint;

    /** 是否启用。 */
    private boolean enabled;

    /** 展示排序值。 */
    private int sortOrder;

    /** 创建时间。 */
    private LocalDateTime createdAt;

    /** 更新时间。 */
    private LocalDateTime updatedAt;

    /** @return 数据库主键。 */
    public Long getId() { return id; }

    /** @param id 数据库主键。 */
    public void setId(Long id) { this.id = id; }

    /** @return 变量键。 */
    public String getVariableKey() { return variableKey; }

    /** @param variableKey 变量键。 */
    public void setVariableKey(String variableKey) { this.variableKey = variableKey; }

    /** @return 展示名称。 */
    public String getDisplayName() { return displayName; }

    /** @param displayName 展示名称。 */
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    /** @return 用途说明。 */
    public String getDescription() { return description; }

    /** @param description 用途说明。 */
    public void setDescription(String description) { this.description = description; }

    /** @return 抽取提示。 */
    public String getExtractionHint() { return extractionHint; }

    /** @param extractionHint 抽取提示。 */
    public void setExtractionHint(String extractionHint) { this.extractionHint = extractionHint; }

    /** @return 是否启用。 */
    public boolean isEnabled() { return enabled; }

    /** @param enabled 是否启用。 */
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    /** @return 排序值。 */
    public int getSortOrder() { return sortOrder; }

    /** @param sortOrder 排序值。 */
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    /** @return 创建时间。 */
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** @param createdAt 创建时间。 */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** @return 更新时间。 */
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    /** @param updatedAt 更新时间。 */
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
