package com.luzhiwen.collectagent.memory.application;

import com.luzhiwen.collectagent.memory.domain.LongTermMemoryCandidate;
import com.luzhiwen.collectagent.memory.domain.LongTermMemoryType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 长期记忆候选校验器。
 */
@Component
public class LongTermMemoryCandidateValidator {

    /** preference 允许字段。 */
    private static final Set<String> PREFERENCE_FIELDS = Set.of("channel", "time_window");

    /** preference channel 值。 */
    private static final Set<String> PREFERENCE_CHANNELS = Set.of("sms", "phone", "wechat");

    /** preference time_window 值。 */
    private static final Set<String> PREFERENCE_TIMES = Set.of("morning", "afternoon", "evening");

    /** fact 允许 fact_key。 */
    private static final Set<String> FACT_KEYS = Set.of(
            "employment_status", "family_status", "health_status", "financial_status"
    );

    /** episode 允许字段。 */
    private static final Set<String> EPISODE_FIELDS = Set.of("promise_date", "promise_amount", "fulfilled");

    /** 身份证、银行卡等长数字敏感信息。 */
    private static final Pattern SENSITIVE_NUMBER = Pattern.compile("(?<!\\d)\\d{15,19}(?!\\d)");

    /**
     * 校验候选。
     *
     * @param candidate 原始候选
     * @param validEventIds 本批次合法事件ID
     * @return 校验后的候选
     */
    public Optional<LongTermMemoryCandidate> validate(
            LongTermMemoryCandidate candidate,
            Set<String> validEventIds
    ) {
        if (candidate == null || candidate.getMemoryType() == null) {
            return Optional.empty();
        }
        String content = LongTermMemoryContentUtil.normalize(candidate.getContent());
        if (content.isBlank() || content.length() > 500 || SENSITIVE_NUMBER.matcher(content).find()) {
            return Optional.empty();
        }
        if (candidate.getConfidence() < 0.0D || candidate.getConfidence() > 1.0D) {
            return Optional.empty();
        }
        List<String> sourceEventIds = candidate.getSourceEventIds() == null
                ? List.of()
                : candidate.getSourceEventIds().stream()
                .filter(validEventIds::contains)
                .distinct()
                .toList();
        if (sourceEventIds.isEmpty()) {
            return Optional.empty();
        }
        Map<String, Object> structured = validateStructured(candidate.getMemoryType(), candidate.getStructured());
        return Optional.of(new LongTermMemoryCandidate(
                candidate.getMemoryType(),
                content,
                structured,
                sourceEventIds,
                candidate.getConfidence()
        ));
    }

    /**
     * 按类型校验结构化字段，非法 key 会被丢弃。
     *
     * @param type 记忆类型
     * @param input 原始结构化字段
     * @return 合法结构化字段
     */
    private Map<String, Object> validateStructured(LongTermMemoryType type, Map<String, Object> input) {
        if (input == null || input.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        switch (type) {
            case PREFERENCE -> validatePreference(input, result);
            case FACT -> validateFact(input, result);
            case EPISODE -> validateEpisode(input, result);
            case PROCEDURE -> {
                // 第一版 procedure 暂不开放结构化字段。
            }
        }
        return result;
    }

    /**
     * 校验偏好字段。
     *
     * @param input 输入
     * @param result 输出
     */
    private void validatePreference(Map<String, Object> input, Map<String, Object> result) {
        Object channel = input.get("channel");
        if (channel != null && PREFERENCE_CHANNELS.contains(String.valueOf(channel))) {
            result.put("channel", String.valueOf(channel));
        }
        Object timeWindow = input.get("time_window");
        if (timeWindow != null && PREFERENCE_TIMES.contains(String.valueOf(timeWindow))) {
            result.put("time_window", String.valueOf(timeWindow));
        }
        result.keySet().retainAll(PREFERENCE_FIELDS);
    }

    /**
     * 校验事实字段。
     *
     * @param input 输入
     * @param result 输出
     */
    private void validateFact(Map<String, Object> input, Map<String, Object> result) {
        Object factKey = input.get("fact_key");
        Object factValue = input.get("fact_value");
        if (factKey != null && FACT_KEYS.contains(String.valueOf(factKey))
                && factValue != null && !String.valueOf(factValue).isBlank()) {
            result.put("fact_key", String.valueOf(factKey));
            result.put("fact_value", String.valueOf(factValue));
        }
    }

    /**
     * 校验承诺情景字段。
     *
     * @param input 输入
     * @param result 输出
     */
    private void validateEpisode(Map<String, Object> input, Map<String, Object> result) {
        Object dateValue = input.get("promise_date");
        if (dateValue != null) {
            try {
                result.put("promise_date", LocalDate.parse(String.valueOf(dateValue)).toString());
            } catch (Exception ignored) {
                // 非法日期直接丢弃。
            }
        }
        Object amountValue = input.get("promise_amount");
        if (amountValue != null) {
            try {
                BigDecimal amount = new BigDecimal(String.valueOf(amountValue));
                if (amount.signum() > 0) {
                    result.put("promise_amount", amount);
                }
            } catch (Exception ignored) {
                // 非法金额直接丢弃。
            }
        }
        Object fulfilled = input.get("fulfilled");
        if (fulfilled instanceof Boolean booleanValue) {
            result.put("fulfilled", booleanValue);
        }
        result.keySet().retainAll(EPISODE_FIELDS);
    }
}

