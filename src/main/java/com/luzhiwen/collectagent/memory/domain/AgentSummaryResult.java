package com.luzhiwen.collectagent.memory.domain;

/**
 * 摘要生成结果。
 */
public class AgentSummaryResult {

    /** 结构化摘要文本。 */
    private final String summary;

    /** 生成摘要使用的模型名称。 */
    private final String modelName;

    /** 摘要估算 Token 数。 */
    private final int tokenCount;

    /**
     * 创建摘要生成结果。
     *
     * @param summary 结构化摘要文本
     * @param modelName 模型名称
     * @param tokenCount 摘要估算 Token 数
     */
    public AgentSummaryResult(String summary, String modelName, int tokenCount) {
        this.summary = summary;
        this.modelName = modelName;
        this.tokenCount = tokenCount;
    }

    /** @return 结构化摘要文本。 */
    public String getSummary() { return summary; }

    /** @return 模型名称。 */
    public String getModelName() { return modelName; }

    /** @return 摘要估算 Token 数。 */
    public int getTokenCount() { return tokenCount; }
}
