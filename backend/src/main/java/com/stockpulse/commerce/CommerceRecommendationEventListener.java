package com.stockpulse.commerce;

import com.stockpulse.domain.PricingSuggestion;
import com.stockpulse.domain.Product;
import com.stockpulse.domain.ReorderSuggestion;
import com.stockpulse.domain.enums.SuggestionStatus;
import com.stockpulse.repository.PricingSuggestionRepository;
import com.stockpulse.repository.ReorderSuggestionRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.annotation.Propagation;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class CommerceRecommendationEventListener {

    private final CommerceStrategy commerceStrategy;
    private final PricingSuggestionRepository pricingSuggestionRepository;
    private final ReorderSuggestionRepository reorderSuggestionRepository;
    private final ConcurrentMap<String, Object> eventLocks = new ConcurrentHashMap<>();

    public CommerceRecommendationEventListener(
            @Qualifier("AI") CommerceStrategy commerceStrategy,
            PricingSuggestionRepository pricingSuggestionRepository,
            ReorderSuggestionRepository reorderSuggestionRepository) {
        this.commerceStrategy = commerceStrategy;
        this.pricingSuggestionRepository = pricingSuggestionRepository;
        this.reorderSuggestionRepository = reorderSuggestionRepository;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(CommerceRecommendationEvent event) {
        Product product = event.product();
        String lockKey = product.getId() + ":" + event.triggerReason();
        synchronized (eventLocks.computeIfAbsent(lockKey, ignored -> new Object())) {
            createPendingSuggestions(event, product);
        }
    }

    private void createPendingSuggestions(CommerceRecommendationEvent event, Product product) {
        CommerceRecommendation recommendation = commerceStrategy.recommend(product, event.triggerReason());

        if (!pricingSuggestionRepository.existsByProductIdAndTriggerReasonAndStatus(
                product.getId(), event.triggerReason(), SuggestionStatus.PENDING)) {
            PricingRecommendation pricing = recommendation.getPricing();
            pricingSuggestionRepository.save(new PricingSuggestion(
                    product.getId(),
                    product.getCurrentPrice(),
                    pricing.getRecommendedPrice(),
                    pricing.getDirection(),
                    pricing.getConfidence(),
                    pricing.getReasoning(),
                    event.triggerReason()
            ));
        }

        if (!reorderSuggestionRepository.existsByProductIdAndTriggerReasonAndStatus(
                product.getId(), event.triggerReason(), SuggestionStatus.PENDING)) {
            ReorderRecommendation reorder = recommendation.getReorder();
            reorderSuggestionRepository.save(new ReorderSuggestion(
                    product.getId(),
                    product.getStockLevel(),
                    reorder.getRecommendedQuantity(),
                    reorder.getLeadTimeDays(),
                    reorder.getConfidence(),
                    reorder.getReasoning(),
                    event.triggerReason()
            ));
        }
    }
}