package com.luzhiwen.collectagent.memory.application;

import com.luzhiwen.collectagent.memory.domain.LongTermMemoryPendingRecord;
import com.luzhiwen.collectagent.memory.infrastructure.persistence.repository.LongTermMemoryPendingRepository;
import com.luzhiwen.collectagent.model.application.EmbeddingClient;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 长期记忆 pending 审核服务。
 */
@Service
public class LongTermMemoryPendingService {

    /** pending 仓储。 */
    private final LongTermMemoryPendingRepository pendingRepository;

    /** 写入服务。 */
    private final LongTermMemoryWriteService writeService;

    /** Embedding客户端。 */
    private final EmbeddingClient embeddingClient;

    /**
     * 创建 pending 服务。
     *
     * @param pendingRepository pending仓储
     * @param writeService 写入服务
     * @param embeddingClient Embedding客户端
     */
    public LongTermMemoryPendingService(
            LongTermMemoryPendingRepository pendingRepository,
            LongTermMemoryWriteService writeService,
            EmbeddingClient embeddingClient
    ) {
        this.pendingRepository = pendingRepository;
        this.writeService = writeService;
        this.embeddingClient = embeddingClient;
    }

    /**
     * 查询 pending。
     *
     * @param status 状态
     * @param limit 数量
     * @return 列表
     */
    public List<LongTermMemoryPendingRecord> list(String status, int limit) {
        return pendingRepository.listByStatus(status, Math.max(1, Math.min(limit, 200)));
    }

    /**
     * 人工审核通过并转为 active。
     *
     * @param id 主键
     * @param reviewer 审核人
     * @param note 说明
     */
    public void approve(long id, String reviewer, String note) {
        if (!embeddingClient.isAvailable()) {
            throw new IllegalStateException("Embedding 未启用，无法审核通过长期记忆");
        }
        LongTermMemoryPendingRecord pending = pendingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("pending 不存在：" + id));
        if (!"pending".equals(pending.getStatus())) {
            throw new IllegalStateException("只有 pending 状态可审核");
        }
        writeService.approvePending(pending);
        pendingRepository.markApproved(id, reviewer, note);
    }

    /**
     * 人工拒绝 pending。
     *
     * @param id 主键
     * @param reviewer 审核人
     * @param note 说明
     */
    public void reject(long id, String reviewer, String note) {
        pendingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("pending 不存在：" + id));
        pendingRepository.markRejected(id, reviewer, note);
    }
}
