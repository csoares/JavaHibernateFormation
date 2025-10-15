package com.formation.hibernate.controller.good;

import com.formation.hibernate.converter.OrderConverter;
import com.formation.hibernate.dto.OrderDto;
import com.formation.hibernate.dto.OrderSummaryDto;
import com.formation.hibernate.dto.UserSummaryDto;
import com.formation.hibernate.entity.Order;
import com.formation.hibernate.repository.OrderRepository;
import com.formation.hibernate.util.PerformanceMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
            "Buscar pedido por ID com JOINs otimizados (User + Department em 1 query)",
            () -> {
                // ✅ BOA PRÁTICA: Native query com JOINs explícitos e SEM BLOB
                // VANTAGEM: Carrega Order + User + Department numa única query SQL
                // VANTAGEM: NÃO carrega o BLOB (invoicePdf) desnecessariamente
                // VANTAGEM: Retorna Object[] para evitar carregar entidade completa
                // RESULTADO: Máxima eficiência - 1 query em vez de 3 (N+1)
                // NOTE: Using native SQL returning Object[] to avoid BLOB loading
                //       This is necessary because Hibernate 6 ignores @Basic(fetch=LAZY) for byte[]
                Optional<Object[]> orderData = orderRepository.findOrderWithUserAndDepartmentNative(id);

                if (orderData.isPresent()) {
                    Object[] wrapper = orderData.get();
                    // NOTE: Spring Data JPA wraps native query results in Object[]
                    // So wrapper[0] contains the actual row data as Object[]
                    Object[] row = (Object[]) wrapper[0];

                    // Manual mapping from Object[] to OrderDto to avoid loading Order entity
                    // Columns: id, order_number, order_date, total_amount, status, user_id,
                    //          user_id2, user_name, user_email, user_created, dept_id, dept_name
                    // Note: PostgreSQL native queries return BigInteger for BIGINT columns
                    OrderDto dto = new OrderDto(
                        ((Number) row[0]).longValue(),                              // id
                        (String) row[1],                                            // order_number
                        ((java.sql.Timestamp) row[2]).toLocalDateTime(),            // order_date
                        (BigDecimal) row[3],                                        // total_amount
                        Order.OrderStatus.valueOf((String) row[4])                 // status
                    );

                    // ✅ BOA PRÁTICA: Dados de User e Department vêm na mesma query (JOINs)
                    // VANTAGEM: Sem N+1 problem - tudo numa única consulta SQL
                    // RESULTADO: Performance máxima com JOINs otimizados
                    String userName = (String) row[7];        // user_name
                    String userEmail = (String) row[8];       // user_email
                    java.sql.Timestamp userCreatedTs = (java.sql.Timestamp) row[9]; // user_created
                    LocalDateTime userCreated = userCreatedTs != null ? userCreatedTs.toLocalDateTime() : null;
                    String departmentName = (String) row[11]; // dept_name

                    dto.setUser(new UserSummaryDto(
                        ((Number) row[6]).longValue(),                              // user_id2
                        userName,
                        userEmail,
                        userCreated,
                        departmentName
                    ));

                    logger.info("✅ Pedido encontrado com 1 query otimizada: {} (User: {}, Department: {})",
                        dto.getOrderNumber(), userName, departmentName != null ? departmentName : "N/A");
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

    /*
     * 🎓 ENDPOINT DOWNLOAD PDF - Demonstração de Como Carregar BLOB Corretamente
     */

    // ✅ BOA PRÁTICA: Carregar BLOB APENAS quando explicitamente necessário
    // VANTAGEM: Endpoint dedicado para download - não afeta outros endpoints
    // VANTAGEM: Retorna byte[] diretamente com headers HTTP corretos
    // IMPORTANTE: Este é o ÚNICO endpoint que carrega o BLOB
    @GetMapping("/{id}/invoice/download")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable Long id) {
        String operationId = "downloadInvoicePdf-" + id;

        return performanceMonitor.measure(operationId,
            "Download de PDF do pedido (carregamento EXPLÍCITO de BLOB)",
            () -> {
                // ✅ BOA PRÁTICA: Query que carrega APENAS o BLOB
                // VANTAGEM: Não carrega outras colunas desnecessariamente
                // VANTAGEM: Evita problemas de serialização com @JsonIgnore
                byte[] pdfData = orderRepository.findInvoicePdfById(id);

                if (pdfData == null || pdfData.length == 0) {
                    logger.warn("⚠️ Pedido {} não encontrado ou não possui PDF", id);
                    return ResponseEntity.notFound().build();
                }

                // Get order number for filename (without loading full entity)
                Object[] metadata = orderRepository.findOrderMetadataById(id);
                String orderNumber = (metadata != null && metadata.length > 1 && metadata[1] != null) ?
                    (String) metadata[1] : "order-" + id;

                // ✅ BOA PRÁTICA: Headers HTTP corretos para download de arquivo
                // Content-Type: application/pdf (navegador sabe que é PDF)
                // Content-Disposition: attachment (força download)
                // Content-Length: tamanho em bytes
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_PDF);
                headers.setContentDispositionFormData("attachment",
                    "invoice-" + orderNumber + ".pdf");
                headers.setContentLength(pdfData.length);

                logger.info("✅ PDF do pedido {} baixado com sucesso ({} KB)",
                    orderNumber, pdfData.length / 1024);

                // ✅ BOA PRÁTICA: ResponseEntity com headers customizados
                // RESULTADO: Navegador faz download do arquivo corretamente
                return ResponseEntity
                    .ok()
                    .headers(headers)
                    .body(pdfData);
            });
    }

    // ✅ BOA PRÁTICA: Verificar se PDF existe SEM carregar o BLOB
    // VANTAGEM: Consulta leve que retorna apenas boolean
    // VANTAGEM: Cliente pode verificar antes de fazer download
    @GetMapping("/{id}/invoice/exists")
    @Transactional(readOnly = true)
    public ResponseEntity<Boolean> checkInvoiceExists(@PathVariable Long id) {
        String operationId = "checkInvoiceExists-" + id;

        return performanceMonitor.measure(operationId,
            "Verificar existência de PDF sem carregar dados",
            () -> {
                // ✅ BOA PRÁTICA: Usa query customizada que retorna apenas boolean
                // VANTAGEM: NÃO carrega o BLOB (apenas verifica IS NOT NULL)
                // RESULTADO: Query extremamente rápida e leve
                boolean exists = orderRepository.orderHasPdf(id);

                logger.info("✅ Verificação de PDF para pedido {}: {}", id, exists);

                return ResponseEntity.ok(exists);
            });
    }

    // ✅ BOA PRÁTICA: Retornar metadados do PDF sem carregar o arquivo
    // VANTAGEM: Cliente recebe informações úteis (tamanho, etc) antes de baixar
    @GetMapping("/{id}/invoice/metadata")
    @Transactional(readOnly = true)
    public ResponseEntity<Object> getInvoiceMetadata(@PathVariable Long id) {
        String operationId = "getInvoiceMetadata-" + id;

        return performanceMonitor.measure(operationId,
            "Obter metadados do PDF sem carregar dados",
            () -> {
                // ✅ BOA PRÁTICA: Query que usa LENGTH() para tamanho do BLOB
                // VANTAGEM: Não carrega o BLOB em memória - apenas calcula tamanho
                Object[] wrapper = orderRepository.findOrderMetadataById(id);

                if (wrapper == null || wrapper.length == 0) {
                    return ResponseEntity.notFound().build();
                }

                // Spring Data wraps native query results in Object[]
                // So wrapper[0] is the actual row as Object[]
                Object[] result = wrapper[0] instanceof Object[] ? (Object[]) wrapper[0] : wrapper;

                //Native queries return BigInteger for BIGINT columns in PostgreSQL
                Long orderId = result[0] instanceof Number ? ((Number) result[0]).longValue() : null;
                String orderNumber = (String) result[1];
                Integer pdfSizeBytes = result[2] != null ? ((Number) result[2]).intValue() : 0;

                java.util.Map<String, Object> metadata = new java.util.HashMap<>();
                metadata.put("orderId", orderId);
                metadata.put("orderNumber", orderNumber);
                metadata.put("hasPdf", pdfSizeBytes > 0);
                metadata.put("pdfSizeBytes", pdfSizeBytes);
                metadata.put("pdfSizeKB", pdfSizeBytes / 1024);
                metadata.put("pdfSizeMB", pdfSizeBytes / (1024.0 * 1024.0));

                logger.info("✅ Metadados do PDF obtidos para pedido {} ({} bytes)",
                    orderNumber, pdfSizeBytes);

                return ResponseEntity.ok(metadata);
            });
    }

    // ✅ BOA PRÁTICA: Retornar PDF em Base64 (para uso em JSON/APIs)
    @GetMapping("/{id}/invoice/base64")
    @Transactional(readOnly = true)
    public ResponseEntity<Object> getInvoicePdfAsBase64(@PathVariable Long id) {
        String operationId = "getInvoicePdfAsBase64-" + id;

        return performanceMonitor.measure(operationId,
            "Obter PDF como Base64 para JSON",
            () -> {
                // ✅ BOA PRÁTICA: Carrega apenas o BLOB
                byte[] pdfData = orderRepository.findInvoicePdfById(id);

                if (pdfData == null || pdfData.length == 0) {
                    logger.warn("⚠️ Pedido {} não encontrado ou não possui PDF", id);
                    return ResponseEntity.notFound().build();
                }

                // Get metadata
                Object[] metadata = orderRepository.findOrderMetadataById(id);
                String orderNumber = (metadata != null && metadata.length > 1 && metadata[1] != null) ?
                    (String) metadata[1] : "order-" + id;

                // Convert to Base64
                String base64Pdf = java.util.Base64.getEncoder().encodeToString(pdfData);

                java.util.Map<String, Object> response = new java.util.HashMap<>();
                response.put("orderId", id);
                response.put("orderNumber", orderNumber);
                response.put("pdfBase64", base64Pdf);
                response.put("pdfSizeBytes", pdfData.length);
                response.put("mimeType", "application/pdf");

                logger.info("✅ PDF do pedido {} convertido para Base64 ({} KB)",
                    orderNumber, pdfData.length / 1024);

                return ResponseEntity.ok(response);
            });
    }
}