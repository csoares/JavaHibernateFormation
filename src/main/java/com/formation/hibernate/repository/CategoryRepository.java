package com.formation.hibernate.repository;

import com.formation.hibernate.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 🎓 CATEGORY REPOSITORY - Padrões Simples e Eficazes
 * 
 * Interface de repositório que demonstra:
 * ✅ Repositório clean e focado (single responsibility)
 * ✅ Query Methods para consultas simples
 * ✅ JOIN FETCH para carregar relacionamentos
 * ✅ Projecções DTO ordenadas
 * ✅ Métodos bem nomeados e auto-explicativos
 */

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // ✅ BOA PRÁTICA: Query Method para pesquisa por nome único
    // Spring gera: SELECT * FROM categories WHERE name = ?
    // Usa automaticamente o índice idx_category_name da entidade
    // Retorna Optional para evitar NullPointerException
    Optional<Category> findByName(String name);

    // ✅ BOA PRÁTICA: Consulta optimizada com JOIN FETCH
    // Carrega Category + todos os Products numa única consulta
    // Útil quando sabemos que vamos aceder aos produtos da categoria
    // Evita lazy loading posterior (problema N+1)
    @Query("SELECT c FROM Category c JOIN FETCH c.products WHERE c.id = :id")
    Optional<Category> findByIdWithProducts(@Param("id") Long id);

    // ✅ BOA PRÁTICA: Projecção DTO com ordenação
    // Carrega apenas os campos necessários (não products)
    // ORDER BY c.name: retorna categorias ordenadas alfabeticamente
    // Ideal para dropdowns, listagens simples, etc.
    @Query("SELECT new com.formation.hibernate.dto.CategoryDto(c.id, c.name, c.description) " +
           "FROM Category c ORDER BY c.name")
    List<com.formation.hibernate.dto.CategoryDto> findAllCategorySummaries();

    /*
     * 🎓 MÉTODOS ADICIONAIS ÚTEIS (podem ser implementados conforme necessário)
     */

    // Query Method para pesquisa case-insensitive
    // Optional<Category> findByNameIgnoreCase(String name);

    // Consulta para categorias com produtos acima de determinado preço
    // @Query("SELECT DISTINCT c FROM Category c JOIN c.products p WHERE p.price > :minPrice")
    // List<Category> findCategoriesWithProductsAbovePrice(@Param("minPrice") BigDecimal minPrice);

    // Contagem de produtos por categoria
    // @Query("SELECT c.name, COUNT(p) FROM Category c LEFT JOIN c.products p GROUP BY c.id, c.name")
    // List<Object[]> getCategoryProductCounts();
}
}