package com.stockpulse.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpulse.commerce.CommerceRecommendation;
import com.stockpulse.commerce.CommerceStrategy;
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
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AICommerceStrategyTest {

    @Mock
    private LLMGateway llmGateway;

    @Mock
    private CommerceStrategy ruleBasedStrategy;

    @Mock
    private ProductRepository productRepository;

    private AICommerceStrategy strategy;
    private Product product;

    @BeforeEach
    void setUp() {
        strategy = new AICommerceStrategy(llmGateway, new ObjectMapper(), ruleBasedStrategy, productRepository);
        product = new Product("p1", "SKU-1", "Widget", Category.ELECTRONICS,
                new BigDecimal("100.00"), 5, 20, 12, ProductStatus.ACTIVE);
    }

    @Test
    void inventoryLowPromptContainsInventoryTradeoffInputs() {
        String prompt = strategy.buildPrompt(product, TriggerReason.INVENTORY_LOW);

        assertThat(prompt).contains("stock=5", "reorderThreshold=20", "currentPrice=100.00", "demandVelocity=12");
        assertThat(prompt).contains("scarcity pricing", "customer retention");
        assertThat(prompt).doesNotContain("categoryAverageVelocity");
    }

    @Test
    void demandSpikePromptContainsCategoryAverageTradeoffInputs() {
        Product peer = new Product();
        peer.setDemandVelocity(4);
        when(productRepository.findByCategory(Category.ELECTRONICS)).thenReturn(List.of(peer, product));

        String prompt = strategy.buildPrompt(product, TriggerReason.DEMAND_SPIKE);

        assertThat(prompt).contains("stock=5", "currentPrice=100.00", "demandVelocity=12", "categoryAverageVelocity=8.0");
        assertThat(prompt).contains("conversion loss", "temporary demand spike");
        assertThat(prompt).doesNotContain("reorderThreshold=20");
    }

    @Test
    void validResponseIsParsedIntoCommerceRecommendation() {
        when(llmGateway.complete(contains("scarcity pricing"))).thenReturn(validResponse());

        CommerceRecommendation recommendation = strategy.recommend(product, TriggerReason.INVENTORY_LOW);

        assertThat(recommendation.getPricing().getRecommendedPrice()).isEqualByComparingTo("110.00");
        assertThat(recommendation.getPricing().getDirection()).isEqualTo(ChangeDirection.INCREASE);
        assertThat(recommendation.getReorder().getRecommendedQuantity()).isEqualTo(30);
        assertThat(recommendation.getTriggerReason()).isEqualTo(TriggerReason.INVENTORY_LOW);
    }

    @Test
    void invalidResponseFallsBackToRuleBasedStrategy() {
        CommerceRecommendation fallback = new CommerceRecommendation(null, null, TriggerReason.DEMAND_SPIKE);
        when(productRepository.findByCategory(Category.ELECTRONICS)).thenReturn(List.of(product));
        when(llmGateway.complete(contains("conversion loss"))).thenReturn("{\"recommendedPrice\": -1}");
        when(ruleBasedStrategy.recommend(product, TriggerReason.DEMAND_SPIKE)).thenReturn(fallback);

        assertThat(strategy.recommend(product, TriggerReason.DEMAND_SPIKE)).isSameAs(fallback);
    }

    private String validResponse() {
        return "{" +
                "\"recommendedPrice\":110.00," +
                "\"priceDirection\":\"INCREASE\"," +
                "\"priceConfidence\":0.85," +
                "\"priceReasoning\":\"Scarcity supports a measured increase.\"," +
                "\"recommendedQuantity\":30," +
                "\"reorderConfidence\":0.90," +
                "\"reorderReasoning\":\"Restore stock above threshold.\"" +
                "}";
    }
}