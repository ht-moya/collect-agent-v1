package com.luzhiwen.collectagent.eventing.application;

import com.luzhiwen.collectagent.eventing.domain.EventType;
import com.luzhiwen.collectagent.eventing.infrastructure.persistence.entity.EventLayerEntity;
import com.luzhiwen.collectagent.eventing.infrastructure.persistence.entity.OutboxEventEntity;
import com.luzhiwen.collectagent.eventing.infrastructure.persistence.repository.EventLayerRepository;
import com.luzhiwen.collectagent.eventing.infrastructure.persistence.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 原始事件追加服务。
 *
 * <p>事件写入、会话内序号分配和 Outbox 写入在同一事务内完成，只追加不修改。</p>
 */
@Service
public class EventAppendService {

    /** 原始事件数据访问接口。 */
    private final EventLayerRepository eventLayerRepository;

    /** Outbox 数据访问接口。 */
    private final OutboxEventRepository outboxEventRepository;

    /** 会话内事件序号生成器。 */
    private final SessionSequenceGenerator sessionSequenceGenerator;

    /** JSON 序列化器。 */
    private final ObjectMapper objectMapper;

    /**
     * 创建事件追加服务。
     *
     * @param eventLayerRepository 原始事件数据访问接口
     * @param outboxEventRepository Outbox 数据访问接口
     * @param sessionSequenceGenerator 会话内事件序号生成器
     * @param objectMapper JSON 序列化器
     */
    public EventAppendService(
            EventLayerRepository eventLayerRepository,
            OutboxEventRepository outboxEventRepository,
            SessionSequenceGenerator sessionSequenceGenerator,
            ObjectMapper objectMapper
    ) {
        this.eventLayerRepository = eventLayerRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.sessionSequenceGenerator = sessionSequenceGenerator;
        this.objectMapper = objectMapper;
    }

    /**
     * 追加原始事件并创建 Outbox 记录。
     *
     * @param traceId 请求链路标识
     * @param sessionId 会话唯一标识
     * @param customerId 客户唯一标识
     * @param eventType 事件类型
     * @param payload 事件载荷
     * @return 已保存的事件
     */
    @Transactional
    public EventLayerEntity append(
            String traceId,
            String sessionId,
            String customerId,
            EventType eventType,
            Map<String, Object> payload
    ) {
        // 先分配会话内序号，再写入不可变事件；event_id 仍用于精确指认事件。
        String payloadJson = serialize(payload);
        String eventId = UUID.randomUUID().toString().replace("-", "");
        long sessionSeq = sessionSequenceGenerator.next(sessionId);
        EventLayerEntity event = eventLayerRepository.save(new EventLayerEntity(
                eventId,
                traceId,
                sessionId,
                customerId,
                sessionSeq,
                eventType,
                payloadJson,
                LocalDateTime.now()
        ));
        // Outbox 与事件同事务写入，由 Relay 异步驱动记忆投影和摘要。
        outboxEventRepository.save(new OutboxEventEntity(
                eventId,
                "SESSION",
                sessionId,
                eventType.name(),
                payloadJson
        ));
        return event;
    }

    /**
     * 将对象序列化为 JSON。
     *
     * @param value 待序列化对象
     * @return JSON 字符串
     */
    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalStateException("事件载荷序列化失败", exception);
        }
    }
}
