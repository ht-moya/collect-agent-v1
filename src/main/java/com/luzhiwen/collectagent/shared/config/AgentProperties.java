package com.luzhiwen.collectagent.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 智能催收 Agent 配置。
 */
@ConfigurationProperties(prefix = "agent")
public class AgentProperties {

    /**
     * DeepSeek 大模型配置。
     */
    private Llm llm = new Llm();

    /**
     * Embedding 模型配置。
     */
    private Embedding embedding = new Embedding();

    /**
     * 长期记忆配置。
     */
    private Ltm ltm = new Ltm();

    /**
     * Outbox 配置。
     */
    private Outbox outbox = new Outbox();

    /**
     * 会话生命周期配置。
     */
    private Session session = new Session();

    /**
     * 短期记忆配置。
     */
    private Stm stm = new Stm();

    /**
     * Embedding 模型配置。
     */
    public static class Embedding {

        /** 是否启用 Embedding。 */
        private boolean enabled = false;

        /** Embedding 服务提供方。 */
        private String provider = "dashscope";

        /** OpenAI 兼容接口地址。 */
        private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";

        /** API Key。 */
        private String apiKey = "";

        /** Embedding 模型名称。 */
        private String model = "text-embedding-v4";

        /** 向量维度。 */
        private int dimension = 1024;

        /** 请求超时秒数。 */
        private int timeoutSeconds = 30;

        /** 单次批量向量化文本数量。 */
        private int batchSize = 10;

        /** 是否启用。 @return true 表示启用。 */
        public boolean isEnabled() { return enabled; }

        /** @param enabled 是否启用。 */
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        /** @return 提供方。 */
        public String getProvider() { return provider; }

        /** @param provider 提供方。 */
        public void setProvider(String provider) { this.provider = provider; }

        /** @return 接口地址。 */
        public String getBaseUrl() { return baseUrl; }

        /** @param baseUrl 接口地址。 */
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

        /** @return API Key。 */
        public String getApiKey() { return apiKey; }

        /** @param apiKey API Key。 */
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }

        /** @return 模型名称。 */
        public String getModel() { return model; }

        /** @param model 模型名称。 */
        public void setModel(String model) { this.model = model; }

        /** @return 向量维度。 */
        public int getDimension() { return dimension; }

        /** @param dimension 向量维度。 */
        public void setDimension(int dimension) { this.dimension = dimension; }

        /** @return 超时秒数。 */
        public int getTimeoutSeconds() { return timeoutSeconds; }

        /** @param timeoutSeconds 超时秒数。 */
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

        /** @return 批量大小。 */
        public int getBatchSize() { return batchSize; }

