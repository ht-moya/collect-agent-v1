package com.luzhiwen.collectagent.memory.domain;

/**
 * 智能催收会话阶段。
 */
public enum AgentStage {

    /** 开场阶段，主要完成身份核验。 */
    OPENING,

    /** 提醒阶段，告知债务和逾期信息。 */
    REMINDING,

    /** 协商阶段，收集还款意愿、日期和金额。 */
    NEGOTIATING,

    /** 确认阶段，复述并确认客户承诺。 */
    CONFIRMING,

    /** 已结束阶段。 */
    CLOSED
}
