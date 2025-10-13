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

/**
 * 🚨 ORDER BAD CONTROLLER - Demonstração de MÁS PRÁTICAS com BLOBS (NÃO COPIAR!)
 * 
 * ⚠️ AVISO CRÍTICO: Este controlador demonstra práticas EXTREMAMENTE PERIGOSAS!
 * 
 * Más práticas específicas para entidades com BLOB demonstradas:
 * ❌ Carregamento de BLOBS desnecessariamente (OutOfMemoryError garantido!)
 * ❌ Ausência de transacções read-only para consultas
 * ❌ Problemas N+1 sistemáticos em relações complexas
 * ❌ Carregamento completo sem paginação (incluindo PDFs pesados!)
 * ❌ Filtragem em memória de grandes volumes com BLOBS
 * ❌ Cálculos agregados carregando todos os dados em memória
 * ❌ Acesso a lazy properties que disparam consultas extras
 * ❌ Falta de EntityGraphs para carregar apenas dados necessários
 * ❌ Conversões DTO que forçam carregamento de todas as relações
 * 
 * 🎯 PERIGO: Com BLOBS, estes problemas causam OutOfMemoryError rapidamente!
 * 
 * 📚 Use este controlador APENAS para comparar com OrderGoodController!
 */

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

    // ❌ MÁ PRÁTICA: Sem EntityGraph (mas precisa transação para lazy loading funcionar)
    // PROBLEMA: Sem EntityGraph causa múltiplas consultas separadas
    // RESULTADO: N+1 problem - 1 query para order + 1 para user + 1 para department
    @GetMapping("/{id}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<OrderDto> getOrderById(@PathVariable Long id) {
        String operationId = "getOrderById-bad-" + id;

        return performanceMonitor.measure(operationId,
            "Buscar pedido por ID SEM otimizações (múltiplas consultas - N+1)",
            () -> {
                // ❌ MÁ PRÁTICA: findById sem EntityGraph
                // PROBLEMA: Carrega Order mas não as relações (user, department, orderItems)
                // RESULTADO: Cada acesso posterior dispara consulta separada (N+1)
                Optional<Order> order = orderRepository.findById(id);

                if (order.isPresent()) {
                    Order o = order.get();

                    // ❌ MÁ PRÁTICA: Acessos lazy disparam consultas extras
                    // PROBLEMA: o.getUser() dispara SELECT do User
                    // PROBLEMA: o.getUser().getDepartment() dispara SELECT do Department
                    // RESULTADO: 3 queries em vez de 1 com JOIN
                    String userName = o.getUser() != null ? o.getUser().getName() : "N/A";
                    String departmentName = o.getUser() != null && o.getUser().getDepartment() != null ?
                        o.getUser().getDepartment().getName() : "N/A";

                    // ❌ MÁ PRÁTICA: Acesso a colecção lazy dispara mais consultas
                    // PROBLEMA: o.getOrderItems() dispara SELECT de todos os OrderItems
                    // PROBLEMA: .size() força carregamento completo da colecção
                    // RESULTADO: Mais 1-2 queries desnecessárias
                    int itemCount = o.getOrderItems() != null ? o.getOrderItems().size() : 0;

                    // NOTE: Avoided invoicePdf access to prevent BLOB loading issues
                    // BLOB fields should only be loaded when explicitly needed

                    OrderDto dto = orderConverter.toDto(o);
                    logger.warn("⚠️ Pedido encontrado com múltiplas consultas (N+1): {} (User: {}, Dept: {}, Items: {})",
                        dto.getOrderNumber(), userName, departmentName, itemCount);
                    return ResponseEntity.ok(dto);
                } else {
                    logger.warn("⚠️ Pedido não encontrado: {}", id);
                    return ResponseEntity.notFound().build();
                }
            });
    }

    // ❌ MÁ PRÁTICA: Sem paginação, carregando todos incluindo BLOBs
    // PROBLEMA: findAll() carrega TODOS os registros incluindo PDFs
    // PROBLEMA: Com 10.000 pedidos x 1MB PDF = 10GB de memória!
    // RESULTADO: OutOfMemoryError garantido em produção
    @GetMapping
    public ResponseEntity<List<OrderDto>> getAllOrders() {
        String operationId = "getAllOrders-bad";

        return performanceMonitor.measure(operationId,
            "Buscar TODOS os pedidos SEM paginação (MUITO PERIGOSO!)",
            () -> {
                // ❌ MÁ PRÁTICA: findAll() carrega TODOS os pedidos incluindo BLOBs
                // PROBLEMA: Hibernate carrega todos os Orders com os PDFs (BLOBs)
                // PROBLEMA: Sem paginação = todos os dados na memória
                // RESULTADO: Com muitos pedidos, OutOfMemoryError inevitável
                List<Order> orders = orderRepository.findAll();

                // ❌ MÁ PRÁTICA: Conversão força carregamento de todas as relações e BLOBs
                // PROBLEMA: toDto() acede a todas as propriedades, incluindo BLOBs
                // PROBLEMA: Força carregamento de user, department, orderItems
                // RESULTADO: Multiplica exponencialmente o uso de memória
                List<OrderDto> orderDtos = orderConverter.toDtoList(orders);

                long totalBlobSize = orders.stream()
                    .mapToLong(o -> o.getInvoicePdf() != null ? o.getInvoicePdf().length : 0)
                    .sum();

                logger.error("🚨 EXTREMAMENTE PERIGOSO! Carregados {} pedidos com {} bytes de PDFs! OutOfMemoryError iminente!",
                    orders.size(), totalBlobSize);

                return ResponseEntity.ok(orderDtos);
            });
    }

    // ❌ MÁ PRÁTICA: Filtro por status sem optimização
    // PROBLEMA: Carrega TODOS os pedidos (incluindo BLOBs) para filtrar poucos
    // PROBLEMA: Filtragem em memória em vez de consulta WHERE
    // RESULTADO: Desperdício massivo de recursos e OutOfMemoryError
    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderDto>> getOrdersByStatus(@PathVariable Order.OrderStatus status) {
        String operationId = "getOrdersByStatus-bad-" + status;

        return performanceMonitor.measure(operationId,
            "Buscar pedidos por status SEM otimização (carrega tudo + filtra)",
            () -> {
                // ❌ MÁ PRÁTICA: Carrega todos e filtra em memória
                // PROBLEMA: findAll() traz 100% dos dados (incluindo PDFs pesados)
                // PROBLEMA: .filter() processa em memória o que deveria ser WHERE na BD
                // PROBLEMA: Desperdiça 90%+ dos dados carregados
                // RESULTADO: Lentidão extrema + risco de OutOfMemoryError
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

    // ❌ MÁ PRÁTICA: Busca por número sem optimização
    // PROBLEMA: Carrega TODOS os pedidos (incluindo PDFs) para encontrar UM
    // PROBLEMA: Busca linear em memória em vez de índice na base de dados
    // RESULTADO: Performance O(n) degradada + OutOfMemoryError potencial
    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<OrderDto> getOrderByNumber(@PathVariable String orderNumber) {
        String operationId = "getOrderByNumber-bad-" + orderNumber.hashCode();

        return performanceMonitor.measure(operationId,
            "Buscar pedido por número SEM otimização",
            () -> {
                // ❌ MÁ PRÁTICA: Carrega todos incluindo BLOBs para encontrar um
                // PROBLEMA: findAll() carrega milhares de pedidos + PDFs pesados
                // PROBLEMA: .filter() faz busca linear em memória (lento)
                // PROBLEMA: Deveria usar WHERE orderNumber = ? com índice
                // RESULTADO: Desperdício massivo para encontrar 1 registo
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

    // ❌ MÁ PRÁTICA: Cálculo ineficiente em memória
    // PROBLEMA: Carrega TODOS os pedidos para calcular total de UM departamento
    // PROBLEMA: Cálculo em memória em vez de SUM() na base de dados
    // RESULTADO: OutOfMemoryError + performance terrível
    @GetMapping("/department/{departmentId}/total")
    public ResponseEntity<BigDecimal> getTotalAmountByDepartment(
            @PathVariable Long departmentId,
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate) {

        String operationId = "getTotalAmountByDepartment-bad-" + departmentId;

        return performanceMonitor.measure(operationId,
            "Calcular total por departamento carregando TODOS os pedidos",
            () -> {
                // ❌ MÁ PRÁTICA: Carrega todos os pedidos para calcular em memória
                // PROBLEMA: findAll() traz TODOS os pedidos (incluindo BLOBs pesados)
                // PROBLEMA: Deveria usar SELECT SUM(totalAmount) WHERE...
                // RESULTADO: Carrega GB de dados para calcular um número
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

    // ❌ MÁ PRÁTICA: Carregamento desnecessário de BLOBs
    // PROBLEMA: Carrega PDFs pesados quando só precisa de dados do pedido
    // PROBLEMA: findAll() traz todos os pedidos + BLOBs para filtrar poucos
    // RESULTADO: Desperdício massivo de memória com BLOBs desnecessários
    @GetMapping("/high-value")
    public ResponseEntity<List<OrderDto>> getHighValueOrdersWithBlobs(
            @RequestParam BigDecimal minAmount) {

        String operationId = "getHighValueOrdersWithBlobs-bad";

        return performanceMonitor.measure(operationId,
            "Buscar pedidos de alto valor CARREGANDO PDFs desnecessariamente",
            () -> {
                // ❌ MÁ PRÁTICA: Carrega todos incluindo BLOBs pesados
                // PROBLEMA: findAll() carrega todos os PDFs (potencialmente GB)
                // PROBLEMA: Filtragem em memória em vez de WHERE totalAmount >= ?
                // PROBLEMA: Desperdiça 90%+ dos BLOBs carregados
                // RESULTADO: OutOfMemoryError quase garantido
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

    // ❌ MÁ PRÁTICA: Estatísticas ineficientes
    // PROBLEMA: Carrega TODOS os pedidos (incluindo BLOBs) para estatísticas simples
    // PROBLEMA: Deveria usar COUNT() e SUM() agregados na base de dados
    // RESULTADO: OutOfMemoryError para cálculos que deveriam ser instantâneos
    @GetMapping("/statistics")
    public ResponseEntity<String> getOrderStatistics() {
        String operationId = "getOrderStatistics-bad";

        return performanceMonitor.measure(operationId,
            "Gerar estatísticas carregando TODOS os pedidos em memória",
            () -> {
                // ❌ MÁ PRÁTICA: Carrega todos para calcular estatísticas em memória
                // PROBLEMA: findAll() traz TODOS os pedidos + PDFs (GB de dados)
                // PROBLEMA: Estatísticas simples deveriam ser consultas COUNT/SUM
                // PROBLEMA: Desperdiça recursos massivos para cálculos básicos
                // RESULTADO: Minutos para algo que deveria ser milissegundos
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