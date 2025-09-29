package com.formation.hibernate.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;

import java.util.List;

/**
 * 🎓 ENTIDADE CATEGORY - Demonstração de Relacionamentos One-to-Many
 * 
 * Esta entidade representa uma categoria de produtos e demonstra:
 * ✅ Configuração simples mas eficiente de entidade
 * ✅ Índices para consultas por nome de categoria
 * ✅ Relacionamento one-to-many optimizado com @BatchSize
 * ✅ Estrutura clean sem complexidade desnecessária
 * ❌ Exemplos de configurações inadequadas comentadas
 */

// ✅ BOA PRÁTICA: Entidade simples com configuração clean
@Entity

// ✅ BOA PRÁTICA: Nome explícito da tabela + índice para consultas
// Categorias são frequentemente pesquisadas por nome
@Table(name = "categories", indexes = {
    // Índice essencial para consultas "WHERE name = ?" ou "WHERE name LIKE '%?%'"
    @Index(name = "idx_category_name", columnList = "name")
})
public class Category {

    // ✅ BOA PRÁTICA: Chave primária com geração automática
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ BOA PRÁTICA: Campo nome único e indexado
    // unique = true evita categorias duplicadas + cria índice automático
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    // ✅ BOA PRÁTICA: Campo opcional para descrição mais detalhada
    @Column(length = 500)
    private String description;

    // ✅ BOA PRÁTICA: Relacionamento um-para-muitos optimizado
    // Uma categoria pode ter muitos produtos
    // mappedBy = "category": indica que Product tem a foreign key
    // LAZY: só carrega produtos quando explicitamente necessário
    // @BatchSize(25): carrega produtos em lotes de 25 (evita N+1)
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    @BatchSize(size = 25)
    private List<Product> products;

    /*
     * 🚨 MÁS PRÁTICAS - EXEMPLOS EDUCATIVOS
     */
    
    // ❌ MÁ PRÁTICA: FetchType.EAGER em colecções
    // PROBLEMA: Sempre carrega TODOS os produtos da categoria
    // IMPACTO: Se uma categoria tem 500 produtos, sempre carrega os 500!
    // RESULTADO: Consultas enormes + possível OutOfMemoryError
    // @OneToMany(mappedBy = "category", fetch = FetchType.EAGER)
    // private List<Product> products;

    // ❌ MÁ PRÁTICA: Sem @BatchSize
    // PROBLEMA: Se carregar 50 categorias, faz 51 consultas (1 + 50)
    // @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    // private List<Product> products; // SEM @BatchSize

    // ✅ BOA PRÁTICA: Construtor padrão para JPA
    public Category() {}

    // ✅ BOA PRÁTICA: Construtor funcional para criação de objectos
    public Category(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /*
     * 🎓 GETTERS E SETTERS - Configuração Standard JPA
     */

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    // ⚠️ ATENÇÃO: Este getter pode disparar consulta para carregar produtos!
    // Use JOIN FETCH ou EntityGraph quando precisar dos produtos da categoria
    public List<Product> getProducts() { return products; }
    public void setProducts(List<Product> products) { this.products = products; }
}