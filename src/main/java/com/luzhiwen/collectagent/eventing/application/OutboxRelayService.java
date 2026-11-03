package com.luzhiwen.collectagent.eventing.application;

import com.luzhiwen.collectagent.eventing.domain.OutboxStatus;
import com.luzhiwen.collectagent.eventing.infrastructure.persistence.entity.OutboxEventEntity;
import com.luzhiwen.collectagent.eventing.infrastructure.persistence.repository.OutboxEventRepository;

import com.luzhiwen.collectagent.shared.config.AgentProperties;
import com.luzhiwen.collectagent.eventing.infrastructure.persistence.entity.EventLayerEntity;
import com.luzhiwen.collectagent.eventing.infrastructure.persistence.repository.EventLayerRepository;
import com.luzhiwen.collectagent.eventing.application.MemoryWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时 Outbox Relay，把原始事件投递给本地 Memory Worker。
 */
@Service
public class OutboxRelayService {

    /** 当前类日志器。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxRelayService.class);

    /** Outbox 数据访问接口。 */
    private final OutboxEventRepository outboxEventRepository;

    /** 原始事件数据访问接口。 */
    private final EventLayerRepository eventLayerRepository;

    /** 本地记忆 Worker。 */
    private final MemoryWorker memoryWorker;

    /** Agent 配置。 */
    private final AgentProperties agentProperties;

    /**
     * 创建 Outbox Relay。
     *
     * @param outboxEventRepository Outbox 数据访问接口
     * @param eventLayerRepository 原始事件数据访问接口
     * @param memoryWorker 本地记忆 Worker
     * @param agentProperties Agent 配置
     */
    public OutboxRelayService(
            OutboxEventRepository outboxEventRepository,
            EventLayerRepository eventLayerRepository,
            MemoryWorker memoryWorker,
            AgentProperties agentProperties
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.eventLayerRepository = eventLayerRepository;
        this.memoryWorker = memoryWorker;
        this.agentProperties = agentProperties;
    }

    /**
     * 定时扫描并处理待投递 Outbox 事件。
     */
    @Scheduled(fixedDelayString = "${agent.outbox.relay-interval-ms:2000}")
    public synchronized void relay() {
        int batchSize = Math.max(1, agentProperties.getOutbox().getBatchSize());
        List<OutboxEventEntity> events = outboxEventRepository
                .findByStatusAndNextRetryAtLessThanEqualOrderByIdAsc(
                        OutboxStatus.PENDING,
                        LocalDateTime.now(),
                        PageRequest.of(0, batchSize)
                );
        for (OutboxEventEntity outboxEvent : events) {
            processOne(outboxEvent);
        }
    }

    /**
     * 处理单条 Outbox 事件。
     *
     * @param outboxEvent Outbox 事件
     */
    private void processOne(OutboxEventEntity outboxEvent) {
        outboxEvent.setStatus(OutboxStatus.PROCESSING);
        outboxEventRepository.save(outboxEvent);
        try {
            EventLayerEntity event = eventLayerRepository.findByEventId(outboxEvent.getEventId())
                    .orElseThrow(() -> new IllegalStateException("原始事件不存在：" + outboxEvent.getEventId()));
            memoryWorker.process(event);
            outboxEvent.setStatus(OutboxStatus.PROCESSED);
            outboxEvent.setProcessedAt(LocalDateTime.now());
            outboxEvent.setLastError(null);
            outboxEventRepository.save(outboxEvent);
        } catch (Exception exception) {
            LOGGER.error("Outbox 事件处理失败，eventId={}", outboxEvent.getEventId(), exception);
            int retryCount = outboxEvent.getRetryCount() + 1;
            outboxEvent.setRetryCount(retryCount);
            outboxEvent.setLastError(exception.getMessage());
            if (retryCount >= agentProperties.getOutbox().getMaxRetryCount()) {
                outboxEvent.setStatus(OutboxStatus.FAILED);
            } else {
                outboxEvent.setStatus(OutboxStatus.PENDING);
                outboxEvent.setNextRetryAt(LocalDateTime.now().plusSeconds(5L * retryCount));
            }
            outboxEventRepository.save(outboxEvent);
        }
    }
}
