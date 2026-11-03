package com.luzhiwen.collectagent.eventing.domain;

/**
 * 原始事件类型。
 */
public enum EventType {

    /**
     * 会话开始事件。
     */
    SESSION_STARTED,

    /**
     * 用户消息事件。
     */
    USER_MESSAGE,

    /**
     * 工具调用事件。
     */
    TOOL_CALL,

    /**
     * 工具结果事件。
     */
    TOOL_RESULT,

    /**
     * 助手回复事件。
     */
    ASSISTANT_MESSAGE,

    /**
     * 会话结束事件。
     */
    SESSION_CLOSED
}
