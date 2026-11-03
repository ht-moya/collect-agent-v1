package com.luzhiwen.collectagent.memory.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 长期记忆文本归一化和哈希工具。
 */
public final class LongTermMemoryContentUtil {

    /** 工具类不允许实例化。 */
    private LongTermMemoryContentUtil() {
    }

    /**
     * 归一化内容，统一空白并去除首尾空格。
     *
     * @param content 原始内容
     * @return 归一化内容
     */
    public static String normalize(String content) {
        return content == null ? "" : content.trim().replaceAll("\\s+", " ");
    }

    /**
     * 计算 UTF-8 内容的 MD5。
     *
     * @param content 归一化后的内容
     * @return 32位小写MD5
     */
    public static String md5(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 JDK 不支持 MD5", exception);
        }
    }
}
