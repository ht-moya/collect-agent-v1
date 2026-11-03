package com.luzhiwen.collectagent.memory.infrastructure.persistence.repository;

import com.luzhiwen.collectagent.memory.infrastructure.persistence.entity.AgentMemoryExtractionCheckpointEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 长期记忆抽取位点数据访问接口。
 */
public interface AgentMemoryExtractionCheckpointRepository extends JpaRepository<AgentMemoryExtractionCheckpointEntity, String> {

    /**
     * 锁定会话抽取位点。
     *
     * @param sessionId 会话ID
     * @return 抽取位点
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from AgentMemoryExtractionCheckpointEntity c where c.sessionId = :sessionId")
    Optional<AgentMemoryExtractionCheckpointEntity> findBySessionIdForUpdate(@Param("sessionId") String sessionId);
}
