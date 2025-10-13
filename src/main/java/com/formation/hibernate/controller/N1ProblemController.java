package com.formation.hibernate.controller;

import com.formation.hibernate.entity.User;
import com.formation.hibernate.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 🎓 N+1 PROBLEM CONTROLLER - Demonstração ULTRA SIMPLES
 * 
 * Este controlador foi DRASTICAMENTE simplificado para focar APENAS no N+1:
 * ✅ Sem DTOs, Converters, Services ou Performance Monitor
 * ✅ Apenas User + Department
 * ✅ Comparação directa: problema vs solução
 * ✅ Código mínimo para compreender o conceito
 * 
 * 🎯 FOCO: Ver N+1 acontecer e como resolver
 */

@RestController
@RequestMapping("/api/n1-demo")
public class N1ProblemController {

    private static final Logger logger = LoggerFactory.getLogger(N1ProblemController.class);

    private final UserRepository userRepository;

    public N1ProblemController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /*
     * 🚨 DEMONSTRAÇÃO DO PROBLEMA N+1
     */

    @GetMapping("/bad/{id}")
    @Transactional(readOnly = true)
    public String getUserWithN1Problem(@PathVariable Long id) {
        logger.warn("🚨 INICIANDO OPERAÇÃO COM PROBLEMA N+1");
        
        // ❌ PROBLEMA: Query 1 - Busca o User
        Optional<User> userOpt = userRepository.findById(id);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            // ❌ PROBLEMA: Query 2 - Lazy loading do Department
            // Esta linha dispara uma query extra!
            String departmentName = user.getDepartment() != null ? 
                user.getDepartment().getName() : "Sem Departamento";
            
            String result = String.format("User: %s | Department: %s", 
                user.getName(), departmentName);
            
            logger.error("❌ PROBLEMA N+1: Executaram 2 queries para dados simples!");
            return result;
        } else {
            return "User não encontrado";
        }
    }

    /*
     * ✅ SOLUÇÃO 1: ENTITYGRAPH
     */

    @GetMapping("/good-entitygraph/{id}")
    @Transactional(readOnly = true)
    public String getUserWithEntityGraph(@PathVariable Long id) {
        logger.info("✅ INICIANDO OPERAÇÃO COM ENTITYGRAPH");
        
        // ✅ SOLUÇÃO: Uma única query com JOIN
        Optional<User> userOpt = userRepository.findByIdWithDepartment(id);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            // ✅ SEM QUERY EXTRA: Department já carregado!
            String departmentName = user.getDepartment() != null ? 
                user.getDepartment().getName() : "Sem Departamento";
            
            String result = String.format("User: %s | Department: %s", 
                user.getName(), departmentName);
            
            logger.info("✅ OPTIMIZADO: Apenas 1 query executada!");
            return result;
        } else {
            return "User não encontrado";
        }
    }

    /*
     * ✅ SOLUÇÃO 2: JOIN FETCH
     */

    @GetMapping("/good-joinfetch/{id}")
    @Transactional(readOnly = true)
    public String getUserWithJoinFetch(@PathVariable Long id) {
        logger.info("✅ INICIANDO OPERAÇÃO COM JOIN FETCH");
        
        // ✅ SOLUÇÃO: JOIN FETCH explícito na query
        Optional<User> userOpt = userRepository.findByIdWithDepartmentJoinFetch(id);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            // ✅ SEM QUERY EXTRA: Department carregado via JOIN FETCH!
            String departmentName = user.getDepartment() != null ? 
                user.getDepartment().getName() : "Sem Departamento";
            
            String result = String.format("User: %s | Department: %s", 
                user.getName(), departmentName);
            
            logger.info("✅ OPTIMIZADO: JOIN FETCH executou apenas 1 query!");
            return result;
        } else {
            return "User não encontrado";
        }
    }

    /*
     * 📊 DEMONSTRAÇÃO EM LOTE (N+1 mais evidente)
     */

    @GetMapping("/batch-bad")
    @Transactional(readOnly = true)
    public String getAllUsersWithN1Problem() {
        logger.warn("🚨 INICIANDO OPERAÇÃO EM LOTE COM PROBLEMA N+1");
        
        // ❌ PROBLEMA: Query 1 - Busca todos os Users
        List<User> users = userRepository.findAll();
        
        StringBuilder result = new StringBuilder("Users:\n");
        
        // ❌ PROBLEMA: Para cada User, mais uma query!
        for (User user : users) {
            String departmentName = user.getDepartment() != null ? 
                user.getDepartment().getName() : "Sem Departamento";
            
            result.append(String.format("- %s (%s)\n", user.getName(), departmentName));
        }
        
        logger.error("❌ PROBLEMA N+1 MASSIVO: {} users = 1 + {} queries = {} queries total!",
            users.size(), users.size(), users.size() + 1);
        
        return result.toString();
    }

    @GetMapping("/batch-good")
    @Transactional(readOnly = true)
    public String getAllUsersOptimized() {
        logger.info("✅ INICIANDO OPERAÇÃO EM LOTE OPTIMIZADA");
        
        // ✅ SOLUÇÃO: Uma query com JOIN para todos
        List<User> users = userRepository.findAllWithDepartment();
        
        StringBuilder result = new StringBuilder("Users:\n");
        
        // ✅ SEM QUERIES EXTRAS: Todos os departments já carregados!
        for (User user : users) {
            String departmentName = user.getDepartment() != null ? 
                user.getDepartment().getName() : "Sem Departamento";
            
            result.append(String.format("- %s (%s)\n", user.getName(), departmentName));
        }
        
        logger.info("✅ OPTIMIZADO: {} users carregados com apenas 1 query!", users.size());
        
        return result.toString();
    }

    /*
     * 📊 DEMONSTRAÇÃO LIMITADA (10 users) - MAIS CLARA!
     * Com apenas 10 users, a diferença de performance fica MUITO mais visível
     */

    @GetMapping("/batch-bad-limited")
    @Transactional(readOnly = true)
    public String get10UsersWithN1Problem() {
        logger.warn("🚨 INICIANDO OPERAÇÃO LIMITADA (10 users) COM PROBLEMA N+1");

        long startTime = System.currentTimeMillis();

        // ❌ PROBLEMA: Query 1 - Busca 10 users
        List<User> users = userRepository.findFirst10Users();

        StringBuilder result = new StringBuilder("Users (Limited to 10):\n");

        // ❌ PROBLEMA: Para cada User, mais uma query! (10 queries extras)
        for (User user : users) {
            String departmentName = user.getDepartment() != null ?
                user.getDepartment().getName() : "Sem Departamento";

            result.append(String.format("- %s (%s)\n", user.getName(), departmentName));
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        logger.error("❌ PROBLEMA N+1: {} users = 1 + {} queries = {} queries total! Tempo: {}ms",
            users.size(), users.size(), users.size() + 1, duration);

        result.append(String.format("\n⏱️  Total queries: %d | Tempo: %dms", users.size() + 1, duration));

        return result.toString();
    }

    @GetMapping("/batch-good-limited")
    @Transactional(readOnly = true)
    public String get10UsersOptimized() {
        logger.info("✅ INICIANDO OPERAÇÃO LIMITADA (10 users) OPTIMIZADA");

        long startTime = System.currentTimeMillis();

        // ✅ SOLUÇÃO: Uma query com JOIN para os 10 users
        List<User> users = userRepository.findFirst10UsersWithDepartment();

        StringBuilder result = new StringBuilder("Users (Limited to 10):\n");

        // ✅ SEM QUERIES EXTRAS: Todos os departments já carregados!
        for (User user : users) {
            String departmentName = user.getDepartment() != null ?
                user.getDepartment().getName() : "Sem Departamento";

            result.append(String.format("- %s (%s)\n", user.getName(), departmentName));
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        logger.info("✅ OPTIMIZADO: {} users carregados com apenas 1 query! Tempo: {}ms", users.size(), duration);

        result.append(String.format("\n⏱️  Total queries: 1 | Tempo: %dms", duration));

        return result.toString();
    }

    /*
     * 📊 DEMONSTRAÇÃO MÉDIA (1000 users) - REALISTA E CLARA!
     * Sweet spot: mostra diferença clara sem overhead excessivo
     */

    @GetMapping("/batch-bad-medium")
    @Transactional(readOnly = true)
    public String get1000UsersWithN1Problem() {
        logger.warn("🚨 INICIANDO OPERAÇÃO MÉDIA (1000 users) COM PROBLEMA N+1");

        long startTime = System.currentTimeMillis();

        // ❌ PROBLEMA: Query 1 - Busca 1000 users
        List<User> users = userRepository.findFirst1000Users();

        StringBuilder result = new StringBuilder("Users (Limited to 1000):\n");

        // ❌ PROBLEMA: Para cada User, mais uma query! (1000 queries extras)
        for (User user : users) {
            String departmentName = user.getDepartment() != null ?
                user.getDepartment().getName() : "Sem Departamento";

            result.append(String.format("- %s (%s)\n", user.getName(), departmentName));
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        logger.error("❌ PROBLEMA N+1: {} users = 1 + {} queries = {} queries total! Tempo: {}ms",
            users.size(), users.size(), users.size() + 1, duration);

        result.append(String.format("\n⏱️  Total queries: %d | Tempo: %dms", users.size() + 1, duration));

        return result.toString();
    }

    @GetMapping("/batch-good-medium")
    @Transactional(readOnly = true)
    public String get1000UsersOptimized() {
        logger.info("✅ INICIANDO OPERAÇÃO MÉDIA (1000 users) OPTIMIZADA");

        long startTime = System.currentTimeMillis();

        // ✅ SOLUÇÃO: Uma query com JOIN para os 1000 users
        List<User> users = userRepository.findFirst1000UsersWithDepartment();

        StringBuilder result = new StringBuilder("Users (Limited to 1000):\n");

        // ✅ SEM QUERIES EXTRAS: Todos os departments já carregados!
        for (User user : users) {
            String departmentName = user.getDepartment() != null ?
                user.getDepartment().getName() : "Sem Departamento";

            result.append(String.format("- %s (%s)\n", user.getName(), departmentName));
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        logger.info("✅ OPTIMIZADO: {} users carregados com apenas 1 query! Tempo: {}ms", users.size(), duration);

        result.append(String.format("\n⏱️  Total queries: 1 | Tempo: %dms", duration));

        return result.toString();
    }

    /*
     * 📈 COMPARAÇÃO SIMPLES
     */

    @GetMapping("/compare")
    public String getComparison() {
        return """
            🔍 COMPARAÇÃO N+1 PROBLEM:

            ❌ PROBLEMA:
            - GET /api/n1-demo/bad/{id} - 2 queries
            - GET /api/n1-demo/batch-bad-limited - 11 queries (10 users)
            - GET /api/n1-demo/batch-bad-medium - 1,001 queries (1000 users) ⭐
            - GET /api/n1-demo/batch-bad - 1,001 queries (1.1M users)

            ✅ SOLUÇÕES:
            - GET /api/n1-demo/good-entitygraph/{id} - 1 query
            - GET /api/n1-demo/good-joinfetch/{id} - 1 query
            - GET /api/n1-demo/batch-good-limited - 1 query (10 users)
            - GET /api/n1-demo/batch-good-medium - 1 query (1000 users) ⭐
            - GET /api/n1-demo/batch-good - 1 query (1.1M users)

            💡 RECOMENDAÇÃO:
            - *-limited (10): Melhor para ensino - diferença muito clara
            - *-medium (1000): Melhor para demonstrar ganho real - ⭐ RECOMENDADO ⭐
            - Full (1.1M): Demonstra overhead e problemas de escala

            💡 TESTE: Active logs SQL e veja a diferença!
            logging.level.org.hibernate.SQL=DEBUG
            """;
    }
}