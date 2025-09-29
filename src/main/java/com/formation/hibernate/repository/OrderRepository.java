package com.formation.hibernate.repository;

import com.formation.hibernate.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * OrderRepository - Advanced Repository Techniques Implementation
 * 
 * This interface demonstrates the most sophisticated repository techniques:
 * ✅ Nested EntityGraphs for complex relationships
 * ✅ Careful BLOB management (avoiding unnecessary PDFs)
 * ✅ Aggregate queries for financial reports
 * ✅ Multiple optimization strategies (JOIN FETCH, projections, etc.)
 * ✅ Filters with pagination for large volumes
 * ✅ Date range queries using indexes
 */

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {


    @EntityGraph(value = "Order.withUserAndDepartment", type = EntityGraph.EntityGraphType.FETCH)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdWithUserAndDepartment(@Param("id") Long id);

    @Query("SELECT o FROM Order o " +
           "JOIN FETCH o.user u " +
           "JOIN FETCH u.department d " +
           "WHERE o.orderNumber = :orderNumber")
    Optional<Order> findByOrderNumberWithDetails(@Param("orderNumber") String orderNumber);


    @Query("SELECT new com.formation.hibernate.dto.OrderSummaryDto(o.id, o.orderNumber, o.orderDate, o.totalAmount, o.status) " +
           "FROM Order o WHERE o.user.id = :userId ORDER BY o.orderDate DESC")
    List<com.formation.hibernate.dto.OrderSummaryDto> findOrderSummariesByUserId(@Param("userId") Long userId);


    @Query("SELECT o FROM Order o JOIN FETCH o.user u WHERE o.status = :status")
    Page<Order> findByStatusWithUser(@Param("status") Order.OrderStatus status, Pageable pageable);


    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.user.department.id = :departmentId AND o.orderDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalAmountByDepartmentAndDateRange(
        @Param("departmentId") Long departmentId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);


    @Query("SELECT o.id, o.orderNumber, o.orderDate, o.totalAmount, o.status FROM Order o WHERE o.totalAmount > :minAmount")
    List<Object[]> findOrdersWithoutBlobData(@Param("minAmount") BigDecimal minAmount);



    @Query("SELECT o FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate ORDER BY o.orderDate DESC")
    List<Order> findOrdersByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);


    @Query("SELECT o.status, COUNT(o), AVG(o.totalAmount) FROM Order o GROUP BY o.status")
    List<Object[]> getOrderStatistics();


    Page<Order> findByUserId(Long userId, Pageable pageable);

    Page<Order> findByStatus(String status, Pageable pageable);


    // Pedidos por departamento (útil para relatórios departamentais)
    // @Query("SELECT o FROM Order o WHERE o.user.department.id = :departmentId")
    // Page<Order> findByUserDepartmentId(@Param("departmentId") Long departmentId, Pageable pageable);

    // Pedidos recentes (últimos 30 dias)
    // @Query("SELECT o FROM Order o WHERE o.orderDate >= :since ORDER BY o.orderDate DESC")
    // List<Order> findRecentOrders(@Param("since") LocalDateTime since);

    // Top clientes por valor total de pedidos
    // @Query("SELECT o.user, SUM(o.totalAmount) FROM Order o GROUP BY o.user ORDER BY SUM(o.totalAmount) DESC")
    // List<Object[]> findTopCustomersByTotalAmount();
}