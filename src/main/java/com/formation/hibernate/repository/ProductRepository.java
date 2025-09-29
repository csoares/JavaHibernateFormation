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
 * ProductRepository - Advanced Repository Techniques Implementation
 * 
 * This interface demonstrates sophisticated techniques for product management:
 * ✅ Careful BLOB management (avoiding unnecessary loading)
 * ✅ Multiple query strategies (JOIN FETCH, projections, etc.)
 * ✅ Efficient pagination for large data volumes
 * ✅ Aggregate queries for reports and statistics
 * ✅ Query Methods for common searches
 */

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.id = :id")
    Optional<Product> findByIdWithCategory(@Param("id") Long id);

    @Query("SELECT p.id, p.name, p.description, p.price, p.stockQuantity FROM Product p WHERE p.category.name = :categoryName")
    List<Object[]> findProductsWithoutImageByCategory(@Param("categoryName") String categoryName);

    @Query("SELECT new com.formation.hibernate.dto.ProductSummaryDto(p.id, p.name, p.price, c.name) " +
           "FROM Product p JOIN p.category c")
    List<com.formation.hibernate.dto.ProductSummaryDto> findAllProductSummaries();

    @Query("SELECT p FROM Product p JOIN FETCH p.category")
    Page<Product> findAllWithCategory(Pageable pageable);

    List<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    List<Product> findByNameContainingIgnoreCase(String name);



    @Query("SELECT c.name, COUNT(p), AVG(p.price) FROM Product p JOIN p.category c GROUP BY c.id, c.name")
    List<Object[]> getProductStatisticsByCategory();


    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    Page<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);


    // Query Method para produtos em falta
    // List<Product> findByStockQuantityLessThan(Integer minStock);

    // Query Method para produtos disponíveis
    // List<Product> findByStockQuantityGreaterThan(Integer minStock);

    // Consulta para produtos mais vendidos
    // @Query("SELECT p, SUM(oi.quantity) as totalSold FROM Product p " +
    //        "JOIN p.orderItems oi GROUP BY p ORDER BY totalSold DESC")
    // List<Object[]> findBestSellingProducts();
}