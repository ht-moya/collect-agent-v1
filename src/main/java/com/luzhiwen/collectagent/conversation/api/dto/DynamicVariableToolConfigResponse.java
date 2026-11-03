package com.luzhiwen.collectagent.conversation.api.dto;

import java.time.LocalDateTime;

/**
 * 动态变量抽取工具配置响应。
 */
public class DynamicVariableToolConfigResponse {

    /** 数据库主键。 */
    private Long id;

    /** 工具名称。 */
    private String toolName;

    /** 工具基础描述。 */
    private String description;

    /** 是否强制调用。 */
    private boolean forceCall;

    /** 是否启用。 */
    private boolean enabled;

    /** 更新时间。 */
    private LocalDateTime updatedAt;

    /** @return 数据库主键。 */
    public Long getId() { return id; }

    /** @param id 数据库主键。 */
    public void setId(Long id) { this.id = id; }

    /** @return 工具名称。 */
    public String getToolName() { return toolName; }

    /** @param toolName 工具名称。 */
    public void setToolName(String toolName) { this.toolName = toolName; }

    /** @return 工具描述。 */
    public String getDescription() { return description; }

    /** @param description 工具描述。 */
    public void setDescription(String description) { this.description = description; }

    /** @return 是否强制调用。 */
    public boolean isForceCall() { return forceCall; }

    /** @param forceCall 是否强制调用。 */
    public void setForceCall(boolean forceCall) { this.forceCall = forceCall; }

    /** @return 是否启用。 */
    public boolean isEnabled() { return enabled; }

    /** @param enabled 是否启用。 */
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    /** @return 更新时间。 */
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    /** @param updatedAt 更新时间。 */
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
