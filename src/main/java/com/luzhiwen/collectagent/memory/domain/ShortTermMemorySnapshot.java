package com.luzhiwen.collectagent.memory.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 短期记忆缓存快照。
 *
 * <p>缓存只保存最近事件、状态视图快照和最新摘要，事实源仍是 event_layer、agent_session_state 和 agent_summary。</p>
 */
public class ShortTermMemorySnapshot {

    /** 租户唯一标识。 */
    private String tenantId;

    /** 会话唯一标识。 */
    private String sessionId;

    /** 客户唯一标识。 */
    private String customerId;

    /** 最近事件列表。 */
    private List<ShortTermMemoryEvent> recentEvents = new ArrayList<>();

    /** 当前会话状态 JSON。 */
    private String stateJson = "{}";

    /** 当前会话状态视图 JSON。 */
    private String stateViewJson = "{}";

    /** 最新摘要文本。 */
    private String latestSummary = "{}";

    /** 最新摘要覆盖到的会话内序号。 */
    private Long latestSummaryCoversSeqTo;

    /** 快照构建时间。 */
    private LocalDateTime builtAt;

    /** 创建空快照，供缓存反序列化使用。 */
    public ShortTermMemorySnapshot() {
    }

    /**
     * 创建短期记忆快照。
     *
     * @param tenantId 租户唯一标识
     * @param sessionId 会话唯一标识
     * @param customerId 客户唯一标识
     * @param recentEvents 最近事件
     * @param stateJson 当前状态 JSON
     * @param stateViewJson 当前状态视图 JSON
     * @param latestSummary 最新摘要
     * @param latestSummaryCoversSeqTo 最新摘要覆盖到的序号
     * @param builtAt 构建时间
     */
    public ShortTermMemorySnapshot(
            String tenantId,
            String sessionId,
            String customerId,
            List<ShortTermMemoryEvent> recentEvents,
            String stateJson,
            String stateViewJson,
            String latestSummary,
            Long latestSummaryCoversSeqTo,
            LocalDateTime builtAt
    ) {
        this.tenantId = tenantId;
        this.sessionId = sessionId;
        this.customerId = customerId;
        this.recentEvents = new ArrayList<>(recentEvents);
        this.stateJson = stateJson;
        this.stateViewJson = stateViewJson;
        this.latestSummary = latestSummary;
        this.latestSummaryCoversSeqTo = latestSummaryCoversSeqTo;
        this.builtAt = builtAt;
    }

    /** @return 租户唯一标识。 */
    public String getTenantId() { return tenantId; }

    /** @param tenantId 租户唯一标识。 */
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    /** @return 会话唯一标识。 */
    public String getSessionId() { return sessionId; }

    /** @param sessionId 会话唯一标识。 */
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    /** @return 客户唯一标识。 */
    public String getCustomerId() { return customerId; }

    /** @param customerId 客户唯一标识。 */
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    /** @return 最近事件列表。 */
    public List<ShortTermMemoryEvent> getRecentEvents() { return recentEvents; }

    /** @param recentEvents 最近事件列表。 */
    public void setRecentEvents(List<ShortTermMemoryEvent> recentEvents) {
        this.recentEvents = recentEvents == null ? new ArrayList<>() : recentEvents;
    }

    /** @return 当前状态 JSON。 */
    public String getStateJson() { return stateJson; }

    /** @param stateJson 当前状态 JSON。 */
    public void setStateJson(String stateJson) { this.stateJson = stateJson; }

    /** @return 当前状态视图 JSON。 */
    public String getStateViewJson() { return stateViewJson; }

    /** @param stateViewJson 当前状态视图 JSON。 */
    public void setStateViewJson(String stateViewJson) { this.stateViewJson = stateViewJson; }

    /** @return 最新摘要文本。 */
    public String getLatestSummary() { return latestSummary; }

    /** @param latestSummary 最新摘要文本。 */
    public void setLatestSummary(String latestSummary) { this.latestSummary = latestSummary; }

    /** @return 最新摘要覆盖到的会话内序号。 */
    public Long getLatestSummaryCoversSeqTo() { return latestSummaryCoversSeqTo; }

    /** @param latestSummaryCoversSeqTo 最新摘要覆盖到的会话内序号。 */
    public void setLatestSummaryCoversSeqTo(Long latestSummaryCoversSeqTo) {
        this.latestSummaryCoversSeqTo = latestSummaryCoversSeqTo;
    }

    /** @return 快照构建时间。 */
    public LocalDateTime getBuiltAt() { return builtAt; }

    /** @param builtAt 快照构建时间。 */
    public void setBuiltAt(LocalDateTime builtAt) { this.builtAt = builtAt; }
}
