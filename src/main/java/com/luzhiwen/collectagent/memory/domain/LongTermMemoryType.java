package com.luzhiwen.collectagent.memory.domain;

/**
 * 长期记忆类型。
 */
public enum LongTermMemoryType {

    /** 用户联系或沟通偏好。 */
    PREFERENCE("preference"),

    /** 稳定个人事实。 */
    FACT("fact"),

    /** 一次承诺或履约情景。 */
    EPISODE("episode"),

    /** 可复用处理流程。 */
    PROCEDURE("procedure");

    /** 数据库保存的小写名称。 */
    private final String value;

    /**
     * 创建记忆类型。
     *
     * @param value 数据库值
     */
    LongTermMemoryType(String value) {
        this.value = value;
    }

    /** @return 数据库值。 */
    public String getValue() { return value; }

    /**
     * 从数据库或模型值解析类型。
     *
     * @param value 类型值
     * @return 记忆类型
     */
    public static LongTermMemoryType fromValue(String value) {
        for (LongTermMemoryType type : values()) {
            if (type.value.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("不支持的长期记忆类型：" + value);
    }
}
