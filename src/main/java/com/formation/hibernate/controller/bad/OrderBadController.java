package com.formation.hibernate.controller.bad;

import com.formation.hibernate.converter.OrderConverter;
import com.formation.hibernate.dto.OrderDto;
import com.formation.hibernate.entity.Order;
import com.formation.hibernate.repository.OrderRepository;
import com.formation.hibernate.util.PerformanceMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/bad/orders")
public class OrderBadController {

    private static final Logger logger = LoggerFactory.getLogger(OrderBadController.class);

    private final OrderRepository orderRepository;
    private final OrderConverter orderConverter;
    private final PerformanceMonitor performanceMonitor;

    public OrderBadController(OrderRepository orderRepository, OrderConverter orderConverter, PerformanceMonitor performanceMonitor) {
        this.orderRepository = orderRepository;
        this.orderConverter = orderConverter;
        this.performanceMonitor = performanceMonitor;
    }

    // MÁ PRÁTICA: Sem transação read-only, sem EntityGraph
    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrderById(@PathVariable Long id) {
        String operationId = "getOrderById-bad-" + id;

        return performanceMonitor.measure(operationId,
            "Buscar pedido por ID SEM otimizações (múltiplas consultas)",
            () -> {
                // MÁ PRÁTICA: findById sem EntityGraph
                Optional<Order> order = orderRepository.findById(id);

                if (order.isPresent()) {
                    Order o = order.get();

                    // MÁ PRÁTICA: Acessos lazy trigger consultas extras
                    String userName = o.getUser() != null ? o.getUser().getName() : "N/A";
                    String departmentName = o.getUser() != null && o.getUser().getDepartment() != null ?
                        o.getUser().getDepartment().getName() : "N/A";

                    // MÁ PRÁTICA: Acesso a coleção lazy trigger mais consultas
                    int itemCount = o.getOrderItems() != null ? o.getOrderItems().size() : 0;

                    // MÁ PRÁTICA: Potencial carregamento do blob PDF desnecessariamente
                    boolean hasPdf = o.getInvoicePdf() != null;

                    OrderDto dto = orderConverter.toDto(o);
                    logger.warn("⚠️ Pedido encontrado com múltiplas consultas: {} (User: {}, Dept: {}, Items: {}, PDF: {})",
                        dto.getOrderNumber(), userName, departmentName, itemCount, hasPdf);
                    return ResponseEntity.ok(dto);
                } else {
                    logger.warn("⚠️ Pedido não encontrado: {}", id);
                    return ResponseEntity.notFound().build();
                }
            });
    }

    // MÁ PRÁTICA: Sem paginação, carregando todos incluindo blobs
    @GetMapping
    public ResponseEntity<List<OrderDto>> getAllOrders() {
        String operationId = "getAllOrders-bad";

        return performanceMonitor.measure(operationId,
            "Buscar TODOS os pedidos SEM paginação (MUITO PERIGOSO!)",
            () -> {
                // MÁ PRÁTICA: findAll() carrega TODOS os pedidos incluindo blobs
                List<Order> orders = orderRepository.findAll();

                // MÁ PRÁTICA: Conversão força carregamento de todas as relações e blobs
                List<OrderDto> orderDtos = orderConverter.toDtoList(orders);

                long totalBlobSize = orders.stream()
                    .mapToLong(o -> o.getInvoicePdf() != null ? o.getInvoicePdf().length : 0)
                    .sum();

                logger.error("🚨 EXTREMAMENTE PERIGOSO! Carregados {} pedidos com {} bytes de PDFs! OutOfMemoryError iminente!",
                    orders.size(), totalBlobSize);

                return ResponseEntity.ok(orderDtos);
            });
    }

    // MÁ PRÁTICA: Filtro por status sem otimização
    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderDto>> getOrdersByStatus(@PathVariable Order.OrderStatus status) {
        String operationId = "getOrdersByStatus-bad-" + status;

        return performanceMonitor.measure(operationId,
            "Buscar pedidos por status SEM otimização (carrega tudo + filtra)",
            () -> {
                // MÁ PRÁTICA: Carrega todos e filtra em memória
                List<Order> allOrders = orderRepository.findAll();
                List<Order> filteredOrders = allOrders.stream()
                    .filter(order -> order.getStatus() == status)
                    .toList();

                List<OrderDto> orderDtos = orderConverter.toDtoList(filteredOrders);

                logger.error("🚨 PÉSSIMA PRÁTICA! Carregados {} pedidos total para filtrar {} com status {}",
                    allOrders.size(), filteredOrders.size(), status);

                return ResponseEntity.ok(orderDtos);
            });
    }

