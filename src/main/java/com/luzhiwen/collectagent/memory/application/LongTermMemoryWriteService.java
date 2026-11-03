package com.luzhiwen.collectagent.memory.application;

import com.luzhiwen.collectagent.memory.domain.LongTermMemoryCandidate;
import com.luzhiwen.collectagent.memory.domain.LongTermMemoryPendingRecord;
import com.luzhiwen.collectagent.memory.domain.LongTermMemoryRecord;
import com.luzhiwen.collectagent.memory.domain.LongTermMemoryScope;
import com.luzhiwen.collectagent.memory.domain.LongTermMemoryStatus;
import com.luzhiwen.collectagent.memory.domain.LongTermMemoryType;
import com.luzhiwen.collectagent.memory.infrastructure.persistence.repository.LongTermMemoryPendingRepository;
import com.luzhiwen.collectagent.memory.infrastructure.persistence.repository.LongTermMemoryRepository;
import com.luzhiwen.collectagent.model.application.EmbeddingClient;
import com.luzhiwen.collectagent.shared.config.AgentProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 长期记忆写入服务。
 */
@Service
public class LongTermMemoryWriteService {

    /** 当前类日志器。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(LongTermMemoryWriteService.class);

    /** 长期记忆仓储。 */
    private final LongTermMemoryRepository memoryRepository;

    /** pending 仓储。 */
    private final LongTermMemoryPendingRepository pendingRepository;

    /** Embedding 客户端。 */
    private final EmbeddingClient embeddingClient;

    /** Agent 配置。 */
    private final AgentProperties agentProperties;

    /**
     * 创建写入服务。
     *
     * @param memoryRepository 长期记忆仓储
     * @param pendingRepository pending仓储
     * @param embeddingClient Embedding客户端
     * @param agentProperties Agent配置
     */
    public LongTermMemoryWriteService(
            LongTermMemoryRepository memoryRepository,
            LongTermMemoryPendingRepository pendingRepository,
            EmbeddingClient embeddingClient,
            AgentProperties agentProperties
    ) {
        this.memoryRepository = memoryRepository;
        this.pendingRepository = pendingRepository;
        this.embeddingClient = embeddingClient;
        this.agentProperties = agentProperties;
    }

    /**
     * 按置信度分流并写入候选。
     *
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param candidates 候选列表
     */
    public void writeCandidates(String tenantId, String userId, List<LongTermMemoryCandidate> candidates) {
        List<LongTermMemoryCandidate> activeCandidates = new ArrayList<>();
        int pendingCount = 0;
        int skippedCount = 0;
        for (LongTermMemoryCandidate candidate : candidates) {
            if (candidate.getConfidence() < agentProperties.getLtm().getPendingConfidence()) {
                skippedCount++;
                LOGGER.info("长期记忆候选被丢弃：置信度过低，type={}，confidence={}，pendingThreshold={}，content={}",
                        candidate.getMemoryType(), candidate.getConfidence(),
                        agentProperties.getLtm().getPendingConfidence(), candidate.getContent());
                continue;
            }
            if (candidate.getConfidence() < agentProperties.getLtm().getActiveConfidence()) {
                savePending(tenantId, userId, candidate);
                pendingCount++;
                LOGGER.info("长期记忆候选进入 pending：type={}，confidence={}，activeThreshold={}，content={}",
                        candidate.getMemoryType(), candidate.getConfidence(),
                        agentProperties.getLtm().getActiveConfidence(), candidate.getContent());
                continue;
            }
            activeCandidates.add(candidate);
        }
        LOGGER.info("长期记忆候选分流完成：total={}，active={}，pending={}，skipped={}",
                candidates.size(), activeCandidates.size(), pendingCount, skippedCount);
        if (activeCandidates.isEmpty()) {
            return;
        }
        List<List<Float>> embeddings = embeddingClient.embed(
                activeCandidates.stream().map(LongTermMemoryCandidate::getContent).toList()
        );
        for (int index = 0; index < activeCandidates.size(); index++) {
            persistActive(tenantId, userId, activeCandidates.get(index), embeddings.get(index));
        }
    }

