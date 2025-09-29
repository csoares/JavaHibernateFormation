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
 * 🎓 ORDER REPOSITORY - Demonstração Completa de Técnicas Avançadas
 * 
 * Esta interface demonstra as técnicas mais sofisticadas de repositório:
 * ✅ EntityGraphs aninhados para relacionamentos complexos
 * ✅ Gestão cuidadosa de BLOBs (evitar PDFs desnecessários)
 * ✅ Consultas agregadas para relatórios financeiros
 * ✅ Múltiplas estratégias de optimização (JOIN FETCH, projecções, etc.)
 * ✅ Filtros com paginação para grandes volumes
 * ✅ Consultas por intervalos de datas usando índices
 * ❌ Exemplos perigosos com BLOBs comentados
 */

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /*
     * 🎓 TÉCNICAS DE ENTITYGRAPH - Resolução Sofisticada de N+1
     */

    // ✅ BOA PRÁTICA: EntityGraph aninhado (subgraph)
    // Usa o NamedEntityGraph "Order.withUserAndDepartment" da entidade Order
    // Carrega Order → User → Department numa única consulta complexa
    // Evita 3 consultas separadas (1 order + 1 user + 1 department)
    @EntityGraph(value = "Order.withUserAndDepartment", type = EntityGraph.EntityGraphType.FETCH)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdWithUserAndDepartment(@Param("id") Long id);

    // ✅ BOA PRÁTICA: JOIN FETCH múltiplo explícito
    // Alternativa ao EntityGraph, mais explícita
    // Carrega Order + User + Department numa única consulta JPQL
    @Query("SELECT o FROM Order o " +
           "JOIN FETCH o.user u " +
           "JOIN FETCH u.department d " +
           "WHERE o.orderNumber = :orderNumber")
    Optional<Order> findByOrderNumberWithDetails(@Param("orderNumber") String orderNumber);

    /*
     * 🎓 PROJECÇÕES DTO - Eficiência Máxima
     */

    // ✅ BOA PRÁTICA: Projecção DTO para listagens
    // SELECT new ...Dto() cria DTOs directamente na consulta
    // Não carrega invoicePdf (BLOB) nem relacionamentos desnecessários
    // ORDER BY otimizado usando índice idx_order_date
    @Query("SELECT new com.formation.hibernate.dto.OrderSummaryDto(o.id, o.orderNumber, o.orderDate, o.totalAmount, o.status) " +
           "FROM Order o WHERE o.user.id = :userId ORDER BY o.orderDate DESC")
    List<com.formation.hibernate.dto.OrderSummaryDto> findOrderSummariesByUserId(@Param("userId") Long userId);

    /*
     * 🎓 PAGINAÇÃO COM FILTROS
     */

    // ✅ BOA PRÁTICA: Filtro por status + paginação + JOIN FETCH
    // Combina filtro eficiente (usa índice idx_order_status) com paginação
    // JOIN FETCH carrega user junto para evitar lazy loading posterior
    @Query("SELECT o FROM Order o JOIN FETCH o.user u WHERE o.status = :status")
    Page<Order> findByStatusWithUser(@Param("status") Order.OrderStatus status, Pageable pageable);

    /*
     * 🎓 CONSULTAS AGREGADAS - Relatórios Financeiros
     */

    // ✅ BOA PRÁTICA: Agregação complexa para relatórios
    // SUM() para totais financeiros por departamento e período
    // Usa múltiplos índices: idx_order_date + relacionamentos
    // Ideal para dashboards executivos
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.user.department.id = :departmentId AND o.orderDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalAmountByDepartmentAndDateRange(
        @Param("departmentId") Long departmentId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);

    /*
     * 🎓 GESTÃO ESTRATÉGICA DE BLOBs
     */

    // ✅ BOA PRÁTICA: Consulta SEM BLOBs
    // CRÍTICO: SELECT específico exclui invoicePdf (campo BLOB)
    // Para listagens onde não precisamos dos PDFs
    // Muito mais eficiente que carregar entidades completas
    @Query("SELECT o.id, o.orderNumber, o.orderDate, o.totalAmount, o.status FROM Order o WHERE o.totalAmount > :minAmount")
    List<Object[]> findOrdersWithoutBlobData(@Param("minAmount") BigDecimal minAmount);

    /*
     * 🚨 MÁS PRÁTICAS - EXEMPLOS PERIGOSOS COM BLOBs!
     */
    
    // ❌ MÁ PRÁTICA: Consulta que carrega entidades completas (incluindo PDFs)
    // PROBLEMA: Carrega invoicePdf (BLOB) para todos os pedidos que satisfazem critério
    // RESULTADO: Se há 1000 pedidos > valor, carrega 1000 PDFs (potencialmente GB!)
    // IMPACTO: OutOfMemoryError + timeouts + crash da aplicação
    // @Query("SELECT o FROM Order o WHERE o.totalAmount > :minAmount")
    // List<Order> findOrdersWithBlobData(@Param("minAmount") BigDecimal minAmount);

    // ❌ MÁ PRÁTICA: findAll() em tabela com BLOBs
    // PROBLEMA: Carrega TODOS os pedidos + TODOS os PDFs
    // RESULTADO: Crash garantido da aplicação
    // NUNCA usar: List<Order> findAll();

    /*
     * 🎓 CONSULTAS OPTIMIZADAS POR INTERVALOS DE TEMPO
     */

    // ✅ BOA PRÁTICA: Consulta por intervalo de datas com índice
    // Usa o índice idx_order_date para eficiência máxima
    // BETWEEN é optimizado pela maioria das bases de dados
    // ORDER BY DESC aproveita o mesmo índice
    @Query("SELECT o FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate ORDER BY o.orderDate DESC")
    List<Order> findOrdersByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /*
     * 🎓 RELATÓRIOS E ESTATÍSTICAS
     */

    // ✅ BOA PRÁTICA: Estatísticas agregadas SEM carregar entidades
    // COUNT(o) e AVG(o.totalAmount) calculados na base de dados
    // GROUP BY status usa índice idx_order_status
    // Retorna apenas dados agregados, não entidades completas
    @Query("SELECT o.status, COUNT(o), AVG(o.totalAmount) FROM Order o GROUP BY o.status")
    List<Object[]> getOrderStatistics();

    /*
     * 🎓 MÉTODOS DE PAGINAÇÃO - Performance Testing
     * Essenciais para validar performance com grandes volumes
     */

    // ✅ BOA PRÁTICA: Filtro por utilizador com paginação
    // Usa índice idx_order_user + paginação eficiente
    Page<Order> findByUserId(Long userId, Pageable pageable);

    // ✅ BOA PRÁTICA: Filtro por status com paginação
    // Usa índice idx_order_status + paginação eficiente
    Page<Order> findByStatus(String status, Pageable pageable);

    /*
     * 🎓 MÉTODOS ADICIONAIS ÚTEIS (implementar conforme necessário)
     */

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