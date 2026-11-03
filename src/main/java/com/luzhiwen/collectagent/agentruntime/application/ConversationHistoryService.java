package com.luzhiwen.collectagent.agentruntime.application;

import com.luzhiwen.collectagent.eventing.application.EventPayloadReader;
import com.luzhiwen.collectagent.eventing.domain.EventType;
import com.luzhiwen.collectagent.eventing.infrastructure.persistence.entity.EventLayerEntity;
import com.luzhiwen.collectagent.eventing.infrastructure.persistence.repository.EventLayerRepository;
import com.luzhiwen.collectagent.model.application.LlmMessage;
import com.luzhiwen.collectagent.model.application.LlmToolCall;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * 从 event_layer 回放标准 LLM 消息历史。
 */
@Service
public class ConversationHistoryService {

    /** 原始事件数据访问接口。 */
    private final EventLayerRepository eventLayerRepository;

    /** 事件载荷读取器。 */
    private final EventPayloadReader eventPayloadReader;

    /** JSON 序列化器。 */
    private final ObjectMapper objectMapper;

    /**
     * 创建会话历史回放服务。
     *
     * @param eventLayerRepository 原始事件数据访问接口
     * @param eventPayloadReader 事件载荷读取器
     * @param objectMapper JSON 序列化器
     */
    public ConversationHistoryService(
            EventLayerRepository eventLayerRepository,
            EventPayloadReader eventPayloadReader,
            ObjectMapper objectMapper
    ) {
        this.eventLayerRepository = eventLayerRepository;
        this.eventPayloadReader = eventPayloadReader;
        this.objectMapper = objectMapper;
    }

    /**
     * 回放摘要覆盖位点之后、当前用户消息之前的最近 N 轮对话。
     *
     * @param sessionId 会话唯一标识
     * @param afterSeqExclusive 摘要已覆盖到的会话序号，不含该序号
     * @param beforeSeqExclusive 当前用户消息序号，不含该序号
     * @param maxTurns 最大对话轮次
     * @return 标准消息列表
     */
    @Transactional(readOnly = true)
    public List<LlmMessage> loadRecentTurns(
            String sessionId,
            long afterSeqExclusive,
            long beforeSeqExclusive,
            int maxTurns
    ) {
        List<EventLayerEntity> events = eventLayerRepository
                .findBySessionIdAndSessionSeqGreaterThanOrderBySessionSeqAsc(sessionId, afterSeqExclusive);
        List<List<EventLayerEntity>> turns = groupCompleteTurns(events, beforeSeqExclusive);
        int safeMaxTurns = Math.max(1, maxTurns);
        int fromIndex = Math.max(0, turns.size() - safeMaxTurns);
        List<EventLayerEntity> selectedEvents = new ArrayList<>();
        for (int index = fromIndex; index < turns.size(); index++) {
            selectedEvents.addAll(turns.get(index));
        }
        return toMessages(selectedEvents);
    }

    /**
     * 按 USER_MESSAGE 分组完整轮次，并排除当前用户消息。
     *
     * @param events 候选事件
     * @param beforeSeqExclusive 当前用户消息序号
     * @return 轮次事件列表
     */
    private List<List<EventLayerEntity>> groupCompleteTurns(
            List<EventLayerEntity> events,
            long beforeSeqExclusive
    ) {
        List<List<EventLayerEntity>> turns = new ArrayList<>();
        List<EventLayerEntity> currentTurn = null;
        for (EventLayerEntity event : events) {
            if (event.getSessionSeq() != null && event.getSessionSeq() >= beforeSeqExclusive) {
                break;
            }
            if (event.getEventType() == EventType.USER_MESSAGE) {
                currentTurn = new ArrayList<>();
                turns.add(currentTurn);
            }
            // 摘要位点可能落在某一轮中间；遇到非完整轮次前缀时直接丢弃，避免产生孤立 tool 消息。
            if (currentTurn != null) {
                currentTurn.add(event);
            }
        }
        return turns;
    }

    /**
     * 将事件转换为标准 LLM 消息。
     *
     * @param events 事件列表
     * @return 标准消息列表
     */
    private List<LlmMessage> toMessages(List<EventLayerEntity> events) {
        List<LlmMessage> messages = new ArrayList<>();
        List<LlmToolCall> pendingToolCalls = new ArrayList<>();
        for (EventLayerEntity event : events) {
            if (event.getEventType() == EventType.TOOL_CALL) {
                LlmToolCall toolCall = toToolCall(event);
                if (toolCall != null) {
                    pendingToolCalls.add(toolCall);
                }
                continue;
            }
            if (event.getEventType() == EventType.TOOL_RESULT) {
                appendPendingToolCalls(messages, pendingToolCalls);
                String toolCallId = eventPayloadReader.read(event).path("toolCallId").asString("");
                if (toolCallId.isBlank() && !pendingToolCalls.isEmpty()) {
                    toolCallId = pendingToolCalls.get(pendingToolCalls.size() - 1).getToolCallId();
                }
                String content = eventPayloadReader.read(event).path("result").asString("");
                if (!toolCallId.isBlank()) {
                    messages.add(new LlmMessage("tool", content, toolCallId, List.of()));
                }
                pendingToolCalls.clear();
                continue;
            }
            appendPendingToolCalls(messages, pendingToolCalls);
            pendingToolCalls.clear();
            if (event.getEventType() == EventType.USER_MESSAGE) {
                messages.add(new LlmMessage("user", eventPayloadReader.text(event, "content")));
            }
            if (event.getEventType() == EventType.ASSISTANT_MESSAGE) {
                messages.add(new LlmMessage("assistant", eventPayloadReader.text(event, "content")));
            }
        }
        appendPendingToolCalls(messages, pendingToolCalls);
        return messages;
    }

    /**
     * 将待处理工具调用追加为 assistant 消息。
     *
     * @param messages 消息列表
     * @param pendingToolCalls 待处理工具调用
     */
    private void appendPendingToolCalls(List<LlmMessage> messages, List<LlmToolCall> pendingToolCalls) {
        if (pendingToolCalls.isEmpty()) {
            return;
        }
        messages.add(new LlmMessage("assistant", null, null, new ArrayList<>(pendingToolCalls)));
    }

    /**
     * 将原始事件转换为标准工具调用。
     *
     * @param event 工具调用事件
     * @return 工具调用，无法解析时返回 null
     */
    private LlmToolCall toToolCall(EventLayerEntity event) {
        JsonNode payload = eventPayloadReader.read(event);
        String toolCallId = payload.path("toolCallId").asString("");
        if (toolCallId.isBlank()) {
            toolCallId = "call_legacy_" + event.getId();
        }
        String toolName = payload.path("toolName").asString("");
        JsonNode args = payload.path("args");
        if (toolName.isBlank()) {
            return null;
        }
        try {
            return new LlmToolCall(
                    toolCallId,
                    toolName,
                    objectMapper.writeValueAsString(args)
            );
        } catch (JacksonException exception) {
            return null;
        }
    }
}
