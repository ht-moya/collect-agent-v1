package com.luzhiwen.collectagent.conversation.api.dto;

/**
 * 动态变量抽取工具配置请求。
 */
public class DynamicVariableToolConfigRequest {

    /** 工具基础描述。 */
    private String description;

    /** 是否强制模型调用工具。 */
    private boolean forceCall = true;

    /** 是否启用动态变量抽取。 */
    private boolean enabled = true;

    /** @return 工具基础描述。 */
    public String getDescription() { return description; }

    /** @param description 工具基础描述。 */
    public void setDescription(String description) { this.description = description; }

    /** @return 是否强制调用。 */
    public boolean isForceCall() { return forceCall; }

    /** @param forceCall 是否强制调用。 */
    public void setForceCall(boolean forceCall) { this.forceCall = forceCall; }

    /** @return 是否启用。 */
    public boolean isEnabled() { return enabled; }

    /** @param enabled 是否启用。 */
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
