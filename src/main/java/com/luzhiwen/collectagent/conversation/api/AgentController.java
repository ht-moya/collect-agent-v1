package com.luzhiwen.collectagent.conversation.api;

import com.luzhiwen.collectagent.agentruntime.application.AgentOrchestrator;
import com.luzhiwen.collectagent.conversation.api.dto.AgentChatRequest;
import com.luzhiwen.collectagent.conversation.api.dto.AgentChatResponse;
import com.luzhiwen.collectagent.shared.web.TraceIdFilter;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agent 聊天接口。
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    /** Agent 编排器。 */
    private final AgentOrchestrator agentOrchestrator;

    /**
     * 创建 Agent 控制器。
     *
     * @param agentOrchestrator Agent 编排器
     */
    public AgentController(AgentOrchestrator agentOrchestrator) {
        this.agentOrchestrator = agentOrchestrator;
    }

    /**
     * 处理聊天请求。
     *
     * @param request 聊天请求
     * @return 聊天响应
     */
    @PostMapping("/chat")
    public AgentChatResponse chat(@Valid @RequestBody AgentChatRequest request) {
        return agentOrchestrator.chat(request, MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY));
    }
}
