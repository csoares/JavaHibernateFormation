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
 * UserRepository - Complete Repository Best Practices Implementation
 * 
 * This interface demonstrates all essential repository techniques:
 * ✅ EntityGraphs for elegant N+1 problem resolution
 * ✅ JOIN FETCH for explicit query optimization
 * ✅ DTO projections for maximum data efficiency
 * ✅ Mandatory pagination for large volumes
 * ✅ Native queries when JPQL is not sufficient
 * ✅ Automatic Query Methods for simple cases
 */

@Repository
public interface UserRepository extends JpaRepository<User, Long> {


    @EntityGraph(value = "User.withDepartment", type = EntityGraph.EntityGraphType.FETCH)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithDepartment(@Param("id") Long id);

    @Query("SELECT u FROM User u JOIN FETCH u.department d WHERE u.email = :email")
    Optional<User> findByEmailWithDepartment(@Param("email") String email);

    @Query("SELECT new com.formation.hibernate.dto.UserSummaryDto(u.id, u.name, u.email, u.createdAt, d.name) " +
           "FROM User u LEFT JOIN u.department d")
    List<com.formation.hibernate.dto.UserSummaryDto> findAllUserSummaries();

    @EntityGraph(value = "User.withDepartment", type = EntityGraph.EntityGraphType.FETCH)
    Page<User> findAll(Pageable pageable);

    @Query("SELECT u FROM User u JOIN FETCH u.department d WHERE d.name = :departmentName")
    List<User> findByDepartmentNameOptimized(@Param("departmentName") String departmentName);



    @Query(value = "SELECT u.* FROM users u INNER JOIN departments d ON u.department_id = d.id WHERE d.budget > :minBudget",
           nativeQuery = true)
    List<User> findUsersInDepartmentsWithBudgetAbove(@Param("minBudget") Double minBudget);

    @Query("SELECT COUNT(u) FROM User u WHERE u.department.id = :departmentId")
    Long countByDepartmentId(@Param("departmentId") Long departmentId);


    Page<User> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<User> findByDepartmentId(Long departmentId, Pageable pageable);
}