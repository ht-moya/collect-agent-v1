package com.luzhiwen.collectagent.conversation.api;

import com.luzhiwen.collectagent.conversation.api.dto.SessionCloseRequest;
import com.luzhiwen.collectagent.conversation.api.dto.SessionCloseResponse;
import com.luzhiwen.collectagent.conversation.application.AgentSessionLifecycleService;
import com.luzhiwen.collectagent.shared.web.TraceIdFilter;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agent 会话生命周期接口。
 */
@RestController
@RequestMapping("/api/agent/sessions")
public class AgentSessionController {

    /** 会话生命周期服务。 */
    private final AgentSessionLifecycleService lifecycleService;

    /**
     * 创建会话接口。
     *
     * @param lifecycleService 生命周期服务
     */
    public AgentSessionController(AgentSessionLifecycleService lifecycleService) {
        this.lifecycleService = lifecycleService;
    }

    /**
     * 结束会话。
     *
     * @param sessionId 会话ID
     * @param request 关闭请求
     * @return 关闭响应
     */
    @PostMapping("/{sessionId}/close")
    public SessionCloseResponse close(
            @PathVariable String sessionId,
            @RequestBody(required = false) SessionCloseRequest request
    ) {
        SessionCloseRequest safe = request == null ? new SessionCloseRequest() : request;
        return lifecycleService.closeSession(
                MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY),
                sessionId,
                safe.getReason()
        );
    }
}
