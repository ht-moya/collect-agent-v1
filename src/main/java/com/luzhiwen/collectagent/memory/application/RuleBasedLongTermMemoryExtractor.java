package com.luzhiwen.collectagent.memory.application;

import com.luzhiwen.collectagent.eventing.application.EventPayloadReader;
import com.luzhiwen.collectagent.eventing.domain.EventType;
import com.luzhiwen.collectagent.eventing.infrastructure.persistence.entity.EventLayerEntity;
import com.luzhiwen.collectagent.memory.domain.LongTermMemoryCandidate;
import com.luzhiwen.collectagent.memory.domain.LongTermMemoryType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 无 LLM 时使用的规则长期记忆抽取器。
 */
@Component
@ConditionalOnProperty(prefix = "agent.llm", name = "enabled", havingValue = "false", matchIfMissing = true)
public class RuleBasedLongTermMemoryExtractor implements LongTermMemoryExtractor {

    /** 事件载荷读取器。 */
    private final EventPayloadReader eventPayloadReader;

    /**
     * 创建规则抽取器。
     *
     * @param eventPayloadReader 事件载荷读取器
     */
    public RuleBasedLongTermMemoryExtractor(EventPayloadReader eventPayloadReader) {
        this.eventPayloadReader = eventPayloadReader;
    }

    /** {@inheritDoc} */
    @Override
    public List<LongTermMemoryCandidate> extract(
            String traceId,
            String sessionId,
            String userId,
            List<EventLayerEntity> events
    ) {
        List<LongTermMemoryCandidate> result = new ArrayList<>();
        for (EventLayerEntity event : events) {
            if (event.getEventType() != EventType.USER_MESSAGE) {
                continue;
            }
            String content = eventPayloadReader.text(event, "content");
            if (content.contains("短信")) {
                result.add(candidate(LongTermMemoryType.PREFERENCE, "用户更接受短信联系",
                        Map.of("channel", "sms"), event, 0.82D));
            }
            if (content.contains("电话")) {
                result.add(candidate(LongTermMemoryType.PREFERENCE, "用户更接受电话联系",
                        Map.of("channel", "phone"), event, 0.82D));
            }
            if (content.contains("失业")) {
                result.add(candidate(LongTermMemoryType.FACT, "用户目前处于失业状态",
                        Map.of("fact_key", "employment_status", "fact_value", "unemployed"), event, 0.80D));
            }
            if (content.contains("生病") || content.contains("疾病")) {
                result.add(candidate(LongTermMemoryType.FACT, "用户存在健康方面困难",
                        Map.of("fact_key", "health_status", "fact_value", "illness"), event, 0.78D));
            }
        }
        return result;
    }

    /**
     * 创建规则候选。
     *
     * @param type 类型
     * @param content 内容
     * @param structured 结构化字段
     * @param event 来源事件
     * @param confidence 置信度
     * @return 候选
     */
    private LongTermMemoryCandidate candidate(
            LongTermMemoryType type,
            String content,
            Map<String, Object> structured,
            EventLayerEntity event,
            double confidence
    ) {
        return new LongTermMemoryCandidate(
                type,
                content,
                structured,
                List.of(event.getEventId()),
                confidence
        );
    }
}