    /**
     * 人工审核通过后强制写入活跃记忆。
     *
     * @param pending pending记录
     */
    public void approvePending(LongTermMemoryPendingRecord pending) {
        List<Float> embedding = embeddingClient.embed(List.of(pending.getContent())).get(0);
        LongTermMemoryCandidate candidate = new LongTermMemoryCandidate(
                pending.getMemoryType(),
                pending.getContent(),
                pending.getStructured(),
                pending.getSourceEventIds(),
                Math.max(pending.getConfidence(), agentProperties.getLtm().getActiveConfidence())
        );
        persistActive(pending.getTenantId(), pending.getUserId(), candidate, embedding);
    }

    /**
     * 写入或合并一条活跃记忆。
     *
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param candidate 候选
     * @param embedding 向量
     */
    private void persistActive(
            String tenantId,
            String userId,
            LongTermMemoryCandidate candidate,
            List<Float> embedding
    ) {
        // 先把候选转换为真正的数据库记录，包括 ULID、内容哈希、向量模型和初始状态。
        LongTermMemoryRecord incoming = toRecord(tenantId, userId, candidate, embedding);

        // 第一步做精确去重：只要租户、用户、类型和内容哈希完全相同，就认为是同一条记忆。
        LongTermMemoryRecord exact = memoryRepository.findExact(
                tenantId, userId, candidate.getMemoryType(), incoming.getContentHash()
        ).orElse(null);
        if (exact != null) {
            // 精确重复不新增记录，只合并来源事件、置信度和结构化字段。
            memoryRepository.mergeActive(merge(exact, incoming));
            LOGGER.info("长期记忆精确合并：memoryId={}，type={}，content={}",
                    exact.getMemoryId(), incoming.getMemoryType(), incoming.getContent());
            return;
        }

        // FACT 先按 fact_key + fact_value 做业务级去重，避免同义表述绕过内容哈希和语义阈值。
        if (reconcileStructuredFact(tenantId, userId, incoming)) {
            return;
        }

        // 第二步做语义去重：先按余弦相似度取同类型记忆 Top3。
        List<LongTermMemoryRecord> dense = memoryRepository.rankByCosine(
                embedding,
                memoryRepository.findDenseCandidates(
                        tenantId, userId, candidate.getMemoryType(), embeddingClient.modelName()
                ),
                3
        );

        // 同时通过 MySQL FULLTEXT ngram 取关键词候选 Top3。
        List<LongTermMemoryRecord> keyword = memoryRepository.findKeywordCandidates(
                tenantId, userId, candidate.getMemoryType(), candidate.getContent(), 3
        );

        // 把稠密候选放到 Map 中，便于判断关键词候选是否也出现在稠密候选中。
        Map<String, LongTermMemoryRecord> denseMap = new HashMap<>();
        dense.forEach(record -> denseMap.put(record.getMemoryId(), record));

        // 计算两路召回交集；只有两路都命中的记忆才进入写前去重判断。
        Set<String> intersection = new LinkedHashSet<>();
        keyword.forEach(record -> {
            if (denseMap.containsKey(record.getMemoryId())) {
                intersection.add(record.getMemoryId());
            }
        });

        // 从交集中选出余弦相似度最高的一条作为最可能的已有记忆。
        LongTermMemoryRecord best = intersection.stream()
                .map(denseMap::get)
                .max(java.util.Comparator.comparingDouble(LongTermMemoryRecord::getScore))
                .orElse(null);
        if (best != null) {
            double similarity = best.getScore();

            // 相似度较高但结构化字段冲突，说明这是同一事实的新值：旧记忆失效，新记忆生效。
            if (similarity > 0.75D && hasStructuredConflict(best, incoming)) {
                memoryRepository.markSuperseded(best.getMemoryId());
                memoryRepository.insertActive(incoming);
                LOGGER.info("长期记忆结构化冲突替换：oldMemoryId={}，newMemoryId={}，similarity={}",
                        best.getMemoryId(), incoming.getMemoryId(), similarity);
                return;
            }

            // 相似度很高且没有冲突，认为是同一条记忆：合并来源、置信度和结构化字段。
            if (similarity > 0.75D && !hasStructuredConflict(best, incoming)) {
                memoryRepository.mergeActive(merge(best, incoming));
                LOGGER.info("长期记忆语义合并：memoryId={}，similarity={}，incomingContent={}",
                        best.getMemoryId(), similarity, incoming.getContent());
                return;
            }

            // 相似度较高且新内容更具体：更新内容、内容哈希和向量，并重算 embedding。
            if (similarity > 0.75D && isRefinement(best, incoming)) {
                LongTermMemoryRecord refined = merge(best, incoming);
                refined.setContent(incoming.getContent());
                refined.setContentHash(incoming.getContentHash());
                refined.setEmbedding(incoming.getEmbedding());
                refined.setEmbeddingModel(incoming.getEmbeddingModel());
                memoryRepository.refineContent(refined);
                LOGGER.info("长期记忆内容细化：memoryId={}，similarity={}，refinedContent={}",
                        best.getMemoryId(), similarity, refined.getContent());
                return;
            }
        }

        // 如果两路没有交集，但纯语义 Top1 相似度仍然超过 0.90，也可降级认定为同一条记忆。
        if (!dense.isEmpty()) {
            LongTermMemoryRecord top = dense.get(0);
            if (top.getScore() > 0.90D && !hasStructuredConflict(top, incoming)) {
                memoryRepository.mergeActive(merge(top, incoming));
                LOGGER.info("长期记忆纯语义 Top1 合并：memoryId={}，similarity={}，incomingContent={}",
                        top.getMemoryId(), top.getScore(), incoming.getContent());
                return;
            }
        }

        // 所有去重和合并条件都不满足，说明这是一条新长期记忆，直接新增为 active。
        memoryRepository.insertActive(incoming);
        LOGGER.info("长期记忆新增：memoryId={}，type={}，content={}",
                incoming.getMemoryId(), incoming.getMemoryType(), incoming.getContent());
    }

