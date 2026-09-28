package com.stockpulse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "inventory_snapshots")
public class InventorySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank(message = "Product ID cannot be blank")
    @Column(name = "product_id", nullable = false)
    private String productId;

    @NotNull(message = "Stock level is required")
    @Min(value = 0, message = "Stock level cannot be negative")
    @Column(name = "stock_level", nullable = false)
    private Integer stockLevel;

    @NotNull(message = "Demand velocity is required")
    @Min(value = 0, message = "Demand velocity cannot be negative")
    @Column(name = "demand_velocity", nullable = false)
    private Integer demandVelocity;

    @Column(name = "captured_at", nullable = false)
    private LocalDateTime capturedAt = LocalDateTime.now();

    public InventorySnapshot() {
    }

    public InventorySnapshot(String productId, Integer stockLevel, Integer demandVelocity) {
        this.productId = productId;
        this.stockLevel = stockLevel;
        this.demandVelocity = demandVelocity;
        this.capturedAt = LocalDateTime.now();
    }

    public InventorySnapshot(String id, String productId, Integer stockLevel, Integer demandVelocity, LocalDateTime capturedAt) {
        this.id = id;
        this.productId = productId;
        this.stockLevel = stockLevel;
        this.demandVelocity = demandVelocity;
        this.capturedAt = (capturedAt != null) ? capturedAt : LocalDateTime.now();
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

    public Integer getStockLevel() {
        return stockLevel;
    }

    public void setStockLevel(Integer stockLevel) {
        this.stockLevel = stockLevel;
    }

    public Integer getDemandVelocity() {
        return demandVelocity;
    }

    public void setDemandVelocity(Integer demandVelocity) {
        this.demandVelocity = demandVelocity;
    }

    public LocalDateTime getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(LocalDateTime capturedAt) {
        this.capturedAt = capturedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InventorySnapshot that = (InventorySnapshot) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "InventorySnapshot{" +
                "id='" + id + '\'' +
                ", productId='" + productId + '\'' +
                ", stockLevel=" + stockLevel +
                ", demandVelocity=" + demandVelocity +
                ", capturedAt=" + capturedAt +
                '}';
    }
}
