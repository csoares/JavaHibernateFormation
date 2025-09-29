package com.formation.hibernate.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.util.List;

/**
 * 🎓 ENTIDADE PRODUCT - Demonstração Avançada de Mapeamento JPA
 * 
 * Esta entidade representa um produto e demonstra:
 * ✅ Múltiplos índices estratégicos para diferentes tipos de consulta
 * ✅ Gestão eficiente de BLOBs (dados de imagem)
 * ✅ Uso correcto de BigDecimal para valores monetários
 * ✅ Relacionamentos bidirecionais optimizados
 * ✅ Mapeamento de campos com nomes personalizados
 * ❌ Exemplos de configurações perigosas para performance
 */

// ✅ BOA PRÁTICA: Entidade com configuração robusta de índices
@Entity

// ✅ BOA PRÁTICA: Múltiplos índices baseados em padrões de consulta real
// Produtos são pesquisados por nome, categoria e preço frequentemente
@Table(name = "products", indexes = {
    // Índice para pesquisas por nome ("iPhone", "Samsung Galaxy", etc.)
    @Index(name = "idx_product_name", columnList = "name"),
    // Índice na foreign key para JOINs eficientes com Category
    @Index(name = "idx_product_category", columnList = "category_id"),
    // Índice para consultas por faixa de preço (produtos até €100, etc.)
    @Index(name = "idx_product_price", columnList = "price")
})
public class Product {

    // ✅ BOA PRÁTICA: Chave primária auto-gerada
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ BOA PRÁTICA: Campo obrigatório com tamanho adequado
    // length = 200: suficiente para nomes de produtos detalhados
    @Column(nullable = false, length = 200)
    private String name;

    // ✅ BOA PRÁTICA: Descrição opcional mais extensa
    // length = 1000: permite descrições detalhadas do produto
    @Column(length = 1000)
    private String description;

    // ✅ BOA PRÁTICA: BigDecimal para valores monetários
    // precision = 10: até €99.999.999,99 (10 dígitos totais)
    // scale = 2: duas casas decimais para cêntimos
    // NUNCA usar double/float para dinheiro! (problemas de precisão)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    // ✅ BOA PRÁTICA: Campo de stock com nome personalizado na BD
    // Java: stockQuantity (camelCase) ↔ BD: stock_quantity (snake_case)
    @Column(name = "stock_quantity")
    private Integer stockQuantity;

    // ✅ BOA PRÁTICA: @ManyToOne com LAZY loading
    // Muitos produtos pertencem a uma categoria
    // LAZY: só carrega category quando explicitamente acedida
    // JoinColumn especifica o nome da coluna FK na tabela products
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    // ✅ BOA PRÁTICA: @OneToMany optimizado para relacionamento inverso
    // Um produto pode estar em muitos itens de pedido
    // mappedBy = "product": OrderItem tem a foreign key
    // @BatchSize evita problema N+1 quando aceder aos order items
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    @BatchSize(size = 25)
    private List<OrderItem> orderItems;

    // ✅ BOA PRÁTICA: BLOB com LAZY loading - CRÍTICO para performance!
    // @Lob: indica dados grandes (imagem pode ter vários MB)
    // @Basic(fetch = LAZY): ESSENCIAL - só carrega quando explicitamente acedida
    // SEM LAZY: todas as imagens seriam carregadas sempre (OutOfMemoryError!)
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "image_data")
    private byte[] imageData;

    /*
     * 🚨 MÁS PRÁTICAS - EXEMPLOS PERIGOSOS PARA PERFORMANCE!
     */
    
    // ❌ MÁ PRÁTICA: @Lob sem FetchType.LAZY
    // PROBLEMA: Carrega TODAS as imagens sempre que carregar produtos
    // IMPACTO: Se listar 100 produtos, carrega 100 imagens (talvez 500MB+)
    // RESULTADO: OutOfMemoryError garantido + consultas gigantescas
    // @Lob
    // @Column(name = "image_data")
    // private byte[] imageData; // SEM @Basic(fetch = LAZY)

    // ❌ MÁ PRÁTICA: FetchType.EAGER em @ManyToOne
    // PROBLEMA: Sempre carrega category mesmo quando desnecessário
    // RESULTADO: JOINs desnecessários + mais dados na memória
    // @ManyToOne(fetch = FetchType.EAGER)
    // @JoinColumn(name = "category_id")
    // private Category category;

    // ❌ MÁ PRÁTICA: double/float para preços
    // PROBLEMA: Erros de precisão em cálculos monetários
    // EXEMPLO: 0.1 + 0.2 ≠ 0.3 em floating point!
    // RESULTADO: Cálculos incorrectos em faturas e pagamentos
    // @Column(nullable = false)
    // private double price; // NUNCA fazer isto para dinheiro!

    // ❌ MÁ PRÁTICA: Sem índices em campos pesquisados
    // PROBLEMA: Consultas por preço fazem table scan completo
    // RESULTADO: Performance degrada exponencialmente com dados

    // ✅ BOA PRÁTICA: Construtor padrão obrigatório para JPA
    public Product() {}

    // ✅ BOA PRÁTICA: Construtor funcional para criação de produtos
    // Inclui todos os campos essenciais excepto ID (auto-gerado) e imageData (opcional)
    public Product(String name, String description, BigDecimal price, Integer stockQuantity, Category category) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.category = category;
    }

    /*
     * 🎓 GETTERS E SETTERS - Atenção Especial aos BLOBs
     */

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }

    // ⚠️ ATENÇÃO: Pode disparar consulta se category não estiver carregada
    // Use JOIN FETCH ou EntityGraph quando precisar da categoria
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    // ⚠️ ATENÇÃO: Pode disparar consulta se orderItems não estiverem carregados
    // @BatchSize(25) torna isto mais eficiente
    public List<OrderItem> getOrderItems() { return orderItems; }
    public void setOrderItems(List<OrderItem> orderItems) { this.orderItems = orderItems; }

    // 🚨 PERIGO: Este getter SEMPRE dispara consulta para carregar a imagem!
    // SÓ chame quando realmente precisar da imagem (mostrar, download, etc.)
    // Para listar produtos SEM imagens, use consultas específicas que excluam este campo
    public byte[] getImageData() { return imageData; }
    public void setImageData(byte[] imageData) { this.imageData = imageData; }
}