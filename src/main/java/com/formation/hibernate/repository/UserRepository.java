package com.formation.hibernate.repository;

import com.formation.hibernate.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 🎓 USER REPOSITORY - Demonstração Completa de Boas e Más Práticas
 * 
 * Esta interface demonstra todas as técnicas essenciais de repositório:
 * ✅ EntityGraphs para resolução elegante de problemas N+1
 * ✅ JOIN FETCH para optimização explícita de consultas
 * ✅ Projecções DTO para máxima eficiência de dados
 * ✅ Paginação obrigatória para grandes volumes
 * ✅ Consultas nativas quando JPQL não é suficiente
 * ✅ Query Methods automáticos para casos simples
 * ❌ Exemplos comentados de práticas perigosas
 */

// ✅ BOA PRÁTICA: @Repository indica que esta interface é um repositório Spring
// Habilita tradução automática de exceções JPA para DataAccessException
@Repository

// ✅ BOA PRÁTICA: Herda de JpaRepository<Entity, ID>
// Fornece métodos CRUD prontos: save(), findById(), findAll(), delete(), etc.
// Generic: User = tipo da entidade, Long = tipo da chave primária
public interface UserRepository extends JpaRepository<User, Long> {

    /*
     * 🎓 MÉTODOS OTIMIZADOS - BOAS PRÁTICAS
     */

    // ✅ BOA PRÁTICA: @EntityGraph resolve problema N+1
    // EntityGraphType.FETCH: carrega department junto com user numa única query
    // Usa o NamedEntityGraph "User.withDepartment" definido na entidade User
    // SEM isto: 2 queries (1 para user, 1 para department)
    // COM isto: 1 query com JOIN
    @EntityGraph(value = "User.withDepartment", type = EntityGraph.EntityGraphType.FETCH)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithDepartment(@Param("id") Long id);

    // ✅ BOA PRÁTICA: JOIN FETCH explícito na JPQL
    // Alternativa ao EntityGraph, mais explícita
    // Garante que department é carregado numa única query
    @Query("SELECT u FROM User u JOIN FETCH u.department d WHERE u.email = :email")
    Optional<User> findByEmailWithDepartment(@Param("email") String email);

    // ✅ BOA PRÁTICA: Projeção DTO - MÁXIMA EFICIÊNCIA
    // Carrega APENAS os dados necessários (não entidades completas)
    // Mais rápido que carregar entidades quando só precisamos de alguns campos
    // LEFT JOIN: inclui users sem department (department pode ser null)
    @Query("SELECT new com.formation.hibernate.dto.UserSummaryDto(u.id, u.name, u.email, u.createdAt, d.name) " +
           "FROM User u LEFT JOIN u.department d")
    List<com.formation.hibernate.dto.UserSummaryDto> findAllUserSummaries();

    // ✅ BOA PRÁTICA: Paginação + EntityGraph
    // SEMPRE usar paginação para grandes volumes de dados
    // EntityGraph garante que department vem junto, evitando N+1
    // Pageable controla page, size, sorting
    @EntityGraph(value = "User.withDepartment", type = EntityGraph.EntityGraphType.FETCH)
    Page<User> findAll(Pageable pageable);

    // ✅ BOA PRÁTICA: Consulta customizada com JOIN FETCH
    // Para casos específicos onde Query Method não é suficiente
    // JOIN FETCH garante que department vem na mesma query
    @Query("SELECT u FROM User u JOIN FETCH u.department d WHERE d.name = :departmentName")
    List<User> findByDepartmentNameOptimized(@Param("departmentName") String departmentName);

    /*
     * 🚨 MÁS PRÁTICAS - EXEMPLOS DO QUE NÃO FAZER!
     */

    // ❌ MÁ PRÁTICA: Query Method sem otimização
    // PROBLEMA: Vai fazer findByDepartmentName(), depois para cada user vai buscar o department
    // RESULTADO: N+1 problem - se retornar 100 users, executa 101 queries!
    // SOLUÇÃO: Use findByDepartmentNameOptimized() acima
    // List<User> findByDepartmentName(String departmentName);

    // ❌ MÁ PRÁTICA: Carregar todos os dados sem paginação
    // PROBLEMA: Com milhares de users, vai carregar TUDO na memória
    // RESULTADO: OutOfMemoryError + performance terrível + timeout
    // SOLUÇÃO: SEMPRE use Page<User> findAll(Pageable)
    // @Query("SELECT u FROM User u")
    // List<User> findAllWithoutPagination();

    // ❌ MÁ PRÁTICA: Query sem índices
    // PROBLEMA: Se department.budget não tiver índice, vai fazer table scan
    // RESULTADO: Performance degradada exponencialmente com dados
    // @Query("SELECT u FROM User u WHERE u.department.budget > :minBudget")
    // List<User> findUsersWithBudgetAboveSlowVersion(@Param("minBudget") Double minBudget);

    /*
     * 🎓 TÉCNICAS AVANÇADAS
     */

    // ✅ BOA PRÁTICA: Query nativa quando JPQL não é suficiente
    // nativeQuery = true: usa SQL direto da base de dados
    // Útil para queries complexas ou otimizações específicas do BD
    // INNER JOIN: mais eficiente que LEFT JOIN quando sabemos que relation existe
    @Query(value = "SELECT u.* FROM users u INNER JOIN departments d ON u.department_id = d.id WHERE d.budget > :minBudget",
           nativeQuery = true)
    List<User> findUsersInDepartmentsWithBudgetAbove(@Param("minBudget") Double minBudget);

    // ✅ BOA PRÁTICA: COUNT eficiente
    // Só conta registros, não carrega dados na memória
    // Muito mais rápido que fazer findAll().size()
    @Query("SELECT COUNT(u) FROM User u WHERE u.department.id = :departmentId")
    Long countByDepartmentId(@Param("departmentId") Long departmentId);

    /*
     * 🎓 QUERY METHODS - Spring Data JPA automático
     * Spring gera implementação automaticamente baseado no nome do método
     */

    // ✅ BOA PRÁTICA: Query Method com paginação
    // ContainingIgnoreCase = LIKE '%name%' case-insensitive
    // Spring gera automaticamente: WHERE LOWER(u.name) LIKE LOWER('%?%')
    Page<User> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // ✅ BOA PRÁTICA: Query Method simples com paginação
    // Spring gera automaticamente: WHERE u.department.id = ?
    // Usa o índice idx_user_department definido na entidade
    Page<User> findByDepartmentId(Long departmentId, Pageable pageable);

    /*
     * 🎓 MÉTODOS ESPECÍFICOS PARA DEMONSTRAÇÃO DO PROBLEMA N+1
     */

    // ✅ BOA PRÁTICA: JOIN FETCH explícito para resolver N+1
    // Alternativa ao EntityGraph usando JPQL directo
    // Carrega User + Department numa única query
    @Query("SELECT u FROM User u JOIN FETCH u.department d WHERE u.id = :id")
    Optional<User> findByIdWithDepartmentJoinFetch(@Param("id") Long id);

    // ✅ BOA PRÁTICA: EntityGraph para carregar todos os users com departments
    // Resolve problema N+1 em operações de listagem
    // Sem isto: 1 query para users + N queries para departments
    // Com isto: 1 query apenas com JOIN
    @EntityGraph(value = "User.withDepartment", type = EntityGraph.EntityGraphType.FETCH)
    @Query("SELECT u FROM User u")
    List<User> findAllWithDepartment();
}