package com.stockpulse.commerce;

import com.stockpulse.domain.Product;
import com.stockpulse.domain.enums.TriggerReason;

public record CommerceRecommendationEvent(Product product, TriggerReason triggerReason) {
}