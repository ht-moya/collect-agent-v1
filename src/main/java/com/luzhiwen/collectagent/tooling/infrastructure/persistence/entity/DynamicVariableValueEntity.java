package com.luzhiwen.collectagent.tooling.infrastructure.persistence.entity;

import com.luzhiwen.collectagent.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.UniqueConstraint;


/**
 * 会话动态变量值。
 */
@Entity
@Table(name = "dynamic_variable_value", uniqueConstraints = @UniqueConstraint(name = "uk_dynamic_value_session_key", columnNames = {"session_id", "variable_key"}))
public class DynamicVariableValueEntity extends AuditableEntity {

    /** 数据库主键。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 会话唯一标识。 */
    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    /** 客户唯一标识。 */
    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    /** 变量键。 */
    @Column(name = "variable_key", nullable = false, length = 64)
    private String variableKey;

    /** 变量值。 */
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "variable_value", nullable = false)
    private String variableValue;

    /** 来源原始事件主键。 */
    @Column(name = "source_event_id")
    private Long sourceEventId;

    /** 来源请求链路标识。 */
    @Column(name = "trace_id", length = 64)
    private String traceId;

    /** 抽取置信度。 */
    @Column(name = "confidence")
    private Double confidence;


    /** 创建动态变量值实体，JPA 使用无参构造器。 */
    public DynamicVariableValueEntity() {
    }


    /** @return 数据库主键。 */
    public Long getId() { return id; }

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

}

