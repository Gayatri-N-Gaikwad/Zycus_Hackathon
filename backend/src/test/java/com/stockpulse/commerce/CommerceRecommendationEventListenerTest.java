package com.stockpulse.commerce;

import com.stockpulse.domain.Product;
import com.stockpulse.domain.enums.Category;
import com.stockpulse.domain.enums.ChangeDirection;
import com.stockpulse.domain.enums.ProductStatus;
import com.stockpulse.domain.enums.SuggestionStatus;
import com.stockpulse.domain.enums.TriggerReason;
import com.stockpulse.repository.PricingSuggestionRepository;
import com.stockpulse.repository.ReorderSuggestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommerceRecommendationEventListenerTest {

    @Mock
    private CommerceStrategy commerceStrategy;

    @Mock
    private PricingSuggestionRepository pricingSuggestionRepository;

    @Mock
    private ReorderSuggestionRepository reorderSuggestionRepository;

    private CommerceRecommendationEventListener listener;
    private Product product;

    @BeforeEach
    void setUp() {
        listener = new CommerceRecommendationEventListener(
                commerceStrategy, pricingSuggestionRepository, reorderSuggestionRepository);
        product = new Product("p1", "SKU-1", "Widget", Category.ELECTRONICS,
                new BigDecimal("100.00"), 5, 20, 12, ProductStatus.ACTIVE);
    }

    @Test
    void lowStockTriggerGeneratesBothSuggestionTypes() {
        when(commerceStrategy.recommend(product, TriggerReason.INVENTORY_LOW))
                .thenReturn(recommendation(TriggerReason.INVENTORY_LOW));

        listener.handle(new CommerceRecommendationEvent(product, TriggerReason.INVENTORY_LOW));

        verify(commerceStrategy).recommend(product, TriggerReason.INVENTORY_LOW);
        verify(pricingSuggestionRepository).save(any());
        verify(reorderSuggestionRepository).save(any());
    }

    @Test
    void demandSpikeTriggerGeneratesBothSuggestionTypes() {
        when(commerceStrategy.recommend(product, TriggerReason.DEMAND_SPIKE))
                .thenReturn(recommendation(TriggerReason.DEMAND_SPIKE));

        listener.handle(new CommerceRecommendationEvent(product, TriggerReason.DEMAND_SPIKE));

        verify(commerceStrategy).recommend(product, TriggerReason.DEMAND_SPIKE);
        verify(pricingSuggestionRepository).save(any());
        verify(reorderSuggestionRepository).save(any());
    }

    @Test
    void duplicatePendingSuggestionsAreNotCreated() {
        when(commerceStrategy.recommend(product, TriggerReason.INVENTORY_LOW))
                .thenReturn(recommendation(TriggerReason.INVENTORY_LOW));
        when(pricingSuggestionRepository.existsByProductIdAndTriggerReasonAndStatus(
                "p1", TriggerReason.INVENTORY_LOW, SuggestionStatus.PENDING)).thenReturn(true);
        when(reorderSuggestionRepository.existsByProductIdAndTriggerReasonAndStatus(
                "p1", TriggerReason.INVENTORY_LOW, SuggestionStatus.PENDING)).thenReturn(true);

        listener.handle(new CommerceRecommendationEvent(product, TriggerReason.INVENTORY_LOW));

        verify(pricingSuggestionRepository, never()).save(any());
        verify(reorderSuggestionRepository, never()).save(any());
    }

    private CommerceRecommendation recommendation(TriggerReason triggerReason) {
        return new CommerceRecommendation(
                new PricingRecommendation(new BigDecimal("110.00"), ChangeDirection.INCREASE, 0.9, "Pricing reason"),
                new ReorderRecommendation(55, 0.9, "Reorder reason", 7),
                triggerReason);
    }
}