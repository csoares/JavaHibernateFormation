package com.formation.hibernate.repository;

import com.formation.hibernate.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * DepartmentRepository - Repository Patterns Implementation
 * 
 * This interface demonstrates various Spring Data JPA repository techniques:
 * ✅ Automatic Query Methods based on method name
 * ✅ Custom JPQL queries with @Query
 * ✅ JOIN FETCH to solve N+1 problems
 * ✅ DTO projections for maximum efficiency
 * ✅ Aggregate queries with GROUP BY
 * ✅ Appropriate use of @Repository and generics
 */

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Optional<Department> findByName(String name);

    @Query("SELECT d FROM Department d JOIN FETCH d.users WHERE d.id = :id")
    Optional<Department> findByIdWithUsers(@Param("id") Long id);

    @Query("SELECT new com.formation.hibernate.dto.DepartmentDto(d.id, d.name, d.description, d.budget) " +
           "FROM Department d")
    List<com.formation.hibernate.dto.DepartmentDto> findAllDepartmentSummaries();

    @Query("SELECT d.name, COUNT(u) FROM Department d LEFT JOIN d.users u GROUP BY d.id, d.name")
    List<Object[]> getDepartmentUserCounts();

    List<Department> findByBudgetGreaterThan(Double budget);
}