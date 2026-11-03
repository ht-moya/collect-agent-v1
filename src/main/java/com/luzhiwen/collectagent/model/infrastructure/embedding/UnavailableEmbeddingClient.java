package com.luzhiwen.collectagent.model.infrastructure.embedding;

import com.luzhiwen.collectagent.model.application.EmbeddingClient;
import com.luzhiwen.collectagent.shared.config.AgentProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Embedding 关闭时使用的占位客户端。
 */
@Component
@ConditionalOnProperty(prefix = "agent.embedding", name = "enabled", havingValue = "false", matchIfMissing = true)
public class UnavailableEmbeddingClient implements EmbeddingClient {

    /** Agent 配置。 */
    private final AgentProperties agentProperties;

    /**
     * 创建不可用 Embedding 客户端。
     *
     * @param agentProperties Agent 配置
     */
    public UnavailableEmbeddingClient(AgentProperties agentProperties) {
        this.agentProperties = agentProperties;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAvailable() { return false; }

    /** {@inheritDoc} */
    @Override
    public String modelName() { return agentProperties.getEmbedding().getModel(); }

    /** {@inheritDoc} */
    @Override
    public int dimension() { return agentProperties.getEmbedding().getDimension(); }

    /** {@inheritDoc} */
    @Override
    public List<List<Float>> embed(List<String> texts) {
        throw new IllegalStateException("Embedding 服务未启用");
    }
}
