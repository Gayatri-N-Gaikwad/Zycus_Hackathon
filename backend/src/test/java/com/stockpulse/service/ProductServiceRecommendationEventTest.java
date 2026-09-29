package com.stockpulse.service;

import com.stockpulse.commerce.CommerceStrategy;
import com.stockpulse.commerce.CommerceRecommendationEvent;
import com.stockpulse.domain.Product;
import com.stockpulse.domain.enums.Category;
import com.stockpulse.domain.enums.ProductStatus;
import com.stockpulse.domain.enums.TriggerReason;
import com.stockpulse.repository.PricingSuggestionRepository;
import com.stockpulse.repository.ProductRepository;
import com.stockpulse.repository.ReorderSuggestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceRecommendationEventTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PricingSuggestionRepository pricingSuggestionRepository;

    @Mock
    private ReorderSuggestionRepository reorderSuggestionRepository;

    @Mock
    private CommerceStrategy commerceStrategy;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ProductService productService;
    private Product product;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, pricingSuggestionRepository,
                reorderSuggestionRepository, commerceStrategy, eventPublisher);
        product = new Product("p1", "SKU-1", "Widget", Category.ELECTRONICS,
                new BigDecimal("100.00"), 50, 20, 4, ProductStatus.ACTIVE);
        when(productRepository.findById("p1")).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
    }

    @Test
    void lowStockUpdatePublishesInventoryLowEvent() {
        productService.updateStock("p1", 5);

        CommerceRecommendationEvent event = publishedEvent();
        assertThat(event.product()).isSameAs(product);
        assertThat(event.triggerReason()).isEqualTo(TriggerReason.INVENTORY_LOW);
    }

    @Test
    void demandSpikeUpdatePublishesDemandSpikeEvent() {
        when(productRepository.findByCategory(Category.ELECTRONICS)).thenReturn(List.of());
        Product update = new Product();
        update.setDemandVelocity(10);

        productService.updateProduct("p1", update);

        assertThat(publishedEvent().triggerReason()).isEqualTo(TriggerReason.DEMAND_SPIKE);
    }

    private CommerceRecommendationEvent publishedEvent() {
        ArgumentCaptor<CommerceRecommendationEvent> captor =
                ArgumentCaptor.forClass(CommerceRecommendationEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        return captor.getValue();
    }
}