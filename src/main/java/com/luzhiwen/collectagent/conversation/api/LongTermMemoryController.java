package com.luzhiwen.collectagent.conversation.api;

import com.luzhiwen.collectagent.conversation.api.dto.LongTermMemoryReviewRequest;
import com.luzhiwen.collectagent.memory.application.LongTermMemoryPendingService;
import com.luzhiwen.collectagent.memory.domain.LongTermMemoryPendingRecord;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 长期记忆 pending 人工审核接口。
 */
@RestController
@RequestMapping("/api/long-term-memory")
public class LongTermMemoryController {

    /** pending 服务。 */
    private final LongTermMemoryPendingService pendingService;

    /**
     * 创建长期记忆接口。
     *
     * @param pendingService pending服务
     */
    public LongTermMemoryController(LongTermMemoryPendingService pendingService) {
        this.pendingService = pendingService;
    }

    /** @param status 状态 @param limit 数量 @return pending列表。 */
    @GetMapping("/pending")
    public List<LongTermMemoryPendingRecord> pending(
            @RequestParam(defaultValue = "pending") String status,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return pendingService.list(status, limit);
    }

    /** @param id 主键 @param request 审核请求。 */
    @PostMapping("/pending/{id}/approve")
    public void approve(@PathVariable long id, @RequestBody(required = false) LongTermMemoryReviewRequest request) {
        LongTermMemoryReviewRequest safe = request == null ? new LongTermMemoryReviewRequest() : request;
        pendingService.approve(id, safe.getReviewer(), safe.getNote());
    }

    /** @param id 主键 @param request 审核请求。 */
    @PostMapping("/pending/{id}/reject")
    public void reject(@PathVariable long id, @RequestBody(required = false) LongTermMemoryReviewRequest request) {
        LongTermMemoryReviewRequest safe = request == null ? new LongTermMemoryReviewRequest() : request;
        pendingService.reject(id, safe.getReviewer(), safe.getNote());
    }
}
