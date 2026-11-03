package com.luzhiwen.collectagent.debug.api.dto;

/**
 * 单次动态变量缓存命中采样结果。
 */
public class VariableCacheBenchmarkSample {

    /** 采样序号，从 1 开始。 */
    private int iteration;

    /** 本次请求的输入 Token 数。 */
    private int promptTokens;

    /** 命中前缀缓存的 Token 数。 */
    private int cachedTokens;

    /** 未命中缓存的输入 Token 数。 */
    private int cacheMissTokens;

    /** 缓存命中率，范围为 0 到 1。 */
    private double cacheHitRate;

    /** 模型调用耗时毫秒数。 */
    private long latencyMs;

    /** @return 采样序号。 */
    public int getIteration() { return iteration; }

    /** @param iteration 采样序号。 */
    public void setIteration(int iteration) { this.iteration = iteration; }

    /** @return 输入 Token 数。 */
    public int getPromptTokens() { return promptTokens; }

    /** @param promptTokens 输入 Token 数。 */
    public void setPromptTokens(int promptTokens) { this.promptTokens = promptTokens; }

    /** @return 命中缓存 Token 数。 */
    public int getCachedTokens() { return cachedTokens; }

    /** @param cachedTokens 命中缓存 Token 数。 */
    public void setCachedTokens(int cachedTokens) { this.cachedTokens = cachedTokens; }

    /** @return 未命中缓存 Token 数。 */
    public int getCacheMissTokens() { return cacheMissTokens; }

    /** @param cacheMissTokens 未命中缓存 Token 数。 */
    public void setCacheMissTokens(int cacheMissTokens) { this.cacheMissTokens = cacheMissTokens; }

    /** @return 缓存命中率。 */
    public double getCacheHitRate() { return cacheHitRate; }

    /** @param cacheHitRate 缓存命中率。 */
    public void setCacheHitRate(double cacheHitRate) { this.cacheHitRate = cacheHitRate; }

    /** @return 模型调用耗时。 */
    public long getLatencyMs() { return latencyMs; }

    /** @param latencyMs 模型调用耗时。 */
    public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }
}
