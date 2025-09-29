package com.formation.hibernate.repository;

import com.formation.hibernate.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 🎓 DEPARTMENT REPOSITORY - Simplificado para Demonstração N+1
 * 
 * Repositório mínimo focado apenas no problema N+1:
 * ✅ Apenas métodos básicos do JpaRepository
 * ✅ Sem complexidades desnecessárias
 * 
 * 🎯 FOCO: Suporte básico para entidade Department
 */

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    // Métodos básicos herdados do JpaRepository são suficientes
    // para demonstração do problema N+1
}