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

/**
 * 🎓 ORDER GOOD CONTROLLER - Demonstração de Excelência com BLOBS e Performance
 * 
 * Este controlador exemplifica as melhores práticas para entidades com BLOB:
 * ✅ Transações read-only optimizadas para consultas
 * ✅ EntityGraphs estratégicos para resolver problemas N+1
 * ✅ Consultas que EVITAM carregar BLOBs desnecessariamente
 * ✅ Paginação obrigatória para escalabilidade com BLOBs
 * ✅ Projecções DTO para máxima eficiência de dados
 * ✅ Consultas agregadas SQL em vez de cálculos em memória
 * ✅ Índices optimizados para consultas por data
 * ✅ Monitorização integrada de performance e métricas
 * ✅ Logging estruturado para observabilidade
 * ✅ Controlo total de respostas HTTP com ResponseEntity
 * ✅ Tratamento adequado de erros e casos excepcionais
 * ✅ Separação clara entre dados essenciais e BLOBs pesados
 */

// ✅ BOA PRÁTICA: @RestController combina @Controller + @ResponseBody
@RestController

// ✅ BOA PRÁTICA: @RequestMapping no nível da classe para prefixo comum
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

    /*
     * 🎓 ENDPOINT GET BY ID - Demonstração de Optimizações com BLOB
     */
    
    // ✅ BOA PRÁTICA: @Transactional(readOnly = true) para consultas
    // VANTAGEM: Hibernate não faz dirty checking (mais eficiente)
    // VANTAGEM: Base de dados pode optimizar consultas read-only
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<OrderDto> getOrderById(@PathVariable Long id) {
        String operationId = "getOrderById-good-" + id;

        return performanceMonitor.measure(operationId,
            "Buscar pedido por ID com EntityGraph (User + Department)",
            () -> {
                // ✅ BOA PRÁTICA: EntityGraph estratégico para BLOBS
                // VANTAGEM: Carrega Order + User + Department numa única query
                // IMPORTANTE: NÃO carrega o BLOB (invoicePdf) desnecessariamente
                // RESULTADO: Máxima eficiência sem desperdício de memória
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

    /*
     * 🎓 ENDPOINT PAGINATED - Demonstração de Paginação com BLOBS
     */
    
    // ✅ BOA PRÁTICA: Paginação obrigatória para entidades com BLOB
    // VANTAGEM: Evita carregar milhares de pedidos + PDFs de uma vez
    // VANTAGEM: Consulta directa por status (usa índice)
    // RESULTADO: Performance consistente independente do volume de dados
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
                // ✅ BOA PRÁTICA: Paginação com JOIN FETCH estratégico
                // VANTAGEM: PageRequest limita resultados (evita OutOfMemoryError)
                // VANTAGEM: Sort optimiza ordem (usa índice em order_date)
                // VANTAGEM: findByStatusWithUser() evita N+1 mas NÃO carrega BLOBs
                Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
                Page<Order> orders = orderRepository.findByStatusWithUser(status, pageable);

                Page<OrderDto> orderDtos = orders.map(orderConverter::toDto);

                logger.info("✅ Página {} de pedidos {} carregada: {} elementos",
                    page, status, orderDtos.getNumberOfElements());

                return ResponseEntity.ok(orderDtos);
            });
    }

    /*
     * 🎓 ENDPOINT PROJECTION - Demonstração de Máxima Eficiência
     */
    
    // ✅ BOA PRÁTICA: Projecção DTO para dados resumidos
    // VANTAGEM: Carrega APENAS os campos necessários (nunca BLOBs)
    // VANTAGEM: SELECT específico em vez de entidades completas
    // RESULTADO: Performance máxima para listagens e resumos
    @GetMapping("/user/{userId}/summaries")
    @Transactional(readOnly = true)
    public ResponseEntity<List<OrderSummaryDto>> getOrderSummariesByUser(@PathVariable Long userId) {
        String operationId = "getOrderSummariesByUser-good-" + userId;

        return performanceMonitor.measure(operationId,
            "Buscar resumos de pedidos por usuário com projeção JPQL",
            () -> {
                // ✅ BOA PRÁTICA: Projecção JPQL - MÁXIMA EFICIÊNCIA
                // VANTAGEM: SELECT new OrderSummaryDto(...) cria DTOs directamente
                // VANTAGEM: NÃO carrega entidades nem BLOBs pesados
                // VANTAGEM: Transferência mínima de dados pela rede
                // RESULTADO: Performance óptima para resumos
                List<OrderSummaryDto> summaries = orderRepository.findOrderSummariesByUserId(userId);

                logger.info("✅ {} resumos de pedidos carregados para usuário {}", summaries.size(), userId);

                return ResponseEntity.ok(summaries);
            });
    }

    /*
     * 🎓 ENDPOINT SEARCH - Demonstração de Consulta Optimizada
     */
    
    // ✅ BOA PRÁTICA: Consulta por índice único sem carregar BLOB
    // VANTAGEM: WHERE orderNumber = ? usa índice (performance O(1))
    // VANTAGEM: JOIN FETCH carrega relações necessárias
    // IMPORTANTE: NÃO carrega invoicePdf até ser explicitamente pedido
    @GetMapping("/number/{orderNumber}")
    @Transactional(readOnly = true)
    public ResponseEntity<OrderDto> getOrderByNumber(@PathVariable String orderNumber) {
        String operationId = "getOrderByNumber-good-" + orderNumber.hashCode();

        return performanceMonitor.measure(operationId,
            "Buscar pedido por número com JOIN FETCH múltiplo",
            () -> {
                // ✅ BOA PRÁTICA: Múltiplos JOIN FETCH estratégicos
                // VANTAGEM: Uma única query para Order + User + Department
                // VANTAGEM: Usa índice único em orderNumber (instantâneo)
                // IMPORTANTE: Método não inclui BLOB no EntityGraph
                // RESULTADO: Dados completos sem carregar MB de PDF
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