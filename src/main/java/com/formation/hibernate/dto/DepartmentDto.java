package com.formation.hibernate.dto;

import java.util.List;

public class DepartmentDto {
    private Long id;
    private String name;
    private String description;
    private Double budget;
    private List<UserSummaryDto> users;

    public DepartmentDto() {}

    public DepartmentDto(Long id, String name, String description, Double budget) {
        this.id = id;
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

    public List<UserSummaryDto> getUsers() { return users; }
    public void setUsers(List<UserSummaryDto> users) { this.users = users; }
}