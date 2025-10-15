package com.formation.hibernate.controller;

import com.formation.hibernate.converter.UserConverter;
import com.formation.hibernate.dto.UserDto;
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

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;

/**
 * 🎓 PAGINATION CONTROLLER - Demonstração Focada de Paginação Eficiente
 * 
 * Este controlador demonstra APENAS técnicas de paginação para facilitar o aprendizado:
 * 
 * 📚 CONCEITOS FOCADOS:
 * ✅ Page vs List: Quando usar cada abordagem
 * ✅ Pageable: Configuração flexível (page, size, sort)
 * ✅ Sort: Ordenação simples e múltipla
 * ✅ Performance: Como evitar OutOfMemoryError
 * ✅ UX: Metadados para navegação (totalPages, hasNext, etc.)
 * ✅ Validação: Limites seguros para parâmetros
 * 
 * 🔍 COMPARAÇÃO SIMPLES:
 * - findAll() = PERIGO (carrega tudo)
 * - findAll(Pageable) = SEGURO (carrega página)
 * 
 * 🎯 FOCO: Gestão inteligente de grandes volumes de dados
 */

@RestController
@RequestMapping("/api/pagination-demo")
public class PaginationController {

    private static final Logger logger = LoggerFactory.getLogger(PaginationController.class);

    private final UserRepository userRepository;
    private final UserConverter userConverter;
    private final PerformanceMonitor performanceMonitor;

    public PaginationController(UserRepository userRepository, UserConverter userConverter, PerformanceMonitor performanceMonitor) {
        this.userRepository = userRepository;
        this.userConverter = userConverter;
        this.performanceMonitor = performanceMonitor;
    }

    /*
     * 🚨 DEMONSTRAÇÃO DO PROBLEMA SEM PAGINAÇÃO
     */

    @GetMapping("/bad/all")
    @Transactional(readOnly = true)
    public ResponseEntity<List<UserDto>> getAllUsersWithoutPagination() {
        String operationId = "pagination-problem-all";

        return performanceMonitor.measure(operationId,
            "❌ Buscar TODOS os users SEM paginação (PERIGOSO!)",
            () -> {
                // ❌ PROBLEMA: Carrega TODOS os registos na memória
                List<User> allUsers = userRepository.findAll();
                
                // ❌ PROBLEMA: Converte TODOS para DTO (mais memória)
                List<UserDto> allUserDtos = userConverter.toDtoList(allUsers);
                
                logger.error("❌ PERIGO: Carregados {} users SEM paginação! Risco de OutOfMemoryError!",
                    allUsers.size());
                
                return ResponseEntity.ok(allUserDtos);
            });
    }

    /*
     * ⚠️ DEMONSTRAÇÃO: PAGINAÇÃO BÁSICA (SEM EntityGraph)
     *
     * Este endpoint usa paginação MAS não usa EntityGraph.
     * Quando os DTOs são convertidos e acessam department.name,
     * ocorre um N+1 problem: 1 query para users + N queries para departments.
     */

    @GetMapping("/good")
    @Transactional(readOnly = true)
    public ResponseEntity<Page<UserDto>> getUsersPaginated(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        String operationId = "pagination-basic-" + page + "-" + size;

        return performanceMonitor.measure(operationId,
            "⚠️ Buscar users COM paginação MAS sem EntityGraph (pode ter N+1)",
            () -> {
                // ✅ SOLUÇÃO parcial: Pageable limita resultados
                Pageable pageable = PageRequest.of(page, size);

                // ⚠️ PROBLEMA: Sem EntityGraph, pode causar N+1
                Page<User> userPage = userRepository.findAllWithoutEntityGraph(pageable);

                // ⚠️ PROBLEMA: Conversão para DTO acessa department = N+1!
                Page<UserDto> userDtoPage = userPage.map(userConverter::toDto);

                logger.info("⚠️ PAGINAÇÃO BÁSICA: Página {} com {} users (de {} total) - verifique N+1 nos logs!",
                    page, userDtoPage.getNumberOfElements(), userDtoPage.getTotalElements());

                return ResponseEntity.ok(userDtoPage);
            });
    }

    /*
     * ✅ SOLUÇÃO 2: PAGINAÇÃO COM ORDENAÇÃO
     */

