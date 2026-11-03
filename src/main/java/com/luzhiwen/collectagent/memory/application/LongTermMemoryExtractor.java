package com.luzhiwen.collectagent.memory.application;

import com.luzhiwen.collectagent.eventing.infrastructure.persistence.entity.EventLayerEntity;
import com.luzhiwen.collectagent.memory.domain.LongTermMemoryCandidate;

import java.util.List;

/**
 * 长期记忆抽取器。
 */
public interface LongTermMemoryExtractor {

    /**
     * 从事件批次抽取长期记忆候选。
     *
     * @param traceId 链路ID
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param events 事件列表
     * @return 候选记忆
     */
    List<LongTermMemoryCandidate> extract(
            String traceId,
            String sessionId,
            String userId,
            List<EventLayerEntity> events
    );
}
