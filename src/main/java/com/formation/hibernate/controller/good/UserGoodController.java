package com.formation.hibernate.controller.good;

import com.formation.hibernate.converter.UserConverter;
import com.formation.hibernate.dto.UserDto;
import com.formation.hibernate.dto.UserSummaryDto;
import com.formation.hibernate.entity.User;
import com.formation.hibernate.repository.UserRepository;
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

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/good/users")
public class UserGoodController {

    private static final Logger logger = LoggerFactory.getLogger(UserGoodController.class);

    private final UserRepository userRepository;
    private final UserConverter userConverter;
    private final PerformanceMonitor performanceMonitor;

    public UserGoodController(UserRepository userRepository, UserConverter userConverter, PerformanceMonitor performanceMonitor) {
        this.userRepository = userRepository;
        this.userConverter = userConverter;
        this.performanceMonitor = performanceMonitor;
    }

    // BOM: Transação read-only para consultas
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        String operationId = "getUserById-good-" + id;

        return performanceMonitor.measure(operationId,
            "Buscar usuário por ID com EntityGraph (otimizado)",
            () -> {
                // BOM: Usa EntityGraph para evitar N+1 problem
                Optional<User> user = userRepository.findByIdWithDepartment(id);

                if (user.isPresent()) {
                    UserDto dto = userConverter.toDto(user.get());
                    logger.info("✅ Usuário encontrado: {} (Departamento: {})",
                        dto.getName(), dto.getDepartment() != null ? dto.getDepartment().getName() : "N/A");
                    return ResponseEntity.ok(dto);
                } else {
                    logger.warn("⚠️ Usuário não encontrado: {}", id);
                    return ResponseEntity.notFound().build();
                }
            });
    }

    // BOM: Paginação eficiente com EntityGraph
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<Page<UserDto>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy) {

        String operationId = "getAllUsers-good-page-" + page;

        return performanceMonitor.measure(operationId,
            String.format("Buscar usuários paginados (página %d, tamanho %d) com EntityGraph", page, size),
            () -> {
                // BOM: Paginação com EntityGraph
                Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
                Page<User> users = userRepository.findAll(pageable);

                Page<UserDto> userDtos = users.map(userConverter::toDto);

                logger.info("✅ Página {} de usuários carregada: {} elementos de {} total",
                    page, userDtos.getNumberOfElements(), userDtos.getTotalElements());

                return ResponseEntity.ok(userDtos);
            });
    }

    // BOM: Projeção para listagem rápida
    @GetMapping("/summaries")
    @Transactional(readOnly = true)
    public ResponseEntity<List<UserSummaryDto>> getUserSummaries() {
        String operationId = "getUserSummaries-good";

        return performanceMonitor.measure(operationId,
            "Buscar resumos de usuários com projeção JPQL",
            () -> {
                // BOM: Usa projeção JPQL para carregar apenas dados necessários
                List<UserSummaryDto> summaries = userRepository.findAllUserSummaries();

                logger.info("✅ {} resumos de usuários carregados eficientemente", summaries.size());

                return ResponseEntity.ok(summaries);
            });
    }

    // BOM: Consulta otimizada por departamento
    @GetMapping("/department/{departmentName}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<UserDto>> getUsersByDepartment(@PathVariable String departmentName) {
        String operationId = "getUsersByDepartment-good-" + departmentName;

        return performanceMonitor.measure(operationId,
            "Buscar usuários por departamento com JOIN FETCH",
            () -> {
                // BOM: Usa consulta otimizada com JOIN FETCH
                List<User> users = userRepository.findByDepartmentNameOptimized(departmentName);
                List<UserDto> userDtos = userConverter.toDtoList(users);

                logger.info("✅ {} usuários encontrados no departamento '{}'", users.size(), departmentName);

                return ResponseEntity.ok(userDtos);
            });
    }

    // BOM: Busca por email com otimização
    @GetMapping("/email/{email}")
    @Transactional(readOnly = true)
    public ResponseEntity<UserDto> getUserByEmail(@PathVariable String email) {
        String operationId = "getUserByEmail-good-" + email.hashCode();

        return performanceMonitor.measure(operationId,
            "Buscar usuário por email com JOIN FETCH",
            () -> {
                // BOM: Usa consulta otimizada com JOIN FETCH
                Optional<User> user = userRepository.findByEmailWithDepartment(email);

                if (user.isPresent()) {
                    UserDto dto = userConverter.toDto(user.get());
                    logger.info("✅ Usuário encontrado por email: {}", dto.getName());
                    return ResponseEntity.ok(dto);
                } else {
                    logger.warn("⚠️ Usuário não encontrado com email: {}", email);
                    return ResponseEntity.notFound().build();
                }
            });
    }

    // BOM: Contagem eficiente
    @GetMapping("/department/{departmentId}/count")
    @Transactional(readOnly = true)
    public ResponseEntity<Long> getUserCountByDepartment(@PathVariable Long departmentId) {
        String operationId = "getUserCountByDepartment-good-" + departmentId;

        return performanceMonitor.measure(operationId,
            "Contar usuários por departamento (consulta COUNT otimizada)",
            () -> {
                // BOM: Usa consulta COUNT específica
                Long count = userRepository.countByDepartmentId(departmentId);

                logger.info("✅ Departamento {} tem {} usuários", departmentId, count);

                return ResponseEntity.ok(count);
            });
    }

    @GetMapping("/performance/summary")
    public ResponseEntity<String> getPerformanceSummary() {
        performanceMonitor.printSummary();
        return ResponseEntity.ok("Performance summary printed to logs");
    }
}