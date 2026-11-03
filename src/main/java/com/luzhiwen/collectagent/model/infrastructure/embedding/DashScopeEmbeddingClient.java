package com.luzhiwen.collectagent.model.infrastructure.embedding;

import com.luzhiwen.collectagent.model.application.EmbeddingClient;
import com.luzhiwen.collectagent.shared.config.AgentProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于阿里百炼 OpenAI 兼容接口的 Embedding 客户端。
 */
@Component
@ConditionalOnProperty(prefix = "agent.embedding", name = "enabled", havingValue = "true")
public class DashScopeEmbeddingClient implements EmbeddingClient {

    /** Agent 配置。 */
    private final AgentProperties agentProperties;

    /** JSON 序列化器。 */
    private final ObjectMapper objectMapper;

    /** HTTP 客户端。 */
    private final RestClient restClient;

    /**
     * 创建 DashScope Embedding 客户端。
     *
     * @param agentProperties Agent 配置
     * @param objectMapper JSON 序列化器
     */
    public DashScopeEmbeddingClient(AgentProperties agentProperties, ObjectMapper objectMapper) {
        this.agentProperties = agentProperties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(agentProperties.getEmbedding().getTimeoutSeconds()));
        requestFactory.setReadTimeout(Duration.ofSeconds(agentProperties.getEmbedding().getTimeoutSeconds()));
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(normalizeBaseUrl(agentProperties.getEmbedding().getBaseUrl()))
                .requestFactory(requestFactory);
        if (isAvailable()) {
            builder.defaultHeader("Authorization", "Bearer " + agentProperties.getEmbedding().getApiKey());
        }
        this.restClient = builder.build();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAvailable() {
        AgentProperties.Embedding embedding = agentProperties.getEmbedding();
        return embedding.isEnabled() && embedding.getApiKey() != null && !embedding.getApiKey().isBlank();
    }

    /** {@inheritDoc} */
    @Override
    public String modelName() { return agentProperties.getEmbedding().getModel(); }

    /** {@inheritDoc} */
    @Override
    public int dimension() { return agentProperties.getEmbedding().getDimension(); }

    /** {@inheritDoc} */
    @Override
    public List<List<Float>> embed(List<String> texts) {
        if (!isAvailable()) {
            throw new IllegalStateException("Embedding 服务未启用或未配置 DASHSCOPE_API_KEY");
        }
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        List<List<Float>> result = new ArrayList<>();
        int batchSize = Math.max(1, agentProperties.getEmbedding().getBatchSize());
        for (int start = 0; start < texts.size(); start += batchSize) {
            int end = Math.min(start + batchSize, texts.size());
            result.addAll(embedBatch(texts.subList(start, end)));
        }
        return result;
    }

    /**
     * 调用单批 Embedding。
     *
     * @param texts 文本列表
     * @return 向量列表
     */
    private List<List<Float>> embedBatch(List<String> texts) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", modelName());
        request.put("input", texts);
        request.put("dimensions", dimension());
        request.put("encoding_format", "float");
        String response = restClient.post()
                .uri("embeddings")
                .header("Content-Type", "application/json")
                .body(serialize(request))
                .retrieve()
                .body(String.class);
        return parseResponse(response, texts.size());
    }

    /**
     * 解析 Embedding 响应。
     *
     * @param response 响应 JSON
     * @param expectedCount 预期向量数
     * @return 向量列表
     */
    private List<List<Float>> parseResponse(String response, int expectedCount) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.path("data");
            if (!data.isArray() || data.size() != expectedCount) {
                throw new IllegalStateException("Embedding 返回数量不正确");
            }
            List<List<Float>> vectors = new ArrayList<>();
            for (JsonNode item : data) {
                JsonNode embeddingNode = item.path("embedding");
                if (!embeddingNode.isArray() || embeddingNode.size() != dimension()) {
                    throw new IllegalStateException("Embedding 返回维度不正确");
                }
                List<Float> vector = new ArrayList<>(embeddingNode.size());
                for (JsonNode value : embeddingNode) {
                    vector.add((float) value.asDouble());
                }
                vectors.add(vector);
            }
            return vectors;
        } catch (JacksonException exception) {
            throw new IllegalStateException("Embedding 响应解析失败", exception);
        }
    }

    /**
     * 序列化请求。
     *
     * @param request 请求对象
     * @return JSON
     */
    private String serialize(Map<String, Object> request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Embedding 请求序列化失败", exception);
        }
    }

    /**
     * 确保 baseUrl 以斜杠结尾。
     *
     * @param baseUrl 基础地址
     * @return 规范化地址
     */
    private String normalizeBaseUrl(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }
}
