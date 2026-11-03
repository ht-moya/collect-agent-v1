package com.luzhiwen.collectagent.memory.application;

import java.security.SecureRandom;

/**
 * 26位 ULID 生成器。
 */
public final class UlidGenerator {

    /** Crockford Base32 字符表。 */
    private static final char[] ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();

    /** 安全随机数生成器。 */
    private static final SecureRandom RANDOM = new SecureRandom();

    /** 工具类不允许实例化。 */
    private UlidGenerator() {
    }

    /**
     * 生成 ULID。
     *
     * @return 26位 ULID
     */
    public static String next() {
        char[] value = new char[26];
        long timestamp = System.currentTimeMillis();
        for (int index = 9; index >= 0; index--) {
            value[index] = ALPHABET[(int) (timestamp & 31)];
            timestamp >>>= 5;
        }
        for (int index = 10; index < 26; index++) {
            value[index] = ALPHABET[RANDOM.nextInt(ALPHABET.length)];
        }
        return new String(value);
    }
}
