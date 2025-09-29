package com.formation.hibernate.controller.good;

import com.formation.hibernate.converter.OrderConverter;
import com.formation.hibernate.dto.OrderDto;
import com.formation.hibernate.dto.OrderSummaryDto;
import com.formation.hibernate.entity.Order;
import com.formation.hibernate.repository.OrderRepository;
import com.formation.hibernate.util.PerformanceMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/good/orders")
public class OrderGoodController {

    private static final Logger logger = LoggerFactory.getLogger(OrderGoodController.class);

    private final OrderRepository orderRepository;
    private final OrderConverter orderConverter;
    private final PerformanceMonitor performanceMonitor;

    public OrderGoodController(OrderRepository orderRepository, OrderConverter orderConverter, PerformanceMonitor performanceMonitor) {
        this.orderRepository = orderRepository;
        this.orderConverter = orderConverter;
        this.performanceMonitor = performanceMonitor;
    }

    // BOM: Transação read-only com EntityGraph
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<OrderDto> getOrderById(@PathVariable Long id) {
        String operationId = "getOrderById-good-" + id;

        return performanceMonitor.measure(operationId,
            "Buscar pedido por ID com EntityGraph (User + Department)",
            () -> {
                // BOM: Usa EntityGraph para carregar relações necessárias
                Optional<Order> order = orderRepository.findByIdWithUserAndDepartment(id);

                if (order.isPresent()) {
                    OrderDto dto = orderConverter.toDto(order.get());
                    logger.info("✅ Pedido encontrado: {} (User: {}, Department: {})",
                        dto.getOrderNumber(),
                        dto.getUser() != null ? dto.getUser().getName() : "N/A",
                        dto.getUser() != null ? dto.getUser().getDepartmentName() : "N/A");
                    return ResponseEntity.ok(dto);
                } else {
                    logger.warn("⚠️ Pedido não encontrado: {}", id);
                    return ResponseEntity.notFound().build();
                }
            });
    }

    // BOM: Paginação eficiente por status
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<Page<OrderDto>> getOrdersByStatus(
            @RequestParam Order.OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        String operationId = "getOrdersByStatus-good-" + status + "-page-" + page;

        return performanceMonitor.measure(operationId,
            String.format("Buscar pedidos por status %s (página %d)", status, page),
            () -> {
                // BOM: Paginação com JOIN FETCH
                Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
                Page<Order> orders = orderRepository.findByStatusWithUser(status, pageable);

                Page<OrderDto> orderDtos = orders.map(orderConverter::toDto);

                logger.info("✅ Página {} de pedidos {} carregada: {} elementos",
                    page, status, orderDtos.getNumberOfElements());

                return ResponseEntity.ok(orderDtos);
            });
    }

    // BOM: Projeção para listagem de usuário específico
    @GetMapping("/user/{userId}/summaries")
    @Transactional(readOnly = true)
    public ResponseEntity<List<OrderSummaryDto>> getOrderSummariesByUser(@PathVariable Long userId) {
        String operationId = "getOrderSummariesByUser-good-" + userId;

        return performanceMonitor.measure(operationId,
            "Buscar resumos de pedidos por usuário com projeção JPQL",
            () -> {
                // BOM: Usa projeção JPQL para dados essenciais
                List<OrderSummaryDto> summaries = orderRepository.findOrderSummariesByUserId(userId);

                logger.info("✅ {} resumos de pedidos carregados para usuário {}", summaries.size(), userId);

                return ResponseEntity.ok(summaries);
            });
    }

    // BOM: Consulta por número do pedido com otimização
    @GetMapping("/number/{orderNumber}")
    @Transactional(readOnly = true)
    public ResponseEntity<OrderDto> getOrderByNumber(@PathVariable String orderNumber) {
        String operationId = "getOrderByNumber-good-" + orderNumber.hashCode();

        return performanceMonitor.measure(operationId,
            "Buscar pedido por número com JOIN FETCH múltiplo",
            () -> {
                // BOM: Consulta otimizada com múltiplos JOIN FETCH
                Optional<Order> order = orderRepository.findByOrderNumberWithDetails(orderNumber);

                if (order.isPresent()) {
                    OrderDto dto = orderConverter.toDto(order.get());
                    logger.info("✅ Pedido encontrado por número: {}", dto.getOrderNumber());
                    return ResponseEntity.ok(dto);
                } else {
                    logger.warn("⚠️ Pedido não encontrado com número: {}", orderNumber);
                    return ResponseEntity.notFound().build();
                }
            });
    }

    // BOM: Consulta agregada eficiente
    @GetMapping("/department/{departmentId}/total")
    @Transactional(readOnly = true)
    public ResponseEntity<BigDecimal> getTotalAmountByDepartment(
            @PathVariable Long departmentId,
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate) {

        String operationId = "getTotalAmountByDepartment-good-" + departmentId;

        return performanceMonitor.measure(operationId,
            "Calcular total por departamento com agregação SQL",
            () -> {
                // BOM: Usa agregação SQL para cálculo eficiente
                BigDecimal total = orderRepository.getTotalAmountByDepartmentAndDateRange(
                    departmentId, startDate, endDate);

                logger.info("✅ Total calculado para departamento {}: {}",
                    departmentId, total != null ? total : BigDecimal.ZERO);

                return ResponseEntity.ok(total != null ? total : BigDecimal.ZERO);
            });
    }

    // BOM: Consulta sem carregar blobs desnecessariamente
    @GetMapping("/high-value")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Object[]>> getHighValueOrdersWithoutBlobs(
            @RequestParam BigDecimal minAmount) {

        String operationId = "getHighValueOrdersWithoutBlobs-good";

        return performanceMonitor.measure(operationId,
            "Buscar pedidos de alto valor SEM carregar PDFs",
            () -> {
                // BOM: Consulta específica sem carregar blobs
                List<Object[]> orders = orderRepository.findOrdersWithoutBlobData(minAmount);

                logger.info("✅ {} pedidos de alto valor encontrados (sem carregar PDFs)",
                    orders.size());

                return ResponseEntity.ok(orders);
            });
    }

    // BOM: Consulta por intervalo de datas com índice
    @GetMapping("/date-range")
    @Transactional(readOnly = true)
    public ResponseEntity<List<OrderDto>> getOrdersByDateRange(
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate) {

        String operationId = "getOrdersByDateRange-good";

        return performanceMonitor.measure(operationId,
            "Buscar pedidos por intervalo de datas (índice otimizado)",
            () -> {
                // BOM: Usa índice na coluna order_date
                List<Order> orders = orderRepository.findOrdersByDateRange(startDate, endDate);
                List<OrderDto> orderDtos = orderConverter.toDtoList(orders);

                logger.info("✅ {} pedidos encontrados entre {} e {}",
                    orders.size(), startDate, endDate);

                return ResponseEntity.ok(orderDtos);
            });
    }

    // BOM: Estatísticas sem carregar entidades completas
    @GetMapping("/statistics")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Object[]>> getOrderStatistics() {
        String operationId = "getOrderStatistics-good";

        return performanceMonitor.measure(operationId,
            "Gerar estatísticas de pedidos com GROUP BY eficiente",
            () -> {
                // BOM: Usa GROUP BY para estatísticas eficientes
                List<Object[]> statistics = orderRepository.getOrderStatistics();

                logger.info("✅ Estatísticas geradas para {} status diferentes", statistics.size());

                return ResponseEntity.ok(statistics);
            });
    }
}