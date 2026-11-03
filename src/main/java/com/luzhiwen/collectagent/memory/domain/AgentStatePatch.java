package com.luzhiwen.collectagent.memory.domain;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LLM 最终回答中携带的状态变化补丁。
 *
 * <p>LLM 不返回完整 state，只返回阶段和槽位候选；后端负责白名单校验和合并。</p>
 */
public class AgentStatePatch {

    /** 建议变更到的会话阶段，空值表示不修改。 */
    private String stage;

    /** 建议写入的槽位候选。 */
    private Map<String, AgentSlotPatch> slots = new LinkedHashMap<>();

    /**
     * 创建空状态补丁，供 JSON 反序列化和测试使用。
     */
    public AgentStatePatch() {
    }

    /**
     * 创建状态补丁。
     *
     * @param stage 建议阶段
     * @param slots 槽位候选
     */
    public AgentStatePatch(String stage, Map<String, AgentSlotPatch> slots) {
        this.stage = stage;
        this.slots = slots == null ? new LinkedHashMap<>() : new LinkedHashMap<>(slots);
    }

    /** @return 建议阶段。 */
    public String getStage() { return stage; }

    /** @param stage 建议阶段。 */
    public void setStage(String stage) { this.stage = stage; }

    /** @return 槽位候选。 */
    public Map<String, AgentSlotPatch> getSlots() { return slots; }

    /** @param slots 槽位候选。 */
    public void setSlots(Map<String, AgentSlotPatch> slots) {
        this.slots = slots == null ? new LinkedHashMap<>() : slots;
    }

    /** @return 补丁是否为空。 */
    public boolean isEmpty() {
        return (stage == null || stage.isBlank()) && (slots == null || slots.isEmpty());
    }
}
