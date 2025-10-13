package com.formation.hibernate.converter;

import com.formation.hibernate.dto.UserDto;
import com.formation.hibernate.dto.UserSummaryDto;
import com.formation.hibernate.dto.DepartmentDto;
import com.formation.hibernate.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserConverter {

    public UserConverter() {
    }

    public UserDto toDto(User user) {
        if (user == null) return null;

        UserDto dto = new UserDto(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getCreatedAt()
        );

        if (user.getDepartment() != null) {
            dto.setDepartment(new DepartmentDto(
                user.getDepartment().getId(),
                user.getDepartment().getName(),
                user.getDepartment().getDescription(),
                user.getDepartment().getBudget()
            ));
        }

        // NOTE: Orders collection is NOT loaded by default to avoid BLOB loading issues
        // If you need orders, load them explicitly with a specific query that excludes BLOBs
        // Example: Use OrderRepository.findOrderSummariesByUserId(userId)

        // if (user.getOrders() != null) {
        //     dto.setOrders(
        //         user.getOrders().stream()
        //             .map(order -> new OrderSummaryDto(
        //                 order.getId(),
        //                 order.getOrderNumber(),
        //                 order.getOrderDate(),
        //                 order.getTotalAmount(),
        //                 order.getStatus()
        //             ))
        //             .collect(Collectors.toList())
        //     );
        // }

        return dto;
    }

    public UserSummaryDto toSummaryDto(User user) {
        if (user == null) return null;

        String departmentName = user.getDepartment() != null ?
            user.getDepartment().getName() : null;

        return new UserSummaryDto(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getCreatedAt(),
            departmentName
        );
    }

    public List<UserDto> toDtoList(List<User> users) {
        if (users == null) return null;
        return users.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public List<UserSummaryDto> toSummaryDtoList(List<User> users) {
        if (users == null) return null;
        return users.stream()
            .map(this::toSummaryDto)
            .collect(Collectors.toList());
    }

    public User toEntity(UserDto dto) {
        if (dto == null) return null;

        User user = new User();
        user.setId(dto.getId());
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setCreatedAt(dto.getCreatedAt());

        // Note: Department será definido separadamente para evitar dependência circular

        return user;
    }
}