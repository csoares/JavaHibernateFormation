package com.formation.hibernate.controller;

import com.formation.hibernate.service.DataPopulationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/data")
public class DataController {

    private static final Logger logger = LoggerFactory.getLogger(DataController.class);

    private final DataPopulationService dataPopulationService;

    public DataController(DataPopulationService dataPopulationService) {
        this.dataPopulationService = dataPopulationService;
    }


    @GetMapping("/statistics")
    public ResponseEntity<String> getDatabaseStatistics() {
        try {
            dataPopulationService.printDatabaseStatistics();
            return ResponseEntity.ok("📊 Estatísticas impressas nos logs");
        } catch (Exception e) {
            logger.error("❌ Erro ao obter estatísticas", e);
            return ResponseEntity.internalServerError()
                .body("❌ Erro ao obter estatísticas: " + e.getMessage());
        }
    }

    @DeleteMapping("/clear")
    public ResponseEntity<String> clearDatabase() {
        try {
            logger.warn("🗑️ Limpando toda a base de dados...");
            dataPopulationService.clearDatabase();
            return ResponseEntity.ok("🗑️ Base de dados limpa com sucesso!");
        } catch (Exception e) {
            logger.error("❌ Erro ao limpar base de dados", e);
            return ResponseEntity.internalServerError()
                .body("❌ Erro ao limpar base de dados: " + e.getMessage());
        }
    }
}