    // MÁ PRÁTICA: Busca por número sem otimização
    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<OrderDto> getOrderByNumber(@PathVariable String orderNumber) {
        String operationId = "getOrderByNumber-bad-" + orderNumber.hashCode();

        return performanceMonitor.measure(operationId,
            "Buscar pedido por número SEM otimização",
            () -> {
                // MÁ PRÁTICA: Carrega todos incluindo blobs para encontrar um
                List<Order> allOrders = orderRepository.findAll();
                Optional<Order> order = allOrders.stream()
                    .filter(o -> orderNumber.equals(o.getOrderNumber()))
                    .findFirst();

                if (order.isPresent()) {
                    OrderDto dto = orderConverter.toDto(order.get());
                    logger.error("🚨 PÉSSIMA PRÁTICA! Carregados {} pedidos (incluindo PDFs) para encontrar 1!",
                        allOrders.size());
                    return ResponseEntity.ok(dto);
                } else {
                    logger.warn("⚠️ Pedido não encontrado: {} (após carregar {} registros)",
                        orderNumber, allOrders.size());
                    return ResponseEntity.notFound().build();
                }
            });
    }

    // MÁ PRÁTICA: Cálculo ineficiente em memória
    @GetMapping("/department/{departmentId}/total")
    public ResponseEntity<BigDecimal> getTotalAmountByDepartment(
            @PathVariable Long departmentId,
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate) {

        String operationId = "getTotalAmountByDepartment-bad-" + departmentId;

        return performanceMonitor.measure(operationId,
            "Calcular total por departamento carregando TODOS os pedidos",
            () -> {
                // MÁ PRÁTICA: Carrega todos os pedidos para calcular em memória
                List<Order> allOrders = orderRepository.findAll();

                BigDecimal total = allOrders.stream()
                    .filter(order -> order.getUser() != null &&
                                   order.getUser().getDepartment() != null &&
                                   order.getUser().getDepartment().getId().equals(departmentId) &&
                                   order.getOrderDate().isAfter(startDate) &&
                                   order.getOrderDate().isBefore(endDate))
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                logger.error("🚨 PÉSSIMA PRÁTICA! Carregados {} pedidos para calcular total de 1 departamento!",
                    allOrders.size());

                return ResponseEntity.ok(total);
            });
    }

    // MÁ PRÁTICA: Carregamento desnecessário de blobs
    @GetMapping("/high-value")
    public ResponseEntity<List<OrderDto>> getHighValueOrdersWithBlobs(
            @RequestParam BigDecimal minAmount) {

        String operationId = "getHighValueOrdersWithBlobs-bad";

        return performanceMonitor.measure(operationId,
            "Buscar pedidos de alto valor CARREGANDO PDFs desnecessariamente",
            () -> {
                // MÁ PRÁTICA: Carrega todos incluindo blobs pesados
                List<Order> allOrders = orderRepository.findAll();
                List<Order> highValueOrders = allOrders.stream()
                    .filter(order -> order.getTotalAmount().compareTo(minAmount) >= 0)
                    .toList();

                List<OrderDto> orderDtos = orderConverter.toDtoList(highValueOrders);

                long totalBlobSize = allOrders.stream()
                    .mapToLong(o -> o.getInvoicePdf() != null ? o.getInvoicePdf().length : 0)
                    .sum();

                logger.error("🚨 PÉSSIMA PRÁTICA! Carregados {} MB de PDFs desnecessariamente para encontrar {} pedidos!",
                    totalBlobSize / (1024 * 1024), highValueOrders.size());

                return ResponseEntity.ok(orderDtos);
            });
    }

    // MÁ PRÁTICA: Estatísticas ineficientes
    @GetMapping("/statistics")
    public ResponseEntity<String> getOrderStatistics() {
        String operationId = "getOrderStatistics-bad";

        return performanceMonitor.measure(operationId,
            "Gerar estatísticas carregando TODOS os pedidos em memória",
            () -> {
                // MÁ PRÁTICA: Carrega todos para calcular estatísticas em memória
                List<Order> allOrders = orderRepository.findAll();

                long pendingCount = allOrders.stream().filter(o -> o.getStatus() == Order.OrderStatus.PENDING).count();
                long confirmedCount = allOrders.stream().filter(o -> o.getStatus() == Order.OrderStatus.CONFIRMED).count();
                long shippedCount = allOrders.stream().filter(o -> o.getStatus() == Order.OrderStatus.SHIPPED).count();
                long deliveredCount = allOrders.stream().filter(o -> o.getStatus() == Order.OrderStatus.DELIVERED).count();

                BigDecimal averageAmount = allOrders.stream()
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(allOrders.size()), java.math.RoundingMode.HALF_UP);

                String statistics = String.format(
                    "Pending: %d, Confirmed: %d, Shipped: %d, Delivered: %d, Average: %s",
                    pendingCount, confirmedCount, shippedCount, deliveredCount, averageAmount);

                logger.error("🚨 PÉSSIMA PRÁTICA! Carregados {} pedidos para calcular estatísticas simples!",
                    allOrders.size());

                return ResponseEntity.ok(statistics);
            });
    }
}