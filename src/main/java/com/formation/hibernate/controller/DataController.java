package com.formation.hibernate.controller;

import com.formation.hibernate.service.DataPopulationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * 📁 DATA CONTROLLER - Controlador para Gestão de Dados de Teste
 * 
 * Este controlador demonstra boas práticas para operações de gestão:
 * ✅ Endpoints para estatísticas e limpeza de dados
 * ✅ Tratamento adequado de excepções com ResponseEntity
 * ✅ Logging estruturado para operações administrativas
 * ✅ Uso de serviços para lógica de negócio
 * ✅ Respostas HTTP apropriadas (200, 500)
 * ✅ Injeção de dependências por construtor
 * 
 * 🎯 Utilidade: Facilita testes e demonstrações do sistema
 */

// ✅ BOA PRÁTICA: @RestController para API REST
@RestController

// ✅ BOA PRÁTICA: @RequestMapping centralizado
@RequestMapping("/api/data")
public class DataController {

    // ✅ BOA PRÁTICA: Logger para monitorização
    private static final Logger logger = LoggerFactory.getLogger(DataController.class);

    // ✅ BOA PRÁTICA: Final field para imutabilidade
    private final DataPopulationService dataPopulationService;

    // EntityManager for native queries
    @PersistenceContext
    private EntityManager entityManager;

    // ✅ BOA PRÁTICA: Constructor injection
    public DataController(DataPopulationService dataPopulationService) {
        this.dataPopulationService = dataPopulationService;
    }


    /*
     * 📈 ENDPOINT STATISTICS - Demonstração de Monitorização
     */
    
    // ✅ BOA PRÁTICA: Endpoint GET para consulta de dados
    // VANTAGEM: Não modifica estado (idempotente)
    // VANTAGEM: Try-catch com logging estruturado
    @GetMapping("/statistics")
    public ResponseEntity<String> getDatabaseStatistics() {
        try {
            // ✅ BOA PRÁTICA: Delega lógica para o serviço
            // VANTAGEM: Controlador só gere HTTP, serviço gere negócio
            // RESULTADO: Separação clara de responsabilidades
            dataPopulationService.printDatabaseStatistics();
            
            // ✅ BOA PRÁTICA: ResponseEntity.ok() para HTTP 200
            return ResponseEntity.ok("📊 Estatísticas impressas nos logs");
        } catch (Exception e) {
            // ✅ BOA PRÁTICA: Log de erros com stack trace
            logger.error("❌ Erro ao obter estatísticas", e);
            
            // ✅ BOA PRÁTICA: HTTP 500 para erros internos
            return ResponseEntity.internalServerError()
                .body("❌ Erro ao obter estatísticas: " + e.getMessage());
        }
    }

    /*
     * 💾 ENDPOINT DATABASE SIZE - Check database size
     */

    @GetMapping("/size")
    public ResponseEntity<Map<String, Object>> getDatabaseSize() {
        try {
            Map<String, Object> result = new HashMap<>();

            // Get database size (PostgreSQL)
            String dbSizeQuery = "SELECT pg_database.datname AS database_name, " +
                                 "pg_size_pretty(pg_database_size(pg_database.datname)) AS size_pretty, " +
                                 "pg_database_size(pg_database.datname) AS size_bytes " +
                                 "FROM pg_database WHERE datname = current_database()";

            List<Object[]> dbSizeResult = entityManager.createNativeQuery(dbSizeQuery).getResultList();

            if (!dbSizeResult.isEmpty()) {
                Object[] row = dbSizeResult.get(0);
                result.put("database_name", row[0]);
                result.put("size_human_readable", row[1]);
                result.put("size_bytes", ((Number) row[2]).longValue());
                result.put("size_mb", ((Number) row[2]).longValue() / (1024.0 * 1024.0));
                result.put("size_gb", ((Number) row[2]).longValue() / (1024.0 * 1024.0 * 1024.0));
            }

            // Get table sizes
            String tableSizeQuery = "SELECT schemaname, tablename, " +
                                   "pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size_pretty, " +
                                   "pg_total_relation_size(schemaname||'.'||tablename) AS size_bytes " +
                                   "FROM pg_tables WHERE schemaname = 'public' " +
                                   "ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC";

            List<Object[]> tableSizes = entityManager.createNativeQuery(tableSizeQuery).getResultList();

            Map<String, Map<String, Object>> tables = new HashMap<>();
            for (Object[] row : tableSizes) {
                String tableName = (String) row[1];
                Map<String, Object> tableInfo = new HashMap<>();
                tableInfo.put("size_human_readable", row[2]);
                tableInfo.put("size_bytes", ((Number) row[3]).longValue());
                tableInfo.put("size_mb", ((Number) row[3]).longValue() / (1024.0 * 1024.0));
                tables.put(tableName, tableInfo);
            }
            result.put("tables", tables);

            logger.info("Database size retrieved successfully");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error getting database size", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Error getting database size: " + e.getMessage()));
        }
    }

    /*
     * 🗑️ ENDPOINT CLEAR - Demonstração de Operação Destrutiva
     */

    // ✅ BOA PRÁTICA: DELETE para operações destrutivas
    // VANTAGEM: Verbo HTTP semânticamente correcto
    // IMPORTANTE: Operação perigosa com logging apropriado
    @DeleteMapping("/clear")
    public ResponseEntity<String> clearDatabase() {
        try {
            // ✅ BOA PRÁTICA: Log de warn para operações críticas
            // VANTAGEM: Auditoria de ações destrutivas
            logger.warn("🗑️ Limpando toda a base de dados...");

            // ✅ BOA PRÁTICA: Delega operação complexa ao serviço
            dataPopulationService.clearDatabase();

            return ResponseEntity.ok("🗑️ Base de dados limpa com sucesso!");
        } catch (Exception e) {
            // ✅ BOA PRÁTICA: Log de erro com stack trace completo
            logger.error("❌ Erro ao limpar base de dados", e);

            // ✅ BOA PRÁTICA: Erro HTTP 500 com mensagem descritiva
            return ResponseEntity.internalServerError()
                .body("❌ Erro ao limpar base de dados: " + e.getMessage());
        }
    }
}