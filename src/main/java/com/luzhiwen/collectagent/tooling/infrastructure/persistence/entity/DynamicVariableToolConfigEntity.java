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
 * 动态变量抽取工具配置。
 */
@Entity
@Table(name = "dynamic_variable_tool_config", uniqueConstraints = @UniqueConstraint(name = "uk_dynamic_tool_name", columnNames = "tool_name"))
public class DynamicVariableToolConfigEntity extends AuditableEntity {

    /** 数据库主键。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 工具唯一名称。 */
    @Column(name = "tool_name", nullable = false, length = 128)
    private String toolName;

    /** 工具基础描述。 */
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "description", nullable = false)
    private String description;

    /** 是否强制模型调用该工具。 */
    @Column(name = "force_call", nullable = false)
    private boolean forceCall;

    /** 是否启用动态变量抽取。 */
    @Column(name = "enabled", nullable = false)
    private boolean enabled;


    /** 创建工具配置实体，JPA 使用无参构造器。 */
    public DynamicVariableToolConfigEntity() {
    }

    /**
     * 创建工具配置实体。
     *
     * @param toolName 工具名称
     * @param description 工具基础描述
     * @param forceCall 是否强制调用
     * @param enabled 是否启用
     */
    public DynamicVariableToolConfigEntity(
            String toolName,
            String description,
            boolean forceCall,
            boolean enabled
    ) {
        this.toolName = toolName;
        this.description = description;
        this.forceCall = forceCall;
        this.enabled = enabled;
    }


    /** @return 数据库主键。 */
    public Long getId() { return id; }

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

}

