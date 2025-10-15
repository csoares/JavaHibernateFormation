package com.formation.hibernate.controller.bad;

import com.formation.hibernate.converter.OrderConverter;
import com.formation.hibernate.dto.OrderDto;
import com.formation.hibernate.dto.UserSummaryDto;
import com.formation.hibernate.entity.Order;
import com.formation.hibernate.entity.User;
import com.formation.hibernate.repository.OrderRepository;
import com.formation.hibernate.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final OrderConverter orderConverter;
    private final PerformanceMonitor performanceMonitor;

    public OrderBadController(OrderRepository orderRepository, UserRepository userRepository, OrderConverter orderConverter, PerformanceMonitor performanceMonitor) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
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
                // ❌ MÁ PRÁTICA: Native query that EXCLUDES BLOB and relations
                // PROBLEMA: Retorna apenas dados do Order, sem carregar user/department
                // RESULTADO: Cada acesso a relações dispara consulta separada (N+1)
                // NOTE: Using native SQL returning Object[] to avoid BLOB loading
                //       This is necessary because Hibernate 6 ignores @Basic(fetch=LAZY) for byte[]
                Optional<Object[]> orderData = orderRepository.findOrderWithoutBlobOrRelationsNative(id);

                if (orderData.isPresent()) {
                    Object[] wrapper = orderData.get();
                    // NOTE: Spring Data JPA wraps native query results in Object[]
                    // So wrapper[0] contains the actual row data as Object[]
                    Object[] row = (Object[]) wrapper[0];

                    // Manual mapping from Object[] to OrderDto to avoid loading Order entity
                    // Columns: id, order_number, order_date, total_amount, status, user_id
                    // Note: PostgreSQL native queries return BigInteger for BIGINT columns
                    OrderDto dto = new OrderDto(
                        ((Number) row[0]).longValue(),                              // id
                        (String) row[1],                                            // order_number
                        ((java.sql.Timestamp) row[2]).toLocalDateTime(),            // order_date
                        (BigDecimal) row[3],                                        // total_amount
                        Order.OrderStatus.valueOf((String) row[4])                 // status
                    );

                    // ❌ MÁ PRÁTICA: Separate queries for user and department (N+1 problem)
                    // PROBLEMA: Cada relação dispara SELECT separado
                    // RESULTADO: 3 queries em vez de 1 com JOINs
                    Long userId = ((Number) row[5]).longValue();
                    Optional<User> userOpt = userRepository.findById(userId);  // Query 2: SELECT user

                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        String userName = user.getName();
                        String departmentName = "N/A";

                        // ❌ MÁ PRÁTICA: Lazy loading dispara consulta extra para department
                        if (user.getDepartment() != null) {
                            departmentName = user.getDepartment().getName();  // Query 3: SELECT department
                        }

                        dto.setUser(new UserSummaryDto(
                            user.getId(),
                            userName,
                            user.getEmail(),
                            user.getCreatedAt(),
                            departmentName
                        ));

                        logger.warn("⚠️ Pedido encontrado com múltiplas consultas (N+1): {} (User: {}, Dept: {})",
                            dto.getOrderNumber(), userName, departmentName);
                    }

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

                // Note: Not accessing invoicePdf to avoid triggering BLOB loading
                // (even bad examples need to run without crashing)
                logger.error("🚨 EXTREMAMENTE PERIGOSO! Carregados {} pedidos! OutOfMemoryError iminente!",
                    orders.size());

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
                // PROBLEMA: Loads all order data (without BLOBs to avoid crash)
                // PROBLEMA: .filter() processa em memória o que deveria ser WHERE na BD
                // PROBLEMA: Desperdiça 90%+ dos dados carregados
                // RESULTADO: Lentidão extrema + risco de OutOfMemoryError
                List<Object[]> allOrdersData = orderRepository.findAllOrdersWithoutBlob();
                List<Object[]> filteredOrdersData = allOrdersData.stream()
                    .filter(row -> {
                        String statusStr = (String) row[4];
                        return status.name().equals(statusStr);
                    })
                    .toList();

                // Map to DTOs manually
                List<OrderDto> orderDtos = filteredOrdersData.stream()
                    .map(row -> {
                        return new OrderDto(
                            ((Number) row[0]).longValue(),
                            (String) row[1],
                            ((java.sql.Timestamp) row[2]).toLocalDateTime(),
                            (java.math.BigDecimal) row[3],
                            Order.OrderStatus.valueOf((String) row[4])
                        );
                    })
                    .toList();

                logger.error("🚨 PÉSSIMA PRÁTICA! Carregados {} pedidos total para filtrar {} com status {}",
                    allOrdersData.size(), filteredOrdersData.size(), status);

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
                // ❌ MÁ PRÁTICA: Carrega todos para encontrar um
                // PROBLEMA: Loads all order data (without BLOBs to avoid crash)
                // PROBLEMA: .filter() faz busca linear em memória (lento)
                // PROBLEMA: Deveria usar WHERE orderNumber = ? com índice
                // RESULTADO: Desperdício massivo para encontrar 1 registo
                List<Object[]> allOrdersData = orderRepository.findAllOrdersWithoutBlob();
                Optional<Object[]> orderData = allOrdersData.stream()
                    .filter(row -> {
                        String num = (String) row[1];
                        return orderNumber.equals(num);
                    })
                    .findFirst();

                if (orderData.isPresent()) {
                    Object[] row = orderData.get();
                    OrderDto dto = new OrderDto(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        ((java.sql.Timestamp) row[2]).toLocalDateTime(),
                        (java.math.BigDecimal) row[3],
                        Order.OrderStatus.valueOf((String) row[4])
                    );
                    logger.error("🚨 PÉSSIMA PRÁTICA! Carregados {} pedidos para encontrar 1!",
                        allOrdersData.size());
                    return ResponseEntity.ok(dto);
                } else {
                    logger.warn("⚠️ Pedido não encontrado: {} (após carregar {} registros)",
                        orderNumber, allOrdersData.size());
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

                // Note: Not accessing invoicePdf to avoid triggering BLOB loading
                // (even bad examples need to run without crashing)
                logger.error("🚨 PÉSSIMA PRÁTICA! Carregados {} pedidos desnecessariamente para encontrar {} pedidos!",
                    allOrders.size(), highValueOrders.size());

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