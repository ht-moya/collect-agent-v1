package com.luzhiwen.collectagent.model.application;

import java.util.List;

/**
 * 文本向量化客户端。
 */
public interface EmbeddingClient {

    /**
     * 判断 Embedding 服务是否可用。
     *
     * @return true 表示可用
     */
    boolean isAvailable();

    /** @return 当前向量模型名称。 */
    String modelName();

    /** @return 向量维度。 */
    int dimension();

    /**
     * 批量生成向量。
     *
     * @param texts 文本列表
     * @return 与输入顺序一致的向量列表
     */
    List<List<Float>> embed(List<String> texts);
}
