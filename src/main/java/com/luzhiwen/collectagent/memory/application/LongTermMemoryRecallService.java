package com.luzhiwen.collectagent.memory.application;

import com.luzhiwen.collectagent.memory.domain.LongTermMemoryRecord;
import com.luzhiwen.collectagent.memory.domain.LongTermMemoryType;
import com.luzhiwen.collectagent.memory.domain.LongTermMemoryView;
import com.luzhiwen.collectagent.memory.infrastructure.persistence.repository.LongTermMemoryRepository;
import com.luzhiwen.collectagent.model.application.EmbeddingClient;
import com.luzhiwen.collectagent.shared.config.AgentProperties;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 长期记忆 RRF 召回服务。
 */
@Service
public class LongTermMemoryRecallService {

    /** 记忆仓储。 */
    private final LongTermMemoryRepository memoryRepository;

    /** Embedding 客户端。 */
    private final EmbeddingClient embeddingClient;

    /** 热度更新服务。 */
    private final LongTermMemoryAccessService accessService;

    /** JSON 序列化器。 */
    private final ObjectMapper objectMapper;

    /** Agent 配置。 */
    private final AgentProperties agentProperties;

    /**
     * 创建召回服务。
     *
     * @param memoryRepository 记忆仓储
     * @param embeddingClient Embedding客户端
     * @param accessService 热度更新服务
     * @param objectMapper JSON序列化器
     * @param agentProperties Agent配置
     */
    public LongTermMemoryRecallService(
            LongTermMemoryRepository memoryRepository,
            EmbeddingClient embeddingClient,
            LongTermMemoryAccessService accessService,
            ObjectMapper objectMapper,
            AgentProperties agentProperties
    ) {
        this.memoryRepository = memoryRepository;
        this.embeddingClient = embeddingClient;
        this.accessService = accessService;
        this.objectMapper = objectMapper;
        this.agentProperties = agentProperties;
    }

    /**
     * 召回并注入前 TopK 长期记忆。
     *
     * @param userId 用户ID
     * @param message 当前消息
     * @param stateViewJson 状态视图
     * @return 长期记忆视图
     */
    public List<LongTermMemoryView> recall(String userId, String message, String stateViewJson) {
        // 没有可用的 Embedding 服务时，直接跳过长期记忆召回，聊天主流程继续执行。
        if (!embeddingClient.isAvailable()) {
            return List.of();
        }

        // 把用户当前消息、还款意愿、拒绝原因和困难原因拼成召回查询文本。
        String query = buildQuery(message, stateViewJson);

        // 调用 text-embedding-v4，把查询文本转换成 1024 维查询向量。
        List<Float> queryEmbedding = embeddingClient.embed(List.of(query)).get(0);

        // 第一路是稠密召回：读取同一租户、用户和向量模型的候选，再在 Java 中计算余弦相似度。
        List<LongTermMemoryRecord> dense = memoryRepository.rankByCosine(
                queryEmbedding,
                memoryRepository.findDenseRecallCandidates(
                        AgentSessionStateService.DEFAULT_TENANT_ID, userId, embeddingClient.modelName()),
                agentProperties.getLtm().getDenseTopK()
        );

        // 第二路是关键词召回：使用 MySQL FULLTEXT ngram 匹配记忆内容。
        List<LongTermMemoryRecord> keyword = memoryRepository.findKeywordRecallCandidates(
                AgentSessionStateService.DEFAULT_TENANT_ID, userId, query,
                agentProperties.getLtm().getKeywordTopK()
        );

        // 两路都没有命中时直接返回空列表，避免后续无意义的融合计算。
        if (dense.isEmpty() && keyword.isEmpty()) {
            return List.of();
        }

        // 以 memoryId 为键合并两路候选，保证同一条记忆只保留一份完整数据。
        Map<String, LongTermMemoryRecord> records = new HashMap<>();
        dense.forEach(record -> records.put(record.getMemoryId(), record));
        keyword.forEach(record -> records.putIfAbsent(record.getMemoryId(), record));

        // 按两路排名计算 RRF 分数；同一记忆在两路都出现时，RRF 分数会累加。
        Map<String, Double> rrfScores = calculateRrf(
                List.of(ids(dense), ids(keyword)), agentProperties.getLtm().getRrfK()
        );

        // 获取本轮最大 RRF 分数，用于把不同候选的 RRF 分数归一化到 0~1。
        double maxRrf = rrfScores.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0D);
        LocalDateTime now = LocalDateTime.now();

