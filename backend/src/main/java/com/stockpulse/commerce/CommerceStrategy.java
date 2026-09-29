package com.stockpulse.commerce;

import com.stockpulse.domain.Product;
import com.stockpulse.domain.enums.TriggerReason;

/**
 * Unified commerce strategy contract.
 * Both rule-based and future AI strategies implement this interface.
 * Consumers (controllers, future event handlers) are fully decoupled from strategy choice.
 */
public interface CommerceStrategy {
    CommerceRecommendation recommend(Product product, TriggerReason triggerReason);
}
