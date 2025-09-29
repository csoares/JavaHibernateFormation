package com.formation.hibernate.controller;

import com.formation.hibernate.entity.User;
import com.formation.hibernate.repository.UserRepository;
import com.formation.hibernate.util.PerformanceMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 🎓 N+1 PROBLEM CONTROLLER - Demonstração Focada do Problema N+1
 * 
 * Este controlador demonstra APENAS o problema N+1 para facilitar o aprendizado:
 * 
 * 📚 CONCEITOS FOCADOS:
 * ✅ Problema N+1: O que é e como acontece
 * ✅ Lazy Loading: Como causa queries extras
 * ✅ EntityGraph: Solução elegante para N+1
 * ✅ JOIN FETCH: Solução alternativa via JPQL
 * ✅ Performance Monitoring: Como medir a diferença
 * 
 * 🔍 EXEMPLO SIMPLES:
 * - 1 query para buscar User
 * - +1 query para buscar Department (PROBLEMA!)
 * - Solução: Carregar ambos numa única query
 * 
 * 🎯 FOCO: Apenas User + Department para simplicidade
 */

@RestController
@RequestMapping("/api/n1-demo")
public class N1ProblemController {

    private static final Logger logger = LoggerFactory.getLogger(N1ProblemController.class);

    private final UserRepository userRepository;
    private final PerformanceMonitor performanceMonitor;

    public N1ProblemController(UserRepository userRepository, PerformanceMonitor performanceMonitor) {
        this.userRepository = userRepository;
        this.performanceMonitor = performanceMonitor;
    }

    /*
     * 🚨 DEMONSTRAÇÃO DO PROBLEMA N+1
     */

