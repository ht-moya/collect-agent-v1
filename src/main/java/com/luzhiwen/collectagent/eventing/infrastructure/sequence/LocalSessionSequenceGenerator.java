package com.luzhiwen.collectagent.eventing.infrastructure.sequence;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.luzhiwen.collectagent.eventing.application.SessionSequenceGenerator;
import com.luzhiwen.collectagent.eventing.infrastructure.persistence.repository.EventLayerRepository;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于本地内存和数据库最大值的会话序号生成器。
 *
 * <p>当前项目没有 Redis，因此先在单 JVM 内保证并发安全；首次获取序号时查询数据库 MAX(session_seq) 兜底。
 * 后续切换 Redis 时，用 Redis INCR 实现同名接口即可。</p>
 */
@Component
public class LocalSessionSequenceGenerator implements SessionSequenceGenerator {

    /** 会话序号缓存，键为会话标识，值为当前已分配的最大序号。 */
    private final Cache<String, AtomicLong> sequenceCache;

    /** 原始事件数据访问接口，用于缓存未命中时读取数据库最大值。 */
    private final EventLayerRepository eventLayerRepository;

    /**
     * 创建本地会话序号生成器。
     *
     * @param eventLayerRepository 原始事件数据访问接口
     */
    public LocalSessionSequenceGenerator(EventLayerRepository eventLayerRepository) {
        this.eventLayerRepository = eventLayerRepository;
        this.sequenceCache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterAccess(java.time.Duration.ofHours(2))
                .build();
    }

    /**
     * 获取指定会话的下一个序号。
     *
     * @param sessionId 会话唯一标识
     * @return 下一个会话内单调递增序号
     */
    @Override
    public long next(String sessionId) {
        AtomicLong sequence = sequenceCache.get(sessionId, this::loadLatestSequence);
        return sequence.incrementAndGet();
    }

    /**
     * 从数据库加载当前会话最大序号。
     *
     * @param sessionId 会话唯一标识
     * @return 当前已使用的最大序号
     */
    private AtomicLong loadLatestSequence(String sessionId) {
        long databaseMax = eventLayerRepository.findMaxSessionSeqBySessionId(sessionId);
        return new AtomicLong(databaseMax);
    }
}
