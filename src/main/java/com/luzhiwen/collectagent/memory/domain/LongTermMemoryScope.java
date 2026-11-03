package com.luzhiwen.collectagent.memory.domain;

/**
 * 长期记忆作用域常量。
 */
public final class LongTermMemoryScope {

    /** 用户级，可跨会话召回。 */
    public static final String USER = "user";

    /** 单个会话级。 */
    public static final String SESSION = "session";

    /** 全局级。 */
    public static final String GLOBAL = "global";

    /** 工具类不允许实例化。 */
    private LongTermMemoryScope() {
    }
}
