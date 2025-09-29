package com.formation.hibernate.dto;

import java.math.BigDecimal;

public class OrderItemDto {
    private Long id;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private ProductSummaryDto product;

    public OrderItemDto() {}

    public OrderItemDto(Long id, Integer quantity, BigDecimal unitPrice,
                       BigDecimal totalPrice) {
        this.id = id;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    public ProductSummaryDto getProduct() { return product; }
    public void setProduct(ProductSummaryDto product) { this.product = product; }
}