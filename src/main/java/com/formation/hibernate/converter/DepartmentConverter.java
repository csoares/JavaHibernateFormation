package com.formation.hibernate.converter;

import com.formation.hibernate.dto.DepartmentDto;
import com.formation.hibernate.dto.UserSummaryDto;
import com.formation.hibernate.entity.Department;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DepartmentConverter {

    public DepartmentConverter() {
    }

    public DepartmentDto toDto(Department department) {
        if (department == null) return null;

        DepartmentDto dto = new DepartmentDto(
            department.getId(),
            department.getName(),
            department.getDescription(),
            department.getBudget()
        );

        if (department.getUsers() != null) {
            dto.setUsers(
                department.getUsers().stream()
                    .map(user -> new UserSummaryDto(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getCreatedAt(),
                        department.getName()
                    ))
                    .collect(Collectors.toList())
            );
        }

        return dto;
    }

    public DepartmentDto toDtoWithoutUsers(Department department) {
        if (department == null) return null;

        return new DepartmentDto(
            department.getId(),
            department.getName(),
            department.getDescription(),
            department.getBudget()
        );
    }

    public List<DepartmentDto> toDtoList(List<Department> departments) {
        if (departments == null) return null;
        return departments.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public Department toEntity(DepartmentDto dto) {
        if (dto == null) return null;

        Department department = new Department();
        department.setId(dto.getId());
        department.setName(dto.getName());
        department.setDescription(dto.getDescription());
        department.setBudget(dto.getBudget());

        return department;
    }
}