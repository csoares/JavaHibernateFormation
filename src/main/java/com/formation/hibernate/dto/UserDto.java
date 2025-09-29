package com.formation.hibernate.dto;

import java.time.LocalDateTime;
import java.util.List;

public class UserDto {
    private Long id;
    private String name;
    private String email;
    private LocalDateTime createdAt;
    private DepartmentDto department;
    private List<OrderSummaryDto> orders;

    public UserDto() {}

    public UserDto(Long id, String name, String email, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.createdAt = createdAt;
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

    public DepartmentDto getDepartment() { return department; }
    public void setDepartment(DepartmentDto department) { this.department = department; }

    public List<OrderSummaryDto> getOrders() { return orders; }
    public void setOrders(List<OrderSummaryDto> orders) { this.orders = orders; }
}