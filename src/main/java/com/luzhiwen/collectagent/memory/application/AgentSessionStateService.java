package com.luzhiwen.collectagent.memory.application;

import com.luzhiwen.collectagent.eventing.application.EventPayloadReader;
import com.luzhiwen.collectagent.eventing.domain.EventType;
import com.luzhiwen.collectagent.eventing.infrastructure.persistence.entity.EventLayerEntity;
import com.luzhiwen.collectagent.memory.domain.AgentSlotPatch;
import com.luzhiwen.collectagent.memory.domain.AgentStage;
import com.luzhiwen.collectagent.memory.domain.AgentStatePolicy;
import com.luzhiwen.collectagent.memory.domain.AgentStatePatch;
import com.luzhiwen.collectagent.memory.infrastructure.persistence.entity.AgentSessionStateEntity;
import com.luzhiwen.collectagent.memory.infrastructure.persistence.repository.AgentSessionStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Agent 会话状态服务。
 *
 * <p>状态表只由主链路和状态投影更新。摘要不会修改该表，也不会影响其乐观锁版本。</p>
 */
@Service
public class AgentSessionStateService {

    /** 默认租户标识，后续接入登录上下文后可替换。 */
    public static final String DEFAULT_TENANT_ID = "default-tenant";

    /** 当前业务固定任务类型。 */
    private static final String REPAYMENT_REMINDER_TASK = "repayment_reminder";

    /** 当前类日志器。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(AgentSessionStateService.class);

    /** 状态数据访问接口。 */
    private final AgentSessionStateRepository stateRepository;

    /** 状态行并发初始化器。 */
    private final AgentSessionStateProvisioner stateProvisioner;

    /** 事件载荷读取器。 */
    private final EventPayloadReader eventPayloadReader;

    /** JSON 序列化器。 */
    private final ObjectMapper objectMapper;

    /** 可写槽位白名单。 */
    private static final Set<String> WRITABLE_SLOTS = Set.copyOf(AgentStatePolicy.ALLOWED_PATCH_FIELDS);


    /**
     * 创建会话状态服务。
     *
     * @param stateRepository 状态数据访问接口
     * @param stateProvisioner 状态行并发初始化器
     * @param eventPayloadReader 事件载荷读取器
     * @param objectMapper JSON 序列化器
     */
    public AgentSessionStateService(
            AgentSessionStateRepository stateRepository,
            AgentSessionStateProvisioner stateProvisioner,
            EventPayloadReader eventPayloadReader,
            ObjectMapper objectMapper
    ) {
        this.stateRepository = stateRepository;
        this.stateProvisioner = stateProvisioner;
        this.eventPayloadReader = eventPayloadReader;
        this.objectMapper = objectMapper;
    }

    /**
     * 将原始事件投影到会话状态。
     *
     * @param event 原始事件
     * @return 更新后的会话状态
     */
    @Transactional
    public AgentSessionStateEntity applyEvent(EventLayerEntity event) {
        // 锁定会话状态行，确保同一会话的状态更新串行执行。
        AgentSessionStateEntity state = lockOrProvision(event);
        long eventSeq = event.getSessionSeq() == null ? 0L : event.getSessionSeq();
        // 使用会话内 seq 做幂等判断，旧事件不能覆盖已经处理的新状态。
        if (eventSeq > 0 && state.getLastSeq() >= eventSeq) {
            return state;
        }
        ObjectNode stateRoot = readStateObject(state.getStateJson());
        applyEventToState(stateRoot, event);
        if (event.getEventType() == EventType.SESSION_CLOSED) {
            state.setStage(AgentStage.CLOSED.name());
        }
        refreshPendingSlots(stateRoot, state.getStage());
        state.setStateJson(stateRoot.toString());
        state.setLastSeq(Math.max(state.getLastSeq(), eventSeq));
        return stateRepository.save(state);
    }