    @GetMapping("/good-with-sort")
    @Transactional(readOnly = true)
    public ResponseEntity<Page<UserDto>> getUsersPaginatedWithSort(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        String operationId = "pagination-sort-" + page + "-" + size + "-" + sortBy;

        return performanceMonitor.measure(operationId,
            "✅ Buscar users COM paginação E ordenação",
            () -> {
                // ✅ SOLUÇÃO: Sort direccional (ASC/DESC)
                Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? 
                    Sort.Direction.DESC : Sort.Direction.ASC;
                Sort sort = Sort.by(direction, sortBy);
                
                // ✅ SOLUÇÃO: Pageable com ordenação
                Pageable pageable = PageRequest.of(page, size, sort);
                
                Page<User> userPage = userRepository.findAll(pageable);
                Page<UserDto> userDtoPage = userPage.map(userConverter::toDto);
                
                logger.info("✅ PAGINAÇÃO + SORT: Página {} ordenada por {} {} com {} users",
                    page, sortBy, sortDir.toUpperCase(), userDtoPage.getNumberOfElements());
                
                return ResponseEntity.ok(userDtoPage);
            });
    }

    /*
     * ✅ SOLUÇÃO 3: PAGINAÇÃO COM FILTRO DE PESQUISA
     */

    @GetMapping("/search")
    @Transactional(readOnly = true)
    public ResponseEntity<Page<UserDto>> searchUsersPaginated(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        String operationId = "pagination-search-" + name + "-" + page;

        return performanceMonitor.measure(operationId,
            "✅ Pesquisar users COM paginação (filtro + página)",
            () -> {
                // ✅ SOLUÇÃO: Paginação + filtro numa única operação
                Pageable pageable = PageRequest.of(page, size, Sort.by("name"));
                
                // ✅ SOLUÇÃO: Query Method com paginação automática
                Page<User> userPage = userRepository.findByNameContainingIgnoreCase(name, pageable);
                Page<UserDto> userDtoPage = userPage.map(userConverter::toDto);
                
                logger.info("✅ PESQUISA PAGINADA: '{}' encontrou {} users na página {} (de {} total)",
                    name, userDtoPage.getNumberOfElements(), page, userDtoPage.getTotalElements());
                
                return ResponseEntity.ok(userDtoPage);
            });
    }

    /*
     * ✅ SOLUÇÃO 4: PAGINAÇÃO + ENTITYGRAPH (Evita N+1)
     *
     * NOTA: O método findAll(Pageable) do UserRepository JÁ usa @EntityGraph
     * por isso TODOS os endpoints de paginação evitam N+1 automaticamente.
     * Este endpoint demonstra explicitamente que usar EntityGraph mantém
     * a performance consistente mesmo ao converter para DTOs.
     */

    @GetMapping("/optimized")
    @Transactional(readOnly = true)
    public ResponseEntity<Page<UserDto>> getUsersPaginatedOptimized(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        String operationId = "pagination-optimized-" + page + "-" + size;

        return performanceMonitor.measure(operationId,
            "✅ Buscar users COM paginação + EntityGraph (sem N+1)",
            () -> {
                // ✅ SOLUÇÃO: Paginação SEM ordenação extra (mais rápido)
                Pageable pageable = PageRequest.of(page, size);

                // ✅ SOLUÇÃO: findAll() usa @EntityGraph do UserRepository
                // Carrega users + departments numa única query (evita N+1)
                Page<User> userPage = userRepository.findAll(pageable);
                Page<UserDto> userDtoPage = userPage.map(userConverter::toDto);

                logger.info("✅ PAGINAÇÃO OPTIMIZADA: Página {} com {} users e departments carregados",
                    page, userDtoPage.getNumberOfElements());

                return ResponseEntity.ok(userDtoPage);
            });
    }

    /*
     * 📊 DEMONSTRAÇÃO DE METADADOS DE PAGINAÇÃO
     */

