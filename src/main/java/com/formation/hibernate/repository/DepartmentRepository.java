package com.formation.hibernate.repository;

import com.formation.hibernate.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    // BOM: Consulta otimizada por nome
    Optional<Department> findByName(String name);

    // BOM: Consulta com join fetch para evitar N+1
    @Query("SELECT d FROM Department d JOIN FETCH d.users WHERE d.id = :id")
    Optional<Department> findByIdWithUsers(@Param("id") Long id);

    // BOM: Consulta com projeção
    @Query("SELECT new com.formation.hibernate.dto.DepartmentDto(d.id, d.name, d.description, d.budget) " +
           "FROM Department d")
    List<com.formation.hibernate.dto.DepartmentDto> findAllDepartmentSummaries();

    // BOM: Consulta agregada
    @Query("SELECT d.name, COUNT(u) FROM Department d LEFT JOIN d.users u GROUP BY d.id, d.name")
    List<Object[]> getDepartmentUserCounts();

    // BOM: Consulta por orçamento
    List<Department> findByBudgetGreaterThan(Double budget);
}