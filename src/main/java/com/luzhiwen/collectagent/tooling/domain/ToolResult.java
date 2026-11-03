package com.luzhiwen.collectagent.tooling.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工具执行结果。
 */
public class ToolResult {

    /**
     * 工具是否执行成功。
     */
    private final boolean success;

    /**
     * 返回给 Agent 的简短文本。
     */
    private final String message;

    /**
     * 结构化的工具返回数据。
     */
    private final Map<String, Object> data;

    /**
     * 创建工具执行结果。
     *
     * @param success 是否成功
     * @param message 返回文本
     * @param data 结构化数据
     */
    public ToolResult(boolean success, String message, Map<String, Object> data) {
        this.success = success;
        this.message = message;
        this.data = Collections.unmodifiableMap(new LinkedHashMap<>(data));
    }

    /**
     * 创建成功结果。
     *
     * @param message 返回文本
     * @param data 结构化数据
     * @return 成功结果
     */
    public static ToolResult success(String message, Map<String, Object> data) {
        return new ToolResult(true, message, data);
    }

    /**
     * 创建失败结果。
     *
     * @param message 失败原因
     * @return 失败结果
     */
    public static ToolResult failure(String message) {
        return new ToolResult(false, message, Map.of());
    }

    /**
     * 判断执行是否成功。
     *
     * @return true 表示成功
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * 获取返回文本。
     *
     * @return 返回文本
     */
    public String getMessage() {
        return message;
    }

    /**
     * 获取结构化数据。
     *
     * @return 不可修改的结构化数据
     */
    public Map<String, Object> getData() {
        return data;
    }
}

