package com.formation.hibernate.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

/**
 * Department Entity - JPA/Hibernate Best Practices Implementation
 * 
 * This entity represents a business department and demonstrates:
 * ✅ Correct index configuration for performance
 * ✅ Efficient management of bidirectional relationships
 * ✅ Appropriate use of @BatchSize to avoid N+1 problems
 * ✅ Database constraints at the application level
 */

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

    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    @BatchSize(size = 25)
    @JsonIgnore  // Prevent circular reference: Department -> User -> Department
    private List<User> users;


    public Department() {}

    public Department(String name, String description, Double budget) {
        this.name = name;
        this.description = description;
        this.budget = budget;
    }


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