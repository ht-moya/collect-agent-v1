package com.luzhiwen.collectagent.memory.application;

import com.luzhiwen.collectagent.memory.infrastructure.persistence.repository.LongTermMemoryRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 长期记忆召回热度更新服务。
 */
@Service
public class LongTermMemoryAccessService {

    /** 长期记忆仓储。 */
    private final LongTermMemoryRepository memoryRepository;

    /**
     * 创建热度更新服务。
     *
     * @param memoryRepository 长期记忆仓储
     */
    public LongTermMemoryAccessService(LongTermMemoryRepository memoryRepository) {
        this.memoryRepository = memoryRepository;
    }

    /**
     * 异步更新召回次数。
     *
     * @param memoryIds 记忆ID
     */
    @Async
    public void recordAccess(List<String> memoryIds) {
        memoryRepository.recordAccess(memoryIds);
    }
}
