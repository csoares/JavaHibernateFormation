package com.formation.hibernate.controller;

import com.formation.hibernate.service.DataPopulationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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