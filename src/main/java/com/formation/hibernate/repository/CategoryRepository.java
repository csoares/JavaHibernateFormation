package com.formation.hibernate.repository;

import com.formation.hibernate.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * CategoryRepository - Simple and Effective Patterns Implementation
 * 
 * Repository interface that demonstrates:
 * ✅ Clean and focused repository (single responsibility)
 * ✅ Query Methods for simple queries
 * ✅ JOIN FETCH to load relationships
 * ✅ Ordered DTO projections
 * ✅ Well-named and self-explanatory methods
 */

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);

    @Query("SELECT c FROM Category c JOIN FETCH c.products WHERE c.id = :id")
    Optional<Category> findByIdWithProducts(@Param("id") Long id);

    @Query("SELECT new com.formation.hibernate.dto.CategoryDto(c.id, c.name, c.description) " +
           "FROM Category c ORDER BY c.name")
    List<com.formation.hibernate.dto.CategoryDto> findAllCategorySummaries();


    // Query Method para pesquisa case-insensitive
    // Optional<Category> findByNameIgnoreCase(String name);

    // Consulta para categorias com produtos acima de determinado preço
    // @Query("SELECT DISTINCT c FROM Category c JOIN c.products p WHERE p.price > :minPrice")
    // List<Category> findCategoriesWithProductsAbovePrice(@Param("minPrice") BigDecimal minPrice);

    // Contagem de produtos por categoria
    // @Query("SELECT c.name, COUNT(p) FROM Category c LEFT JOIN c.products p GROUP BY c.id, c.name")
    // List<Object[]> getCategoryProductCounts();
}