        /** @param batchSize 批量大小。 */
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    }

    /**
     * 长期记忆配置。
     */
    public static class Ltm {

        /** 每多少个完整对话轮次抽取一次。 */
        private int extractionTurnInterval = 10;

        /** 稠密召回数量。 */
        private int denseTopK = 20;

        /** 关键词召回数量。 */
        private int keywordTopK = 20;

        /** 最终注入数量。 */
        private int finalTopK = 5;

        /** RRF 平滑常数。 */
        private int rrfK = 60;

        /** 活跃记忆最低置信度。 */
        private double activeConfidence = 0.7D;

        /** 待审核记忆最低置信度。 */
        private double pendingConfidence = 0.5D;

        /** 时间衰减半衰期天数。 */
        private int recencyHalfLifeDays = 90;

        /** @return 抽取轮次间隔。 */
        public int getExtractionTurnInterval() { return extractionTurnInterval; }

        /** @param extractionTurnInterval 抽取轮次间隔。 */
        public void setExtractionTurnInterval(int extractionTurnInterval) { this.extractionTurnInterval = extractionTurnInterval; }

        /** @return 稠密召回数量。 */
        public int getDenseTopK() { return denseTopK; }

        /** @param denseTopK 稠密召回数量。 */
        public void setDenseTopK(int denseTopK) { this.denseTopK = denseTopK; }

        /** @return 关键词召回数量。 */
        public int getKeywordTopK() { return keywordTopK; }

        /** @param keywordTopK 关键词召回数量。 */
        public void setKeywordTopK(int keywordTopK) { this.keywordTopK = keywordTopK; }

        /** @return 最终注入数量。 */
        public int getFinalTopK() { return finalTopK; }

        /** @param finalTopK 最终注入数量。 */
        public void setFinalTopK(int finalTopK) { this.finalTopK = finalTopK; }

        /** @return RRF 平滑常数。 */
        public int getRrfK() { return rrfK; }

        /** @param rrfK RRF 平滑常数。 */
        public void setRrfK(int rrfK) { this.rrfK = rrfK; }

        /** @return 活跃置信度阈值。 */
        public double getActiveConfidence() { return activeConfidence; }

        /** @param activeConfidence 活跃置信度阈值。 */
        public void setActiveConfidence(double activeConfidence) { this.activeConfidence = activeConfidence; }

        /** @return 待审核置信度阈值。 */
        public double getPendingConfidence() { return pendingConfidence; }

        /** @param pendingConfidence 待审核置信度阈值。 */
        public void setPendingConfidence(double pendingConfidence) { this.pendingConfidence = pendingConfidence; }

        /** @return 半衰期天数。 */
        public int getRecencyHalfLifeDays() { return recencyHalfLifeDays; }

        /** @param recencyHalfLifeDays 半衰期天数。 */
        public void setRecencyHalfLifeDays(int recencyHalfLifeDays) { this.recencyHalfLifeDays = recencyHalfLifeDays; }
    }
    /**
     * 获取 DeepSeek 配置。
     *
     * @return DeepSeek 配置
     */
    public Llm getLlm() {
        return llm;
    }

    /**
     * 设置 DeepSeek 配置。
     *
     * @param llm DeepSeek 配置
     */
    public void setLlm(Llm llm) {
        this.llm = llm;
    }

    /**
     * 获取 Embedding 配置。
     *
     * @return Embedding 配置
     */
    public Embedding getEmbedding() {
        return embedding;
    }

    /**
     * 设置 Embedding 配置。
     *
     * @param embedding Embedding 配置
     */
    public void setEmbedding(Embedding embedding) {
        this.embedding = embedding;
    }

    /**
     * 获取长期记忆配置。
     *
     * @return 长期记忆配置
     */
    public Ltm getLtm() {
        return ltm;
    }

    /**
     * 设置长期记忆配置。
     *
     * @param ltm 长期记忆配置
     */
    public void setLtm(Ltm ltm) {
        this.ltm = ltm;
    }

    /**
     * 获取短期记忆配置。
     *
     * @return 短期记忆配置
     */
    public Stm getStm() {
        return stm;
    }

    /**
     * 设置短期记忆配置。
     *
     * @param stm 短期记忆配置
     */
    public void setStm(Stm stm) {
        this.stm = stm;
    }

    /**
     * 获取会话生命周期配置。
     *
     * @return 会话配置
     */
    public Session getSession() {
        return session;
    }

    /**
     * 设置会话生命周期配置。
     *
     * @param session 会话配置
     */
    public void setSession(Session session) {
        this.session = session;
    }

    /**
     * 获取 Outbox 配置。
     *
     * @return Outbox 配置
     */
    public Outbox getOutbox() {
        return outbox;
    }

    /**
     * 设置 Outbox 配置。
     *
     * @param outbox Outbox 配置
     */
    public void setOutbox(Outbox outbox) {
        this.outbox = outbox;
    }


    /**
     * DeepSeek 大模型配置。
     */
    public static class Llm {

        /**
         * 是否启用 DeepSeek 规划器。
         */
        private boolean enabled = false;

        /**
         * DeepSeek OpenAI 兼容接口地址。
         */
        private String baseUrl = "https://api.deepseek.com";

        /**
         * DeepSeek API Key。
         */
        private String apiKey = "";

        /**
         * DeepSeek 聊天模型名称。
         */
        private String chatModel = "deepseek-chat";

        /**
         * 模型温度。
         */
        private double temperature = 0.2D;

        /**
         * 模型请求超时时间，单位为秒。
         */
        private int timeoutSeconds = 30;

        /**
         * 获取是否启用 DeepSeek。
         *
         * @return true 表示启用
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置是否启用 DeepSeek。
         *
         * @param enabled true 表示启用
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 获取接口地址。
         *
         * @return 接口地址
         */
        public String getBaseUrl() {
            return baseUrl;
        }

        /**
         * 设置接口地址。
         *
         * @param baseUrl 接口地址
         */
        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        /**
         * 获取 API Key。
         *
         * @return API Key
         */
        public String getApiKey() {
            return apiKey;
        }

        /**
         * 设置 API Key。
         *
         * @param apiKey API Key
         */
        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        /**
         * 获取聊天模型名称。
         *
         * @return 聊天模型名称
         */
        public String getChatModel() {
            return chatModel;
        }

        /**
         * 设置聊天模型名称。
         *
         * @param chatModel 聊天模型名称
         */
        public void setChatModel(String chatModel) {
            this.chatModel = chatModel;
        }

        /**
         * 获取模型温度。
         *
         * @return 模型温度
         */
        public double getTemperature() {
            return temperature;
        }

        /**
         * 设置模型温度。
         *
         * @param temperature 模型温度
         */
        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        /**
         * 获取请求超时时间。
         *
         * @return 超时秒数
         */
        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        /**
         * 设置请求超时时间。
         *
         * @param timeoutSeconds 超时秒数
         */
        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }

    /**
     * 短期记忆配置。
     */
    public static class Stm {

        /**
         * 短期记忆中保留的最近事件数量。
         */
        private int recentEventLimit = 20;

        /**
         * 发给模型的历史对话轮次上限。
         */
        private int historyTurnLimit = 6;

        /**
         * Caffeine 短期记忆缓存过期时间，单位为分钟。
         */
        private int cacheTtlMinutes = 30;

        /**
         * 短期摘要最大字符数，规则回退摘要使用。
         */
        private int summaryMaxChars = 500;

        /**
         * 未摘要事件达到该数量后触发结构化压缩。
         */
        private int summaryTriggerEventCount = 12;

        /**
         * 压缩时保留的最近未摘要事件数量。
         */
        private int summaryKeepRecentEventCount = 6;

        /**
         * 轮次结束时，未摘要事件估算 Token 达到该值后触发结构化压缩。
         */
        private int summaryTriggerTokenEstimate = 4000;

        /**
         * 主 Planner 调用前的上下文 Token 预算。
         */
        private int contextBudgetTokenEstimate = 6000;

        /**
         * 上下文估算时每个 Token 对应的字符数。
         */
        private int contextCharsPerToken = 2;

        /**
         * 摘要提示词版本。
         */
        private String summaryPromptVersion = "v1";

        /**
         * 获取最近事件数量。
         *
         * @return 最近事件数量
         */
        public int getRecentEventLimit() {
            return recentEventLimit;
        }

        /**
         * 设置最近事件数量。
         *
         * @param recentEventLimit 最近事件数量
         */
        public void setRecentEventLimit(int recentEventLimit) {
            this.recentEventLimit = recentEventLimit;
        }

        /**
         * 获取发送给模型的历史对话轮次上限。
         *
         * @return 历史对话轮次上限
         */
        public int getHistoryTurnLimit() {
            return historyTurnLimit;
        }

        /**
         * 设置发送给模型的历史对话轮次上限。
         *
         * @param historyTurnLimit 历史对话轮次上限
         */
        public void setHistoryTurnLimit(int historyTurnLimit) {
            this.historyTurnLimit = historyTurnLimit;
        }

        /**
         * 获取缓存过期分钟数。
         *
         * @return 缓存过期分钟数
         */
        public int getCacheTtlMinutes() {
            return cacheTtlMinutes;
        }

        /**
         * 设置缓存过期分钟数。
         *
         * @param cacheTtlMinutes 缓存过期分钟数
         */
        public void setCacheTtlMinutes(int cacheTtlMinutes) {
            this.cacheTtlMinutes = cacheTtlMinutes;
        }

        /**
         * 获取摘要触发的未摘要事件数量。
         *
         * @return 事件数量
         */
        public int getSummaryTriggerEventCount() {
            return summaryTriggerEventCount;
        }

        /**
         * 设置摘要触发的未摘要事件数量。
         *
         * @param summaryTriggerEventCount 事件数量
         */
        public void setSummaryTriggerEventCount(int summaryTriggerEventCount) {
            this.summaryTriggerEventCount = summaryTriggerEventCount;
        }

        /**
         * 获取压缩时保留的最近事件数量。
         *
         * @return 最近事件数量
         */
        public int getSummaryKeepRecentEventCount() {
            return summaryKeepRecentEventCount;
        }

        /**
         * 设置压缩时保留的最近事件数量。
         *
         * @param summaryKeepRecentEventCount 最近事件数量
         */
        public void setSummaryKeepRecentEventCount(int summaryKeepRecentEventCount) {
            this.summaryKeepRecentEventCount = summaryKeepRecentEventCount;
        }

        /**
         * 获取摘要触发 Token 阈值。
         *
         * @return Token 阈值
         */
        public int getSummaryTriggerTokenEstimate() {
            return summaryTriggerTokenEstimate;
        }

        /**
         * 设置摘要触发 Token 阈值。
         *
         * @param summaryTriggerTokenEstimate Token 阈值
         */
        public void setSummaryTriggerTokenEstimate(int summaryTriggerTokenEstimate) {
            this.summaryTriggerTokenEstimate = summaryTriggerTokenEstimate;
        }

        /**
         * 获取上下文预算 Token 阈值。
         *
         * @return 上下文预算 Token 阈值
         */
        public int getContextBudgetTokenEstimate() {
            return contextBudgetTokenEstimate;
        }

        /**
         * 设置上下文预算 Token 阈值。
         *
         * @param contextBudgetTokenEstimate 上下文预算 Token 阈值
         */
        public void setContextBudgetTokenEstimate(int contextBudgetTokenEstimate) {
            this.contextBudgetTokenEstimate = contextBudgetTokenEstimate;
        }

        /**
         * 获取上下文估算时每个 Token 对应的字符数。
         *
         * @return 每个 Token 对应的字符数
         */
        public int getContextCharsPerToken() {
            return contextCharsPerToken;
        }

        /**
         * 设置上下文估算时每个 Token 对应的字符数。
         *
         * @param contextCharsPerToken 每个 Token 对应的字符数
         */
        public void setContextCharsPerToken(int contextCharsPerToken) {
            this.contextCharsPerToken = contextCharsPerToken;
        }

        /**
         * 获取摘要提示词版本。
         *
         * @return 提示词版本
         */
        public String getSummaryPromptVersion() {
            return summaryPromptVersion;
        }

        /**
         * 设置摘要提示词版本。
         *
         * @param summaryPromptVersion 提示词版本
         */
        public void setSummaryPromptVersion(String summaryPromptVersion) {
            this.summaryPromptVersion = summaryPromptVersion;
        }

        /**
         * 获取短期摘要最大字符数。
         *
         * @return 最大字符数
         */
        public int getSummaryMaxChars() {
            return summaryMaxChars;
        }

        /**
         * 设置短期摘要最大字符数。
         *
         * @param summaryMaxChars 最大字符数
         */
        public void setSummaryMaxChars(int summaryMaxChars) {
            this.summaryMaxChars = summaryMaxChars;
        }
    }

    /**
     * 会话生命周期配置。
     */
    public static class Session {

        /** 是否自动关闭超时会话。 */
        private boolean autoCloseEnabled = true;

        /** 会话超时分钟数。 */
        private long timeoutMinutes = 30L;

        /** 超时扫描间隔毫秒数。 */
        private long scanIntervalMs = 60_000L;

        /** @return 是否自动关闭超时会话。 */
        public boolean isAutoCloseEnabled() { return autoCloseEnabled; }

        /** @param autoCloseEnabled 是否自动关闭。 */
        public void setAutoCloseEnabled(boolean autoCloseEnabled) { this.autoCloseEnabled = autoCloseEnabled; }

        /** @return 超时分钟数。 */
        public long getTimeoutMinutes() { return timeoutMinutes; }

        /** @param timeoutMinutes 超时分钟数。 */
        public void setTimeoutMinutes(long timeoutMinutes) { this.timeoutMinutes = timeoutMinutes; }

        /** @return 扫描间隔毫秒数。 */
        public long getScanIntervalMs() { return scanIntervalMs; }

        /** @param scanIntervalMs 扫描间隔毫秒数。 */
        public void setScanIntervalMs(long scanIntervalMs) { this.scanIntervalMs = scanIntervalMs; }
    }
    /**
     * Outbox 配置。
     */
    public static class Outbox {

        /**
         * Relay 扫描间隔毫秒数。
         */
        private long relayIntervalMs = 2000L;

        /**
         * 单次最多处理事件数。
         */
        private int batchSize = 50;

        /**
         * 最大重试次数。
         */
        private int maxRetryCount = 5;

        /**
         * 获取扫描间隔毫秒数。
         *
         * @return 扫描间隔毫秒数
         */
        public long getRelayIntervalMs() {
            return relayIntervalMs;
        }

        /**
         * 设置扫描间隔毫秒数。
         *
         * @param relayIntervalMs 扫描间隔毫秒数
         */
        public void setRelayIntervalMs(long relayIntervalMs) {
            this.relayIntervalMs = relayIntervalMs;
        }

        /**
         * 获取单次批处理数量。
         *
         * @return 批处理数量
         */
        public int getBatchSize() {
            return batchSize;
        }

        /**
         * 设置单次批处理数量。
         *
         * @param batchSize 批处理数量
         */
        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        /**
         * 获取最大重试次数。
         *
         * @return 最大重试次数
         */
        public int getMaxRetryCount() {
            return maxRetryCount;
        }

        /**
         * 设置最大重试次数。
         *
         * @param maxRetryCount 最大重试次数
         */
        public void setMaxRetryCount(int maxRetryCount) {
            this.maxRetryCount = maxRetryCount;
        }
    }

}







