package com.formation.hibernate.converter;

import com.formation.hibernate.dto.OrderItemDto;
import com.formation.hibernate.dto.ProductSummaryDto;
import com.formation.hibernate.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderItemConverter {

    public OrderItemConverter() {
    }

    public OrderItemDto toDto(OrderItem orderItem) {
        if (orderItem == null) return null;

        OrderItemDto dto = new OrderItemDto(
            orderItem.getId(),
            orderItem.getQuantity(),
            orderItem.getUnitPrice(),
            orderItem.getTotalPrice()
        );

        if (orderItem.getProduct() != null) {
            String categoryName = orderItem.getProduct().getCategory() != null ?
                orderItem.getProduct().getCategory().getName() : null;

            dto.setProduct(new ProductSummaryDto(
                orderItem.getProduct().getId(),
                orderItem.getProduct().getName(),
                orderItem.getProduct().getPrice(),
                categoryName
            ));
        }

        return dto;
    }

    public List<OrderItemDto> toDtoList(List<OrderItem> orderItems) {
        if (orderItems == null) return null;
        return orderItems.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public OrderItem toEntity(OrderItemDto dto) {
        if (dto == null) return null;

        OrderItem orderItem = new OrderItem();
        orderItem.setId(dto.getId());
        orderItem.setQuantity(dto.getQuantity());
        orderItem.setUnitPrice(dto.getUnitPrice());
        orderItem.setTotalPrice(dto.getTotalPrice());

        return orderItem;
    }
}