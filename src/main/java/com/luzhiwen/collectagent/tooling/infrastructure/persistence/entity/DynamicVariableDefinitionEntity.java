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
 * 动态变量定义。
 */
@Entity
@Table(name = "dynamic_variable_definition", uniqueConstraints = @UniqueConstraint(name = "uk_dynamic_variable_key", columnNames = "variable_key"))
public class DynamicVariableDefinitionEntity extends AuditableEntity {

    /** 数据库主键。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 变量键，例如 address。 */
    @Column(name = "variable_key", nullable = false, length = 64)
    private String variableKey;

    /** 变量展示名称。 */
    @Column(name = "display_name", nullable = false, length = 128)
    private String displayName;

    /** 变量用途说明。 */
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "description", nullable = false)
    private String description;

    /** 给模型使用的抽取提示。 */
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "extraction_hint")
    private String extractionHint;

    /** 是否启用该动态变量。 */
    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    /** 展示排序值。 */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;


    /** 创建动态变量定义实体，JPA 使用无参构造器。 */
    public DynamicVariableDefinitionEntity() {
    }


    /** @return 数据库主键。 */
    public Long getId() { return id; }

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

