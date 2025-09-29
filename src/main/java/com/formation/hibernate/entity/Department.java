package com.formation.hibernate.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;

import java.util.List;

@Entity
@Table(name = "departments", indexes = {
    @Index(name = "idx_dept_name", columnList = "name")
})
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "budget")
    private Double budget;

    // BOM: FetchType.LAZY por defeito para coleções com @BatchSize
    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    @BatchSize(size = 25)
    private List<User> users;

    // MÁ PRÁTICA: Comentado - FetchType.EAGER para coleções
    // @OneToMany(mappedBy = "department", fetch = FetchType.EAGER)
    // private List<User> users;

    // MÁ PRÁTICA: Comentado - Sem índices nas colunas pesquisadas
    // @Column(nullable = false, unique = true, length = 100)
    // private String name;

    public Department() {}

    public Department(String name, String description, Double budget) {
        this.name = name;
        this.description = description;
        this.budget = budget;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getBudget() { return budget; }
    public void setBudget(Double budget) { this.budget = budget; }

    public List<User> getUsers() { return users; }
    public void setUsers(List<User> users) { this.users = users; }
}