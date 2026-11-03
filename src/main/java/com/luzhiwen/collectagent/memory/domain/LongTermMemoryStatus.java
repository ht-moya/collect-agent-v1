package com.luzhiwen.collectagent.memory.domain;

/**
 * 长期记忆状态常量。
 */
public final class LongTermMemoryStatus {

    /** 当前有效。 */
    public static final String ACTIVE = "active";

    /** 已被新版本替代。 */
    public static final String SUPERSEDED = "superseded";

    /** 已删除。 */
    public static final String DELETED = "deleted";

    /** 工具类不允许实例化。 */
    private LongTermMemoryStatus() {
    }
}
