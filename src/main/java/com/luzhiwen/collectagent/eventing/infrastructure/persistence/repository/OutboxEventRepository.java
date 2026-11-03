package com.luzhiwen.collectagent.eventing.infrastructure.persistence.repository;

import com.luzhiwen.collectagent.eventing.domain.OutboxStatus;
import com.luzhiwen.collectagent.eventing.infrastructure.persistence.entity.OutboxEventEntity;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Outbox 数据访问接口。
 */
public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {

    /**
     * 查询到期且待处理的事件。
     *
     * @param status 事件状态
     * @param nextRetryAt 当前时间
     * @param pageable 分页参数
     * @return Outbox 事件列表
     */
    List<OutboxEventEntity> findByStatusAndNextRetryAtLessThanEqualOrderByIdAsc(
            OutboxStatus status,
            LocalDateTime nextRetryAt,
            Pageable pageable
    );

    /**
     * 根据事件唯一标识查询 Outbox 记录。
     *
     * @param eventId 事件唯一标识
     * @return Outbox 记录
     */
    Optional<OutboxEventEntity> findByEventId(String eventId);
}
