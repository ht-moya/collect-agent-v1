package com.luzhiwen.collectagent.eventing.infrastructure.persistence.repository;

import com.luzhiwen.collectagent.eventing.domain.EventType;
import com.luzhiwen.collectagent.eventing.infrastructure.persistence.entity.EventLayerEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 原始事件层数据访问接口。
 */
public interface EventLayerRepository extends JpaRepository<EventLayerEntity, Long> {

    /**
     * 根据事件唯一标识查询事件。
     *
     * @param eventId 事件唯一标识
     * @return 事件实体
     */
    Optional<EventLayerEntity> findByEventId(String eventId);

    /**
     * 按数据库主键升序查询会话全部事件，主要用于兼容历史标准消息回放。
     *
     * @param sessionId 会话唯一标识
     * @return 事件列表
     */
    List<EventLayerEntity> findBySessionIdOrderByIdAsc(String sessionId);

    /**
     * 按会话内序号升序查询会话全部事件。
     *
     * @param sessionId 会话唯一标识
     * @return 事件列表
     */
    List<EventLayerEntity> findBySessionIdOrderBySessionSeqAsc(String sessionId);

    /**
     * 查询会话最近事件，按会话内序号倒序。
     *
     * @param sessionId 会话唯一标识
     * @param pageable 分页参数
     * @return 最近事件列表
     */
    List<EventLayerEntity> findBySessionIdOrderBySessionSeqDesc(String sessionId, Pageable pageable);

    /**
     * 查询指定序号之后的会话事件，按会话内序号升序。
     *
     * @param sessionId 会话唯一标识
     * @param sessionSeq 已处理的最后序号
     * @return 尚未处理的事件列表
     */
    List<EventLayerEntity> findBySessionIdAndSessionSeqGreaterThanOrderBySessionSeqAsc(
            String sessionId,
            Long sessionSeq
    );

    /**
     * 查询指定会话序号区间内的事件。
     *
     * @param sessionId 会话ID
     * @param fromExclusive 起始序号，不含
     * @param toInclusive 结束序号，包含
     * @return 事件列表
     */
    List<EventLayerEntity> findBySessionIdAndSessionSeqGreaterThanAndSessionSeqLessThanEqualOrderBySessionSeqAsc(
            String sessionId,
            Long fromExclusive,
            Long toInclusive
    );

    /**
     * 统计区间内指定类型事件数量。
     *
     * @param sessionId 会话ID
     * @param eventType 事件类型
     * @param fromExclusive 起始序号，不含
     * @param toInclusive 结束序号，包含
     * @return 事件数量
     */
    long countBySessionIdAndEventTypeAndSessionSeqGreaterThanAndSessionSeqLessThanEqual(
            String sessionId,
            EventType eventType,
            Long fromExclusive,
            Long toInclusive
    );

    /**
     * 查询会话当前最大的会话内序号。
     *
     * @param sessionId 会话唯一标识
     * @return 最大序号，没有事件时返回 0
     */
    @Query("select coalesce(max(e.sessionSeq), 0) from EventLayerEntity e where e.sessionId = :sessionId")
    long findMaxSessionSeqBySessionId(@Param("sessionId") String sessionId);

    /**
     * 查询客户全部事件，按数据库主键倒序。
     *
     * @param customerId 客户唯一标识
     * @param pageable 分页参数
     * @return 事件列表
     */
    List<EventLayerEntity> findByCustomerIdOrderByIdDesc(String customerId, Pageable pageable);
}


