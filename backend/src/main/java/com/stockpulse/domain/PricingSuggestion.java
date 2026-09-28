package com.stockpulse.domain;

import com.stockpulse.domain.enums.ChangeDirection;
import com.stockpulse.domain.enums.SuggestionStatus;
import com.stockpulse.domain.enums.TriggerReason;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "pricing_suggestions")
public class PricingSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank(message = "Product ID cannot be blank")
    @Column(name = "product_id", nullable = false)
    private String productId;

    @NotNull(message = "Current price is required")
    @DecimalMin(value = "0.00", message = "Current price cannot be negative")
    @Column(name = "current_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal currentPrice;

    @NotNull(message = "Recommended price is required")
    @DecimalMin(value = "0.00", message = "Recommended price cannot be negative")
    @Column(name = "recommended_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal recommendedPrice;

    @NotNull(message = "Change direction is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "change_direction", nullable = false)
    private ChangeDirection changeDirection;

    @NotNull(message = "Confidence score is required")
    @DecimalMin(value = "0.0", message = "Confidence must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "Confidence must be between 0.0 and 1.0")
    @Column(nullable = false)
    private Double confidence;

    @Column(length = 2000)
    private String reasoning;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SuggestionStatus status = SuggestionStatus.PENDING;

    @NotNull(message = "Trigger reason is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_reason", nullable = false)
    private TriggerReason triggerReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public PricingSuggestion() {
    }

    public PricingSuggestion(String productId, BigDecimal currentPrice, BigDecimal recommendedPrice,
                             ChangeDirection changeDirection, Double confidence, String reasoning,
                             TriggerReason triggerReason) {
        this.productId = productId;
        this.currentPrice = currentPrice;
        this.recommendedPrice = recommendedPrice;
        this.changeDirection = changeDirection;
        this.confidence = confidence;
        this.reasoning = reasoning;
        this.status = SuggestionStatus.PENDING;
        this.triggerReason = triggerReason;
        this.createdAt = LocalDateTime.now();
    }

    public PricingSuggestion(String id, String productId, BigDecimal currentPrice, BigDecimal recommendedPrice,
                             ChangeDirection changeDirection, Double confidence, String reasoning,
                             SuggestionStatus status, TriggerReason triggerReason, LocalDateTime createdAt) {
        this.id = id;
        this.productId = productId;
        this.currentPrice = currentPrice;
        this.recommendedPrice = recommendedPrice;
        this.changeDirection = changeDirection;
        this.confidence = confidence;
        this.reasoning = reasoning;
        this.status = (status != null) ? status : SuggestionStatus.PENDING;
        this.triggerReason = triggerReason;
        this.createdAt = (createdAt != null) ? createdAt : LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public BigDecimal getRecommendedPrice() {
        return recommendedPrice;
    }

    public void setRecommendedPrice(BigDecimal recommendedPrice) {
        this.recommendedPrice = recommendedPrice;
    }

    public ChangeDirection getChangeDirection() {
        return changeDirection;
    }

    public void setChangeDirection(ChangeDirection changeDirection) {
        this.changeDirection = changeDirection;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    public SuggestionStatus getStatus() {
        return status;
    }

    public void setStatus(SuggestionStatus status) {
        this.status = status;
    }

    public TriggerReason getTriggerReason() {
        return triggerReason;
    }

    public void setTriggerReason(TriggerReason triggerReason) {
        this.triggerReason = triggerReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PricingSuggestion that = (PricingSuggestion) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PricingSuggestion{" +
                "id='" + id + '\'' +
                ", productId='" + productId + '\'' +
                ", currentPrice=" + currentPrice +
                ", recommendedPrice=" + recommendedPrice +
                ", changeDirection=" + changeDirection +
                ", confidence=" + confidence +
                ", reasoning='" + reasoning + '\'' +
                ", status=" + status +
                ", triggerReason=" + triggerReason +
                ", createdAt=" + createdAt +
                '}';
    }
}
