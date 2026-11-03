package com.luzhiwen.collectagent.memory.domain;

import java.util.List;
import java.util.Map;

/**
 * LLM 抽取出的长期记忆候选。
 */
public class LongTermMemoryCandidate {

    /** 记忆类型。 */
    private final LongTermMemoryType memoryType;

    /** 自然语言内容。 */
    private final String content;

    /** 结构化字段。 */
    private final Map<String, Object> structured;

    /** 来源事件ID。 */
    private final List<String> sourceEventIds;

    /** 置信度。 */
    private final double confidence;

    /**
     * 创建长期记忆候选。
     *
     * @param memoryType 记忆类型
     * @param content 内容
     * @param structured 结构化字段
     * @param sourceEventIds 来源事件ID
     * @param confidence 置信度
     */
    public LongTermMemoryCandidate(
            LongTermMemoryType memoryType,
            String content,
            Map<String, Object> structured,
            List<String> sourceEventIds,
            double confidence
    ) {
        this.memoryType = memoryType;
        this.content = content;
        this.structured = structured;
        this.sourceEventIds = sourceEventIds;
        this.confidence = confidence;
    }

    /** @return 记忆类型。 */
    public LongTermMemoryType getMemoryType() { return memoryType; }

    /** @return 内容。 */
    public String getContent() { return content; }

    /** @return 结构化字段。 */
    public Map<String, Object> getStructured() { return structured; }

    /** @return 来源事件ID。 */
    public List<String> getSourceEventIds() { return sourceEventIds; }

    /** @return 置信度。 */
    public double getConfidence() { return confidence; }
}
