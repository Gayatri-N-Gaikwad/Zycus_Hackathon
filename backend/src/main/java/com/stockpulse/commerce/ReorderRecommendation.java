package com.stockpulse.commerce;

/**
 * Recommendation/result object for inventory reordering, produced by a CommerceStrategy.
 * NOT a JPA entity — distinct from the persisted ReorderSuggestion domain entity.
 */
public class ReorderRecommendation {

    private final int recommendedQuantity;
    private final double confidence;
    private final String reasoning;
    private final int leadTimeDays;

    public ReorderRecommendation(int recommendedQuantity, double confidence,
                                 String reasoning, int leadTimeDays) {
        this.recommendedQuantity = recommendedQuantity;
        this.confidence = confidence;
        this.reasoning = reasoning;
        this.leadTimeDays = leadTimeDays;
    }

    public int getRecommendedQuantity() {
        return recommendedQuantity;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getReasoning() {
        return reasoning;
    }

    public int getLeadTimeDays() {
        return leadTimeDays;
    }

    @Override
    public String toString() {
        return "ReorderRecommendation{" +
                "recommendedQuantity=" + recommendedQuantity +
                ", confidence=" + confidence +
                ", reasoning='" + reasoning + '\'' +
                ", leadTimeDays=" + leadTimeDays +
                '}';
    }
}