    @GetMapping("/metadata/{page}")
    @Transactional(readOnly = true)
    public ResponseEntity<String> showPaginationMetadata(@PathVariable int page) {
        String operationId = "pagination-metadata-" + page;

        return performanceMonitor.measure(operationId,
            "📊 Demonstrar metadados de paginação",
            () -> {
                Pageable pageable = PageRequest.of(page, 10);
                Page<User> userPage = userRepository.findAll(pageable);
                
                StringBuilder metadata = new StringBuilder();
                metadata.append("📄 METADADOS DA PÁGINA:\n");
                metadata.append(String.format("- Página actual: %d\n", userPage.getNumber()));
                metadata.append(String.format("- Tamanho da página: %d\n", userPage.getSize()));
                metadata.append(String.format("- Elementos nesta página: %d\n", userPage.getNumberOfElements()));
                metadata.append(String.format("- Total de elementos: %d\n", userPage.getTotalElements()));
                metadata.append(String.format("- Total de páginas: %d\n", userPage.getTotalPages()));
                metadata.append(String.format("- É primeira página? %s\n", userPage.isFirst()));
                metadata.append(String.format("- É última página? %s\n", userPage.isLast()));
                metadata.append(String.format("- Tem próxima? %s\n", userPage.hasNext()));
                metadata.append(String.format("- Tem anterior? %s\n", userPage.hasPrevious()));
                
                logger.info("📊 Metadados da página {} exibidos", page);
                
                return ResponseEntity.ok(metadata.toString());
            });
    }

    /*
     * 🔧 ENDPOINT PARA TESTAR LIMITES
     */

    @GetMapping("/stress-test")
    @Transactional(readOnly = true)
    public ResponseEntity<String> stressTestPagination() {
        String operationId = "pagination-stress-test";

        return performanceMonitor.measure(operationId,
            "🔧 Teste de stress: múltiplas páginas sequenciais",
            () -> {
                StringBuilder result = new StringBuilder("🔧 TESTE DE STRESS DE PAGINAÇÃO:\n\n");

                long totalTime = 0;
                long minTime = Long.MAX_VALUE;
                long maxTime = 0;

                // Testa 10 páginas sequenciais para medir consistência
                for (int i = 0; i < 10; i++) {
                    long startTime = System.currentTimeMillis();

                    Pageable pageable = PageRequest.of(i, 50);
                    Page<User> page = userRepository.findAll(pageable);

                    long endTime = System.currentTimeMillis();
                    long duration = endTime - startTime;

                    totalTime += duration;
                    minTime = Math.min(minTime, duration);
                    maxTime = Math.max(maxTime, duration);

                    result.append(String.format("Página %d: %dms (%d users)\n",
                        i, duration, page.getNumberOfElements()));
                }

                long avgTime = totalTime / 10;

                result.append("\n📊 ESTATÍSTICAS:\n");
                result.append(String.format("  • Tempo médio: %dms\n", avgTime));
                result.append(String.format("  • Tempo mínimo: %dms\n", minTime));
                result.append(String.format("  • Tempo máximo: %dms\n", maxTime));
                result.append(String.format("  • Variação: %dms (%.1f%%)\n",
                    maxTime - minTime,
                    ((maxTime - minTime) * 100.0 / avgTime)));
                result.append(String.format("  • Tempo total: %dms\n", totalTime));

                if (maxTime > avgTime * 1.5) {
                    result.append("\n⚠️  AVISO: Performance inconsistente detectada!\n");
                    result.append("   Algumas páginas são significativamente mais lentas.\n");
                } else {
                    result.append("\n✅ Performance consistente entre todas as páginas!\n");
                }

                logger.info("🔧 Teste de stress de paginação concluído - Média: {}ms", avgTime);

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
            "🔍 Compare os tempos de execução:\n" +
            "❌ /bad/all: Alto tempo e memória\n" +
            "✅ /good: Tempo consistente e baixa memória\n\n" +
            "📋 Test endpoints:\n" +
            "- GET /api/pagination-demo/bad/all (PERIGO!)\n" +
            "- GET /api/pagination-demo/good?page=0&size=20\n" +
            "- GET /api/pagination-demo/good-with-sort?page=0&size=10&sortBy=email&sortDir=desc\n" +
            "- GET /api/pagination-demo/search?name=João&page=0&size=5\n" +
            "- GET /api/pagination-demo/optimized?page=2&size=15\n" +
            "- GET /api/pagination-demo/metadata/0\n" +
            "- GET /api/pagination-demo/stress-test"
        );
    }
}