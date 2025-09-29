package com.formation.hibernate.controller;

import com.formation.hibernate.service.DataService;
import org.springframework.web.bind.annotation.*;

/**
 * 🎓 DATA CONTROLLER - Controlador ULTRA SIMPLES para Demonstração N+1
 * 
 * Controlador mínimo focado apenas em gestão de dados para N+1:
 * ✅ Apenas populate, clear e statistics
 * ✅ Sem complexidades desnecessárias
 * 
 * 🎯 FOCO: Suporte básico para criar dados de teste
 */

@RestController
@RequestMapping("/api/data")
public class DataController {

    private final DataService dataService;

    public DataController(DataService dataService) {
        this.dataService = dataService;
    }

    @PostMapping("/populate")
    public String populateData() {
        dataService.populateData();
        return "✅ Dados criados! Agora teste os endpoints N+1";
    }

    @GetMapping("/statistics")
    public String getStatistics() {
        dataService.printStatistics();
        return "📊 Estatísticas impressas nos logs";
    }

    @DeleteMapping("/clear")
    public String clearData() {
        dataService.clearData();
        return "🗑️ Dados removidos";
    }
}