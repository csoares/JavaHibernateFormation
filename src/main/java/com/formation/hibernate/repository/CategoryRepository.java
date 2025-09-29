package com.formation.hibernate.repository;

import com.formation.hibernate.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // BOM: Consulta otimizada por nome
    Optional<Category> findByName(String name);

    // BOM: Consulta com join fetch para evitar N+1
    @Query("SELECT c FROM Category c JOIN FETCH c.products WHERE c.id = :id")
    Optional<Category> findByIdWithProducts(@Param("id") Long id);

    // BOM: Consulta com projeção
    @Query("SELECT new com.formation.hibernate.dto.CategoryDto(c.id, c.name, c.description) " +
           "FROM Category c ORDER BY c.name")
    List<com.formation.hibernate.dto.CategoryDto> findAllCategorySummaries();
}