package com.luzhiwen.collectagent.conversation.api.dto;

/**
 * 动态变量定义保存请求。
 */
public class DynamicVariableDefinitionRequest {

    /** 变量键，例如 address。 */
    private String variableKey;

    /** 变量展示名称。 */
    private String displayName;

    /** 变量用途说明。 */
    private String description;

    /** 给模型使用的抽取提示。 */
    private String extractionHint;

    /** 是否启用。 */
    private boolean enabled = true;

    /** 展示排序值。 */
    private int sortOrder;

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
}
