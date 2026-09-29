package com.stockpulse.service;

import com.stockpulse.commerce.CommerceRecommendation;
import com.stockpulse.commerce.CommerceRecommendationEvent;
import com.stockpulse.commerce.CommerceStrategy;
import com.stockpulse.domain.PricingSuggestion;
import com.stockpulse.domain.Product;
import com.stockpulse.domain.ReorderSuggestion;
import com.stockpulse.domain.enums.Category;
import com.stockpulse.domain.enums.ProductStatus;
import com.stockpulse.domain.enums.SuggestionStatus;
import com.stockpulse.domain.enums.TriggerReason;
import com.stockpulse.repository.PricingSuggestionRepository;
import com.stockpulse.repository.ProductRepository;
import com.stockpulse.repository.ReorderSuggestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final PricingSuggestionRepository pricingSuggestionRepository;
    private final ReorderSuggestionRepository reorderSuggestionRepository;
    private final CommerceStrategy commerceStrategy;
    private final ApplicationEventPublisher eventPublisher;

    public ProductService(ProductRepository productRepository,
                          PricingSuggestionRepository pricingSuggestionRepository,
                          ReorderSuggestionRepository reorderSuggestionRepository,
                          @Qualifier("RULE_BASED") CommerceStrategy commerceStrategy,
                          ApplicationEventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.pricingSuggestionRepository = pricingSuggestionRepository;
        this.reorderSuggestionRepository = reorderSuggestionRepository;
        this.commerceStrategy = commerceStrategy;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Product> getProducts(ProductStatus status, Category category) {
        if (status != null && category != null) {
            return productRepository.findByStatusAndCategory(status, category);
        } else if (status != null) {
            return productRepository.findByStatus(status);
        } else if (category != null) {
            return productRepository.findByCategory(category);
        }
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Product> getProductById(String id) {
        return productRepository.findById(id);
    }

    public Product createProduct(Product product) {
        Product saved = productRepository.save(product);
        publishRecommendationEventIfTriggered(saved);
        return saved;
    }

    public Product updateProduct(String id, Product productDetails) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));

        if (productDetails.getSku() != null) {
            product.setSku(productDetails.getSku());
        }
        if (productDetails.getName() != null) {
            product.setName(productDetails.getName());
        }
        if (productDetails.getCategory() != null) {
            product.setCategory(productDetails.getCategory());
        }
        if (productDetails.getCurrentPrice() != null) {
            product.setCurrentPrice(productDetails.getCurrentPrice());
        }
        if (productDetails.getStockLevel() != null) {
            product.setStockLevel(productDetails.getStockLevel());
        }
        if (productDetails.getReorderThreshold() != null) {
            product.setReorderThreshold(productDetails.getReorderThreshold());
        }
        if (productDetails.getDemandVelocity() != null) {
            product.setDemandVelocity(productDetails.getDemandVelocity());
        }
        if (productDetails.getStatus() != null) {
            product.setStatus(productDetails.getStatus());
        }
        if (productDetails.getCostPrice() != null) {
            product.setCostPrice(productDetails.getCostPrice());
        }
        if (productDetails.getMarginFloor() != null) {
            product.setMarginFloor(productDetails.getMarginFloor());
        }
        if (productDetails.getSupplierId() != null) {
            product.setSupplierId(productDetails.getSupplierId());
        }

        Product saved = productRepository.save(product);
        publishRecommendationEventIfTriggered(saved);
        return saved;
    }

    public Product updateStock(String id, Integer newStockLevel) {
        if (newStockLevel < 0) {
            throw new IllegalArgumentException("Stock level cannot be negative");
        }
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));
        product.setStockLevel(newStockLevel);
        if (newStockLevel == 0) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
        } else if (product.getStatus() == ProductStatus.OUT_OF_STOCK) {
            product.setStatus(ProductStatus.ACTIVE);
        }
        Product saved = productRepository.save(product);
        publishRecommendationEventIfTriggered(saved);
        return saved;
    }

    public Product processOrder(String id, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Order quantity must be positive");
        }
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));
        int remaining = product.getStockLevel() - quantity;
        if (remaining < 0) {
            throw new IllegalStateException("Insufficient stock for product id: " + id);
        }
        product.setStockLevel(remaining);
        if (remaining == 0) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
        }
        Product saved = productRepository.save(product);
        publishRecommendationEventIfTriggered(saved);
        return saved;
    }

    public void deleteProduct(String id) {
        productRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<PricingSuggestion> getPricingSuggestions(String productId, SuggestionStatus status) {
        if (status != null) {
            return pricingSuggestionRepository.findByProductIdAndStatus(productId, status);
        }
        return pricingSuggestionRepository.findAll().stream()
                .filter(s -> s.getProductId().equals(productId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReorderSuggestion> getReorderSuggestions(String productId, SuggestionStatus status) {
        if (status != null) {
            return reorderSuggestionRepository.findByProductIdAndStatus(productId, status);
        }
        return reorderSuggestionRepository.findAll().stream()
                .filter(s -> s.getProductId().equals(productId))
                .toList();
    }

    public PricingSuggestion createPricingSuggestion(PricingSuggestion suggestion) {
        return pricingSuggestionRepository.save(suggestion);
    }

    public ReorderSuggestion createReorderSuggestion(ReorderSuggestion suggestion) {
        return reorderSuggestionRepository.save(suggestion);
    }

    public Optional<PricingSuggestion> suggestPricing(String productId) {
        return productRepository.findById(productId)
                .map(product -> {
                    CommerceRecommendation recommendation = commerceStrategy.recommend(product, TriggerReason.MANUAL);
                    var pricing = recommendation.getPricing();
                    PricingSuggestion suggestion = new PricingSuggestion(
                            product.getId(),
                            product.getCurrentPrice(),
                            pricing.getRecommendedPrice(),
                            pricing.getDirection(),
                            pricing.getConfidence(),
                            pricing.getReasoning(),
                            TriggerReason.MANUAL
                    );
                    suggestion.setStatus(SuggestionStatus.PENDING);
                    return pricingSuggestionRepository.save(suggestion);
                });
    }

    public Optional<ReorderSuggestion> suggestReorder(String productId) {
        return productRepository.findById(productId)
                .map(product -> {
                    CommerceRecommendation recommendation = commerceStrategy.recommend(product, TriggerReason.MANUAL);
                    var reorder = recommendation.getReorder();
                    ReorderSuggestion suggestion = new ReorderSuggestion(
                            product.getId(),
                            product.getStockLevel(),
                            reorder.getRecommendedQuantity(),
                            reorder.getLeadTimeDays(),
                            reorder.getConfidence(),
                            reorder.getReasoning(),
                            TriggerReason.MANUAL
                    );
                    suggestion.setStatus(SuggestionStatus.PENDING);
                    return reorderSuggestionRepository.save(suggestion);
                });
    }

    public PricingSuggestion acceptPricingSuggestion(String productId, String suggestionId) {
        PricingSuggestion suggestion = pricingSuggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new IllegalArgumentException("Pricing suggestion not found: " + suggestionId));
        ensurePendingSuggestion(suggestion.getProductId(), productId, suggestion.getStatus(), "pricing");

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + productId));
        product.setCurrentPrice(suggestion.getRecommendedPrice());
        productRepository.save(product);
        suggestion.setStatus(SuggestionStatus.ACCEPTED);
        return pricingSuggestionRepository.save(suggestion);
    }

    public PricingSuggestion rejectPricingSuggestion(String productId, String suggestionId) {
        PricingSuggestion suggestion = pricingSuggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new IllegalArgumentException("Pricing suggestion not found: " + suggestionId));
        ensurePendingSuggestion(suggestion.getProductId(), productId, suggestion.getStatus(), "pricing");
        suggestion.setStatus(SuggestionStatus.REJECTED);
        return pricingSuggestionRepository.save(suggestion);
    }

    public ReorderSuggestion acceptReorderSuggestion(String productId, String suggestionId) {
        ReorderSuggestion suggestion = reorderSuggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new IllegalArgumentException("Reorder suggestion not found: " + suggestionId));
        ensurePendingSuggestion(suggestion.getProductId(), productId, suggestion.getStatus(), "reorder");

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + productId));
        product.setStockLevel(product.getStockLevel() + suggestion.getRecommendedQuantity());
        if (product.getStockLevel() > 0 && product.getStatus() == ProductStatus.OUT_OF_STOCK) {
            product.setStatus(ProductStatus.ACTIVE);
        }
        productRepository.save(product);
        suggestion.setStatus(SuggestionStatus.ACCEPTED);
        return reorderSuggestionRepository.save(suggestion);
    }

    public ReorderSuggestion rejectReorderSuggestion(String productId, String suggestionId) {
        ReorderSuggestion suggestion = reorderSuggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new IllegalArgumentException("Reorder suggestion not found: " + suggestionId));
        ensurePendingSuggestion(suggestion.getProductId(), productId, suggestion.getStatus(), "reorder");
        suggestion.setStatus(SuggestionStatus.REJECTED);
        return reorderSuggestionRepository.save(suggestion);
    }

    private void ensurePendingSuggestion(String suggestionProductId, String productId,
                                         SuggestionStatus status, String suggestionType) {
        if (!productId.equals(suggestionProductId)) {
            throw new IllegalArgumentException("Suggestion does not belong to product: " + productId);
        }
        if (status != SuggestionStatus.PENDING) {
            throw new IllegalStateException("Cannot change finalized " + suggestionType + " suggestion");
        }
    }

    private void publishRecommendationEventIfTriggered(Product product) {
        if (product.getStockLevel() < product.getReorderThreshold()) {
            eventPublisher.publishEvent(new CommerceRecommendationEvent(product, TriggerReason.INVENTORY_LOW));
            return;
        }

        double categoryAverage = productRepository.findByCategory(product.getCategory()).stream()
                .mapToInt(Product::getDemandVelocity)
                .average()
                .orElse(0.0);
        if (product.getDemandVelocity() > 3.0 * categoryAverage) {
            eventPublisher.publishEvent(new CommerceRecommendationEvent(product, TriggerReason.DEMAND_SPIKE));
        }
    }
}
