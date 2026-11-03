package com.luzhiwen.collectagent.conversation.application;

/**
 * 已关闭会话继续写入时抛出的业务异常。
 */
public class SessionClosedException extends RuntimeException {

    /** 已关闭的会话标识。 */
    private final String sessionId;

    /**
     * 创建已关闭会话异常。
     *
     * @param sessionId 已关闭的会话标识
     */
    public SessionClosedException(String sessionId) {
        super("会话已关闭，请创建新会话后再继续对话：" + sessionId);
        this.sessionId = sessionId;
    }

    /** @return 已关闭的会话标识。 */
    public String getSessionId() {
        return sessionId;
    }
}
