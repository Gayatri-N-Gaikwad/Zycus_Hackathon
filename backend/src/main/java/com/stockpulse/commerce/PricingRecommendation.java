package com.stockpulse.commerce;

import com.stockpulse.domain.enums.ChangeDirection;

import java.math.BigDecimal;

/**
 * Recommendation/result object for pricing, produced by a CommerceStrategy.
 * NOT a JPA entity — distinct from the persisted PricingSuggestion domain entity.
 */
public class PricingRecommendation {

    private final BigDecimal recommendedPrice;
    private final ChangeDirection direction;
    private final double confidence;
    private final String reasoning;

    public PricingRecommendation(BigDecimal recommendedPrice, ChangeDirection direction,
                                 double confidence, String reasoning) {
        this.recommendedPrice = recommendedPrice;
        this.direction = direction;
        this.confidence = confidence;
        this.reasoning = reasoning;
    }

    public BigDecimal getRecommendedPrice() {
        return recommendedPrice;
    }

    public ChangeDirection getDirection() {
        return direction;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getReasoning() {
        return reasoning;
    }

    @Override
    public String toString() {
        return "PricingRecommendation{" +
                "recommendedPrice=" + recommendedPrice +
                ", direction=" + direction +
                ", confidence=" + confidence +
                ", reasoning='" + reasoning + '\'' +
                '}';
    }
}
