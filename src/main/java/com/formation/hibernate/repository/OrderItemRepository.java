package com.formation.hibernate.repository;

import com.formation.hibernate.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * OrderItemRepository - Junction Entity Repository Implementation
 * 
 * This interface demonstrates techniques for complex junction entities:
 * ✅ Query Methods for optimized @ManyToOne relationships
 * ✅ Multiple JOIN FETCH to load several relations in one query
 * ✅ Efficient count queries without loading data
 * ✅ Specific optimizations for junction entities
 * ✅ Aggregate queries for sales reports
 * ✅ Methods for purchase behavior analysis
 */

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    @Query("SELECT oi FROM OrderItem oi " +
           "JOIN FETCH oi.product p " +
           "JOIN FETCH oi.order o " +
           "WHERE o.id = :orderId")
    List<OrderItem> findByOrderIdWithProduct(@Param("orderId") Long orderId);

    long countByOrderId(Long orderId);

    List<OrderItem> findByProductId(Long productId);


    List<OrderItem> findByOrderIdAndProductId(Long orderId, Long productId);

    @Query("SELECT SUM(oi.totalPrice) FROM OrderItem oi WHERE oi.order.id = :orderId")
    Double calculateOrderTotal(@Param("orderId") Long orderId);

    @Query("SELECT oi.product.id, oi.product.name, SUM(oi.quantity) as totalSold " +
           "FROM OrderItem oi " +
           "GROUP BY oi.product.id, oi.product.name " +
           "ORDER BY totalSold DESC")
    List<Object[]> findTopSellingProducts();

}