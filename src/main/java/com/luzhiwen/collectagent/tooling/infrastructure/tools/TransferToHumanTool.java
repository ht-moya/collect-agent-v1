package com.luzhiwen.collectagent.tooling.infrastructure.tools;

import com.luzhiwen.collectagent.tooling.domain.AgentTool;
import com.luzhiwen.collectagent.tooling.domain.ToolResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 转人工工具，为当前会话生成一个本地模拟工单。
 */
@Component
public class TransferToHumanTool implements AgentTool {

    /**
     * 工具唯一名称。
     */
    public static final String TOOL_NAME = "transfer_to_human";

    /**
     * 本地模拟工单序号。
     */
    private final AtomicLong ticketSequence = new AtomicLong(1000L);

    /**
     * 获取工具名称。
     *
     * @return 工具名称
     */
    @Override
    public String name() {
        return TOOL_NAME;
    }

    /**
     * 获取工具用途说明。
     *
     * @return 工具用途说明
     */
    @Override
    public String description() {
        return "当客户无法识别、工具失败或用户明确要求人工时，创建转人工工单";
    }

    /**
     * 获取转人工工具的 Function Calling 参数 Schema。
     *
     * @return 参数 Schema
     */
    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "reason", Map.of(
                                "type", "string",
                                "description", "转人工原因"
                        ),
                        "summary", Map.of(
                                "type", "string",
                                "description", "当前对话摘要"
                        )
                ),
                "required", List.of("reason")
        );
    }
    /**
     * 执行转人工操作。
     *
     * @param arguments 工具参数，可包含 reason 和 summary
     * @return 转人工结果和模拟工单号
     */
    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        String reason = String.valueOf(arguments.getOrDefault("reason", "用户请求人工"));
        String summary = String.valueOf(arguments.getOrDefault("summary", ""));
        String ticketId = "H" + ticketSequence.incrementAndGet();
        String createdAt = LocalDateTime.now().toString();
        String message = "已创建转人工工单 " + ticketId + "，原因：" + reason + "。";

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("ticketId", ticketId);
        data.put("reason", reason);
        data.put("summary", summary);
        data.put("createdAt", createdAt);
        data.put("estimatedResponseMinutes", 5);
        return ToolResult.success(message, data);
    }
}


