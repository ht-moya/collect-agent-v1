package com.luzhiwen.collectagent.eventing.domain;

/**
 * Outbox 事件状态。
 */
public enum OutboxStatus {
    /** 等待处理。 */ PENDING,
    /** 正在处理。 */ PROCESSING,
    /** 处理成功。 */ PROCESSED,
    /** 处理失败。 */ FAILED
}
