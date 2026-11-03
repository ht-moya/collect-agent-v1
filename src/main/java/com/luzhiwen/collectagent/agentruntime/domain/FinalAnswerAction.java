package com.luzhiwen.collectagent.agentruntime.domain;

import java.util.Collections;
import java.util.List;

/**
 * Agent 决定直接回复用户时返回的动作。
 */
public class FinalAnswerAction implements AgentAction {

    /**
     * 最终回复内容。
     */
    private final String answer;

    /**
     * 回复引用的知识块标识列表。
     */
    private final List<String> citations;

    /**
     * 创建最终回复动作。
     *
     * @param answer 最终回复内容
     * @param citations 引用的知识块标识
     */
    public FinalAnswerAction(String answer, List<String> citations) {
        this.answer = answer;
        this.citations = List.copyOf(citations);
    }

    /**
     * 获取最终回复内容。
     *
     * @return 最终回复内容
     */
    public String getAnswer() {
        return answer;
    }

    /**
     * 获取知识块引用列表。
     *
     * @return 不可修改的引用列表
     */
    public List<String> getCitations() {
        return Collections.unmodifiableList(citations);
    }
}

