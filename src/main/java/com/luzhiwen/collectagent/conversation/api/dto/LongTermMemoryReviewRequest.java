package com.luzhiwen.collectagent.conversation.api.dto;

/**
 * 长期记忆人工审核请求。
 */
public class LongTermMemoryReviewRequest {

    /** 审核人。 */
    private String reviewer;

    /** 审核说明。 */
    private String note;

    /** @return 审核人。 */ public String getReviewer() { return reviewer; }
    /** @param reviewer 审核人。 */ public void setReviewer(String reviewer) { this.reviewer = reviewer; }
    /** @return 审核说明。 */ public String getNote() { return note; }
    /** @param note 审核说明。 */ public void setNote(String note) { this.note = note; }
}
