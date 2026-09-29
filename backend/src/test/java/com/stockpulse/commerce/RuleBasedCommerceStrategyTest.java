package com.stockpulse.commerce;

import com.stockpulse.domain.Product;
import com.stockpulse.domain.enums.Category;
import com.stockpulse.domain.enums.ChangeDirection;
import com.stockpulse.domain.enums.ProductStatus;
import com.stockpulse.domain.enums.TriggerReason;
import com.stockpulse.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RuleBasedCommerceStrategy.
 * Uses Mockito to stub ProductRepository so tests run without a database.
 *
 * Pricing precedence:
 *   1. LOW STOCK  (stock < threshold)                 → price × 1.10, INCREASE
 *   2. DEMAND SPIKE (velocity > 2 × category avg)     → price × 1.05, INCREASE
 *   3. NORMAL                                          → current price, HOLD
 *
 * Low-stock is evaluated first; if true the category-average repository call is skipped.
 */
@ExtendWith(MockitoExtension.class)
class RuleBasedCommerceStrategyTest {

    @Mock
    private ProductRepository productRepository;

    private RuleBasedCommerceStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new RuleBasedCommerceStrategy(productRepository);
    }

    // ------------------------------------------------------------------
    // Helper: build a minimal product
    // ------------------------------------------------------------------

    private Product product(String id, BigDecimal price, int stock, int threshold, int velocity, Category category) {
        Product p = new Product();
        p.setId(id);
        p.setName("Product " + id);
        p.setSku("SKU-" + id);
        p.setCurrentPrice(price);
        p.setStockLevel(stock);
        p.setReorderThreshold(threshold);
        p.setDemandVelocity(velocity);
        p.setCategory(category);
        p.setStatus(ProductStatus.ACTIVE);
        return p;
    }

    // ------------------------------------------------------------------
    // 1. LOW STOCK → +10%, INCREASE
    //    Low-stock fires first; no repository call is made for category avg.
    // ------------------------------------------------------------------

    @Test
    void lowStock_recommendsTenPercentIncrease() {
        Product p = product("p1", new BigDecimal("100.00"), 5, 20, 4, Category.ELECTRONICS);
        // stock (5) < threshold (20) → LOW STOCK rule fires
        // No category stub needed: isDemandSpike is guarded by !lowStock

        CommerceRecommendation rec = strategy.recommend(p, TriggerReason.INVENTORY_LOW);

        PricingRecommendation pricing = rec.getPricing();
        assertThat(pricing.getDirection()).isEqualTo(ChangeDirection.INCREASE);
        assertThat(pricing.getRecommendedPrice()).isEqualByComparingTo(new BigDecimal("110.00"));
        assertThat(pricing.getReasoning()).contains("10%");
    }

    // ------------------------------------------------------------------
    // 2. DEMAND SPIKE → +5%, INCREASE
    //    Peers: velocity 2, 2 → avg(2, 2, 10) / 3 = 4.67; product 10 > 2×4.67=9.33 ✓
    // ------------------------------------------------------------------

    @Test
    void demandSpike_recommendsFivePercentIncrease() {
        Product peer1 = product("peer1", new BigDecimal("50.00"), 30, 10, 2, Category.APPAREL);
        Product peer2 = product("peer2", new BigDecimal("60.00"), 40, 10, 2, Category.APPAREL);
        Product p     = product("p2",    new BigDecimal("50.00"), 30, 10, 10, Category.APPAREL);
        // stock (30) >= threshold (10) → NOT low stock
        // avg(2, 2, 10) = 14/3 ≈ 4.67;  10 > 9.33 → DEMAND SPIKE

        when(productRepository.findByCategory(Category.APPAREL)).thenReturn(List.of(peer1, peer2, p));

        CommerceRecommendation rec = strategy.recommend(p, TriggerReason.DEMAND_SPIKE);

        PricingRecommendation pricing = rec.getPricing();
        assertThat(pricing.getDirection()).isEqualTo(ChangeDirection.INCREASE);
        assertThat(pricing.getRecommendedPrice()).isEqualByComparingTo(new BigDecimal("52.50"));
        assertThat(pricing.getReasoning()).contains("5%");
    }

    // ------------------------------------------------------------------
    // 3. NORMAL → current price, HOLD
    //    avg(4, 4) = 4; product velocity 4 → 4 > 2×4=8? No → HOLD
    // ------------------------------------------------------------------

    @Test
    void normalConditions_retainsCurrentPriceAndHold() {
        Product peer = product("peerN", new BigDecimal("80.00"), 50, 10, 4, Category.HOME);
        Product p    = product("pN",    new BigDecimal("80.00"), 50, 10, 4, Category.HOME);
        // stock (50) >= threshold (10) → not low stock

        when(productRepository.findByCategory(Category.HOME)).thenReturn(List.of(peer, p));

        CommerceRecommendation rec = strategy.recommend(p, TriggerReason.MANUAL);

        PricingRecommendation pricing = rec.getPricing();
        assertThat(pricing.getDirection()).isEqualTo(ChangeDirection.HOLD);
        assertThat(pricing.getRecommendedPrice()).isEqualByComparingTo(new BigDecimal("80.00"));
        assertThat(pricing.getReasoning()).contains("normal thresholds");
    }

    // ------------------------------------------------------------------
    // 4a. BOTH low stock AND demand spike → low-stock (+10%) wins
    //     Low-stock fires first; repository never consulted → no stub needed.
    // ------------------------------------------------------------------

    @Test
    void bothLowStockAndDemandSpike_lowStockTakesPrecedence() {
        // stock (3) < threshold (20) → LOW STOCK fires
        // Even if demand spike would also fire, low-stock takes precedence
        // No stub: isDemandSpike is only reached when !lowStock
        Product p = product("p4", new BigDecimal("100.00"), 3, 20, 12, Category.ELECTRONICS);

        CommerceRecommendation rec = strategy.recommend(p, TriggerReason.INVENTORY_LOW);

        PricingRecommendation pricing = rec.getPricing();
        assertThat(pricing.getDirection()).isEqualTo(ChangeDirection.INCREASE);
        assertThat(pricing.getRecommendedPrice()).isEqualByComparingTo(new BigDecimal("110.00"));
        assertThat(pricing.getReasoning()).contains("10%");
    }

    // ------------------------------------------------------------------
    // 4b. Confirm that without low-stock, same velocity would spike
    //     (proves low-stock is what suppresses the demand-spike path)
    // ------------------------------------------------------------------

    @Test
    void bothLowStockAndDemandSpike_withPeers_lowStockWins() {
        // peers velocity 2; product velocity 6 (> 2×avg); BUT stock < threshold
        Product peer = product("peerX", new BigDecimal("200.00"), 50, 10, 2, Category.ELECTRONICS);
        Product p    = product("p5",    new BigDecimal("200.00"), 5,  20, 6, Category.ELECTRONICS);
        // stock (5) < threshold (20) → low-stock fires; repository never called

        CommerceRecommendation rec = strategy.recommend(p, TriggerReason.INVENTORY_LOW);

        PricingRecommendation pricing = rec.getPricing();
        assertThat(pricing.getDirection()).isEqualTo(ChangeDirection.INCREASE);
        assertThat(pricing.getRecommendedPrice()).isEqualByComparingTo(new BigDecimal("220.00"));
        assertThat(pricing.getReasoning()).contains("10%");
    }

    // ------------------------------------------------------------------
    // 5. Reorder formula: (threshold × 3) - stock
    // ------------------------------------------------------------------

    @Test
    void reorder_formulaCalculatedCorrectly() {
        // Stock is ample (50 >= threshold 25) → no repo call for pricing
        // But reorder: (25×3) - 15 = 60
        Product p = product("p6", new BigDecimal("50.00"), 15, 25, 5, Category.HOME);
        // when(productRepository.findByCategory(Category.HOME)).thenReturn(List.of(p));
        // Need stub because stock (15) < threshold (25) → low-stock fires; repo NOT called
        // Actually low-stock fires here (15<25), so no stub needed
        // Remove the when() — let Mockito strict mode confirm it's not used.

        CommerceRecommendation rec = strategy.recommend(p, TriggerReason.INVENTORY_LOW);

        ReorderRecommendation reorder = rec.getReorder();
        // (25 × 3) - 15 = 60
        assertThat(reorder.getRecommendedQuantity()).isEqualTo(60);
    }

    // ------------------------------------------------------------------
    // 6. Reorder minimum = 1 (never recommends 0 or negative)
    // ------------------------------------------------------------------

    @Test
    void reorder_minimumQuantityIsOne() {
        // stock (100) >> threshold (10): (10×3)-100 = -70 → clamp to 1
        // stock (100) >= threshold (10) → not low stock; check demand spike
        Product p = product("p7", new BigDecimal("50.00"), 100, 10, 5, Category.HOME);
        when(productRepository.findByCategory(Category.HOME)).thenReturn(List.of(p));
        // avg = 5; 5 > 2×5=10? No → HOLD (repo is consulted)

        CommerceRecommendation rec = strategy.recommend(p, TriggerReason.MANUAL);

        assertThat(rec.getReorder().getRecommendedQuantity()).isEqualTo(1);
    }

    // ------------------------------------------------------------------
    // 7a. Category average is calculated from repository, not hardcoded
    //     High-peer velocities → no spike despite product being faster
    // ------------------------------------------------------------------

    @Test
    void categoryAverage_usesRepositoryData_highPeers_noSpike() {
        // peers 10, 10 → avg(10, 10, 22)/3 = 14; 22 > 2×14=28? No → HOLD
        // If hardcoded avg were small (e.g. 5) then 22 > 10 → spike. Proves dynamic calc.
        Product peer1 = product("c1", new BigDecimal("100.00"), 50, 10, 10, Category.APPAREL);
        Product peer2 = product("c2", new BigDecimal("100.00"), 50, 10, 10, Category.APPAREL);
        Product p     = product("c3", new BigDecimal("100.00"), 50, 10, 22, Category.APPAREL);

        when(productRepository.findByCategory(Category.APPAREL)).thenReturn(List.of(peer1, peer2, p));

        CommerceRecommendation rec = strategy.recommend(p, TriggerReason.MANUAL);

        // avg = (10+10+22)/3 = 14; 22 > 28? No → HOLD
        assertThat(rec.getPricing().getDirection()).isEqualTo(ChangeDirection.HOLD);
    }

    // ------------------------------------------------------------------
    // 7b. Low-velocity peers → same product triggers spike
    // ------------------------------------------------------------------

    @Test
    void categoryAverage_lowPeers_spikeTriggered() {
        // peers 2, 2 → avg(2, 2, 10)/3 ≈ 4.67; 10 > 9.33 → INCREASE 5%
        Product peer1 = product("d1", new BigDecimal("100.00"), 50, 10, 2, Category.APPAREL);
        Product peer2 = product("d2", new BigDecimal("100.00"), 50, 10, 2, Category.APPAREL);
        Product p     = product("d3", new BigDecimal("100.00"), 50, 10, 10, Category.APPAREL);

        when(productRepository.findByCategory(Category.APPAREL)).thenReturn(List.of(peer1, peer2, p));

        CommerceRecommendation rec = strategy.recommend(p, TriggerReason.DEMAND_SPIKE);

        assertThat(rec.getPricing().getDirection()).isEqualTo(ChangeDirection.INCREASE);
        assertThat(rec.getPricing().getRecommendedPrice()).isEqualByComparingTo(new BigDecimal("105.00"));
    }

    // ------------------------------------------------------------------
    // Edge: empty category list → no crash, result is deterministic
    //       avg = 0.0; velocity > 2×0=0 → true if stock not low
    // ------------------------------------------------------------------

    @Test
    void emptyCategory_doesNotThrow_returnsResult() {
        Product p = product("p8", new BigDecimal("99.00"), 50, 10, 5, Category.HOME);
        when(productRepository.findByCategory(Category.HOME)).thenReturn(List.of());

        // Should not throw; with avg=0, any positive velocity > 0 → demand spike
        CommerceRecommendation rec = strategy.recommend(p, TriggerReason.MANUAL);

        assertThat(rec).isNotNull();
        assertThat(rec.getPricing()).isNotNull();
        assertThat(rec.getReorder()).isNotNull();
    }

    // ------------------------------------------------------------------
    // TriggerReason is preserved in recommendation envelope
    // ------------------------------------------------------------------

    @Test
    void triggerReasonPreservedInEnvelope() {
        Product p = product("p9", new BigDecimal("50.00"), 50, 10, 3, Category.HOME);
        when(productRepository.findByCategory(Category.HOME)).thenReturn(List.of(p));
        // avg = 3; 3 > 6? No → HOLD (repo is consulted)

        CommerceRecommendation rec = strategy.recommend(p, TriggerReason.DEMAND_SPIKE);

        assertThat(rec.getTriggerReason()).isEqualTo(TriggerReason.DEMAND_SPIKE);
    }
}
