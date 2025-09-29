package com.formation.hibernate.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * 🎓 ENTIDADE ORDER_ITEM - Demonstração de Entidade de Ligação
 * 
 * Esta entidade representa um item dentro de um pedido e demonstra:
 * ✅ Entidade de ligação (junction entity) entre Order e Product
 * ✅ Campos adicionais além das foreign keys (quantity, prices)
 * ✅ Índices estratégicos para consultas de relacionamento
 * ✅ Cálculos automáticos de preços totais
 * ✅ Relacionamentos @ManyToOne optimizados
 * ❌ Exemplos de configurações problemáticas
 */

// ✅ BOA PRÁTICA: Entidade de ligação bem estruturada
@Entity

// ✅ BOA PRÁTICA: Índices essenciais para foreign keys
// order_items são frequentemente consultados por pedido ou por produto
@Table(name = "order_items", indexes = {
    // Índice para consultas "todos os itens do pedido X"
    @Index(name = "idx_orderitem_order", columnList = "order_id"),
    // Índice para consultas "em que pedidos aparece o produto Y"
    @Index(name = "idx_orderitem_product", columnList = "product_id")
})
public class OrderItem {

    // ✅ BOA PRÁTICA: Chave primária própria
    // Mesmo sendo entidade de ligação, tem identidade própria
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ BOA PRÁTICA: Campo obrigatório com validação
    // Quantidade deve ser sempre positiva (validação de negócio)
    @Column(nullable = false)
    private Integer quantity;

    // ✅ BOA PRÁTICA: Preço unitário no momento da compra
    // Importante: guarda o preço que estava em vigor quando o pedido foi feito
    // Não depende do preço actual do produto (que pode ter mudado)
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    // ✅ BOA PRÁTICA: Campo calculado para total do item
    // total_price = quantity * unit_price
    // Facilita consultas e relatórios
    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    // ✅ BOA PRÁTICA: @ManyToOne LAZY para Order
    // Muitos itens pertencem a um pedido
    // nullable = false: item DEVE ter um pedido (integridade referencial)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // ✅ BOA PRÁTICA: @ManyToOne LAZY para Product
    // Muitos itens referenciam um produto
    // nullable = false: item DEVE referenciar um produto válido
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /*
     * 🚨 MÁS PRÁTICAS - EXEMPLOS PROBLEMÁTICOS
     */
    
    // ❌ MÁ PRÁTICA: FetchType.EAGER em @ManyToOne
    // PROBLEMA: Sempre carrega order e product, mesmo quando desnecessário
    // RESULTADO: JOINs complexos + mais dados na memória
    // @ManyToOne(fetch = FetchType.EAGER)
    // @JoinColumn(name = "order_id", nullable = false)
    // private Order order;

    // ❌ MÁ PRÁTICA: Não usar BigDecimal para valores monetários
    // PROBLEMA: Erros de precisão em cálculos financeiros
    // RESULTADO: Diferenças em cêntimos que se acumulam
    // @Column(name = "unit_price", nullable = false)
    // private double unitPrice; // PERIGOSO para dinheiro!

    // ❌ MÁ PRÁTICA: Sem índices nas foreign keys
    // PROBLEMA: Consultas para itens de um pedido fazem table scan
    // RESULTADO: Performance terrível em pedidos com muitos itens

    // ✅ BOA PRÁTICA: Construtor padrão para JPA
    public OrderItem() {}

    // ✅ BOA PRÁTICA: Construtor com cálculo automático do total
    // Recebe dados essenciais e calcula automaticamente o preço total
    public OrderItem(Integer quantity, BigDecimal unitPrice, Order order, Product product) {
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        // ✅ Cálculo automático: total = quantidade × preço unitário
        this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        this.order = order;
        this.product = product;
    }

    /*
     * 🎓 GETTERS E SETTERS - Com Lógica de Negócio
     */

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    // ✅ BOA PRÁTICA: Setter com recálculo automático
    // Quando mudar quantidade, recalcula automaticamente o total
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
        // Recalcula total se já temos preço unitário
        if (this.unitPrice != null) {
            this.totalPrice = this.unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    // ✅ BOA PRÁTICA: Setter com recálculo automático
    // Quando mudar preço unitário, recalcula automaticamente o total
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        // Recalcula total se já temos quantidade
        if (this.quantity != null) {
            this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    // ⚠️ ATENÇÃO: Pode disparar consulta se order não estiver carregado
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    // ⚠️ ATENÇÃO: Pode disparar consulta se product não estiver carregado
    // Use JOIN FETCH quando precisar de aceder aos dados do produto
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
}