package com.formation.hibernate.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.List;

/**
 * User Entity - JPA/Hibernate Best Practices Implementation
 * 
 * This entity represents a system user and demonstrates:
 * ✅ Essential techniques for optimal performance
 * ✅ Correct configuration of bidirectional relationships
 * ✅ Strategic use of indexes for frequent queries
 * ✅ EntityGraphs to resolve N+1 problems
 * ✅ @BatchSize for collection optimization
 */

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email"),
    @Index(name = "idx_user_department", columnList = "department_id")
})
@NamedEntityGraph(
    name = "User.withDepartment",
    attributeNodes = {
        @NamedAttributeNode("department")
    }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @BatchSize(size = 25)
    @JsonIgnore  // Prevent circular reference and avoid loading all orders
    private List<Order> orders;


    public User() {}

    public User(String name, String email, Department department) {
        this.name = name;
        this.email = email;
        this.department = department;
    }


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    public List<Order> getOrders() { return orders; }
    public void setOrders(List<Order> orders) { this.orders = orders; }
}