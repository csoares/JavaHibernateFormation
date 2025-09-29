package com.formation.hibernate.repository;

import com.formation.hibernate.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 🎓 ORDER ITEM REPOSITORY - Demonstração de Entidades de Ligação
 * 
 * Esta interface demonstra técnicas para entidades de ligação complexas:
 * ✅ Query Methods para relacionamentos @ManyToOne optimizados
 * ✅ JOIN FETCH múltiplo para carregar várias relações numa consulta
 * ✅ Consultas de contagem eficientes sem carregar dados
 * ✅ Optimizações específicas para entidades de ligação (junction entities)
 * ✅ Consultas agregadas para relatórios de vendas
 * ✅ Métodos para análise de comportamento de compra
 */

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // ✅ BOA PRÁTICA: Query Method simples e eficiente
    // Spring gera automaticamente: WHERE oi.order.id = ?
    // Usa o índice idx_orderitem_order definido na entidade OrderItem
    List<OrderItem> findByOrderId(Long orderId);

    // ✅ BOA PRÁTICA: JOIN FETCH múltiplo para carregar relações
    // Carrega OrderItem + Product + Order numa única query
    // Evita N+1: sem isto seriam 1 + N queries para products + M queries para orders
    // IMPORTANTE: Na prática, order já seria conhecido, mas este exemplo mostra a técnica
    @Query("SELECT oi FROM OrderItem oi " +
           "JOIN FETCH oi.product p " +
           "JOIN FETCH oi.order o " +
           "WHERE o.id = :orderId")
    List<OrderItem> findByOrderIdWithProduct(@Param("orderId") Long orderId);

    // ✅ BOA PRÁTICA: COUNT sem carregar entidades
    // Muito mais eficiente que findByOrderId().size()
    // Executa SELECT COUNT(*) em vez de carregar todos os dados
    long countByOrderId(Long orderId);

    // ✅ BOA PRÁTICA: Query Method para relação @ManyToOne
    // Útil para analytics: "quantos pedidos incluem este produto?"
    // Spring gera: WHERE oi.product.id = ?
    List<OrderItem> findByProductId(Long productId);

    /*
     * 🎓 MÉTODOS ADICIONAIS PARA DEMONSTRAÇÃO
     */

    // ✅ BOA PRÁTICA: Query com múltiplas condições
    // Demonstra como combinar condições em Query Methods
    List<OrderItem> findByOrderIdAndProductId(Long orderId, Long productId);

    // ✅ BOA PRÁTICA: Projeção de valores calculados
    // Soma total de um pedido baseado nos itens (alternativa ao campo calculado)
    @Query("SELECT SUM(oi.totalPrice) FROM OrderItem oi WHERE oi.order.id = :orderId")
    Double calculateOrderTotal(@Param("orderId") Long orderId);

    // ✅ BOA PRÁTICA: Query para relatórios
    // TOP N produtos mais vendidos por quantidade
    @Query("SELECT oi.product.id, oi.product.name, SUM(oi.quantity) as totalSold " +
           "FROM OrderItem oi " +
           "GROUP BY oi.product.id, oi.product.name " +
           "ORDER BY totalSold DESC")
    List<Object[]> findTopSellingProducts();

    /*
     * 🚨 MÁS PRÁTICAS - O QUE NÃO FAZER
     */

    // ❌ MÁ PRÁTICA: Buscar sem otimização de relações
    // PROBLEMA: Vai gerar N+1 se depois acessarmos oi.product ou oi.order
    // SOLUÇÃO: Use findByOrderIdWithProduct() que carrega tudo junto
    // List<OrderItem> findByOrderIdBadVersion(Long orderId);

    // ❌ MÁ PRÁTICA: Usar findAll().size() em vez de count()
    // PROBLEMA: Carrega todas as entidades na memória só para contar
    // RESULTADO: Waste de memória e performance terrível
    // long countBadVersion(); // implementação: return findAll().size();
}