        // 对候选执行最终重排：归一化 RRF、置信度、时间衰减和记忆类型权重。
        List<LongTermMemoryView> result = records.values().stream()
                .peek(record -> record.setScore(calculateFinalScore(
                        rrfScores.getOrDefault(record.getMemoryId(), 0.0D) / maxRrf, record, now)))
                .sorted(java.util.Comparator.comparingDouble(LongTermMemoryRecord::getScore).reversed())
                .limit(agentProperties.getLtm().getFinalTopK())
                .map(record -> new LongTermMemoryView(
                        record.getMemoryId(), record.getMemoryType(), record.getContent(),
                        record.getConfidence(), record.getScore()))
                .toList();

        // 召回完成后异步增加访问次数和最后访问时间，不阻塞当前聊天响应。
        accessService.recordAccess(result.stream().map(LongTermMemoryView::getMemoryId).toList());
        return result;
    }

    /**
     * 构造查询文本。
     *
     * @param message 用户消息
     * @param stateViewJson 状态视图
     * @return 查询文本
     */
    private String buildQuery(String message, String stateViewJson) {
        StringBuilder query = new StringBuilder(message == null ? "" : message);
        try {
            JsonNode slots = objectMapper.readTree(stateViewJson == null ? "{}" : stateViewJson).path("known_slots");
            appendSlot(query, slots, "repayment_intent");
            appendSlot(query, slots, "refuse_reason");
            appendSlot(query, slots, "hardship_reason");
        } catch (Exception ignored) {
            // 状态视图损坏时只使用当前消息。
        }
        return query.toString();
    }

    /**
     * 追加非空槽位。
     *
     * @param query 查询构造器
     * @param slots 槽位
     * @param name 槽位名
     */
    private void appendSlot(StringBuilder query, JsonNode slots, String name) {
        JsonNode value = slots.path(name);
        if (!value.isMissingNode() && !value.isNull() && !value.asString("").isBlank()) {
            query.append(' ').append(value.asString());
        }
    }

    /**
     * 提取排名ID。
     *
     * @param records 记录
     * @return ID列表
     */
    private List<String> ids(List<LongTermMemoryRecord> records) {
        return records.stream().map(LongTermMemoryRecord::getMemoryId).toList();
    }

    /**
     * 计算 RRF。
     *
     * @param rankedLists 排名列表
     * @param k 平滑常数
     * @return 得分
     */
    private Map<String, Double> calculateRrf(List<List<String>> rankedLists, int k) {
        Map<String, Double> scores = new LinkedHashMap<>();
        for (List<String> rankedList : rankedLists) {
            for (int rank = 0; rank < rankedList.size(); rank++) {
                scores.merge(rankedList.get(rank), 1.0D / (k + rank + 1), Double::sum);
            }
        }
        return scores;
    }

    /**
     * 计算最终重排分数。
     *
     * @param normalizedRrf 归一化RRF
     * @param record 记录
     * @param now 当前时间
     * @return 分数
     */
    private double calculateFinalScore(double normalizedRrf, LongTermMemoryRecord record, LocalDateTime now) {
        double ageDays = Math.max(0.0D, Duration.between(record.getUpdatedAt(), now).toHours() / 24.0D);
        double recency = Math.pow(0.5D, ageDays / Math.max(1, agentProperties.getLtm().getRecencyHalfLifeDays()));
        return 0.6D * normalizedRrf + 0.2D * record.getConfidence()
                + 0.1D * recency + 0.1D * typeBoost(record.getMemoryType());
    }

    /**
     * 记忆类型权重。
     *
     * @param type 类型
     * @return 权重
     */
    private double typeBoost(LongTermMemoryType type) {
        return switch (type) {
            case PREFERENCE -> 1.0D;
            case FACT -> 0.85D;
            case EPISODE -> 0.75D;
            case PROCEDURE -> 0.65D;
        };
    }
}

