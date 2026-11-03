package com.luzhiwen.collectagent.memory.application;

import java.util.List;

/**
 * 向量余弦相似度工具。
 */
public final class VectorSimilarityUtil {

    /** 工具类不允许实例化。 */
    private VectorSimilarityUtil() {
    }

    /**
     * 计算余弦相似度。
     *
     * @param left 左向量
     * @param right 右向量
     * @return 0~1 的余弦相似度
     */
    public static double cosine(List<Float> left, List<Float> right) {
        if (left == null || right == null || left.size() != right.size() || left.isEmpty()) {
            return 0.0D;
        }
        double dot = 0.0D;
        double leftNorm = 0.0D;
        double rightNorm = 0.0D;
        for (int index = 0; index < left.size(); index++) {
            double leftValue = left.get(index);
            double rightValue = right.get(index);
            dot += leftValue * rightValue;
            leftNorm += leftValue * leftValue;
            rightNorm += rightValue * rightValue;
        }
        if (leftNorm == 0.0D || rightNorm == 0.0D) {
            return 0.0D;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }
}
