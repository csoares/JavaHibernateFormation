package com.formation.hibernate.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_product_name", columnList = "name"),
    @Index(name = "idx_product_category", columnList = "category_id"),
    @Index(name = "idx_product_price", columnList = "price")
})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity")
    private Integer stockQuantity;

    // BOM: FetchType.LAZY para @ManyToOne
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    // BOM: FetchType.LAZY para coleções
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    @BatchSize(size = 25)
    private List<OrderItem> orderItems;

    // BOM: @Lob com FetchType.LAZY para imagens
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "image_data")
    private byte[] imageData;

    // MÁ PRÁTICA: Comentado - @Lob sem FetchType.LAZY
    // @Lob
    // @Column(name = "image_data")
    // private byte[] imageData;

    // MÁ PRÁTICA: Comentado - FetchType.EAGER
    // @ManyToOne(fetch = FetchType.EAGER)
    // @JoinColumn(name = "category_id")
    // private Category category;

    public Product() {}

    public Product(String name, String description, BigDecimal price, Integer stockQuantity, Category category) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.category = category;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public List<OrderItem> getOrderItems() { return orderItems; }
    public void setOrderItems(List<OrderItem> orderItems) { this.orderItems = orderItems; }

    public byte[] getImageData() { return imageData; }
    public void setImageData(byte[] imageData) { this.imageData = imageData; }
}