    @GetMapping("/bad/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<String> getUserWithN1Problem(@PathVariable Long id) {
        String operationId = "n1-problem-bad-" + id;

        return performanceMonitor.measure(operationId,
            "❌ Buscar user SEM otimização (problema N+1)",
            () -> {
                // ❌ PROBLEMA: Query 1 - Busca o User
                Optional<User> userOpt = userRepository.findById(id);
                
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    
                    // ❌ PROBLEMA: Query 2 - Lazy loading do Department
                    // Esta linha dispara uma query extra!
                    String departmentName = user.getDepartment() != null ? 
                        user.getDepartment().getName() : "Sem Departamento";
                    
                    String result = String.format(
                        "👤 User: %s | 🏢 Department: %s", 
                        user.getName(), 
                        departmentName
                    );
                    
                    logger.warn("❌ PROBLEMA N+1: Executaram 2 queries para dados simples!");
                    return ResponseEntity.ok(result);
                } else {
                    return ResponseEntity.notFound().build();
                }
            });
    }

    /*
     * ✅ SOLUÇÃO 1: ENTITYGRAPH
     */

    @GetMapping("/good-entitygraph/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<String> getUserWithEntityGraph(@PathVariable Long id) {
        String operationId = "n1-solution-entitygraph-" + id;

        return performanceMonitor.measure(operationId,
            "✅ Buscar user COM EntityGraph (1 query apenas)",
            () -> {
                // ✅ SOLUÇÃO: Uma única query com JOIN
                Optional<User> userOpt = userRepository.findByIdWithDepartment(id);
                
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    
                    // ✅ SEM QUERY EXTRA: Department já carregado!
                    String departmentName = user.getDepartment() != null ? 
                        user.getDepartment().getName() : "Sem Departamento";
                    
                    String result = String.format(
                        "👤 User: %s | 🏢 Department: %s", 
                        user.getName(), 
                        departmentName
                    );
                    
                    logger.info("✅ OPTIMIZADO: Apenas 1 query executada!");
                    return ResponseEntity.ok(result);
                } else {
                    return ResponseEntity.notFound().build();
                }
            });
    }

    /*
     * ✅ SOLUÇÃO 2: JOIN FETCH
     */

    @GetMapping("/good-joinfetch/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<String> getUserWithJoinFetch(@PathVariable Long id) {
        String operationId = "n1-solution-joinfetch-" + id;

        return performanceMonitor.measure(operationId,
            "✅ Buscar user COM JOIN FETCH (1 query apenas)",
            () -> {
                // ✅ SOLUÇÃO: JOIN FETCH explícito na query
                Optional<User> userOpt = userRepository.findByIdWithDepartmentJoinFetch(id);
                
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    
                    // ✅ SEM QUERY EXTRA: Department carregado via JOIN FETCH!
                    String departmentName = user.getDepartment() != null ? 
                        user.getDepartment().getName() : "Sem Departamento";
                    
                    String result = String.format(
                        "👤 User: %s | 🏢 Department: %s", 
                        user.getName(), 
                        departmentName
                    );
                    
                    logger.info("✅ OPTIMIZADO: JOIN FETCH executou apenas 1 query!");
                    return ResponseEntity.ok(result);
                } else {
                    return ResponseEntity.notFound().build();
                }
            });
    }

    /*
     * 📊 DEMONSTRAÇÃO EM LOTE (N+1 mais evidente)
     */

    @GetMapping("/batch-bad")
    @Transactional(readOnly = true)
    public ResponseEntity<String> getAllUsersWithN1Problem() {
        String operationId = "n1-batch-problem";

        return performanceMonitor.measure(operationId,
            "❌ Buscar TODOS os users SEM otimização (N+1 MASSIVO)",
            () -> {
                // ❌ PROBLEMA: Query 1 - Busca todos os Users
                List<User> users = userRepository.findAll();
                
                StringBuilder result = new StringBuilder("📋 Lista de Users:\n");
                
                // ❌ PROBLEMA: Para cada User, mais uma query!
                for (User user : users) {
                    String departmentName = user.getDepartment() != null ? 
                        user.getDepartment().getName() : "Sem Departamento";
                    
                    result.append(String.format("- %s (%s)\n", user.getName(), departmentName));
                }
                
                logger.error("❌ PROBLEMA N+1 MASSIVO: {} users = 1 + {} queries = {} queries total!",
                    users.size(), users.size(), users.size() + 1);
                
                return ResponseEntity.ok(result.toString());
            });
    }

    @GetMapping("/batch-good")
    @Transactional(readOnly = true)
    public ResponseEntity<String> getAllUsersOptimized() {
        String operationId = "n1-batch-optimized";

        return performanceMonitor.measure(operationId,
            "✅ Buscar TODOS os users COM EntityGraph (1 query apenas)",
            () -> {
                // ✅ SOLUÇÃO: Uma query com JOIN para todos
                List<User> users = userRepository.findAllWithDepartment();
                
                StringBuilder result = new StringBuilder("📋 Lista de Users:\n");
                
                // ✅ SEM QUERIES EXTRAS: Todos os departments já carregados!
                for (User user : users) {
                    String departmentName = user.getDepartment() != null ? 
                        user.getDepartment().getName() : "Sem Departamento";
                    
                    result.append(String.format("- %s (%s)\n", user.getName(), departmentName));
                }
                
                logger.info("✅ OPTIMIZADO: {} users carregados com apenas 1 query!",
                    users.size());
                
                return ResponseEntity.ok(result.toString());
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
            "🔍 Compare the query counts:\n" +
            "❌ Bad endpoints: Multiple queries\n" +
            "✅ Good endpoints: Single query\n\n" +
            "📋 Test endpoints:\n" +
            "- GET /api/n1-demo/bad/{id}\n" +
            "- GET /api/n1-demo/good-entitygraph/{id}\n" +
            "- GET /api/n1-demo/good-joinfetch/{id}\n" +
            "- GET /api/n1-demo/batch-bad\n" +
            "- GET /api/n1-demo/batch-good"
        );
    }
}