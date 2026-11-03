package com.luzhiwen.collectagent.debug.api.dto;

/**
 * 动态变量缓存命中 A/B 测试请求。
 */
public class VariableCacheBenchmarkRequest {

    /** 测试变体：A 为工具回填变量，B 为系统提示词直接注入变量。 */
    private String variant;

    /** 每个变体重复调用模型的次数。 */
    private int iterations = 6;

    /** 同一轮基准测试的运行标识，用于稳定缓存前缀。 */
    private String runId;

    /** 两次模型调用之间的等待毫秒数。 */
    private long intervalMs = 600L;

    /** 最终发送给模型的用户消息。 */
    private String userMessage = "不要调用工具，只回复 OK。";

    /** @return 测试变体。 */
    public String getVariant() { return variant; }

    /** @param variant 测试变体。 */
    public void setVariant(String variant) { this.variant = variant; }

    /** @return 重复调用次数。 */
    public int getIterations() { return iterations; }

    /** @param iterations 重复调用次数。 */
    public void setIterations(int iterations) { this.iterations = iterations; }

    /** @return 运行标识。 */
    public String getRunId() { return runId; }

    /** @param runId 运行标识。 */
    public void setRunId(String runId) { this.runId = runId; }

    /** @return 调用间隔毫秒数。 */
    public long getIntervalMs() { return intervalMs; }

    /** @param intervalMs 调用间隔毫秒数。 */
    public void setIntervalMs(long intervalMs) { this.intervalMs = intervalMs; }

    /** @return 最终用户消息。 */
    public String getUserMessage() { return userMessage; }

    /** @param userMessage 最终用户消息。 */
    public void setUserMessage(String userMessage) { this.userMessage = userMessage; }
}
