package com.luzhiwen.collectagent.tooling.infrastructure.persistence.repository;

import com.luzhiwen.collectagent.tooling.infrastructure.persistence.entity.DynamicVariableToolConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 动态变量工具配置数据访问接口。
 */
public interface DynamicVariableToolConfigRepository extends JpaRepository<DynamicVariableToolConfigEntity, Long> {

    /**
     * 根据工具名称查询配置。
     *
     * @param toolName 工具名称
     * @return 工具配置
     */
    Optional<DynamicVariableToolConfigEntity> findByToolName(String toolName);
}
