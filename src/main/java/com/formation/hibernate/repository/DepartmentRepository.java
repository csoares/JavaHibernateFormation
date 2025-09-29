package com.formation.hibernate.repository;

import com.formation.hibernate.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 🎓 DEPARTMENT REPOSITORY - Demonstração de Padrões de Repositório
 * 
 * Esta interface demonstra várias técnicas de repositório Spring Data JPA:
 * ✅ Query Methods automáticos baseados no nome do método
 * ✅ Consultas JPQL customizadas com @Query
 * ✅ JOIN FETCH para resolver problemas N+1
 * ✅ Projecções DTO para eficiência máxima
 * ✅ Consultas agregadas com GROUP BY
 * ✅ Uso apropriado de @Repository e genéricos
 */

// ✅ BOA PRÁTICA: @Repository marca interface como repositório Spring
// Habilita tradução automática de excepções JPA para DataAccessException
@Repository

// ✅ BOA PRÁTICA: Extends JpaRepository<Entidade, TipoID>
// Fornece métodos CRUD prontos: save(), findById(), findAll(), delete(), etc.
// Genéricos: Department = tipo da entidade, Long = tipo da chave primária
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    // ✅ BOA PRÁTICA: Query Method automático
    // Spring gera automaticamente: SELECT * FROM departments WHERE name = ?
    // Usa o índice idx_dept_name definido na entidade Department
    Optional<Department> findByName(String name);

    // ✅ BOA PRÁTICA: Consulta JPQL customizada com JOIN FETCH
    // Carrega Department + todos os seus Users numa única consulta
    // Evita problema N+1: sem isto seriam 1 consulta para dept + N para users
    @Query("SELECT d FROM Department d JOIN FETCH d.users WHERE d.id = :id")
    Optional<Department> findByIdWithUsers(@Param("id") Long id);

    // ✅ BOA PRÁTICA: Projecção DTO - MÁXIMA EFICIÊNCIA
    // SELECT new ...Dto() cria DTOs directamente na consulta
    // Mais eficiente que carregar entidades completas e depois converter
    // Só carrega os campos necessários
    @Query("SELECT new com.formation.hibernate.dto.DepartmentDto(d.id, d.name, d.description, d.budget) " +
           "FROM Department d")
    List<com.formation.hibernate.dto.DepartmentDto> findAllDepartmentSummaries();

    // ✅ BOA PRÁTICA: Consulta agregada para relatórios
    // LEFT JOIN garante que departamentos sem utilizadores aparecem (com COUNT = 0)
    // GROUP BY necessário quando usamos funções de agregação como COUNT
    @Query("SELECT d.name, COUNT(u) FROM Department d LEFT JOIN d.users u GROUP BY d.id, d.name")
    List<Object[]> getDepartmentUserCounts();

    // ✅ BOA PRÁTICA: Query Method com condição comparativa
    // Spring gera: SELECT * FROM departments WHERE budget > ?
    // GreaterThan, LessThan, Between, etc. são palavras-chave reconhecidas
    List<Department> findByBudgetGreaterThan(Double budget);
}