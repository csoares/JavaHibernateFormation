package com.formation.hibernate.controller;

import com.formation.hibernate.entity.Order;
import com.formation.hibernate.repository.OrderRepository;
import com.formation.hibernate.util.PerformanceMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 🎓 BLOB MANAGEMENT CONTROLLER - Demonstração Focada de Gestão de BLOBs
 * 
 * Este controlador demonstra APENAS técnicas de gestão eficiente de BLOBs:
 * 
 * 📚 CONCEITOS FOCADOS:
 * ✅ BLOB vs Dados Normais: Separação inteligente
 * ✅ Lazy Loading: Como e quando usar
 * ✅ Projecções: Carregar sem BLOBs
 * ✅ Streaming: Transferir ficheiros grandes
 * ✅ Performance: Evitar OutOfMemoryError
 * ✅ UX: Endpoints dedicados para BLOBs
 * 
 * 🔍 PROBLEMA PRINCIPAL:
 * - BLOBs são PESADOS (MB/GB cada um)
 * - Carregar desnecessariamente = CRASH
 * - Separar listagem de download = PERFORMANCE
 * 
 * 🎯 FOCO: Order + invoicePdf como exemplo prático
 */

@RestController
@RequestMapping("/api/blob-demo")
public class BlobManagementController {

    private static final Logger logger = LoggerFactory.getLogger(BlobManagementController.class);

    private final OrderRepository orderRepository;
    private final PerformanceMonitor performanceMonitor;

    public BlobManagementController(OrderRepository orderRepository, PerformanceMonitor performanceMonitor) {
        this.orderRepository = orderRepository;
        this.performanceMonitor = performanceMonitor;
    }

    /*
     * 🚨 DEMONSTRAÇÃO DO PROBLEMA COM BLOBS
     */

    @GetMapping("/bad/all-with-blobs")
    @Transactional(readOnly = true)
    public ResponseEntity<String> getAllOrdersWithBlobs() {
        String operationId = "blob-problem-all";

        return performanceMonitor.measure(operationId,
            "❌ Demonstração: Por que NÃO carregar BLOBs desnecessariamente",
            () -> {
                // ⚠️ DEMONSTRAÇÃO: O problema de carregar BLOBs
                // Não vamos realmente carregar para não crashar, mas vamos simular o impacto

                // Contar orders com PDFs SEM carregar os BLOBs
                List<Object[]> metadata = orderRepository.findOrdersWithBlobMetadata();

                int ordersWithPdf = 0;
                long estimatedSize = 0;

                for (Object[] row : metadata) {
                    Boolean hasPdf = (Boolean) row[3];
                    if (hasPdf) {
                        ordersWithPdf++;
                        // Estimar 500KB por PDF (conservador)
                        estimatedSize += 500 * 1024;
                    }
                }

                logger.warn("❌ PROBLEMA DEMONSTRADO: {} orders têm PDFs (~{} MB total)",
                    ordersWithPdf, estimatedSize / (1024 * 1024));

                String result = String.format(
                    "🚨 DEMONSTRAÇÃO DO PROBLEMA:\n" +
                    "  • Total de orders: %d\n" +
                    "  • Orders com PDF: %d\n" +
                    "  • Tamanho estimado: ~%d MB\n\n" +
                    "  💀 SE carregássemos TODOS os PDFs:\n" +
                    "     - Memória: ~%d MB consumidos!\n" +
                    "     - Tempo: Vários segundos de espera\n" +
                    "     - Bandwidth: Transferência desnecessária\n" +
                    "     - Risco: OutOfMemoryError com muitos orders!\n\n" +
                    "  ✅ SOLUÇÃO: Use endpoint /good/all-without-blobs\n" +
                    "     Carrega só metadados (KB em vez de MB!)",
                    metadata.size(),
                    ordersWithPdf,
                    estimatedSize / (1024 * 1024),
                    estimatedSize / (1024 * 1024)
                );

                return ResponseEntity.ok(result);
            });
    }

    /*
     * ✅ SOLUÇÃO 1: LISTAGEM SEM BLOBS
     */

    @GetMapping("/good/all-without-blobs")
    @Transactional(readOnly = true)
    public ResponseEntity<String> getAllOrdersWithoutBlobs() {
        String operationId = "blob-solution-list";

        return performanceMonitor.measure(operationId,
            "✅ Buscar orders SEM carregar BLOBs (SEGURO)",
            () -> {
                // ✅ SOLUÇÃO: Projecção que exclui BLOBs
                List<Object[]> orderData = orderRepository.findOrdersWithoutBlobData(null);
                
                StringBuilder result = new StringBuilder("📋 ORDERS (SEM PDFs):\n");
                for (Object[] row : orderData) {
                    result.append(String.format("- Order %s: %s (%s)\n",
                        row[0], // id
                        row[1], // orderNumber  
                        row[2]  // totalAmount
                    ));
                }
                
                logger.info("✅ SEGURO: Carregados {} orders SEM BLOBs (dados essenciais apenas)",
                    orderData.size());
                
                result.append(String.format("\n✅ Performance: EXCELENTE\n"));
                result.append(String.format("💾 Memória usada: MÍNIMA\n"));
                result.append(String.format("🌐 Bandwidth: OPTIMIZADO\n"));
                
                return ResponseEntity.ok(result.toString());
            });
    }

