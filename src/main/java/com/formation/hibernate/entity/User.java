package com.formation.hibernate.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.List;

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

    // BOM: FetchType.LAZY por defeito para @ManyToOne
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    // BOM: FetchType.LAZY para coleções e @BatchSize para otimizar N+1
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @BatchSize(size = 25)
    private List<Order> orders;

    // MÁ PRÁTICA: Comentado - FetchType.EAGER cria problemas de performance
    // @ManyToOne(fetch = FetchType.EAGER)
    // @JoinColumn(name = "department_id")
    // private Department department;

    // MÁ PRÁTICA: Comentado - FetchType.EAGER para coleções
    // @OneToMany(mappedBy = "user", fetch = FetchType.EAGER)
    // private List<Order> orders;

    // MÁ PRÁTICA: Comentado - Sem @BatchSize causa problema N+1
    // @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    // private List<Order> orders;

    public User() {}

    public User(String name, String email, Department department) {
        this.name = name;
        this.email = email;
        this.department = department;
    }

    // Getters e Setters
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