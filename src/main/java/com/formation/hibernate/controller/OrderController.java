package com.formation.hibernate.controller;

import com.formation.hibernate.converter.OrderConverter;
import com.formation.hibernate.dto.OrderDto;
import com.formation.hibernate.entity.Order;
import com.formation.hibernate.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Basic Order Controller - Educational Example
 *
 * Note: Uses DTOs to avoid BLOB serialization issues
 * This controller demonstrates basic REST operations
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderConverter orderConverter;

    public OrderController(OrderRepository orderRepository, OrderConverter orderConverter) {
        this.orderRepository = orderRepository;
        this.orderConverter = orderConverter;
    }

    // Note: Returns DTOs to avoid BLOB loading issues
    // Uses converter which doesn't access BLOB fields
    @GetMapping
    public ResponseEntity<Page<OrderDto>> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        // Note: Using DTO projection query to avoid loading BLOB fields
        // This query only selects needed columns, excluding invoice_pdf
        Page<OrderDto> orderDtos = orderRepository.findAllOrderDtos(pageable);
        return ResponseEntity.ok(orderDtos);
    }

    // Note: Uses query that doesn't load BLOB to avoid PostgreSQL errors
    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrderById(@PathVariable Long id) {
        // Use repository method that excludes BLOB column
        Optional<Object[]> result = orderRepository.findByIdWithoutBlob(id);

        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Map Object[] to OrderDto manually
        // NOTE: Native queries return Object[] wrapped in another Object[]
        Object[] wrapper = result.get();
        Object[] row = (Object[]) wrapper[0];
        OrderDto dto = new OrderDto();
        dto.setId(((Number) row[0]).longValue());
        dto.setOrderNumber((String) row[1]);
        // Native queries return Timestamp, need to convert to LocalDateTime
        dto.setOrderDate(row[2] != null ? ((java.sql.Timestamp) row[2]).toLocalDateTime() : null);
        dto.setTotalAmount((java.math.BigDecimal) row[3]);
        // Native queries return enum as String, need to convert to OrderStatus
        dto.setStatus(row[4] != null ? Order.OrderStatus.valueOf((String) row[4]) : null);

        // Load user if needed (could cause N+1, but acceptable for basic endpoint)
        if (row[5] != null) {
            Long userId = ((Number) row[5]).longValue();
            // For simplicity, leaving user null - could load separately if needed
        }

        return ResponseEntity.ok(dto);
    }

    // Note: Returns DTOs to avoid BLOB loading
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<OrderDto>> getOrdersByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        // Note: Using DTO projection query to avoid loading BLOB fields
        Page<OrderDto> orderDtos = orderRepository.findOrderDtosByUserId(userId, pageable);
        return ResponseEntity.ok(orderDtos);
    }

    // Note: Returns DTOs to avoid BLOB loading
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<OrderDto>> getOrdersByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        // Note: Using DTO projection query to avoid loading BLOB fields
        // Convert status string to enum
        Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
        Page<OrderDto> orderDtos = orderRepository.findOrderDtosByStatus(orderStatus, pageable);
        return ResponseEntity.ok(orderDtos);
    }
}