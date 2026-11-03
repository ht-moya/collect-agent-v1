package com.luzhiwen.collectagent.memory.domain;

/**
 * LLM 返回的单个槽位候选补丁。
 *
 * <p>LLM 只负责给出 value 和 confidence，来源、更新时间和引用由后端补充。</p>
 */
public class AgentSlotPatch {

    /** 槽位候选值。 */
    private Object value;

    /** 候选值置信度，范围为 0 到 1。 */
    private Double confidence;

    /**
     * 创建空槽位补丁，供 JSON 反序列化和测试使用。
     */
    public AgentSlotPatch() {
    }

    /**
     * 创建槽位补丁。
     *
     * @param value 槽位候选值
     * @param confidence 置信度
     */
    public AgentSlotPatch(Object value, Double confidence) {
        this.value = value;
        this.confidence = confidence;
    }

    /** @return 槽位候选值。 */
    public Object getValue() { return value; }

    /** @param value 槽位候选值。 */
    public void setValue(Object value) { this.value = value; }

    /** @return 置信度。 */
    public Double getConfidence() { return confidence; }

    /** @param confidence 置信度。 */
    public void setConfidence(Double confidence) { this.confidence = confidence; }
}
