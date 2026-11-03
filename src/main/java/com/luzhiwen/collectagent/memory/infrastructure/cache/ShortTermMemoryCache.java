package com.luzhiwen.collectagent.memory.infrastructure.cache;

import java.util.Optional;

/**
 * 短期记忆缓存抽象。
 *
 * <p>当前只有 Caffeine 实现，后续可新增 Redis 实现而不修改业务服务。</p>
 */
public interface ShortTermMemoryCache {

    /**
     * 读取缓存内容。
     *
     * @param key 缓存键
     * @return 缓存 JSON
     */
    Optional<String> get(String key);

    /**
     * 写入缓存内容。
     *
     * @param key 缓存键
     * @param value 缓存 JSON
     */
    void put(String key, String value);

    /**
     * 删除缓存内容。
     *
     * @param key 缓存键
     */
    void invalidate(String key);
}
