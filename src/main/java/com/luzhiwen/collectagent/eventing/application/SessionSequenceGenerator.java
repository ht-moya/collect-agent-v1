package com.luzhiwen.collectagent.eventing.application;

/**
 * 会话内事件序号生成器。
 *
 * <p>当前使用本地实现；后续接入 Redis 时可新增 Redis 实现并替换 Spring Bean，不改调用方。</p>
 */
public interface SessionSequenceGenerator {

    /**
     * 获取指定会话的下一个单调递增序号。
     *
     * @param sessionId 会话唯一标识
     * @return 下一个会话内序号
     */
    long next(String sessionId);
}
