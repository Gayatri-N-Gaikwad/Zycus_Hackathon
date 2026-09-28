package com.stockpulse.domain;

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
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "reorder_suggestions")
public class ReorderSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank(message = "Product ID cannot be blank")
    @Column(name = "product_id", nullable = false)
    private String productId;

    @NotNull(message = "Current stock is required")
    @Min(value = 0, message = "Current stock cannot be negative")
    @Column(name = "current_stock", nullable = false)
    private Integer currentStock;

    @NotNull(message = "Recommended quantity is required")
    @Min(value = 0, message = "Recommended quantity cannot be negative")
    @Column(name = "recommended_quantity", nullable = false)
    private Integer recommendedQuantity;

    @NotNull(message = "Suggested lead time days is required")
    @Min(value = 0, message = "Suggested lead time days cannot be negative")
    @Column(name = "suggested_lead_time_days", nullable = false)
    private Integer suggestedLeadTimeDays;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_reason", nullable = false)
    private TriggerReason triggerReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ReorderSuggestion() {
    }

    public ReorderSuggestion(String productId, Integer currentStock, Integer recommendedQuantity,
                             Integer suggestedLeadTimeDays, Double confidence, String reasoning,
                             TriggerReason triggerReason) {
        this.productId = productId;
        this.currentStock = currentStock;
        this.recommendedQuantity = recommendedQuantity;
        this.suggestedLeadTimeDays = suggestedLeadTimeDays;
        this.confidence = confidence;
        this.reasoning = reasoning;
        this.status = SuggestionStatus.PENDING;
        this.triggerReason = triggerReason;
        this.createdAt = LocalDateTime.now();
    }

    public ReorderSuggestion(String id, String productId, Integer currentStock, Integer recommendedQuantity,
                             Integer suggestedLeadTimeDays, Double confidence, String reasoning,
                             SuggestionStatus status, TriggerReason triggerReason, LocalDateTime createdAt) {
        this.id = id;
        this.productId = productId;
        this.currentStock = currentStock;
        this.recommendedQuantity = recommendedQuantity;
        this.suggestedLeadTimeDays = suggestedLeadTimeDays;
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

    public Integer getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(Integer currentStock) {
        this.currentStock = currentStock;
    }

    public Integer getRecommendedQuantity() {
        return recommendedQuantity;
    }

    public void setRecommendedQuantity(Integer recommendedQuantity) {
        this.recommendedQuantity = recommendedQuantity;
    }

    public Integer getSuggestedLeadTimeDays() {
        return suggestedLeadTimeDays;
    }

    public void setSuggestedLeadTimeDays(Integer suggestedLeadTimeDays) {
        this.suggestedLeadTimeDays = suggestedLeadTimeDays;
    }

    // Convenience alias for leadTimeDays
    public Integer getLeadTimeDays() {
        return suggestedLeadTimeDays;
    }

    public void setLeadTimeDays(Integer leadTimeDays) {
        this.suggestedLeadTimeDays = leadTimeDays;
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
        ReorderSuggestion that = (ReorderSuggestion) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ReorderSuggestion{" +
                "id='" + id + '\'' +
                ", productId='" + productId + '\'' +
                ", currentStock=" + currentStock +
                ", recommendedQuantity=" + recommendedQuantity +
                ", suggestedLeadTimeDays=" + suggestedLeadTimeDays +
                ", confidence=" + confidence +
                ", reasoning='" + reasoning + '\'' +
                ", status=" + status +
                ", triggerReason=" + triggerReason +
                ", createdAt=" + createdAt +
                '}';
    }
}
