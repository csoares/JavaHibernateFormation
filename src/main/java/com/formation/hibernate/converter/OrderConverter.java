package com.formation.hibernate.converter;

import com.formation.hibernate.dto.OrderDto;
import com.formation.hibernate.dto.OrderSummaryDto;
import com.formation.hibernate.dto.UserSummaryDto;
import com.formation.hibernate.entity.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderConverter {

    @Autowired
    private OrderItemConverter orderItemConverter;

    public OrderConverter() {
    }

    public OrderDto toDto(Order order) {
        if (order == null) return null;

        OrderDto dto = new OrderDto(
            order.getId(),
            order.getOrderNumber(),
            order.getOrderDate(),
            order.getTotalAmount(),
            order.getStatus()
        );

        if (order.getUser() != null) {
            String departmentName = order.getUser().getDepartment() != null ?
                order.getUser().getDepartment().getName() : null;

            dto.setUser(new UserSummaryDto(
                order.getUser().getId(),
                order.getUser().getName(),
                order.getUser().getEmail(),
                order.getUser().getCreatedAt(),
                departmentName
            ));
        }

        // NOTE: OrderItems collection is NOT loaded by default to avoid BLOB loading issues
        // In Hibernate 6, accessing orderItems can trigger loading of invoice_pdf BLOB
        // If you need order items, load them explicitly with a specific query
        // Example: Use OrderItemRepository.findByOrderId(orderId)

        // if (order.getOrderItems() != null && orderItemConverter != null) {
        //     dto.setOrderItems(orderItemConverter.toDtoList(order.getOrderItems()));
        // }

        return dto;
    }

    public OrderSummaryDto toSummaryDto(Order order) {
        if (order == null) return null;

        return new OrderSummaryDto(
            order.getId(),
            order.getOrderNumber(),
            order.getOrderDate(),
            order.getTotalAmount(),
            order.getStatus()
        );
    }

    public List<OrderDto> toDtoList(List<Order> orders) {
        if (orders == null) return null;
        return orders.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public List<OrderSummaryDto> toSummaryDtoList(List<Order> orders) {
        if (orders == null) return null;
        return orders.stream()
            .map(this::toSummaryDto)
            .collect(Collectors.toList());
    }

    public Order toEntity(OrderDto dto) {
        if (dto == null) return null;

        Order order = new Order();
        order.setId(dto.getId());
        order.setOrderNumber(dto.getOrderNumber());
        order.setOrderDate(dto.getOrderDate());
        order.setTotalAmount(dto.getTotalAmount());
        order.setStatus(dto.getStatus());

        // Note: User será definido separadamente para evitar dependência circular

        return order;
    }
}