    /**
     * 按结构化事实键合并同值事实，或用新值替代旧值。
     *
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param incoming 新记录
     * @return true 表示已完成事实去重并写入数据库
     */
    private boolean reconcileStructuredFact(
            String tenantId,
            String userId,
            LongTermMemoryRecord incoming
    ) {
        if (incoming.getMemoryType() != LongTermMemoryType.FACT) {
            return false;
        }
        String factKey = String.valueOf(incoming.getStructured().getOrDefault("fact_key", ""));
        String factValue = String.valueOf(incoming.getStructured().getOrDefault("fact_value", ""));
        if (factKey.isBlank() || factValue.isBlank()) {
            return false;
        }
        List<LongTermMemoryRecord> existingFacts = memoryRepository.findActiveFactsByKey(
                tenantId, userId, factKey
        );
        if (existingFacts.isEmpty()) {
            return false;
        }
        LongTermMemoryRecord sameValueFact = existingFacts.stream()
                .filter(record -> factValue.equals(String.valueOf(record.getStructured().get("fact_value"))))
                .findFirst()
                .orElse(null);
        if (sameValueFact == null) {
            // 同一事实键出现新值，说明旧值已经失效，但历史记录保留为 superseded。
            existingFacts.forEach(record -> memoryRepository.markSuperseded(record.getMemoryId()));
            memoryRepository.insertActive(incoming);
            LOGGER.info("结构化事实值更新：factKey={}，oldCount={}，newMemoryId={}，newValue={}",
                    factKey, existingFacts.size(), incoming.getMemoryId(), factValue);
            return true;
        }
        // 同值事实合并到一条活跃记录，并清理历史上已经产生的重复行。
        LongTermMemoryRecord canonical = sameValueFact;
        for (LongTermMemoryRecord existing : existingFacts) {
            if (existing.getMemoryId().equals(canonical.getMemoryId())) {
                continue;
            }
            canonical = merge(canonical, existing);
            memoryRepository.markSuperseded(existing.getMemoryId());
        }
        memoryRepository.mergeActive(merge(canonical, incoming));
        LOGGER.info("结构化事实同值合并：factKey={}，factValue={}，memoryId={}，existingCount={}",
                factKey, factValue, canonical.getMemoryId(), existingFacts.size());
        return true;
    }

