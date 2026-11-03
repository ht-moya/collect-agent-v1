package com.luzhiwen.collectagent.conversation.api.dto;

import java.time.LocalDateTime;

/**
 * 会话动态变量值响应。
 */
public class DynamicVariableValueResponse {

    /** 会话唯一标识。 */
    private String sessionId;

    /** 客户唯一标识。 */
    private String customerId;

    /** 变量键。 */
    private String variableKey;

    /** 变量值。 */
    private String variableValue;

    /** 来源事件主键。 */
    private Long sourceEventId;

    /** 来源请求链路标识。 */
    private String traceId;

    /** 抽取置信度。 */
    private Double confidence;

    /** 更新时间。 */
    private LocalDateTime updatedAt;

    /** @return 会话唯一标识。 */
    public String getSessionId() { return sessionId; }

    /** @param sessionId 会话唯一标识。 */
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    /** @return 客户唯一标识。 */
    public String getCustomerId() { return customerId; }

    /** @param customerId 客户唯一标识。 */
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    /** @return 变量键。 */
    public String getVariableKey() { return variableKey; }

    /** @param variableKey 变量键。 */
    public void setVariableKey(String variableKey) { this.variableKey = variableKey; }

    /** @return 变量值。 */
    public String getVariableValue() { return variableValue; }

    /** @param variableValue 变量值。 */
    public void setVariableValue(String variableValue) { this.variableValue = variableValue; }

    /** @return 来源事件主键。 */
    public Long getSourceEventId() { return sourceEventId; }

    /** @param sourceEventId 来源事件主键。 */
    public void setSourceEventId(Long sourceEventId) { this.sourceEventId = sourceEventId; }

    /** @return 来源请求链路标识。 */
    public String getTraceId() { return traceId; }

    /** @param traceId 来源请求链路标识。 */
    public void setTraceId(String traceId) { this.traceId = traceId; }

    /** @return 抽取置信度。 */
    public Double getConfidence() { return confidence; }

    /** @param confidence 抽取置信度。 */
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    /** @return 更新时间。 */
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    /** @param updatedAt 更新时间。 */
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
