package com.formation.hibernate.controller.bad;

import com.formation.hibernate.converter.UserConverter;
import com.formation.hibernate.dto.UserDto;
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
 * 🚨 USER BAD CONTROLLER - Demonstração de MÁS PRÁTICAS (NÃO COPIAR!)
 * 
 * ⚠️ AVISO: Este controlador demonstra práticas PERIGOSAS intencionalmente!
 * 
 * Más práticas demonstradas para fins educativos:
 * ❌ Ausência de transacções read-only (desperdício de recursos)
 * ❌ Problemas N+1 sistemáticos (múltiplas consultas desnecessárias)
 * ❌ Carregamento completo sem paginação (OutOfMemoryError)
 * ❌ Filtragem em memória em vez de consultas optimizadas
 * ❌ Contagens ineficientes carregando dados completos
 * ❌ Acesso a lazy properties que disparam consultas extras
 * ❌ Falta de optimizações de base de dados
 * 
 * 🎯 Use este controlador APENAS para comparar com UserGoodController!
 */

@RestController
@RequestMapping("/api/bad/users")
public class UserBadController {

    private static final Logger logger = LoggerFactory.getLogger(UserBadController.class);

    private final UserRepository userRepository;
    private final UserConverter userConverter;
    private final PerformanceMonitor performanceMonitor;

    public UserBadController(UserRepository userRepository, UserConverter userConverter, PerformanceMonitor performanceMonitor) {
        this.userRepository = userRepository;
        this.userConverter = userConverter;
        this.performanceMonitor = performanceMonitor;
    }

    // MÁ PRÁTICA: Sem EntityGraph (mas precisa transação para lazy loading funcionar)
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        String operationId = "getUserById-bad-" + id;

        return performanceMonitor.measure(operationId,
            "Buscar usuário por ID SEM otimizações (N+1 problem)",
            () -> {
                // MÁ PRÁTICA: Usa findById sem EntityGraph, causando N+1 problem
                Optional<User> user = userRepository.findById(id);

                if (user.isPresent()) {
                    // MÁ PRÁTICA: Acesso lazy trigger consultas extras
                    User u = user.get();
                    String departmentName = u.getDepartment() != null ? u.getDepartment().getName() : "N/A";

                    // Convert to DTO (without loading orders to avoid BLOB issues)
                    UserDto dto = new UserDto(u.getId(), u.getName(), u.getEmail(), u.getCreatedAt());
                    if (u.getDepartment() != null) {
                        dto.setDepartment(new com.formation.hibernate.dto.DepartmentDto(
                            u.getDepartment().getId(),
                            u.getDepartment().getName(),
                            u.getDepartment().getDescription(),
                            u.getDepartment().getBudget()
                        ));
                    }

                    logger.warn("⚠️ Usuário encontrado com N+1: {} (Departamento: {})",
                        dto.getName(), departmentName);
                    return ResponseEntity.ok(dto);
                } else {
                    logger.warn("⚠️ Usuário não encontrado: {}", id);
                    return ResponseEntity.notFound().build();
                }
            });
    }

    // MÁ PRÁTICA: Sem paginação, carrega todos os registros
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<UserDto>> getAllUsers() {
        String operationId = "getAllUsers-bad";

        return performanceMonitor.measure(operationId,
            "Buscar TODOS os usuários SEM paginação (PERIGOSO!)",
            () -> {
                // MÁ PRÁTICA: findAll() sem paginação carrega TODOS os registros
                List<User> users = userRepository.findAll();

                // MÁ PRÁTICA: Conversão para DTO força carregamento de todas as relações
                List<UserDto> userDtos = userConverter.toDtoList(users);

                logger.error("🚨 CUIDADO! Carregados {} usuários SEM paginação - pode causar OutOfMemoryError!",
                    users.size());

                return ResponseEntity.ok(userDtos);
            });
    }

    // MÁ PRÁTICA: Sem otimização de consulta por departamento
    @GetMapping("/department/{departmentName}")
    public ResponseEntity<List<UserDto>> getUsersByDepartment(@PathVariable String departmentName) {
        String operationId = "getUsersByDepartment-bad-" + departmentName;

        return performanceMonitor.measure(operationId,
            "Buscar usuários por departamento SEM otimização (N+1 problem)",
            () -> {
                // MÁ PRÁTICA: Carrega todos e filtra em memória (ou usa consulta não otimizada)
                List<User> allUsers = userRepository.findAll();
                List<User> filteredUsers = allUsers.stream()
                    .filter(user -> user.getDepartment() != null &&
                                  departmentName.equals(user.getDepartment().getName()))
                    .toList();

                // MÁ PRÁTICA: Cada conversão pode trigger mais consultas
                List<UserDto> userDtos = userConverter.toDtoList(filteredUsers);

                logger.error("🚨 PÉSSIMA PRÁTICA! Carregados {} usuários total para filtrar {} do departamento '{}'",
                    allUsers.size(), filteredUsers.size(), departmentName);

                return ResponseEntity.ok(userDtos);
            });
    }

    // MÁ PRÁTICA: Busca por email sem otimização
    @GetMapping("/email/{email}")
    public ResponseEntity<UserDto> getUserByEmail(@PathVariable String email) {
        String operationId = "getUserByEmail-bad-" + email.hashCode();

        return performanceMonitor.measure(operationId,
            "Buscar usuário por email SEM otimização",
            () -> {
                // MÁ PRÁTICA: Carrega todos e busca em memória
                List<User> allUsers = userRepository.findAll();
                Optional<User> user = allUsers.stream()
                    .filter(u -> email.equals(u.getEmail()))
                    .findFirst();

                if (user.isPresent()) {
                    UserDto dto = userConverter.toDto(user.get());
                    logger.error("🚨 PÉSSIMA PRÁTICA! Carregados {} usuários para encontrar 1 por email!",
                        allUsers.size());
                    return ResponseEntity.ok(dto);
                } else {
                    logger.warn("⚠️ Usuário não encontrado com email: {} (após carregar {} registros)",
                        email, allUsers.size());
                    return ResponseEntity.notFound().build();
                }
            });
    }

    // MÁ PRÁTICA: Contagem ineficiente
    @GetMapping("/department/{departmentId}/count")
    public ResponseEntity<Long> getUserCountByDepartment(@PathVariable Long departmentId) {
        String operationId = "getUserCountByDepartment-bad-" + departmentId;

        return performanceMonitor.measure(operationId,
            "Contar usuários por departamento (carregando TODOS)",
            () -> {
                // MÁ PRÁTICA: Carrega todos os usuários para contar
                List<User> allUsers = userRepository.findAll();
                long count = allUsers.stream()
                    .filter(user -> user.getDepartment() != null &&
                                  user.getDepartment().getId().equals(departmentId))
                    .count();

                logger.error("🚨 PÉSSIMA PRÁTICA! Carregados {} usuários para contar {} de um departamento!",
                    allUsers.size(), count);

                return ResponseEntity.ok(count);
            });
    }

    @GetMapping("/performance/summary")
    public ResponseEntity<String> getPerformanceSummary() {
        performanceMonitor.printSummary();
        return ResponseEntity.ok("Performance summary printed to logs - compare bad vs good!");
    }
}