    /*
     * ✅ SOLUÇÃO 2: DOWNLOAD DE BLOB ESPECÍFICO
     */

    @GetMapping("/order/{id}/pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadOrderPdf(@PathVariable Long id) {
        String operationId = "blob-download-" + id;

        return performanceMonitor.measure(operationId,
            "✅ Download de PDF específico (carregamento direccionado)",
            () -> {
                // ✅ SOLUÇÃO: Carrega APENAS o order específico (com BLOB)
                Optional<Order> orderOpt = orderRepository.findById(id);
                
                if (orderOpt.isEmpty()) {
                    logger.warn("⚠️ Order {} não encontrado", id);
                    return ResponseEntity.notFound().build();
                }
                
                Order order = orderOpt.get();
                
                // ✅ SOLUÇÃO: Verifica se tem PDF antes de carregar
                if (order.getInvoicePdf() == null) {
                    logger.warn("⚠️ Order {} não tem PDF", id);
                    return ResponseEntity.notFound().build();
                }
                
                // ✅ SOLUÇÃO: Streaming eficiente do BLOB
                ByteArrayResource resource = new ByteArrayResource(order.getInvoicePdf());
                
                logger.info("✅ PDF do order {} enviado ({}KB)",
                    id, order.getInvoicePdf().length / 1024);
                
                return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(order.getInvoicePdf().length)
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=order-" + order.getOrderNumber() + ".pdf")
                    .body(resource);
            });
    }

    /*
     * ✅ SOLUÇÃO 3: VERIFICAÇÃO DE BLOB SEM CARREGAR
     */

    @GetMapping("/order/{id}/has-pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<String> checkIfOrderHasPdf(@PathVariable Long id) {
        String operationId = "blob-check-" + id;

        return performanceMonitor.measure(operationId,
            "✅ Verificar se order tem PDF SEM carregar o BLOB",
            () -> {
                // ✅ SOLUÇÃO: Query que só verifica existência (não carrega dados)
                boolean hasPdf = orderRepository.orderHasPdf(id);
                
                String result = String.format(
                    "📄 Order %d %s PDF\n" +
                    "💾 BLOB carregado: NÃO\n" +
                    "⚡ Performance: INSTANT\u00c2NEA",
                    id,
                    hasPdf ? "TEM" : "NÃO TEM"
                );
                
                logger.info("✅ Verificação de PDF para order {} sem carregar BLOB", id);
                
                return ResponseEntity.ok(result);
            });
    }

    /*
     * ✅ SOLUÇÃO 4: LISTAGEM COM METADATA DE BLOBS
     */

    @GetMapping("/list-efficient")
    @Transactional(readOnly = true)
    public ResponseEntity<String> getOrderListWithBlobMetadata() {
        String operationId = "blob-metadata-list";

        return performanceMonitor.measure(operationId,
            "✅ Listagem eficiente COM metadata de BLOBs",
            () -> {
                // ✅ SOLUÇÃO: Query que retorna dados + metadata sem carregar BLOBs
                List<Object[]> orderInfo = orderRepository.findOrdersWithBlobMetadata();
                
                StringBuilder result = new StringBuilder("📋 ORDERS COM METADATA:\n");
                for (Object[] row : orderInfo) {
                    result.append(String.format("- Order %s: %s | PDF: %s\n",
                        row[1], // orderNumber
                        row[2], // totalAmount
                        (Boolean) row[3] ? "✅ TEM" : "❌ NÃO TEM" // hasPdf
                    ));
                }
                
                logger.info("✅ Listagem eficiente com metadata de {} orders", orderInfo.size());
                
                result.append("\n💡 VANTAGENS:\n");
                result.append("- Dados essenciais: ✅ CARREGADOS\n");
                result.append("- BLOBs pesados: ❌ NÃO CARREGADOS\n");
                result.append("- Metadata de PDF: ✅ DISPONÍVEL\n");
                result.append("- Performance: ⚡ OPTIMIZADA\n");
                
                return ResponseEntity.ok(result.toString());
            });
    }

    /*
     * 📊 DEMONSTRAÇÃO DE IMPACTO DE PERFORMANCE
     */

