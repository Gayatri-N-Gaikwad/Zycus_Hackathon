package com.stockpulse.domain;

import com.stockpulse.domain.enums.Category;
import com.stockpulse.domain.enums.ProductStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "products")
public class Product {

    @Id
    private String id;

    @NotBlank(message = "SKU cannot be blank")
    @Column(nullable = false, unique = true)
    private String sku;

    @NotBlank(message = "Product name cannot be blank")
    @Column(nullable = false)
    private String name;

    @NotNull(message = "Category is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @NotNull(message = "Current price is required")
    @DecimalMin(value = "0.00", message = "Current price cannot be negative")
    @Column(name = "current_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal currentPrice;

    @NotNull(message = "Stock level is required")
    @Min(value = 0, message = "Stock level cannot be negative")
    @Column(name = "stock_level", nullable = false)
    private Integer stockLevel;

    @NotNull(message = "Reorder threshold is required")
    @Min(value = 0, message = "Reorder threshold cannot be negative")
    @Column(name = "reorder_threshold", nullable = false)
    private Integer reorderThreshold;

    @NotNull(message = "Demand velocity is required")
    @Min(value = 0, message = "Demand velocity cannot be negative")
    @Column(name = "demand_velocity", nullable = false)
    private Integer demandVelocity;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;

    // Sprint 2 Extension Fields (Nullable)
    @DecimalMin(value = "0.00", message = "Cost price cannot be negative")
    @Column(name = "cost_price", precision = 10, scale = 2)
    private BigDecimal costPrice;

    @DecimalMin(value = "0.00", message = "Margin floor cannot be negative")
    @Column(name = "margin_floor", precision = 10, scale = 2)
    private BigDecimal marginFloor;

    @Column(name = "supplier_id")
    private String supplierId;

    public Product() {
    }

    public Product(String id, String sku, String name, Category category, BigDecimal currentPrice,
                   Integer stockLevel, Integer reorderThreshold, Integer demandVelocity, ProductStatus status) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.currentPrice = currentPrice;
        this.stockLevel = stockLevel;
        this.reorderThreshold = reorderThreshold;
        this.demandVelocity = demandVelocity;
        this.status = status;
    }

    public Product(String id, String sku, String name, Category category, BigDecimal currentPrice,
                   Integer stockLevel, Integer reorderThreshold, Integer demandVelocity, ProductStatus status,
                   BigDecimal costPrice, BigDecimal marginFloor, String supplierId) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.currentPrice = currentPrice;
        this.stockLevel = stockLevel;
        this.reorderThreshold = reorderThreshold;
        this.demandVelocity = demandVelocity;
        this.status = status;
        this.costPrice = costPrice;
        this.marginFloor = marginFloor;
        this.supplierId = supplierId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public Integer getStockLevel() {
        return stockLevel;
    }

    public void setStockLevel(Integer stockLevel) {
        this.stockLevel = stockLevel;
    }

    public Integer getReorderThreshold() {
        return reorderThreshold;
    }

    public void setReorderThreshold(Integer reorderThreshold) {
        this.reorderThreshold = reorderThreshold;
    }

    public Integer getDemandVelocity() {
        return demandVelocity;
    }

    public void setDemandVelocity(Integer demandVelocity) {
        this.demandVelocity = demandVelocity;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public BigDecimal getMarginFloor() {
        return marginFloor;
    }

    public void setMarginFloor(BigDecimal marginFloor) {
        this.marginFloor = marginFloor;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return Objects.equals(id, product.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Product{" +
                "id='" + id + '\'' +
                ", sku='" + sku + '\'' +
                ", name='" + name + '\'' +
                ", category=" + category +
                ", currentPrice=" + currentPrice +
                ", stockLevel=" + stockLevel +
                ", reorderThreshold=" + reorderThreshold +
                ", demandVelocity=" + demandVelocity +
                ", status=" + status +
                ", costPrice=" + costPrice +
                ", marginFloor=" + marginFloor +
                ", supplierId='" + supplierId + '\'' +
                '}';
    }
}
