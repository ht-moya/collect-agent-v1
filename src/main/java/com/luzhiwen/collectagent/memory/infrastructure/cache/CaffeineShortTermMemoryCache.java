package com.luzhiwen.collectagent.memory.infrastructure.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.luzhiwen.collectagent.shared.config.AgentProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * 基于 Caffeine 的本地短期记忆缓存。
 *
 * <p>当前项目不依赖 Redis。缓存采用访问后过期，模拟短期记忆的滑动 TTL。</p>
 */
@Component
public class CaffeineShortTermMemoryCache implements ShortTermMemoryCache {

    /** 本地缓存。 */
    private final Cache<String, String> cache;

    /**
     * 创建 Caffeine 短期记忆缓存。
     *
     * @param agentProperties Agent 配置
     */
    public CaffeineShortTermMemoryCache(AgentProperties agentProperties) {
        this.cache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterAccess(Duration.ofMinutes(agentProperties.getStm().getCacheTtlMinutes()))
                .recordStats()
                .build();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<String> get(String key) {
        return Optional.ofNullable(cache.getIfPresent(key));
    }

    /** {@inheritDoc} */
    @Override
    public void put(String key, String value) {
        cache.put(key, value);
    }

    /** {@inheritDoc} */
    @Override
    public void invalidate(String key) {
        cache.invalidate(key);
    }
}
