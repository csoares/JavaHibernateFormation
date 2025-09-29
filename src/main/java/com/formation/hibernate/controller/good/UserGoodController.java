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

/**
 * 🎓 USER GOOD CONTROLLER - Demonstração de Excelência em REST/JPA
 * 
 * Este controlador exemplifica as melhores práticas da indústria:
 * ✅ Transacções read-only optimizadas para consultas
 * ✅ Paginação obrigatória para escalabilidade
 * ✅ EntityGraphs para resolução elegante de problemas N+1
 * ✅ DTOs para isolamento da API e estrutura interna
 * ✅ ResponseEntity para controlo completo das respostas HTTP
 * ✅ Monitorização integrada de performance e métricas
 * ✅ Logging estruturado para observabilidade
 * ✅ Injecção de dependências por construtor
 * ✅ Tratamento adequado de erros e casos excepcionais
 */

// ✅ BOA PRÁTICA: @RestController combina @Controller + @ResponseBody
// Automaticamente serializa retornos para JSON
@RestController

// ✅ BOA PRÁTICA: @RequestMapping no nível da classe para prefixo comum
// Todos os métodos herdam este path base
@RequestMapping("/api/good/users")
public class UserGoodController {

    // ✅ BOA PRÁTICA: Logger para debugging e monitorização
    private static final Logger logger = LoggerFactory.getLogger(UserGoodController.class);

    /*
     * 🎓 DEPENDENCY INJECTION - Final fields para imutabilidade
     */
    private final UserRepository userRepository;
    private final UserConverter userConverter;
    private final PerformanceMonitor performanceMonitor;

    // ✅ BOA PRÁTICA: Constructor injection (preferível a @Autowired)
    public UserGoodController(UserRepository userRepository, UserConverter userConverter, PerformanceMonitor performanceMonitor) {
        this.userRepository = userRepository;
        this.userConverter = userConverter;
        this.performanceMonitor = performanceMonitor;
    }

    /*
     * 🎓 ENDPOINT GET BY ID - Demonstração de Otimizações
     */
    
    // ✅ BOA PRÁTICA: @GetMapping com path variable
    // RESTful: GET /api/good/users/{id}
    @GetMapping("/{id}")
    
    // ✅ BOA PRÁTICA: @Transactional(readOnly = true) para consultas
    // Otimização: Hibernate não faz dirty checking
    // Mais eficiente para operações de leitura
    @Transactional(readOnly = true)
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        // ✅ BOA PRÁTICA: ID único para rastreamento de performance
        String operationId = "getUserById-good-" + id;

