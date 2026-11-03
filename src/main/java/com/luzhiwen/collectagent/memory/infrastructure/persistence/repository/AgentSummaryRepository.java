package com.luzhiwen.collectagent.memory.infrastructure.persistence.repository;

import com.luzhiwen.collectagent.memory.domain.AgentSummaryStatus;
import com.luzhiwen.collectagent.memory.infrastructure.persistence.entity.AgentSummaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Agent 会话摘要数据访问接口。
 */
public interface AgentSummaryRepository extends JpaRepository<AgentSummaryEntity, Long> {

    /**
     * 查询会话最新有效摘要。
     *
     * @param tenantId 租户唯一标识
     * @param sessionId 会话唯一标识
     * @param status 摘要状态
     * @return 最新有效摘要
     */
    Optional<AgentSummaryEntity> findFirstByTenantIdAndSessionIdAndStatusOrderByCoversSeqToDescCreatedAtDescIdDesc(
            String tenantId,
            String sessionId,
            String status
    );
}