    /**
     * 合并 LLM 返回的状态补丁。
     *
     * @param sessionId 会话唯一标识
     * @param patch 状态补丁
     * @param sourceEventId 来源事件主键
     * @return 更新后的会话状态
     */
    @Transactional
    public AgentSessionStateEntity applyPatch(String sessionId, AgentStatePatch patch, Long sourceEventId) {
        AgentSessionStateEntity state = stateRepository.findBySessionIdForUpdate(sessionId)
                .orElseThrow(() -> new IllegalStateException("会话状态不存在：" + sessionId));
        if (patch == null || patch.isEmpty()) {
            return state;
        }
        ObjectNode stateRoot = readStateObject(state.getStateJson());
        applyStagePatch(state, patch.getStage());
        applySlotPatches(stateRoot, patch.getSlots(), state, sourceEventId);
        refreshPendingSlots(stateRoot, state.getStage());
        state.setStateJson(stateRoot.toString());
        return stateRepository.save(state);
    }

    /**
     * 查询会话状态。
     *
     * @param sessionId 会话唯一标识
     * @return 会话状态
     */
    @Transactional(readOnly = true)
    public Optional<AgentSessionStateEntity> getState(String sessionId) {
        return stateRepository.findBySessionId(sessionId);
    }

    /**
     * 锁定会话状态，供关闭会话等生命周期操作使用。
     *
     * @param sessionId 会话唯一标识
     * @return 已加锁状态
     */
    @Transactional
    public Optional<AgentSessionStateEntity> lockState(String sessionId) {
        return stateRepository.findBySessionIdForUpdate(sessionId);
    }

    /**
     * 锁定状态行；不存在时并发安全地创建并重新加锁。
     *
     * @param event 当前事件
     * @return 已锁定状态
     */
    private AgentSessionStateEntity lockOrProvision(EventLayerEntity event) {
        // 不能在初始化子事务之前对不存在的行执行 FOR UPDATE，否则外层间隙锁会阻塞子事务插入。
        if (!stateRepository.existsById(event.getSessionId())) {
            try {
                stateProvisioner.createIfAbsent(
                        event.getSessionId(),
                        event.getCustomerId(),
                        event.getTraceId()
                );
            } catch (DataIntegrityViolationException exception) {
                LOGGER.info("会话状态已由其他线程创建，复用现有记录，sessionId={}", event.getSessionId());
            }
        }
        // 初始化完成后，再锁定已经存在的主键行并执行状态更新。
        return stateRepository.findBySessionIdForUpdate(event.getSessionId())
                .orElseThrow(() -> new IllegalStateException("会话状态创建失败：" + event.getSessionId()));
    }

    /**
     * 将事件内容投影到状态主体。
     *
     * @param stateRoot 状态 JSON
     * @param event 原始事件
     */
    private void applyEventToState(ObjectNode stateRoot, EventLayerEntity event) {
        ObjectNode flags = objectField(stateRoot, "flags");
        if (event.getEventType() == EventType.USER_MESSAGE) {
            String content = eventPayloadReader.text(event, "content");
            if (content.contains("投诉")) {
                flags.put("complaint", true);
            }
            if (content.contains("人工") || content.contains("真人")) {
                flags.put("human_handoff_requested", true);
            }
        }
        if (event.getEventType() == EventType.TOOL_RESULT) {
            applyToolResult(stateRoot, event);
        }
        if (event.getEventType() == EventType.SESSION_CLOSED) {
            flags.put("session_closed", true);
            String reason = eventPayloadReader.text(event, "reason");
            if (reason != null && !reason.isBlank()) {
                flags.put("close_reason", reason);
            }
        }
    }

    /**
     * 将客户查询工具结果写入只读业务槽位。
     *
     * @param stateRoot 状态 JSON
     * @param event 工具结果事件
     */
    private void applyToolResult(ObjectNode stateRoot, EventLayerEntity event) {
        JsonNode payload = eventPayloadReader.read(event);
        String toolName = payload.path("toolName").asString("");
        JsonNode data = payload.path("data");
        if ("customer_query".equals(toolName) && data.isObject()) {
            putSystemSlot(stateRoot, "debt_amount", data.path("outstandingAmount"), event);
            putSystemSlot(stateRoot, "overdue_days", data.path("overdueDays"), event);
        }
        if ("transfer_to_human".equals(toolName)) {
            objectField(stateRoot, "flags").put("human_handoff_requested", true);
        }
    }

