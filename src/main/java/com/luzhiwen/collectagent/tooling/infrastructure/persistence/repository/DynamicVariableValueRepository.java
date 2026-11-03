package com.luzhiwen.collectagent.tooling.infrastructure.persistence.repository;

import com.luzhiwen.collectagent.tooling.infrastructure.persistence.entity.DynamicVariableValueEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 会话动态变量值数据访问接口。
 */
public interface DynamicVariableValueRepository extends JpaRepository<DynamicVariableValueEntity, Long> {

    /**
     * 查询指定会话的全部动态变量值。
     *
     * @param sessionId 会话唯一标识
     * @return 动态变量值列表
     */
    List<DynamicVariableValueEntity> findBySessionIdOrderByVariableKeyAsc(String sessionId);

    /**
     * 查询指定会话和变量键的值。
     *
     * @param sessionId 会话唯一标识
     * @param variableKey 变量键
     * @return 动态变量值
     */
    Optional<DynamicVariableValueEntity> findBySessionIdAndVariableKey(String sessionId, String variableKey);
}
