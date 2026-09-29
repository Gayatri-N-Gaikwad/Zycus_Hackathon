package com.stockpulse.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpulse.commerce.CommerceRecommendation;
import com.stockpulse.commerce.CommerceStrategy;
import com.stockpulse.commerce.PricingRecommendation;
import com.stockpulse.commerce.ReorderRecommendation;
import com.stockpulse.domain.Product;
import com.stockpulse.domain.enums.ChangeDirection;
import com.stockpulse.domain.enums.TriggerReason;
import com.stockpulse.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Locale;

@Component("AI")
public class AICommerceStrategy implements CommerceStrategy {

    private final LLMGateway llmGateway;
    private final ObjectMapper objectMapper;
    private final CommerceStrategy ruleBasedStrategy;
    private final ProductRepository productRepository;

    public AICommerceStrategy(LLMGateway llmGateway,
                              ObjectMapper objectMapper,
                              @Qualifier("RULE_BASED") CommerceStrategy ruleBasedStrategy,
                              ProductRepository productRepository) {
        this.llmGateway = llmGateway;
        this.objectMapper = objectMapper;
        this.ruleBasedStrategy = ruleBasedStrategy;
        this.productRepository = productRepository;
    }

    @Override
    public CommerceRecommendation recommend(Product product, TriggerReason triggerReason) {
        if (triggerReason != TriggerReason.INVENTORY_LOW && triggerReason != TriggerReason.DEMAND_SPIKE) {
            return ruleBasedStrategy.recommend(product, triggerReason);
        }

        try {
            String prompt = buildPrompt(product, triggerReason);
            JsonNode response = objectMapper.readTree(llmGateway.complete(prompt));
            return toRecommendation(product, triggerReason, response);
        } catch (JsonProcessingException | RuntimeException exception) {
            return ruleBasedStrategy.recommend(product, triggerReason);
        }
    }

    String buildPrompt(Product product, TriggerReason triggerReason) {
        if (triggerReason == TriggerReason.INVENTORY_LOW) {
            return "You are a retail inventory advisor. Evaluate this low-inventory case: "
                    + "stock=" + product.getStockLevel()
                    + ", reorderThreshold=" + product.getReorderThreshold()
                    + ", currentPrice=" + product.getCurrentPrice()
                    + ", demandVelocity=" + product.getDemandVelocity() + ". "
                    + "Balance scarcity pricing against customer retention and the risk of reducing sell-through while stock is constrained. "
                    + responseInstructions();
        }

        double categoryAverage = productRepository.findByCategory(product.getCategory()).stream()
                .mapToInt(Product::getDemandVelocity)
                .average()
                .orElse(0.0);
        return "You are a retail demand advisor. Evaluate this demand-spike case: "
                + "stock=" + product.getStockLevel()
                + ", currentPrice=" + product.getCurrentPrice()
                + ", demandVelocity=" + product.getDemandVelocity()
                + ", categoryAverageVelocity=" + categoryAverage + ". "
                + "Balance a price increase against conversion loss and the risk of overreacting to a temporary demand spike. "
                + responseInstructions();
    }

    private String responseInstructions() {
        return "Return only a JSON object with exactly these fields: "
                + "recommendedPrice, priceDirection, priceConfidence, priceReasoning, "
                + "recommendedQuantity, reorderConfidence, reorderReasoning. "
                + "priceDirection must be INCREASE, DECREASE, or HOLD; confidence values must be between 0 and 1.";
    }

    private CommerceRecommendation toRecommendation(Product product,
                                                    TriggerReason triggerReason,
                                                    JsonNode response) {
        BigDecimal recommendedPrice = decimal(response, "recommendedPrice");
        ChangeDirection direction = ChangeDirection.valueOf(response.path("priceDirection").asText().toUpperCase(Locale.ROOT));
        double priceConfidence = boundedConfidence(response, "priceConfidence");
        int quantity = response.path("recommendedQuantity").asInt(0);
        double reorderConfidence = boundedConfidence(response, "reorderConfidence");
        String priceReasoning = text(response, "priceReasoning");
        String reorderReasoning = text(response, "reorderReasoning");

        BigDecimal minimumPrice = product.getCurrentPrice().multiply(BigDecimal.valueOf(0.5));
        BigDecimal maximumPrice = product.getCurrentPrice().multiply(BigDecimal.valueOf(1.5));
        if (recommendedPrice.signum() <= 0
                || recommendedPrice.compareTo(minimumPrice) < 0
                || recommendedPrice.compareTo(maximumPrice) > 0
                || quantity < 1) {
            throw new IllegalArgumentException("LLM recommendation values are invalid");
        }

        return new CommerceRecommendation(
                new PricingRecommendation(recommendedPrice, direction, priceConfidence, priceReasoning),
                new ReorderRecommendation(quantity, reorderConfidence, reorderReasoning, 7),
                triggerReason
        );
    }

    private BigDecimal decimal(JsonNode response, String field) {
        JsonNode value = response.get(field);
        if (value == null || !value.isNumber()) {
            throw new IllegalArgumentException("Missing numeric field: " + field);
        }
        return value.decimalValue();
    }

    private double boundedConfidence(JsonNode response, String field) {
        JsonNode value = response.get(field);
        if (value == null || !value.isNumber() || value.doubleValue() < 0 || value.doubleValue() > 1) {
            throw new IllegalArgumentException("Invalid confidence field: " + field);
        }
        return value.doubleValue();
    }

    private String text(JsonNode response, String field) {
        JsonNode value = response.get(field);
        if (value == null || !value.isTextual() || value.textValue().isBlank()) {
            throw new IllegalArgumentException("Missing text field: " + field);
        }
        return value.textValue();
    }
}