    /**
     * 写入后端维护的只读槽位。
     *
     * @param stateRoot 状态 JSON
     * @param slotName 槽位名称
     * @param value 槽位值
     * @param event 来源事件
     */
    private void putSystemSlot(ObjectNode stateRoot, String slotName, JsonNode value, EventLayerEntity event) {
        if (value == null || value.isMissingNode() || value.isNull()) {
            return;
        }
        ObjectNode slot = objectMapper.createObjectNode();
        slot.set("value", value.deepCopy());
        slot.put("confidence", 1.0);
        slot.put("source_type", "TOOL_RESULT");
        slot.put("source_ref", event.getEventId());
        slot.put("updated_at", LocalDateTime.now().toString());
        objectField(stateRoot, "slots").set(slotName, slot);
    }

    /**
     * 应用阶段补丁并校验迁移白名单。
     *
     * @param state 会话状态
     * @param requestedStage 请求阶段
     */
    private void applyStagePatch(AgentSessionStateEntity state, String requestedStage) {
        if (requestedStage == null || requestedStage.isBlank()) {
            return;
        }
        AgentStage currentStage = parseStage(state.getStage());
        AgentStage targetStage = parseStage(requestedStage);
        if (targetStage == currentStage) {
            return;
        }
        if (!AgentStatePolicy.canTransition(currentStage, targetStage)) {
            LOGGER.warn("忽略非法阶段迁移，sessionId={}，from={}，to={}",
                    state.getSessionId(), currentStage, targetStage);
            return;
        }
        state.setStage(targetStage.name());
    }

    /**
     * 应用槽位补丁并校验槽位值与白名单。
     *
     * @param stateRoot 状态 JSON
     * @param slots 槽位补丁
     * @param state 会话状态
     * @param sourceEventId 来源事件主键
     */
    private void applySlotPatches(
            ObjectNode stateRoot,
            Map<String, AgentSlotPatch> slots,
            AgentSessionStateEntity state,
            Long sourceEventId
    ) {
        if (slots == null || slots.isEmpty()) {
            return;
        }
        ObjectNode persistedSlots = objectField(stateRoot, "slots");
        slots.forEach((slotName, patch) -> {
            if (!WRITABLE_SLOTS.contains(slotName)) {
                LOGGER.warn("忽略白名单外槽位，sessionId={}，slot={}", state.getSessionId(), slotName);
                return;
            }
            Object normalizedValue = normalizeSlotValue(slotName, patch, persistedSlots);
            if (normalizedValue == null) {
                return;
            }
            ObjectNode persistedSlot = objectMapper.createObjectNode();
            persistedSlot.set("value", objectMapper.valueToTree(normalizedValue));
            persistedSlot.put("confidence", normalizeConfidence(patch.getConfidence()));
            persistedSlot.put("source_type", "LLM_EXTRACTION");
            persistedSlot.put("source_ref", sourceEventId == null ? "" : String.valueOf(sourceEventId));
            persistedSlot.put("updated_at", LocalDateTime.now().toString());
            persistedSlots.set(slotName, persistedSlot);
        });
    }

    /**
     * 校验并归一化单个槽位值。
     *
     * @param slotName 槽位名称
     * @param patch 槽位补丁
     * @param persistedSlots 已持久化槽位
     * @return 归一化后的值；无效时返回 null
     */
    private Object normalizeSlotValue(String slotName, AgentSlotPatch patch, ObjectNode persistedSlots) {
        if (patch == null || patch.getValue() == null) {
            return null;
        }
        return switch (slotName) {
            case "promise_date" -> normalizePromiseDate(patch.getValue());
            case "promise_amount" -> normalizePromiseAmount(patch.getValue(), persistedSlots);
            case "repayment_intent" -> normalizeRepaymentIntent(patch.getValue());
            default -> normalizeText(patch.getValue());
        };
    }

    /**
     * 校验承诺还款日期必须为未来日期。
     *
     * @param value 原始值
     * @return 日期字符串；无效时返回 null
     */
    private String normalizePromiseDate(Object value) {
        try {
            LocalDate date = LocalDate.parse(String.valueOf(value));
            return date.isAfter(LocalDate.now()) ? date.toString() : null;
        } catch (Exception exception) {
            return null;
        }
    }

