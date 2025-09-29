package com.formation.hibernate.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 🎓 ENTIDADE USER - Demonstração de Boas e Más Práticas JPA/Hibernate
 * 
 * Esta entidade representa um utilizador do sistema e demonstra:
 * ✅ Técnicas essenciais para performance óptima
 * ✅ Configuração correcta de relacionamentos bidirecionais
 * ✅ Uso estratégico de índices para consultas frequentes
 * ✅ EntityGraphs para resolver problemas N+1
 * ✅ @BatchSize para optimização de colecções
 * ❌ Exemplos comentados de más práticas comuns
 */

// ✅ BOA PRÁTICA: @Entity marca a classe como uma entidade JPA
// O nome da tabela será automaticamente "User" se não especificado
@Entity

// ✅ BOA PRÁTICA: @Table permite customizar nome da tabela e definir índices
// Índices são ESSENCIAIS para performance de consultas
@Table(name = "users", indexes = {
    // Índice no email para buscas rápidas (email é unique e frequentemente pesquisado)
    @Index(name = "idx_user_email", columnList = "email"),
    // Índice na foreign key para JOINs eficientes
    @Index(name = "idx_user_department", columnList = "department_id")
})

// ✅ BOA PRÁTICA: @NamedEntityGraph define quais relacionamentos carregar
// Resolve o problema N+1 de forma declarativa
// Este EntityGraph será usado em consultas que precisam do departamento
@NamedEntityGraph(
    name = "User.withDepartment",
    attributeNodes = {
        @NamedAttributeNode("department")  // Carrega department junto com user
    }
)
public class User {

    // ✅ BOA PRÁTICA: @Id marca o campo como chave primária
    @Id
    // ✅ BOA PRÁTICA: IDENTITY é eficiente para auto-incremento na maioria dos BDs
    // Alternativas: AUTO, SEQUENCE, TABLE (cada uma tem seu caso de uso)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ BOA PRÁTICA: @Column com constraints de validação ao nível da BD
    // nullable = false: NOT NULL constraint
    // length: limita o tamanho (importante para performance e storage)
    @Column(nullable = false, length = 100)
    private String name;

    // ✅ BOA PRÁTICA: unique = true cria constraint UNIQUE + índice automático
    // Email é naturalmente único e frequentemente pesquisado
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    // ✅ BOA PRÁTICA: name especifica o nome da coluna na BD (snake_case vs camelCase)
    // Valor padrão definido aqui evita problemas de NULL
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // ✅ BOA PRÁTICA: @ManyToOne com FetchType.LAZY (padrão, mas explícito)
    // LAZY significa que department só é carregado quando explicitamente acessado
    // Isto evita carregar dados desnecessários
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")  // Nome da coluna FK na tabela users
    private Department department;

    // ✅ BOA PRÁTICA: @OneToMany com LAZY + @BatchSize
    // mappedBy indica que Order tem a foreign key (não User)
    // @BatchSize otimiza carregamento de coleções (carrega 25 orders por vez)
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @BatchSize(size = 25)  // Hibernate carrega até 25 orders numa única query
    private List<Order> orders;

    /*
     * 🚨 MÁS PRÁTICAS - NÃO FAZER ISTO! 
     * Estas configurações estão comentadas para demonstrar problemas comuns
     */
    
    // ❌ MÁ PRÁTICA: FetchType.EAGER em @ManyToOne
    // PROBLEMA: Sempre carrega department, mesmo quando não necessário
    // RESULTADO: Queries desnecessárias + mais dados na memória + JOINs sempre executados
    // @ManyToOne(fetch = FetchType.EAGER)
    // @JoinColumn(name = "department_id")
    // private Department department;

    // ❌ MÁ PRÁTICA: FetchType.EAGER em coleções (@OneToMany)
    // PROBLEMA: Carrega TODOS os orders do user em cada consulta
    // RESULTADO: OutOfMemoryError com muitos orders + queries gigantes + performance terrível
    // @OneToMany(mappedBy = "user", fetch = FetchType.EAGER)
    // private List<Order> orders;

    // ❌ MÁ PRÁTICA: @OneToMany sem @BatchSize
    // PROBLEMA: Problema N+1 - se carregar 100 users, executa 101 queries (1 + 100)
    // RESULTADO: Centenas de queries pequenas em vez de algumas queries eficientes
    // @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    // private List<Order> orders;

    // ❌ MÁ PRÁTICA: Sem índices nas colunas pesquisadas
    // PROBLEMA: Consultas fazem table scan completo
    // RESULTADO: Performance degradada exponencialmente com o tamanho da tabela
    
    // ❌ MÁ PRÁTICA: Não especificar tamanhos de String
    // PROBLEMA: BD pode usar TEXT em vez de VARCHAR, menos eficiente
    // @Column(nullable = false)  // SEM length definido
    // private String name;

    // ✅ BOA PRÁTICA: Construtor padrão obrigatório para JPA
    // JPA precisa deste construtor para criar instâncias via reflection
    public User() {}

    // ✅ BOA PRÁTICA: Construtor de conveniência para criação de objetos
    // Não inclui ID (será gerado pela BD) nem orders (será carregado quando necessário)
    public User(String name, String email, Department department) {
        this.name = name;
        this.email = email;
        this.department = department;
    }

    /*
     * 🎓 GETTERS E SETTERS - Práticas Importantes
     * 
     * ✅ Sempre public para JPA
     * ✅ Seguir convenção JavaBean
     * ⚠️  Cuidado: getters de relacionamentos LAZY podem disparar queries!
     */

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // ⚠️ ATENÇÃO: Este getter pode disparar uma query se department não estiver carregado!
    // Use EntityGraph ou JOIN FETCH para carregar department quando necessário
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    // ⚠️ ATENÇÃO: Este getter pode disparar uma query se orders não estiver carregado!
    // Com @BatchSize, será mais eficiente, mas ainda assim é uma query extra
    public List<Order> getOrders() { return orders; }
    public void setOrders(List<Order> orders) { this.orders = orders; }
}