package com.luzhiwen.collectagent.memory.infrastructure.persistence.repository;

import com.luzhiwen.collectagent.memory.infrastructure.persistence.entity.AgentSessionStateEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Agent 会话状态数据访问接口。
 */
public interface AgentSessionStateRepository extends JpaRepository<AgentSessionStateEntity, String> {

    /**
     * 查询会话状态。
     *
     * @param sessionId 会话唯一标识
     * @return 会话状态
     */
    Optional<AgentSessionStateEntity> findBySessionId(String sessionId);

    /**
     * 在更新状态前锁定会话状态行。
     *
     * @param sessionId 会话唯一标识
     * @return 已加锁的会话状态
     */
    List<AgentSessionStateEntity> findByStageNotAndUpdatedAtBeforeOrderByUpdatedAtAsc(
            String stage,
            LocalDateTime before,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AgentSessionStateEntity s where s.sessionId = :sessionId")
    Optional<AgentSessionStateEntity> findBySessionIdForUpdate(@Param("sessionId") String sessionId);
}