    /**
     * 校验承诺金额必须为正数且不超过当前债务金额。
     *
     * @param value 原始值
     * @param persistedSlots 已持久化槽位
     * @return 金额；无效时返回 null
     */
    private BigDecimal normalizePromiseAmount(Object value, ObjectNode persistedSlots) {
        try {
            BigDecimal amount = new BigDecimal(String.valueOf(value));
            if (amount.signum() <= 0) {
                return null;
            }
            JsonNode debtSlot = persistedSlots.path("debt_amount");
            JsonNode debtValue = debtSlot.isObject() ? debtSlot.path("value") : debtSlot;
            if (!debtValue.isMissingNode() && !debtValue.isNull()) {
                BigDecimal debtAmount = debtValue.decimalValue();
                if (amount.compareTo(debtAmount) > 0) {
                    return null;
                }
            }
            return amount;
        } catch (Exception exception) {
            return null;
        }
    }

    /**
     * 校验还款意愿枚举。
     *
     * @param value 原始值
     * @return 还款意愿；无效时返回 null
     */
    private String normalizeRepaymentIntent(Object value) {
        String intent = String.valueOf(value);
        return Set.of("willing", "unwilling", "hesitant").contains(intent) ? intent : null;
    }

    /**
     * 归一化普通文本槽位。
     *
     * @param value 原始值
     * @return 截断后的文本
     */
    private String normalizeText(Object value) {
        String text = String.valueOf(value).trim();
        if (text.isBlank()) {
            return null;
        }
        return text.length() <= 500 ? text : text.substring(0, 500);
    }

    /**
     * 归一化置信度。
     *
     * @param confidence 原始置信度
     * @return 0 到 1 之间的置信度
     */
    private double normalizeConfidence(Double confidence) {
        if (confidence == null) {
            return 0.8;
        }
        return Math.max(0.0, Math.min(1.0, confidence));
    }

    /**
     * 根据当前阶段重新计算待办槽位。
     *
     * @param stateRoot 状态 JSON
     * @param stageValue 当前阶段
     */
    private void refreshPendingSlots(ObjectNode stateRoot, String stageValue) {
        AgentStage stage = parseStage(stageValue);
        List<String> required = AgentStatePolicy.requiredSlots(stage);
        JsonNode slots = objectField(stateRoot, "slots");
        ArrayNode pending = objectMapper.createArrayNode();
        for (String slotName : required) {
            JsonNode slot = slots.path(slotName);
            JsonNode value = slot.isObject() ? slot.path("value") : slot;
            if (value.isMissingNode() || value.isNull() || value.asString("").isBlank()) {
                pending.add(slotName);
            }
        }
        stateRoot.set("pending", pending);
    }

    /**
     * 读取状态 JSON 对象并补齐固定结构。
     *
     * @param json 状态 JSON
     * @return 状态对象
     */
    private ObjectNode readStateObject(String json) {
        try {
            JsonNode root = objectMapper.readTree(json == null ? "{}" : json);
            ObjectNode objectNode = root instanceof ObjectNode node ? node : objectMapper.createObjectNode();
            objectField(objectNode, "slots");
            objectField(objectNode, "flags");
            if (!objectNode.path("pending").isArray()) {
                objectNode.set("pending", objectMapper.createArrayNode());
            }
            return objectNode;
        } catch (JacksonException exception) {
            ObjectNode fallback = objectMapper.createObjectNode();
            objectField(fallback, "slots");
            objectField(fallback, "flags");
            fallback.set("pending", objectMapper.createArrayNode());
            return fallback;
        }
    }

    /**
     * 获取或创建对象字段。
     *
     * @param parent 父对象
     * @param fieldName 字段名称
     * @return 对象字段
     */
    private ObjectNode objectField(ObjectNode parent, String fieldName) {
        JsonNode current = parent.get(fieldName);
        if (current instanceof ObjectNode objectNode) {
            return objectNode;
        }
        ObjectNode created = objectMapper.createObjectNode();
        parent.set(fieldName, created);
        return created;
    }

    /**
     * 安全解析阶段。
     *
     * @param value 阶段文本
     * @return 阶段枚举
     */
    private AgentStage parseStage(String value) {
        try {
            return AgentStage.valueOf(value);
        } catch (Exception exception) {
            return AgentStage.OPENING;
        }
    }
}





