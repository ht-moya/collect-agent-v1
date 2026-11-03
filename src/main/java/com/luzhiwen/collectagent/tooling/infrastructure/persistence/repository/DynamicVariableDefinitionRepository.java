package com.luzhiwen.collectagent.tooling.infrastructure.persistence.repository;

import com.luzhiwen.collectagent.tooling.infrastructure.persistence.entity.DynamicVariableDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 动态变量定义数据访问接口。
 */
public interface DynamicVariableDefinitionRepository extends JpaRepository<DynamicVariableDefinitionEntity, Long> {

    /**
     * 查询全部变量定义并按展示顺序排序。
     *
     * @return 变量定义列表
     */
    List<DynamicVariableDefinitionEntity> findAllByOrderBySortOrderAscIdAsc();

    /**
     * 查询启用的变量定义并按展示顺序排序。
     *
     * @return 启用的变量定义列表
     */
    List<DynamicVariableDefinitionEntity> findByEnabledTrueOrderBySortOrderAscIdAsc();

    /**
     * 根据变量键查询定义。
     *
     * @param variableKey 变量键
     * @return 变量定义
     */
    Optional<DynamicVariableDefinitionEntity> findByVariableKey(String variableKey);
}