    /**
     * 保存 pending。
     *
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param candidate 候选
     */
    private void savePending(String tenantId, String userId, LongTermMemoryCandidate candidate) {
        String content = LongTermMemoryContentUtil.normalize(candidate.getContent());
        LongTermMemoryPendingRecord pending = new LongTermMemoryPendingRecord();
        pending.setTenantId(tenantId);
        pending.setUserId(userId);
        pending.setScope(LongTermMemoryScope.USER);
        pending.setMemoryType(candidate.getMemoryType());
        pending.setContent(content);
        pending.setContentHash(LongTermMemoryContentUtil.md5(content));
        pending.setStructured(candidate.getStructured());
        pending.setSourceEventIds(candidate.getSourceEventIds());
        pending.setConfidence(candidate.getConfidence());
        pending.setStatus("pending");
        pendingRepository.upsertPending(pending);
    }

    /**
     * 候选转持久化记录。
     *
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param candidate 候选
     * @param embedding 向量
     * @return 记录
     */
    private LongTermMemoryRecord toRecord(
            String tenantId,
            String userId,
            LongTermMemoryCandidate candidate,
            List<Float> embedding
    ) {
        LongTermMemoryRecord record = new LongTermMemoryRecord();
        record.setMemoryId(UlidGenerator.next());
        record.setTenantId(tenantId);
        record.setUserId(userId);
        record.setScope(LongTermMemoryScope.USER);
        record.setMemoryType(candidate.getMemoryType());
        record.setContent(LongTermMemoryContentUtil.normalize(candidate.getContent()));
        record.setContentHash(LongTermMemoryContentUtil.md5(record.getContent()));
        record.setStructured(candidate.getStructured());
        record.setSourceEventIds(candidate.getSourceEventIds());
        record.setConfidence(candidate.getConfidence());
        record.setStatus(LongTermMemoryStatus.ACTIVE);
        record.setEmbedding(embedding);
        record.setEmbeddingModel(embeddingClient.modelName());
        return record;
    }

    /**
     * 合并来源、置信度和结构化字段。
     *
     * @param existing 已存在记录
     * @param incoming 新记录
     * @return 合并记录
     */
    private LongTermMemoryRecord merge(LongTermMemoryRecord existing, LongTermMemoryRecord incoming) {
        Set<String> sourceIds = new LinkedHashSet<>(existing.getSourceEventIds());
        sourceIds.addAll(incoming.getSourceEventIds());
        existing.setSourceEventIds(new ArrayList<>(sourceIds));
        existing.setConfidence(Math.max(existing.getConfidence(), incoming.getConfidence()));
        Map<String, Object> structured = new java.util.LinkedHashMap<>(existing.getStructured());
        structured.putAll(incoming.getStructured());
        existing.setStructured(structured);
        existing.setStatus(LongTermMemoryStatus.ACTIVE);
        return existing;
    }

    /**
     * 判断结构化字段是否冲突。
     *
     * @param existing 已存在记录
     * @param incoming 新记录
     * @return true 表示冲突
     */
    private boolean hasStructuredConflict(LongTermMemoryRecord existing, LongTermMemoryRecord incoming) {
        for (Map.Entry<String, Object> entry : incoming.getStructured().entrySet()) {
            if (existing.getStructured().containsKey(entry.getKey())
                    && !String.valueOf(existing.getStructured().get(entry.getKey()))
                    .equals(String.valueOf(entry.getValue()))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断新内容是否是更具体的细化。
     *
     * @param existing 旧记忆
     * @param incoming 新记忆
     * @return true 表示细化
     */
    private boolean isRefinement(LongTermMemoryRecord existing, LongTermMemoryRecord incoming) {
        return incoming.getContent().length() > existing.getContent().length()
                && incoming.getContent().contains(existing.getContent());
    }
}

