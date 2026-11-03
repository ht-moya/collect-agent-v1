package com.luzhiwen.collectagent.shared.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 本地缓存配置。
 */
@Configuration
public class CacheConfig {

    /**
     * 创建 Caffeine 缓存管理器。
     *
     * @param agentProperties Agent 配置
     * @return Caffeine 缓存管理器
     */
    @Bean
    public CacheManager cacheManager(AgentProperties agentProperties) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("shortTermMemoryCache");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterAccess(Duration.ofMinutes(agentProperties.getStm().getCacheTtlMinutes()))
                .recordStats());
        return cacheManager;
    }
}


