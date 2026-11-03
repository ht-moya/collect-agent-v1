package com.luzhiwen.collectagent.memory.domain;

import java.util.List;
import java.util.Map;

/**
 * Agent 会话状态规则。
 *
 * <p>统一维护可写槽位、阶段迁移和阶段必填槽位，避免状态服务与状态视图重复定义。</p>
 */
public final class AgentStatePolicy {

    /** LLM 可写的槽位白名单。 */
    public static final List<String> ALLOWED_PATCH_FIELDS = List.of(
            "promise_date",
            "promise_amount",
            "repayment_intent",
            "refuse_reason",
            "hardship_reason",
            "contact_preference"
    );

    /** 各阶段允许迁移到的下一阶段。 */
    private static final Map<AgentStage, List<AgentStage>> ALLOWED_TRANSITIONS = Map.of(
            AgentStage.OPENING, List.of(AgentStage.REMINDING),
            AgentStage.REMINDING, List.of(AgentStage.NEGOTIATING),
            AgentStage.NEGOTIATING, List.of(AgentStage.CONFIRMING),
            AgentStage.CONFIRMING, List.of(AgentStage.NEGOTIATING, AgentStage.CLOSED),
            AgentStage.CLOSED, List.of()
    );

    /** 工具类不允许实例化。 */
    private AgentStatePolicy() {
    }

    /**
     * 获取某阶段允许迁移到的下一阶段列表。
     *
     * @param stage 当前阶段
     * @return 允许迁移的阶段列表
     */
    public static List<AgentStage> allowedTransitions(AgentStage stage) {
        return ALLOWED_TRANSITIONS.getOrDefault(stage, List.of());
    }

    /**
     * 判断阶段迁移是否允许。
     *
     * @param from 当前阶段
     * @param to 目标阶段
     * @return true 表示允许迁移
     */
    public static boolean canTransition(AgentStage from, AgentStage to) {
        return from == to || allowedTransitions(from).contains(to);
    }

    /**
     * 获取某阶段当前必须收集的槽位。
     *
     * @param stage 当前阶段
     * @return 必填槽位列表
     */
    public static List<String> requiredSlots(AgentStage stage) {
        return switch (stage) {
            case REMINDING -> List.of("repayment_intent");
            case NEGOTIATING -> List.of("promise_date", "promise_amount", "repayment_intent");
            default -> List.of();
        };
    }
}