        return performanceMonitor.measure(operationId,
            "Buscar usuário por ID com EntityGraph (otimizado)",
            () -> {
                // ✅ BOA PRÁTICA: Método otimizado do repository
                // findByIdWithDepartment() usa EntityGraph para evitar N+1
                // SEM isto: 2 queries (1 user + 1 department)
                // COM isto: 1 query com JOIN
                Optional<User> user = userRepository.findByIdWithDepartment(id);

                if (user.isPresent()) {
                    // ✅ BOA PRÁTICA: Converter entidade para DTO
                    // Separa estrutura interna da API pública
                    // Evita exposição de dados sensíveis/internos
                    UserDto dto = userConverter.toDto(user.get());
                    
                    // ✅ BOA PRÁTICA: Log estruturado para debugging
                    logger.info("✅ Usuário encontrado: {} (Departamento: {})",
                        dto.getName(), dto.getDepartment() != null ? dto.getDepartment().getName() : "N/A");
                    
                    // ✅ BOA PRÁTICA: ResponseEntity.ok() para HTTP 200
                    return ResponseEntity.ok(dto);
                } else {
                    // ✅ BOA PRÁTICA: Log de casos especiais
                    logger.warn("⚠️ Usuário não encontrado: {}", id);
                    
                    // ✅ BOA PRÁTICA: HTTP 404 para recurso não encontrado
                    return ResponseEntity.notFound().build();
                }
            });
    }

    /*
     * 🎓 ENDPOINT GET ALL - Demonstração de Paginação
     */
    
    // ✅ BOA PRÁTICA: @GetMapping sem path = endpoint base da classe
    // GET /api/good/users
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<Page<UserDto>> getAllUsers(
            // ✅ BOA PRÁTICA: @RequestParam com valores padrão
            // Parâmetros opcionais com defaults sensatos
            @RequestParam(defaultValue = "0") int page,      // Página 0 (primeira)
            @RequestParam(defaultValue = "20") int size,     // 20 itens por página
            @RequestParam(defaultValue = "id") String sortBy) { // Ordenar por ID

        String operationId = "getAllUsers-good-page-" + page;

        return performanceMonitor.measure(operationId,
            String.format("Buscar usuários paginados (página %d, tamanho %d) com EntityGraph", page, size),
            () -> {
                // ✅ BOA PRÁTICA: Paginação com Spring Data
                // PageRequest.of() cria objeto Pageable
                // Sort.by() adiciona ordenação
                Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
                
                // ✅ BOA PRÁTICA: Repository com EntityGraph evita N+1
                // Cada user vem com department carregado
                Page<User> users = userRepository.findAll(pageable);

                // ✅ BOA PRÁTICA: Page.map() converte Page<Entity> para Page<DTO>
                // Mantém metadados de paginação (totalElements, totalPages, etc.)
                // Method reference (::) mais limpo que lambda
                Page<UserDto> userDtos = users.map(userConverter::toDto);

                // ✅ BOA PRÁTICA: Log de métricas para monitorização
                logger.info("✅ Página {} de usuários carregada: {} elementos de {} total",
                    page, userDtos.getNumberOfElements(), userDtos.getTotalElements());

                // ✅ BOA PRÁTICA: Retorna Page<DTO> com metadados
                // Cliente recebe: content, totalElements, totalPages, size, number, etc.
                return ResponseEntity.ok(userDtos);
            });
    }

    /*
     * 🎓 ENDPOINT SUMMARIES - Demonstração de Projeção DTO
     */
    
    // ✅ BOA PRÁTICA: Endpoint específico para casos de uso específicos
    // /summaries retorna dados resumidos (mais eficiente)
    @GetMapping("/summaries")
    @Transactional(readOnly = true)
    public ResponseEntity<List<UserSummaryDto>> getUserSummaries() {
        String operationId = "getUserSummaries-good";

        return performanceMonitor.measure(operationId,
            "Buscar resumos de usuários com projeção JPQL",
            () -> {
                // ✅ BOA PRÁTICA: Projeção JPQL - MÁXIMA EFICIÊNCIA
                // Carrega APENAS os campos necessários (não entidades completas)
                // SELECT new UserSummaryDto(...) cria DTOs diretamente na query
                // Mais eficiente que carregar entidades e depois converter
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

    // ✅ BOA PRÁTICA: Endpoint para monitorização
    @GetMapping("/performance/summary")
    public ResponseEntity<String> getPerformanceSummary() {
        performanceMonitor.printSummary();
        return ResponseEntity.ok("Performance summary printed to logs");
    }
}

/*
 * 🎓 RESUMO DAS BOAS PRÁTICAS DEMONSTRADAS NESTE CONTROLLER:
 * 
 * 📋 ESTRUTURA:
 * ✅ @RestController para API REST
 * ✅ @RequestMapping para prefixo comum
 * ✅ Constructor injection para dependencies
 * ✅ Final fields para imutabilidade
 * 
 * 🔄 TRANSAÇÕES:
 * ✅ @Transactional(readOnly = true) para consultas
 * ✅ Otimização: sem dirty checking
 * 
 * 🔍 CONSULTAS:
 * ✅ EntityGraphs para resolver N+1 problems
 * ✅ JOIN FETCH para relações necessárias
 * ✅ Projeções DTO para dados específicos
 * ✅ Consultas COUNT para contagens eficientes
 * 
 * 📊 PAGINAÇÃO:
 * ✅ Page<T> para grandes datasets
 * ✅ @RequestParam com defaults sensatos
 * ✅ PageRequest.of() com ordenação
 * 
 * 🔄 CONVERSÕES:
 * ✅ DTOs para separar API da estrutura interna
 * ✅ Converters para transformações Entity↔DTO
 * ✅ Page.map() para manter metadados de paginação
 * 
 * 📡 HTTP:
 * ✅ ResponseEntity para controlo total da resposta
 * ✅ HTTP status codes apropriados (200, 404)
 * ✅ RESTful URLs (/users/{id}, /users/department/{name})
 * 
 * 📈 MONITORIZAÇÃO:
 * ✅ PerformanceMonitor para métricas
 * ✅ Logging estruturado para debugging
 * ✅ IDs únicos para tracking de operações
 * 
 * 🆚 COMPARE COM UserBadController PARA VER AS DIFERENÇAS!
 */