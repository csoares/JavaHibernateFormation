package com.formation.hibernate.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Order Entity - Advanced JPA/Hibernate Performance Implementation
 * 
 * This entity represents an order in the system and demonstrates:
 * ✅ Efficient BLOB management (PDF files)
 * ✅ Nested EntityGraphs (subgraphs) for multiple relationships
 * ✅ Strategic indexes based on real query patterns
 * ✅ Correct use of @Enumerated for order states
 * ✅ Optimized bidirectional relationships
 * ✅ BigDecimal for safe monetary values
 */

@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_user", columnList = "user_id"),
    @Index(name = "idx_order_date", columnList = "order_date"),
    @Index(name = "idx_order_status", columnList = "status")
})
@NamedEntityGraph(
    name = "Order.withUserAndDepartment",
    attributeNodes = {
        @NamedAttributeNode(value = "user", subgraph = "user-department")
    },
    subgraphs = {
        @NamedSubgraph(name = "user-department", attributeNodes = {
            @NamedAttributeNode("department")
        })
    }
)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate = LocalDateTime.now();

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @BatchSize(size = 50)
    @JsonIgnore  // Prevent loading orderItems in JSON responses (use DTO instead)
    private List<OrderItem> orderItems;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "invoice_pdf", columnDefinition = "bytea")
    @JsonIgnore  // Prevent Jackson from serializing BLOB in JSON responses
    private byte[] invoicePdf;


    public Order() {}

    public Order(String orderNumber, BigDecimal totalAmount, User user) {
        this.orderNumber = orderNumber;
        this.totalAmount = totalAmount;
        this.user = user;
    }

    public enum OrderStatus {
        PENDING,
        CONFIRMED,
        SHIPPED,
        DELIVERED,
        CANCELLED,
        COMPLETED
    }


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

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public List<OrderItem> getOrderItems() { return orderItems; }
    public void setOrderItems(List<OrderItem> orderItems) { this.orderItems = orderItems; }

    public byte[] getInvoicePdf() { return invoicePdf; }
    public void setInvoicePdf(byte[] invoicePdf) { this.invoicePdf = invoicePdf; }
}