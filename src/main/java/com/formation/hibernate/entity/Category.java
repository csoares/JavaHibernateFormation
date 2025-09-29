package com.formation.hibernate.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;

import java.util.List;

@Entity
@Table(name = "categories", indexes = {
    @Index(name = "idx_category_name", columnList = "name")
})
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    // BOM: FetchType.LAZY para coleções
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    @BatchSize(size = 25)
    private List<Product> products;

    // MÁ PRÁTICA: Comentado - FetchType.EAGER para coleções
    // @OneToMany(mappedBy = "category", fetch = FetchType.EAGER)
    // private List<Product> products;

    public Category() {}

    public Category(String name, String description) {
        this.name = name;
        this.description = description;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<Product> getProducts() { return products; }
    public void setProducts(List<Product> products) { this.products = products; }
}