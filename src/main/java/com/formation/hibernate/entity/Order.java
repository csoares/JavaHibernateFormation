package com.formation.hibernate.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 🎓 ENTIDADE ORDER - Demonstração Avançada de Performance JPA/Hibernate
 * 
 * Esta entidade representa um pedido no sistema e demonstra:
 * ✅ Gestão eficiente de BLOBs (ficheiros PDF)
 * ✅ EntityGraphs aninhados (subgraphs) para múltiplos relacionamentos
 * ✅ Índices estratégicos baseados em padrões de consulta reais
 * ✅ Uso correcto de @Enumerated para estados do pedido
 * ✅ Relacionamentos bidirecionais optimizados
 * ✅ BigDecimal para valores monetários seguros
 * ❌ Exemplos comentados de configurações perigosas
 */

@Entity
// ✅ BOA PRÁTICA: Múltiplos índices estratégicos baseados em padrões de consulta
@Table(name = "orders", indexes = {
    // Índice na FK user_id - essencial para JOINs User-Order
    @Index(name = "idx_order_user", columnList = "user_id"),
    // Índice na data - consultas por período são comuns (relatórios, etc.)
    @Index(name = "idx_order_date", columnList = "order_date"),
    // Índice no status - filtragem por status é muito frequente
    @Index(name = "idx_order_status", columnList = "status")
})

// ✅ BOA PRÁTICA: EntityGraph ANINHADO (subgraph)
// Carrega Order -> User -> Department numa única query eficiente
// Evita múltiplas queries separadas
@NamedEntityGraph(
    name = "Order.withUserAndDepartment",
    attributeNodes = {
        // Carrega user usando o subgraph definido abaixo
        @NamedAttributeNode(value = "user", subgraph = "user-department")
    },
    subgraphs = {
        // Subgraph: dentro do user, também carrega department
        @NamedSubgraph(name = "user-department", attributeNodes = {
            @NamedAttributeNode("department")
        })
    }
)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ BOA PRÁTICA: Campo único com índice automático (unique = true)
    // Business key que pode ser usado para pesquisas pelos usuários
    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    // ✅ BOA PRÁTICA: Campo com índice (definido na @Table)
    // Consultas por data/período são muito comuns em sistemas de pedidos
    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate = LocalDateTime.now();

    // ✅ BOA PRÁTICA: BigDecimal para valores monetários
    // precision = 10: até 99.999.999,99 (10 dígitos totais)
    // scale = 2: 2 casas decimais
    // NUNCA usar double/float para dinheiro!
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    // ✅ BOA PRÁTICA: @Enumerated(EnumType.STRING) para legibilidade
    // Armazena "PENDING", "CONFIRMED", etc. em vez de números
    // Mais fácil de debugar e entender na BD
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    // ✅ BOA PRÁTICA: @ManyToOne LAZY com FK obrigatória
    // nullable = false garante integridade referencial
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ✅ BOA PRÁTICA: @OneToMany com cascade e @BatchSize
    // cascade = CascadeType.ALL: operações em Order afetam OrderItems
    // @BatchSize(50): carrega até 50 order items numa query
    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @BatchSize(size = 50)
    private List<OrderItem> orderItems;

    // ✅ BOA PRÁTICA: BLOB com FetchType.LAZY
    // @Lob indica dados grandes (PDF pode ter vários MB)
    // @Basic(fetch = LAZY) garante que só carrega quando explicitamente acessado
    // CRÍTICO: sem LAZY, todos os PDFs seriam carregados sempre!
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "invoice_pdf")
    private byte[] invoicePdf;

    /*
     * 🚨 MÁS PRÁTICAS - EXEMPLOS DO QUE NÃO FAZER!
     */
    
    // ❌ MÁ PRÁTICA: FetchType.EAGER em @ManyToOne
    // PROBLEMA: Sempre carrega user em cada consulta de Order
    // RESULTADO: JOIN desnecessário + mais dados na memória
    // @ManyToOne(fetch = FetchType.EAGER)
    // @JoinColumn(name = "user_id", nullable = false)
    // private User user;

    // ❌ MÁ PRÁTICA: @Lob sem FetchType.LAZY
    // PROBLEMA: Carrega PDFs (potencialmente MB) sempre que carrega Order
    // RESULTADO: OutOfMemoryError + queries enormes + performance terrível
    // @Lob
    // @Column(name = "invoice_pdf")
    // private byte[] invoicePdf;

    // ❌ MÁ PRÁTICA: @Enumerated(EnumType.ORDINAL)
    // PROBLEMA: Usa números (0,1,2,3,4) em vez de strings
    // RESULTADO: Dados ilegíveis na BD + problemas ao reordenar enum
    // @Enumerated(EnumType.ORDINAL)
    // private OrderStatus status;

    // ❌ MÁ PRÁTICA: double/float para dinheiro
    // PROBLEMA: Problemas de precisão com pontos flutuantes
    // RESULTADO: Cálculos incorretos (ex: 0.1 + 0.2 ≠ 0.3)
    // @Column(nullable = false)
    // private double totalAmount;

    // ✅ BOA PRÁTICA: Construtor padrão para JPA
    public Order() {}

    // ✅ BOA PRÁTICA: Construtor de conveniência
    public Order(String orderNumber, BigDecimal totalAmount, User user) {
        this.orderNumber = orderNumber;
        this.totalAmount = totalAmount;
        this.user = user;
    }

    // ✅ BOA PRÁTICA: Enum bem definido com estados claros
    // Representa o ciclo de vida típico de um pedido
    public enum OrderStatus {
        PENDING,     // Aguardando confirmação
        CONFIRMED,   // Confirmado
        SHIPPED,     // Enviado
        DELIVERED,   // Entregue
        CANCELLED    // Cancelado
    }

    /*
     * 🎓 GETTERS E SETTERS COM ANOTAÇÕES EDUCATIVAS
     */

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    // ⚠️ ATENÇÃO: Pode disparar query se user não estiver carregado!
    // Use EntityGraph "Order.withUserAndDepartment" para carregar eficientemente
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    // ⚠️ ATENÇÃO: Pode disparar query se orderItems não estiverem carregados!
    // @BatchSize(50) torna isto mais eficiente, mas ainda é uma query extra
    public List<OrderItem> getOrderItems() { return orderItems; }
    public void setOrderItems(List<OrderItem> orderItems) { this.orderItems = orderItems; }

    // 🚨 PERIGO: Este getter SEMPRE dispara uma query para carregar o PDF!
    // SÓ chame quando realmente precisar do PDF
    // Use consultas específicas que não incluam este campo quando desnecessário
    public byte[] getInvoicePdf() { return invoicePdf; }
    public void setInvoicePdf(byte[] invoicePdf) { this.invoicePdf = invoicePdf; }
}