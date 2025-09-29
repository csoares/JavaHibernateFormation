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

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // BOM: Consulta com EntityGraph para evitar N+1 problem
    @EntityGraph(value = "User.withDepartment", type = EntityGraph.EntityGraphType.FETCH)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithDepartment(@Param("id") Long id);

    // BOM: Consulta otimizada com join fetch
    @Query("SELECT u FROM User u JOIN FETCH u.department d WHERE u.email = :email")
    Optional<User> findByEmailWithDepartment(@Param("email") String email);

    // BOM: Consulta com projeção para dados específicos
    @Query("SELECT new com.formation.hibernate.dto.UserSummaryDto(u.id, u.name, u.email, u.createdAt, d.name) " +
           "FROM User u LEFT JOIN u.department d")
    List<com.formation.hibernate.dto.UserSummaryDto> findAllUserSummaries();

    // BOM: Paginação eficiente
    @EntityGraph(value = "User.withDepartment", type = EntityGraph.EntityGraphType.FETCH)
    Page<User> findAll(Pageable pageable);

    // BOM: Consulta por departamento com otimização
    @Query("SELECT u FROM User u JOIN FETCH u.department d WHERE d.name = :departmentName")
    List<User> findByDepartmentNameOptimized(@Param("departmentName") String departmentName);

    // MÁ PRÁTICA: Comentado - Consulta sem otimização que causa N+1
    // List<User> findByDepartmentName(String departmentName);

    // MÁ PRÁTICA: Comentado - Buscar todos os dados sem paginação
    // @Query("SELECT u FROM User u")
    // List<User> findAllWithoutPagination();

    // BOM: Consulta com transação read-only para otimização
    @Query(value = "SELECT u.* FROM users u INNER JOIN departments d ON u.department_id = d.id WHERE d.budget > :minBudget",
           nativeQuery = true)
    List<User> findUsersInDepartmentsWithBudgetAbove(@Param("minBudget") Double minBudget);

    // BOM: Contagem eficiente
    @Query("SELECT COUNT(u) FROM User u WHERE u.department.id = :departmentId")
    Long countByDepartmentId(@Param("departmentId") Long departmentId);

    // Performance testing methods
    Page<User> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Page<User> findByDepartmentId(Long departmentId, Pageable pageable);
}