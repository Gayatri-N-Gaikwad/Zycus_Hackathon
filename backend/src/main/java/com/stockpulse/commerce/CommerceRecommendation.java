package com.stockpulse.commerce;

import com.stockpulse.domain.enums.TriggerReason;

/**
 * Unified recommendation result produced by a CommerceStrategy.
 * Contains both a pricing recommendation and a reorder recommendation.
 * Consumers receive both signals in a single call, keeping them independent
 * of the underlying strategy implementation.
 */
public class CommerceRecommendation {

    private final PricingRecommendation pricing;
    private final ReorderRecommendation reorder;
    private final TriggerReason triggerReason;

    public CommerceRecommendation(PricingRecommendation pricing,
                                  ReorderRecommendation reorder,
                                  TriggerReason triggerReason) {
        this.pricing = pricing;
        this.reorder = reorder;
        this.triggerReason = triggerReason;
    }

    public PricingRecommendation getPricing() {
        return pricing;
    }

    public ReorderRecommendation getReorder() {
        return reorder;
    }

    public TriggerReason getTriggerReason() {
        return triggerReason;
    }

    @Override
    public String toString() {
        return "CommerceRecommendation{" +
                "pricing=" + pricing +
                ", reorder=" + reorder +
                ", triggerReason=" + triggerReason +
                '}';
    }
}
