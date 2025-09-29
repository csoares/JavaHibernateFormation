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

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // BOM: Consulta com join fetch
    @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.id = :id")
    Optional<Product> findByIdWithCategory(@Param("id") Long id);

    // BOM: Consulta sem carregar blob desnecessariamente
    @Query("SELECT p.id, p.name, p.description, p.price, p.stockQuantity FROM Product p WHERE p.category.name = :categoryName")
    List<Object[]> findProductsWithoutImageByCategory(@Param("categoryName") String categoryName);

    // BOM: Consulta com projeção
    @Query("SELECT new com.formation.hibernate.dto.ProductSummaryDto(p.id, p.name, p.price, c.name) " +
           "FROM Product p JOIN p.category c")
    List<com.formation.hibernate.dto.ProductSummaryDto> findAllProductSummaries();

    // BOM: Paginação eficiente
    @Query("SELECT p FROM Product p JOIN FETCH p.category")
    Page<Product> findAllWithCategory(Pageable pageable);

    // BOM: Consulta por faixa de preço
    List<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    // BOM: Pesquisa por nome com índice
    List<Product> findByNameContainingIgnoreCase(String name);

    // MÁ PRÁTICA: Comentado - Consulta que força carregamento do blob
    // @Query("SELECT p FROM Product p WHERE p.category.name = :categoryName")
    // List<Product> findProductsWithImageByCategory(@Param("categoryName") String categoryName);

    // BOM: Consulta agregada para estatísticas
    @Query("SELECT c.name, COUNT(p), AVG(p.price) FROM Product p JOIN p.category c GROUP BY c.id, c.name")
    List<Object[]> getProductStatisticsByCategory();

    // Performance testing methods
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);
    Page<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
}