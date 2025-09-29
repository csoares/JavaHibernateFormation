package com.formation.hibernate.repository;

import com.formation.hibernate.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 🎓 PRODUCT REPOSITORY - Demonstração Avançada de Técnicas de Repositório
 * 
 * Esta interface demonstra técnicas sofisticadas para gestão de produtos:
 * ✅ Gestão cuidadosa de BLOBs (evitar carregamento desnecessário)
 * ✅ Múltiplas estratégias de consulta (JOIN FETCH, projecções, etc.)
 * ✅ Paginação eficiente para grandes volumes de dados
 * ✅ Consultas agregadas para relatórios e estatísticas
 * ✅ Query Methods para pesquisas comuns
 * ❌ Exemplos comentados de práticas perigosas com BLOBs
 */

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // ✅ BOA PRÁTICA: JOIN FETCH para carregar categoria junto
    // Evita lazy loading posterior quando aceder a product.getCategory()
    // Usa os índices idx_product_category para eficiência
    @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.id = :id")
    Optional<Product> findByIdWithCategory(@Param("id") Long id);

    // ✅ BOA PRÁTICA: Consulta específica SEM BLOBs
    // CRÍTICO: Não selecciona imageData (campo BLOB)
    // Ideal para listagens onde não precisamos das imagens
    // Muito mais eficiente que carregar entidades completas
    @Query("SELECT p.id, p.name, p.description, p.price, p.stockQuantity FROM Product p WHERE p.category.name = :categoryName")
    List<Object[]> findProductsWithoutImageByCategory(@Param("categoryName") String categoryName);

    // ✅ BOA PRÁTICA: Projecção DTO com JOIN
    // Carrega dados de Product + Category numa única consulta
    // Ideal para listagens de produtos com nome da categoria
    @Query("SELECT new com.formation.hibernate.dto.ProductSummaryDto(p.id, p.name, p.price, c.name) " +
           "FROM Product p JOIN p.category c")
    List<com.formation.hibernate.dto.ProductSummaryDto> findAllProductSummaries();

    // ✅ BOA PRÁTICA: Paginação com JOIN FETCH
    // Combina paginação eficiente com carregamento de relacionamentos
    // Essencial para grandes catálogos de produtos
    @Query("SELECT p FROM Product p JOIN FETCH p.category")
    Page<Product> findAllWithCategory(Pageable pageable);

    // ✅ BOA PRÁTICA: Query Method para faixa de preços
    // Spring gera: WHERE price BETWEEN ? AND ?
    // Usa o índice idx_product_price para eficiência
    List<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    // ✅ BOA PRÁTICA: Pesquisa textual com índice
    // ContainingIgnoreCase gera: WHERE LOWER(name) LIKE LOWER('%?%')
    // Usa o índice idx_product_name para boas performances
    List<Product> findByNameContainingIgnoreCase(String name);

    /*
     * 🚨 MÁS PRÁTICAS - EXEMPLOS PERIGOSOS COM BLOBs!
     */
    
    // ❌ MÁ PRÁTICA: Consulta que carrega entidades completas (incluindo BLOBs)
    // PROBLEMA: Carrega imageData (BLOB) para todos os produtos da categoria
    // RESULTADO: Se categoria tem 100 produtos, carrega 100 imagens (potencialmente 500MB+)
    // IMPACTO: OutOfMemoryError garantido + consultas gigantescas
    // @Query("SELECT p FROM Product p WHERE p.category.name = :categoryName")
    // List<Product> findProductsWithImageByCategory(@Param("categoryName") String categoryName);

    // ❌ MÁ PRÁTICA: findAll() sem paginação
    // PROBLEMA: Carrega TODOS os produtos + TODAS as imagens na memória
    // RESULTADO: Crash da aplicação com catálogos grandes
    // NUNCA usar: List<Product> findAll();

    /*
     * 🎓 CONSULTAS AGREGADAS E RELATÓRIOS
     */

    // ✅ BOA PRÁTICA: Consulta agregada para estatísticas de categoria
    // Calcula número de produtos e preço médio por categoria
    // GROUP BY necessário para funções de agregação (COUNT, AVG)
    @Query("SELECT c.name, COUNT(p), AVG(p.price) FROM Product p JOIN p.category c GROUP BY c.id, c.name")
    List<Object[]> getProductStatisticsByCategory();

    /*
     * 🎓 MÉTODOS DE PAGINAÇÃO - Essenciais para Performance
     * Versões paginadas dos métodos anteriores para grandes volumes
     */

    // ✅ BOA PRÁTICA: Pesquisa textual com paginação
    // Combina pesquisa LIKE com paginação eficiente
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // ✅ BOA PRÁTICA: Filtro por categoria com paginação
    // Usa índice idx_product_category + paginação
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    // ✅ BOA PRÁTICA: Filtro por preço com paginação
    // Usa índice idx_product_price + paginação
    Page<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    /*
     * 🎓 MÉTODOS ESPECIAIS PARA GESTÃO DE STOCK
     */

    // Query Method para produtos em falta
    // List<Product> findByStockQuantityLessThan(Integer minStock);

    // Query Method para produtos disponíveis
    // List<Product> findByStockQuantityGreaterThan(Integer minStock);

    // Consulta para produtos mais vendidos
    // @Query("SELECT p, SUM(oi.quantity) as totalSold FROM Product p " +
    //        "JOIN p.orderItems oi GROUP BY p ORDER BY totalSold DESC")
    // List<Object[]> findBestSellingProducts();
}