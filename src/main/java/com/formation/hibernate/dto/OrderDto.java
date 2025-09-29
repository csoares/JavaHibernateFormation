package com.formation.hibernate.dto;

import com.formation.hibernate.entity.Order.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderDto {
    private Long id;
    private String orderNumber;
    private LocalDateTime orderDate;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private UserSummaryDto user;
    private List<OrderItemDto> orderItems;

    public OrderDto() {}

    public OrderDto(Long id, String orderNumber, LocalDateTime orderDate,
                    BigDecimal totalAmount, OrderStatus status) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.orderDate = orderDate;
        this.totalAmount = totalAmount;
        this.status = status;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public UserSummaryDto getUser() { return user; }
    public void setUser(UserSummaryDto user) { this.user = user; }

    public List<OrderItemDto> getOrderItems() { return orderItems; }
    public void setOrderItems(List<OrderItemDto> orderItems) { this.orderItems = orderItems; }
}