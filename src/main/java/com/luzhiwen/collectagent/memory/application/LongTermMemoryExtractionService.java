package com.luzhiwen.collectagent.memory.application;

import com.luzhiwen.collectagent.eventing.domain.EventType;
import com.luzhiwen.collectagent.eventing.infrastructure.persistence.entity.EventLayerEntity;
import com.luzhiwen.collectagent.eventing.infrastructure.persistence.repository.EventLayerRepository;
import com.luzhiwen.collectagent.memory.domain.LongTermMemoryCandidate;
import com.luzhiwen.collectagent.memory.infrastructure.persistence.entity.AgentMemoryExtractionCheckpointEntity;
import com.luzhiwen.collectagent.memory.infrastructure.persistence.repository.AgentMemoryExtractionCheckpointRepository;
import com.luzhiwen.collectagent.model.application.EmbeddingClient;
import com.luzhiwen.collectagent.shared.config.AgentProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 长期记忆抽取编排服务。
 */
@Service
public class LongTermMemoryExtractionService {

    /** 当前类日志器。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(LongTermMemoryExtractionService.class);

    /** 事件仓储。 */
    private final EventLayerRepository eventLayerRepository;

    /** 抽取位点仓储。 */
    private final AgentMemoryExtractionCheckpointRepository checkpointRepository;

    /** 长期记忆抽取器。 */
    private final LongTermMemoryExtractor extractor;

    /** 候选校验器。 */
    private final LongTermMemoryCandidateValidator candidateValidator;

    /** 写入服务。 */
    private final LongTermMemoryWriteService writeService;

    /** Embedding客户端。 */
    private final EmbeddingClient embeddingClient;

    /** Agent配置。 */
    private final AgentProperties agentProperties;

    /**
     * 创建长期记忆抽取服务。
     *
     * @param eventLayerRepository 事件仓储
     * @param checkpointRepository 抽取位点仓储
     * @param extractor 抽取器
     * @param candidateValidator 候选校验器
     * @param writeService 写入服务
     * @param embeddingClient Embedding客户端
     * @param agentProperties Agent配置
     */
    public LongTermMemoryExtractionService(
            EventLayerRepository eventLayerRepository,
            AgentMemoryExtractionCheckpointRepository checkpointRepository,
            LongTermMemoryExtractor extractor,
            LongTermMemoryCandidateValidator candidateValidator,
            LongTermMemoryWriteService writeService,
            EmbeddingClient embeddingClient,
            AgentProperties agentProperties
    ) {
        this.eventLayerRepository = eventLayerRepository;
        this.checkpointRepository = checkpointRepository;
        this.extractor = extractor;
        this.candidateValidator = candidateValidator;
        this.writeService = writeService;
        this.embeddingClient = embeddingClient;
        this.agentProperties = agentProperties;
    }

    /**
     * 在轮次结束事件上检查并执行长期记忆抽取。
     *
     * @param event 当前事件
     */
    public void maybeExtract(EventLayerEntity event) {
        if (event.getEventType() != EventType.ASSISTANT_MESSAGE
                && event.getEventType() != EventType.SESSION_CLOSED) {
            LOGGER.debug("跳过长期记忆抽取：事件类型不触发，sessionId={}，seq={}，eventType={}",
                    event.getSessionId(), event.getSessionSeq(), event.getEventType());
            return;
        }
        if (!embeddingClient.isAvailable()) {
            LOGGER.info("Embedding 未启用，跳过长期记忆抽取，sessionId={}", event.getSessionId());
            return;
        }
        AgentMemoryExtractionCheckpointEntity checkpoint = loadOrCreateCheckpoint(event);
        long fromSeq = checkpoint.getExtractedSeq();
        long toSeq = event.getSessionSeq() == null ? fromSeq : event.getSessionSeq();
        if (toSeq <= fromSeq) {
            LOGGER.info("跳过长期记忆抽取：事件位点未推进，sessionId={}，eventType={}，fromSeq={}，toSeq={}",
                    event.getSessionId(), event.getEventType(), fromSeq, toSeq);
            return;
        }
        if (event.getEventType() == EventType.ASSISTANT_MESSAGE) {
            long completedTurns = eventLayerRepository
                    .countBySessionIdAndEventTypeAndSessionSeqGreaterThanAndSessionSeqLessThanEqual(
                            event.getSessionId(), EventType.ASSISTANT_MESSAGE, fromSeq, toSeq
                    );
            int extractionTurnInterval = agentProperties.getLtm().getExtractionTurnInterval();
            if (completedTurns < extractionTurnInterval) {
                LOGGER.info("跳过长期记忆抽取：完整轮次不足，sessionId={}，fromSeq={}，toSeq={}，completedTurns={}，interval={}",
                        event.getSessionId(), fromSeq, toSeq, completedTurns, extractionTurnInterval);
                return;
            }
        }
        List<EventLayerEntity> events = eventLayerRepository
                .findBySessionIdAndSessionSeqGreaterThanAndSessionSeqLessThanEqualOrderBySessionSeqAsc(
                        event.getSessionId(), fromSeq, toSeq
                );
        if (events.isEmpty()) {
            LOGGER.info("跳过长期记忆抽取：待抽取事件为空，sessionId={}，fromSeq={}，toSeq={}",
                    event.getSessionId(), fromSeq, toSeq);
            return;
        }
        LOGGER.info("开始长期记忆抽取：sessionId={}，eventType={}，fromSeq={}，toSeq={}，eventCount={}",
                event.getSessionId(), event.getEventType(), fromSeq, toSeq, events.size());
        List<LongTermMemoryCandidate> rawCandidates = extractor.extract(
                event.getTraceId(), event.getSessionId(), event.getCustomerId(), events
        );
        Set<String> validEventIds = events.stream().map(EventLayerEntity::getEventId).collect(java.util.stream.Collectors.toSet());
        List<LongTermMemoryCandidate> candidates = new ArrayList<>();
        for (LongTermMemoryCandidate candidate : rawCandidates) {
            candidateValidator.validate(candidate, validEventIds).ifPresent(candidates::add);
        }
        LOGGER.info("长期记忆抽取候选校验完成：sessionId={}，rawCandidates={}，validCandidates={}",
                event.getSessionId(), rawCandidates.size(), candidates.size());
        writeService.writeCandidates(AgentSessionStateService.DEFAULT_TENANT_ID, event.getCustomerId(), candidates);
        checkpoint.setExtractedSeq(toSeq);
        checkpoint.setLastExtractionAt(LocalDateTime.now());
        checkpointRepository.save(checkpoint);
        LOGGER.info("长期记忆抽取完成，sessionId={}，seqFrom={}，seqTo={}，candidates={}",
                event.getSessionId(), fromSeq, toSeq, candidates.size());
    }

    /**
     * 加载或创建抽取位点。
     *
     * @param event 当前事件
     * @return 抽取位点
     */
    private AgentMemoryExtractionCheckpointEntity loadOrCreateCheckpoint(EventLayerEntity event) {
        return checkpointRepository.findById(event.getSessionId())
                .orElseGet(() -> checkpointRepository.save(new AgentMemoryExtractionCheckpointEntity(
                        event.getSessionId(),
                        AgentSessionStateService.DEFAULT_TENANT_ID,
                        event.getCustomerId()
                )));
    }
}
