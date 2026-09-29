package com.stockpulse.commerce;

import com.stockpulse.domain.Product;
import com.stockpulse.domain.enums.ChangeDirection;
import com.stockpulse.domain.enums.TriggerReason;
import com.stockpulse.repository.ProductRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Deterministic, rule-based CommerceStrategy.
 *
 * Pricing precedence (evaluated in order, first match wins):
 *   1. LOW STOCK  — stock < reorderThreshold         → price × 1.10, INCREASE
 *   2. DEMAND SPIKE — demandVelocity > 2× category avg → price × 1.05, INCREASE
 *   3. NORMAL — neither condition                     → current price, HOLD
 *
 * If BOTH low-stock and demand-spike are true, LOW STOCK takes precedence
 * because inventory constraint is the stronger signal.
 *
 * Reorder:
 *   quantity = max(1, (reorderThreshold × 3) - currentStock)
 */
@Component("RULE_BASED")
public class RuleBasedCommerceStrategy implements CommerceStrategy {

    private static final double PRICING_CONFIDENCE_LOW_STOCK    = 0.90;
    private static final double PRICING_CONFIDENCE_DEMAND_SPIKE = 0.80;
    private static final double PRICING_CONFIDENCE_NORMAL       = 0.95;
    private static final double REORDER_CONFIDENCE              = 0.90;

    private static final int    DEFAULT_LEAD_TIME_DAYS          = 7;

    private final ProductRepository productRepository;

    public RuleBasedCommerceStrategy(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public CommerceRecommendation recommend(Product product, TriggerReason triggerReason) {
        PricingRecommendation  pricing = buildPricingRecommendation(product);
        ReorderRecommendation reorder = buildReorderRecommendation(product);
        return new CommerceRecommendation(pricing, reorder, triggerReason);
    }

    // ------------------------------------------------------------------
    // Pricing
    // ------------------------------------------------------------------

    private PricingRecommendation buildPricingRecommendation(Product product) {
        boolean lowStock     = isLowStock(product);
        boolean demandSpike  = !lowStock && isDemandSpike(product);

        if (lowStock) {
            BigDecimal recommended = product.getCurrentPrice()
                    .multiply(BigDecimal.valueOf(1.10))
                    .setScale(2, RoundingMode.HALF_UP);
            return new PricingRecommendation(
                    recommended,
                    ChangeDirection.INCREASE,
                    PRICING_CONFIDENCE_LOW_STOCK,
                    "Stock is below the reorder threshold, so a 10% price increase is recommended while inventory is constrained."
            );
        }

        if (demandSpike) {
            BigDecimal recommended = product.getCurrentPrice()
                    .multiply(BigDecimal.valueOf(1.05))
                    .setScale(2, RoundingMode.HALF_UP);
            return new PricingRecommendation(
                    recommended,
                    ChangeDirection.INCREASE,
                    PRICING_CONFIDENCE_DEMAND_SPIKE,
                    "Demand velocity is more than 2x the category average, so a modest 5% price increase is recommended."
            );
        }

        return new PricingRecommendation(
                product.getCurrentPrice().setScale(2, RoundingMode.HALF_UP),
                ChangeDirection.HOLD,
                PRICING_CONFIDENCE_NORMAL,
                "Inventory and demand are within normal thresholds, so the current price is retained."
        );
    }

    private boolean isLowStock(Product product) {
        return product.getStockLevel() < product.getReorderThreshold();
    }

    private boolean isDemandSpike(Product product) {
        double categoryAvg = categoryAverageDemandVelocity(product);
        return product.getDemandVelocity() > 2.0 * categoryAvg;
    }

    /**
     * Calculates the average demand velocity for all products in the same category.
     * The product itself is included in the average (consistent, reproducible metric).
     * Returns 0.0 if no products exist in the category (prevents division-by-zero).
     */
    double categoryAverageDemandVelocity(Product product) {
        List<Product> peers = productRepository.findByCategory(product.getCategory());
        if (peers == null || peers.isEmpty()) {
            return 0.0;
        }
        return peers.stream()
                .mapToInt(Product::getDemandVelocity)
                .average()
                .orElse(0.0);
    }

    // ------------------------------------------------------------------
    // Reorder
    // ------------------------------------------------------------------

    private ReorderRecommendation buildReorderRecommendation(Product product) {
        int quantity = (product.getReorderThreshold() * 3) - product.getStockLevel();
        if (quantity < 1) {
            quantity = 1;
        }
        return new ReorderRecommendation(
                quantity,
                REORDER_CONFIDENCE,
                "Recommended reorder quantity is calculated as three times the reorder threshold minus current stock.",
                DEFAULT_LEAD_TIME_DAYS
        );
    }
}
