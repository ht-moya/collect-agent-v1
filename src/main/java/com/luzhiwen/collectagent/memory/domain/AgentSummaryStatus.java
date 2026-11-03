package com.luzhiwen.collectagent.memory.domain;

/**
 * 会话摘要状态常量。
 *
 * <p>数据库存储小写字符串，便于与 SQL 设计保持一致。</p>
 */
public final class AgentSummaryStatus {

    /** 当前可用摘要。 */
    public static final String ACTIVE = "active";

    /** 已被更新摘要替代。 */
    public static final String SUPERSEDED = "superseded";

    /** 摘要内容校验失败。 */
    public static final String INVALID = "invalid";

    /** 工具类不允许实例化。 */
    private AgentSummaryStatus() {
    }
}
