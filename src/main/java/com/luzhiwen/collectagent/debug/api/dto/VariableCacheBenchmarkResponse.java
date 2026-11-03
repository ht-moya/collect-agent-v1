package com.luzhiwen.collectagent.debug.api.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 动态变量缓存命中 A/B 测试响应。
 */
public class VariableCacheBenchmarkResponse {

    /** 测试变体。 */
    private String variant;

    /** 运行标识。 */
    private String runId;

    /** 当前变量定义指纹。 */
    private String variableFingerprint;

    /** 所有采样输入 Token 总数。 */
    private int totalPromptTokens;

    /** 所有采样命中缓存 Token 总数。 */
    private int totalCachedTokens;

    /** 平均缓存命中率。 */
    private double averageCacheHitRate;

    /** 单次采样明细。 */
    private List<VariableCacheBenchmarkSample> samples = new ArrayList<>();

    /** @return 测试变体。 */
    public String getVariant() { return variant; }

    /** @param variant 测试变体。 */
    public void setVariant(String variant) { this.variant = variant; }

    /** @return 运行标识。 */
    public String getRunId() { return runId; }

    /** @param runId 运行标识。 */
    public void setRunId(String runId) { this.runId = runId; }

    /** @return 变量定义指纹。 */
    public String getVariableFingerprint() { return variableFingerprint; }

    /** @param variableFingerprint 变量定义指纹。 */
    public void setVariableFingerprint(String variableFingerprint) { this.variableFingerprint = variableFingerprint; }

    /** @return 输入 Token 总数。 */
    public int getTotalPromptTokens() { return totalPromptTokens; }

    /** @param totalPromptTokens 输入 Token 总数。 */
    public void setTotalPromptTokens(int totalPromptTokens) { this.totalPromptTokens = totalPromptTokens; }

    /** @return 命中缓存 Token 总数。 */
    public int getTotalCachedTokens() { return totalCachedTokens; }

    /** @param totalCachedTokens 命中缓存 Token 总数。 */
    public void setTotalCachedTokens(int totalCachedTokens) { this.totalCachedTokens = totalCachedTokens; }

    /** @return 平均缓存命中率。 */
    public double getAverageCacheHitRate() { return averageCacheHitRate; }

    /** @param averageCacheHitRate 平均缓存命中率。 */
    public void setAverageCacheHitRate(double averageCacheHitRate) { this.averageCacheHitRate = averageCacheHitRate; }

    /** @return 单次采样明细。 */
    public List<VariableCacheBenchmarkSample> getSamples() { return samples; }

    /** @param samples 单次采样明细。 */
    public void setSamples(List<VariableCacheBenchmarkSample> samples) {
        this.samples = samples == null ? new ArrayList<>() : samples;
    }
}
