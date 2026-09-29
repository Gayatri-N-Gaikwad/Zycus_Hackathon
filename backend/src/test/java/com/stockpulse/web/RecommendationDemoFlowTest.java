package com.stockpulse.web;

import com.stockpulse.domain.PricingSuggestion;
import com.stockpulse.domain.Product;
import com.stockpulse.domain.ReorderSuggestion;
import com.stockpulse.domain.enums.Category;
import com.stockpulse.domain.enums.ProductStatus;
import com.stockpulse.domain.enums.SuggestionStatus;
import com.stockpulse.repository.PricingSuggestionRepository;
import com.stockpulse.repository.ProductRepository;
import com.stockpulse.repository.ReorderSuggestionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RecommendationDemoFlowTest {

    private static final String PRODUCT_ID = "demo-flow-product";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PricingSuggestionRepository pricingSuggestionRepository;

    @Autowired
    private ReorderSuggestionRepository reorderSuggestionRepository;

    @AfterEach
    void cleanUp() {
        pricingSuggestionRepository.deleteAll(pricingSuggestionRepository.findByProductIdAndStatus(
                PRODUCT_ID, SuggestionStatus.PENDING));
        reorderSuggestionRepository.deleteAll(reorderSuggestionRepository.findByProductIdAndStatus(
                PRODUCT_ID, SuggestionStatus.PENDING));
        productRepository.deleteById(PRODUCT_ID);
    }

    @Test
    void recordSaleCreatesBothPendingSuggestionsAndAcceptingPricingUpdatesProduct() throws Exception {
        Product product = new Product(PRODUCT_ID, "DEMO-FLOW-001", "Demo Flow Product", Category.HOME,
                new BigDecimal("40.00"), 20, 10, 1, ProductStatus.ACTIVE);
        productRepository.save(product);

        mockMvc.perform(put("/api/products/{id}", PRODUCT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stockLevel\":9}"))
                .andExpect(status().isOk());

        awaitPendingSuggestions();

        List<PricingSuggestion> pricing = pricingSuggestionRepository.findByProductIdAndStatus(
                PRODUCT_ID, SuggestionStatus.PENDING);
        List<ReorderSuggestion> reorder = reorderSuggestionRepository.findByProductIdAndStatus(
                PRODUCT_ID, SuggestionStatus.PENDING);
        assertThat(pricing).hasSize(1);
        assertThat(reorder).hasSize(1);

        mockMvc.perform(post("/api/products/{id}/pricing-suggestions/{suggestionId}/accept",
                        PRODUCT_ID, pricing.get(0).getId()))
                .andExpect(status().isOk());

        Product updated = productRepository.findById(PRODUCT_ID).orElseThrow();
        PricingSuggestion accepted = pricingSuggestionRepository.findById(pricing.get(0).getId()).orElseThrow();
        assertThat(updated.getCurrentPrice()).isEqualByComparingTo(pricing.get(0).getRecommendedPrice());
        assertThat(accepted.getStatus()).isEqualTo(SuggestionStatus.ACCEPTED);
    }

    private void awaitPendingSuggestions() throws InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(3));
        while (Instant.now().isBefore(deadline)) {
            boolean pricingReady = pricingSuggestionRepository.existsByProductIdAndTriggerReasonAndStatus(
                    PRODUCT_ID, com.stockpulse.domain.enums.TriggerReason.INVENTORY_LOW, SuggestionStatus.PENDING);
            boolean reorderReady = reorderSuggestionRepository.existsByProductIdAndTriggerReasonAndStatus(
                    PRODUCT_ID, com.stockpulse.domain.enums.TriggerReason.INVENTORY_LOW, SuggestionStatus.PENDING);
            if (pricingReady && reorderReady) {
                return;
            }
            Thread.sleep(25);
        }
        throw new AssertionError("Timed out waiting for async pending suggestions");
    }
}