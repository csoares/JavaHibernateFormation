package com.formation.hibernate.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;

import java.util.List;

/**
 * 🎓 ENTIDADE DEPARTMENT - Demonstração de Boas Práticas JPA/Hibernate
 * 
 * Esta entidade representa um departamento empresarial e demonstra:
 * ✅ Configuração correcta de índices para performance
 * ✅ Gestão eficiente de relacionamentos bidirecionais
 * ✅ Uso apropriado de @BatchSize para evitar problemas N+1
 * ✅ Constraints de base de dados ao nível da aplicação
 * ❌ Exemplos comentados de más práticas para comparação educativa
 */

// ✅ BOA PRÁTICA: @Entity marca esta classe como uma entidade JPA
// O Hibernate irá mapear esta classe para uma tabela na base de dados
@Entity

// ✅ BOA PRÁTICA: @Table permite especificar o nome da tabela e índices
// name = "departments": define explicitamente o nome da tabela
// indexes: define índices para melhorar performance das consultas
@Table(name = "departments", indexes = {
    // Índice no campo 'name' - essencial para consultas por nome do departamento
    // Consultas tipo "WHERE name = ?" usarão este índice em vez de table scan
    @Index(name = "idx_dept_name", columnList = "name")
})
public class Department {

    // ✅ BOA PRÁTICA: @Id marca este campo como chave primária
    @Id
    // ✅ BOA PRÁTICA: IDENTITY usa auto-increment da base de dados
    // Mais eficiente que SEQUENCE para a maioria das bases de dados
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ BOA PRÁTICA: Especificar constraints explicitamente
    // nullable = false: campo obrigatório (NOT NULL constraint)
    // unique = true: garante unicidade + cria índice automático
    // length = 100: limita tamanho (importante para performance)
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    // ✅ BOA PRÁTICA: Campos opcionais podem ter nullable = true (padrão)
    // length = 500: limita tamanho para descrições mais longas
    @Column(length = 500)
    private String description;

    // ✅ BOA PRÁTICA: Especificar nome da coluna quando diferente do campo
    // Mantém clareza entre Java (camelCase) e BD (snake_case)
    @Column(name = "budget")
    private Double budget;

    // ✅ BOA PRÁTICA: @OneToMany com LAZY + @BatchSize para colecções
    // mappedBy = "department": indica que User tem a foreign key, não Department
    // FetchType.LAZY: só carrega users quando explicitamente acedidos
    // @BatchSize(25): quando carregar users, carrega 25 de cada vez (eficiente)
    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    @BatchSize(size = 25)
    private List<User> users;

    /*
     * 🚨 MÁS PRÁTICAS - EXEMPLOS DO QUE NÃO FAZER!
     * Estas configurações estão comentadas para demonstrar problemas comuns
     */
    
    // ❌ MÁ PRÁTICA: FetchType.EAGER em colecções (@OneToMany)
    // PROBLEMA: Carrega TODOS os users do departamento em cada consulta
    // RESULTADO: Performance terrível + possível OutOfMemoryError com muitos users
    // IMPACTO: Se um departamento tem 1000 users, sempre carrega os 1000!
    // @OneToMany(mappedBy = "department", fetch = FetchType.EAGER)
    // private List<User> users;

    // ❌ MÁ PRÁTICA: @OneToMany sem @BatchSize
    // PROBLEMA: Problema N+1 - se carregar 100 departments, executa 101 consultas
    // RESULTADO: 1 consulta para departments + 100 consultas para users de cada um
    // @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    // private List<User> users; // SEM @BatchSize

    // ❌ MÁ PRÁTICA: Não especificar tamanhos ou constraints
    // PROBLEMA: Base de dados pode usar tipos menos eficientes (TEXT vs VARCHAR)
    // RESULTADO: Performance degradada + maior uso de espaço
    // @Column // SEM nullable, unique, length especificados
    // private String name;

    // ❌ MÁ PRÁTICA: Não criar índices em campos pesquisados
    // PROBLEMA: Consultas por nome fazem table scan completo
    // RESULTADO: Performance exponencialmente pior com o crescimento dos dados

    // ✅ BOA PRÁTICA: Construtor padrão obrigatório para JPA
    // O JPA/Hibernate precisa deste construtor para criar instâncias via reflexão
    public Department() {}

    // ✅ BOA PRÁTICA: Construtor de conveniência para criação de objectos
    // Facilita a criação de departamentos nos testes e população de dados
    // Não inclui ID (será gerado automaticamente) nem users (carregados quando necessário)
    public Department(String name, String description, Double budget) {
        this.name = name;
        this.description = description;
        this.budget = budget;
    }

    /*
     * 🎓 GETTERS E SETTERS - Práticas Importantes
     * 
     * ✅ Sempre públicos para JPA poder aceder
     * ✅ Seguir convenção JavaBean (getName, setName)
     * ⚠️  CUIDADO: getter de 'users' pode disparar consulta se não estiver carregado!
     */

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getBudget() { return budget; }
    public void setBudget(Double budget) { this.budget = budget; }

    // ⚠️ ATENÇÃO: Este getter pode disparar uma consulta se users não estiverem carregados!
    // Use consultas com JOIN FETCH ou EntityGraph quando precisar dos users
    // Com @BatchSize(25), será mais eficiente, mas ainda assim é uma consulta extra
    public List<User> getUsers() { return users; }
    public void setUsers(List<User> users) { this.users = users; }
}