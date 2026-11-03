package com.luzhiwen.collectagent.memory.domain;

/**
 * 注入 Agent 上下文的长期记忆视图。
 */
public class LongTermMemoryView {

    /** 记忆ID。 */
    private final String memoryId;
    /** 记忆类型。 */
    private final LongTermMemoryType memoryType;
    /** 记忆内容。 */
    private final String content;
    /** 置信度。 */
    private final double confidence;
    /** 最终重排得分。 */
    private final double score;

    /**
     * 创建长期记忆视图。
     *
     * @param memoryId 记忆ID
     * @param memoryType 类型
     * @param content 内容
     * @param confidence 置信度
     * @param score 重排得分
     */
    public LongTermMemoryView(String memoryId, LongTermMemoryType memoryType, String content, double confidence, double score) {
        this.memoryId = memoryId;
        this.memoryType = memoryType;
        this.content = content;
        this.confidence = confidence;
        this.score = score;
    }

    /** @return 记忆ID。 */ public String getMemoryId() { return memoryId; }
    /** @return 记忆类型。 */ public LongTermMemoryType getMemoryType() { return memoryType; }
    /** @return 内容。 */ public String getContent() { return content; }
    /** @return 置信度。 */ public double getConfidence() { return confidence; }
    /** @return 得分。 */ public double getScore() { return score; }
}