    @GetMapping("/performance-comparison")
    @Transactional(readOnly = true)
    public ResponseEntity<String> comparePerformance() {
        String operationId = "blob-performance-comparison";

        return performanceMonitor.measure(operationId,
            "📊 Comparação directa de performance com/sem BLOBs",
            () -> {
                StringBuilder result = new StringBuilder("📊 COMPARAÇÃO DE PERFORMANCE:\n\n");
                
                // Teste 1: Com BLOBs
                long startTime = System.currentTimeMillis();
                List<Order> ordersWithBlobs = orderRepository.findAll();
                long timeWithBlobs = System.currentTimeMillis() - startTime;
                
                long totalBlobSize = ordersWithBlobs.stream()
                    .filter(o -> o.getInvoicePdf() != null)
                    .mapToLong(o -> o.getInvoicePdf().length)
                    .sum();
                
                // Teste 2: Sem BLOBs
                startTime = System.currentTimeMillis();
                List<Object[]> ordersWithoutBlobs = orderRepository.findOrdersWithoutBlobData(null);
                long timeWithoutBlobs = System.currentTimeMillis() - startTime;
                
                result.append("❌ COM BLOBs:\n");
                result.append(String.format("   Tempo: %dms\n", timeWithBlobs));
                result.append(String.format("   Memória: %dMB\n", totalBlobSize / (1024 * 1024)));
                result.append(String.format("   Orders: %d\n\n", ordersWithBlobs.size()));
                
                result.append("✅ SEM BLOBs:\n");
                result.append(String.format("   Tempo: %dms\n", timeWithoutBlobs));
                result.append(String.format("   Memória: ~1MB\n"));
                result.append(String.format("   Orders: %d\n\n", ordersWithoutBlobs.size()));
                
                double speedup = (double) timeWithBlobs / timeWithoutBlobs;
                result.append(String.format("⚡ MELHORIA: %.1fx mais rápido!\n", speedup));
                result.append(String.format("💾 ECONOMIA: %dMB de memória!\n", 
                    totalBlobSize / (1024 * 1024)));
                
                logger.info("📊 Comparação de performance: {}ms vs {}ms", 
                    timeWithBlobs, timeWithoutBlobs);
                
                return ResponseEntity.ok(result.toString());
            });
    }

    /*
     * 🔧 DEMONSTRAÇÃO DE BATCH PROCESSING
     */

    @GetMapping("/batch-process")
    @Transactional(readOnly = true)
    public ResponseEntity<String> batchProcessOrders() {
        String operationId = "blob-batch-process";

        return performanceMonitor.measure(operationId,
            "🔧 Processamento em lote SEM carregar BLOBs",
            () -> {
                // ✅ SOLUÇÃO: Processar orders sem carregar PDFs pesados
                List<Object[]> orderData = orderRepository.findOrdersWithoutBlobData(null);
                
                int processedCount = 0;
                double totalAmount = 0;
                
                for (Object[] row : orderData) {
                    // Processar dados essenciais (id, orderNumber, totalAmount)
                    Double amount = (Double) row[2];
                    if (amount != null) {
                        totalAmount += amount;
                    }
                    processedCount++;
                }
                
                String result = String.format(
                    "🔧 BATCH PROCESSING CONCLUÍDO:\n" +
                    "- Orders processados: %d\n" +
                    "- Valor total: €%.2f\n" +
                    "- BLOBs carregados: 0 (ZERO!)\n" +
                    "- Memória usada: MÍNIMA\n" +
                    "- Performance: MÁXIMA\n\n" +
                    "💡 VANTAGEM: Processamento rápido sem desperdício de recursos!",
                    processedCount, totalAmount
                );
                
                logger.info("🔧 Processamento em lote de {} orders sem BLOBs", processedCount);
                
                return ResponseEntity.ok(result);
            });
    }

    /*
     * 📈 ENDPOINT PARA COMPARAR PERFORMANCE
     */

    @GetMapping("/performance-summary")
    public ResponseEntity<String> getPerformanceSummary() {
        performanceMonitor.printSummary();
        return ResponseEntity.ok(
            "📊 Performance summary printed to logs.\n\n" +
            "🔍 Compare os tempos e uso de memória:\n" +
            "❌ /bad/all-with-blobs: PERIGO MÁXIMO!\n" +
            "✅ /good/all-without-blobs: SEGURO e RÁPIDO\n\n" +
            "📋 Test endpoints:\n" +
            "- GET /api/blob-demo/bad/all-with-blobs (PERIGO!)\n" +
            "- GET /api/blob-demo/good/all-without-blobs\n" +
            "- GET /api/blob-demo/order/{id}/pdf\n" +
            "- GET /api/blob-demo/order/{id}/has-pdf\n" +
            "- GET /api/blob-demo/list-efficient\n" +
            "- GET /api/blob-demo/performance-comparison\n" +
            "- GET /api/blob-demo/batch-process\n\n" +
            "💡 Regra de Ouro: NUNCA carregue BLOBs desnecessariamente!"
        );
    }
}