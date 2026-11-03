package com.luzhiwen.collectagent.shared.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 请求链路标识过滤器。
 *
 * <p>为每个 HTTP 请求生成 traceId，并写入 MDC 和响应头，便于串联完整日志。</p>
 */
@Component
public class TraceIdFilter extends OncePerRequestFilter {

    /**
     * 请求头中可传入的链路标识名称。
     */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    /**
     * MDC 中保存链路标识的键。
     */
    public static final String TRACE_ID_MDC_KEY = "traceId";

    /**
     * 处理每个 HTTP 请求：优先复用请求头 traceId，否则生成新的 UUID。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param filterChain 后续过滤器链
     * @throws ServletException Servlet 处理异常
     * @throws IOException 输入输出异常
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        MDC.put(TRACE_ID_MDC_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID_MDC_KEY);
        }
    }
}

