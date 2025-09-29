package com.formation.hibernate.repository;

import com.formation.hibernate.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 🎓 USER REPOSITORY - Simplificado para Demonstração N+1
 * 
 * Repositório focado APENAS no problema N+1:
 * ✅ Métodos básicos (problemáticos)
 * ✅ Métodos optimizados (soluções)
 * ✅ Comparação directa entre abordagens
 * 
 * 🎯 FOCO: Demonstrar problema N+1 e suas soluções
 */

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /*
     * ❌ MÉTODOS PROBLEMÁTICOS (causam N+1)
     */

    // Estes métodos são herdados do JpaRepository e causam N+1:
    // findById(Long id) - carrega User sem Department
    // findAll() - carrega todos os Users sem Departments

    /*
     * ✅ SOLUÇÕES PARA O PROBLEMA N+1
     */

    // ✅ SOLUÇÃO 1: EntityGraph
    // Carrega User + Department numa única query
    @EntityGraph(value = "User.withDepartment", type = EntityGraph.EntityGraphType.FETCH)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithDepartment(@Param("id") Long id);

    // ✅ SOLUÇÃO 2: JOIN FETCH explícito
    // Alternativa ao EntityGraph usando JPQL directo
    @Query("SELECT u FROM User u JOIN FETCH u.department d WHERE u.id = :id")
    Optional<User> findByIdWithDepartmentJoinFetch(@Param("id") Long id);

    // ✅ SOLUÇÃO 3: EntityGraph para listagem
    // Resolve N+1 em operações findAll()
    @EntityGraph(value = "User.withDepartment", type = EntityGraph.EntityGraphType.FETCH)
    @Query("SELECT u FROM User u")
    List<User> findAllWithDepartment();

    // ✅ SOLUÇÃO 4: JOIN FETCH para listagem
    // Versão JPQL da solução anterior
    @Query("SELECT u FROM User u JOIN FETCH u.department")
    List<User> findAllWithDepartmentJoinFetch();
}