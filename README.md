# Projeto de Formação - JPA/Hibernate Performance

Este projeto demonstra as **melhores e piores práticas** de JPA/Hibernate através de implementações comparativas, com medição de performance em tempo real.

## 🎯 Objetivo

Criar um ambiente prático para demonstrar:
- ✅ **Boas práticas** de JPA/Hibernate
- ❌ **Más práticas** que causam problemas de performance
- 📊 **Medição de performance** em tempo real
- 🔍 **Análise de queries** SQL geradas
- 💾 **Gestão eficiente de blobs** (PDFs)
- 🔄 **Diferentes tipos de fetch** (LAZY vs EAGER)
- 📖 **Transações read-only** para otimização

## 🏗️ Estrutura do Projeto

### Entidades JPA
```
src/main/java/com/formation/hibernate/entity/
├── User.java          # Usuários com departamento
├── Department.java    # Departamentos
├── Order.java         # Pedidos com PDF (blob)
├── OrderItem.java     # Itens do pedido
├── Product.java       # Produtos com imagem (blob)
└── Category.java      # Categorias de produtos
```

### DTOs e Converters
```
src/main/java/com/formation/hibernate/dto/
└── [Vários DTOs otimizados para diferentes cenários]

src/main/java/com/formation/hibernate/converter/
└── [Converters eficientes entre entidades e DTOs]
```

### Controladores Comparativos
```
src/main/java/com/formation/hibernate/controller/
├── good/              # ✅ Implementações BOAS
│   ├── UserGoodController.java
│   └── OrderGoodController.java
└── bad/               # ❌ Implementações RUINS
    ├── UserBadController.java
    └── OrderBadController.java
```

## 🚀 Como Executar

### 1. Executar a Aplicação
```bash
mvn spring-boot:run
```

### 2. Popular a Base de Dados
```bash
curl -X POST http://localhost:8080/api/data/populate
```
Isto cria:
- 10 departamentos
- 10 categorias
- **10.000 usuários**
- **5.000 produtos** (com imagens blob)
- **100.000 pedidos** (com PDFs blob)

### 3. Verificar Estatísticas
```bash
curl http://localhost:8080/api/data/statistics
```

## 🔍 Demonstrações de Performance

### Alternativa: Comparação por Requests HTTP

Em vez de executar os containers Docker, pode comparar diretamente as práticas fazendo requests HTTP para os endpoints `good` vs `bad`:

#### 🚀 Teste Rápido de Performance
```bash
# Primeiro, certifique-se de que a aplicação está a correr
mvn spring-boot:run

# Populate dados de teste
curl -X POST http://localhost:8080/api/data/populate

# 1. Comparar busca de usuário por ID
echo "=== GOOD PRACTICE - Busca por ID ===" 
time curl -s "http://localhost:8080/api/good/users/1" > /dev/null

echo "=== BAD PRACTICE - Busca por ID ===" 
time curl -s "http://localhost:8080/api/bad/users/1" > /dev/null

# 2. Comparar paginação de usuários
echo "=== GOOD PRACTICE - Paginação ===" 
time curl -s "http://localhost:8080/api/good/users?page=0&size=20" > /dev/null

echo "=== BAD PRACTICE - Todos os usuários ===" 
# CUIDADO: Este endpoint pode ser muito lento!
time curl -s "http://localhost:8080/api/bad/users" > /dev/null

# 3. Comparar busca por departamento
echo "=== GOOD PRACTICE - Por Departamento ===" 
time curl -s "http://localhost:8080/api/good/users/department/Tecnologia" > /dev/null

echo "=== BAD PRACTICE - Por Departamento ===" 
time curl -s "http://localhost:8080/api/bad/users/department/Tecnologia" > /dev/null

# 4. Ver resumo de performance
curl "http://localhost:8080/api/good/users/performance/summary"
curl "http://localhost:8080/api/bad/users/performance/summary"
```

#### 📊 Script de Benchmark Simples
Crie um ficheiro `benchmark.sh`:
```bash
#!/bin/bash

echo "🚀 Iniciando teste de performance..."

# Função para medir tempo de resposta
measure_endpoint() {
    local name=$1
    local url=$2
    echo "Testing: $name"
    
    # Fazer 5 requests e calcular média
    total_time=0
    for i in {1..5}; do
        response_time=$(curl -o /dev/null -s -w '%{time_total}' "$url")
        total_time=$(echo "$total_time + $response_time" | bc)
        echo "  Request $i: ${response_time}s"
    done
    
    avg_time=$(echo "scale=3; $total_time / 5" | bc)
    echo "  ⏱️  Tempo médio: ${avg_time}s"
    echo ""
}

# Comparações
measure_endpoint "GOOD - Buscar usuário" "http://localhost:8080/api/good/users/1"
measure_endpoint "BAD - Buscar usuário" "http://localhost:8080/api/bad/users/1"

measure_endpoint "GOOD - Paginação" "http://localhost:8080/api/good/users?page=0&size=20"
measure_endpoint "BAD - Listar todos" "http://localhost:8080/api/bad/users"

echo "✅ Teste concluído!"
```

Execute: `chmod +x benchmark.sh && ./benchmark.sh`

### Console H2 Database
Acesse: http://localhost:8080/h2-console
- **URL:** `jdbc:h2:mem:testdb`
- **User:** `sa`
- **Password:** `password`

### Comparação de Endpoints

#### ✅ **BOAS PRÁTICAS** - Endpoints `/api/good/`

**Usuários:**
```bash
# Busca otimizada por ID (EntityGraph)
curl "http://localhost:8080/api/good/users/1"

# Paginação eficiente
curl "http://localhost:8080/api/good/users?page=0&size=20"

# Projeção JPQL (apenas dados necessários)
curl "http://localhost:8080/api/good/users/summaries"

# JOIN FETCH otimizado
curl "http://localhost:8080/api/good/users/department/Tecnologia"
```

**Pedidos:**
```bash
# EntityGraph para múltiplas relações
curl "http://localhost:8080/api/good/orders/1"

# Paginação por status
curl "http://localhost:8080/api/good/orders?status=PENDING&page=0&size=10"

# Consulta sem carregar blobs
curl "http://localhost:8080/api/good/orders/high-value?minAmount=1000"
```

#### ❌ **MÁS PRÁTICAS** - Endpoints `/api/bad/`

**Usuários:**
```bash
# N+1 Problem garantido
curl "http://localhost:8080/api/bad/users/1"

# PERIGO: Carrega TODOS os usuários
curl "http://localhost:8080/api/bad/users"

# Filtragem em memória (péssimo!)
curl "http://localhost:8080/api/bad/users/department/Tecnologia"
```

**Pedidos:**
```bash
# Carrega blobs desnecessariamente
curl "http://localhost:8080/api/bad/orders/1"

# MUITO PERIGOSO: Todos os pedidos + PDFs
curl "http://localhost:8080/api/bad/orders"
```

## 📊 Medição de Performance

O projeto inclui um sistema de monitorização que:
- ⏱️ **Mede tempo** de execução de cada operação
- 📝 **Loga detalhes** com emojis para fácil identificação
- 📈 **Gera relatórios** comparativos
- 🎯 **Identifica gargalos** automaticamente

### Ver Resumo de Performance
```bash
curl "http://localhost:8080/api/good/users/performance/summary"
curl "http://localhost:8080/api/bad/users/performance/summary"
```

## 🎓 Pontos de Formação

### 1. **FetchType.LAZY vs EAGER**
```java
// ✅ BOM: LAZY por defeito
@ManyToOne(fetch = FetchType.LAZY)
private Department department;

// ❌ MÁU: EAGER causa carregamento desnecessário
// @ManyToOne(fetch = FetchType.EAGER)
// private Department department;
```

### 2. **EntityGraph vs N+1 Problem**
```java
// ✅ BOM: EntityGraph resolve N+1
@EntityGraph(value = "User.withDepartment")
Optional<User> findByIdWithDepartment(Long id);

// ❌ MÁU: findById causa N+1
Optional<User> findById(Long id);
```

### 3. **Projeções JPQL vs Entidades Completas**
```java
// ✅ BOM: Apenas dados necessários
@Query("SELECT new UserSummaryDto(u.id, u.name, u.email) FROM User u")
List<UserSummaryDto> findAllUserSummaries();

// ❌ MÁU: Carrega tudo
List<User> findAll();
```

### 4. **Gestão de Blobs**
```java
// ✅ BOM: LAZY para blobs
@Lob
@Basic(fetch = FetchType.LAZY)
private byte[] invoicePdf;

// ❌ MÁU: Carrega blob sempre
// @Lob
// private byte[] invoicePdf;
```

### 5. **Transações Read-Only**
```java
// ✅ BOM: Read-only para consultas
@Transactional(readOnly = true)
public List<User> findUsers() { ... }

// ❌ MÁU: Transação write desnecessária
public List<User> findUsers() { ... }
```

### 6. **Índices e Consultas**
```java
// ✅ BOM: Índices nas colunas pesquisadas
@Index(name = "idx_user_email", columnList = "email")

// ✅ BOM: WHERE com índices
@Query("SELECT u FROM User u WHERE u.email = :email")

// ❌ MÁU: Sem índices, filtro em memória
```

### 7. **Paginação**
```java
// ✅ BOM: Paginação sempre
Page<User> findAll(Pageable pageable);

// ❌ MÁU: findAll() sem limite
List<User> findAll();
```

## 🔧 Profiles de Configuração

### Profile `good-performance`
```bash
mvn spring-boot:run -Dspring.profiles.active=good-performance
```
- Batch size otimizado (50)
- Cache de 2º nível ativo
- Logs reduzidos

### Profile `bad-performance`
```bash
mvn spring-boot:run -Dspring.profiles.active=bad-performance
```
- Batch size péssimo (1)
- Sem cache
- Logs detalhados para análise

## 📈 Métricas Esperadas

Com 100.000 registros:

| Operação | Boa Prática | Má Prática | Diferença |
|----------|-------------|------------|-----------|
| Buscar 1 usuário | ~5ms | ~50ms+ | **10x mais lento** |
| Listar 20 usuários | ~20ms | ~2000ms+ | **100x mais lento** |
| Buscar por departamento | ~15ms | ~5000ms+ | **300x mais lento** |
| Estatísticas | ~10ms | ~10000ms+ | **1000x mais lento** |

## ⚠️ Cuidados Importantes

1. **NÃO execute** `/api/bad/users` com muitos dados - pode causar OutOfMemoryError
2. **NÃO execute** `/api/bad/orders` - carrega TODOS os PDFs na memória
3. Use os endpoints ruins apenas para **demonstração** em ambiente controlado
4. Sempre compare logs de performance entre good/bad

## 🎯 Exercícios Sugeridos

1. Execute endpoints `good` e `bad` comparando logs
2. Ative profile `bad-performance` e observe diferenças
3. Use H2 Console para ver queries SQL geradas
4. Meça diferenças de tempo de execução
5. Analise uso de memória com/sem blobs
6. Teste com diferentes tamanhos de página

## 🔄 Limpar Dados

```bash
curl -X DELETE http://localhost:8080/api/data/clear
```

---

---

## 🎓 Branches Educacionais

Este repositório contém vários branches especializados para ensino focado de conceitos específicos:

### 📚 **Main Branch**
- **Branch**: `main`
- **Conteúdo**: Implementação completa com BOAS práticas
- **Uso**: Referência de como fazer correctamente
- **Características**: 
  - EntityGraphs optimizados
  - Paginação em todos os endpoints
  - Gestão eficiente de BLOBs
  - Transacções read-only
  - Comentários educacionais em português

### 🚨 **BadMain Branch**
- **Branch**: `badmain`
- **Conteúdo**: Implementação completa com MÁS práticas intencionais
- **Uso**: Demonstração do que NÃO fazer
- **⚠️ AVISO**: Apenas para fins educacionais!
- **Características**:
  - Problema N+1 sistemático
  - Ausência total de paginação
  - Carregamento desnecessário de BLOBs
  - Filtragem em memória
  - Falta de validação

### 🎯 **Branches Focados em Conceitos Específicos**

#### 📖 **001-n1problem** - Problema N+1
- **Foco**: Demonstração isolada do problema N+1
- **Contém**: 
  - `N1ProblemController` com exemplos simples
  - `README-N1PROBLEM.md` com explicação detalhada
  - Comparação directa: bad vs EntityGraph vs JOIN FETCH
- **Aprenda**: Como detectar, medir e resolver o problema N+1

#### 📄 **002-pagination** - Paginação Eficiente
- **Foco**: Técnicas de paginação para grandes volumes
- **Contém**:
  - `PaginationController` com exemplos práticos
  - `README-PAGINATION.md` com guia completo
  - Demonstração de filtros, ordenação e metadata
- **Aprenda**: Como evitar OutOfMemoryError com paginação inteligente

#### 💾 **003-blob-management** - Gestão de BLOBs
- **Foco**: Gestão eficiente de ficheiros e dados pesados
- **Contém**:
  - `BlobManagementController` com cenários reais
  - `README-BLOB-MANAGEMENT.md` com estratégias
  - Projecções que evitam BLOBs, streaming, metadata
- **Aprenda**: Como trabalhar com BLOBs sem quebrar a performance

### 🚀 **Como Usar os Branches Educacionais**

#### 1. **Aprendizado Progressivo**
```bash
# Começar com conceitos básicos
git checkout 001-n1problem
# Ler README-N1PROBLEM.md
# Testar endpoints /api/n1-demo/*

# Avançar para paginação
git checkout 002-pagination
# Ler README-PAGINATION.md
# Testar endpoints /api/pagination-demo/*

# Dominar gestão de BLOBs
git checkout 003-blob-management
# Ler README-BLOB-MANAGEMENT.md
# Testar endpoints /api/blob-demo/*
```

#### 2. **Comparação de Implementações**
```bash
# Ver implementação má
git checkout badmain
curl "http://localhost:8080/api/users" # LENTO!

# Ver implementação boa
git checkout main
curl "http://localhost:8080/api/good/users?page=0&size=20" # RÁPIDO!
```

#### 3. **Exercícios Práticos**
```bash
# Branch focado para praticar N+1
git checkout 001-n1problem
mvn spring-boot:run
curl "http://localhost:8080/api/n1-demo/bad/1"     # Ver problema
curl "http://localhost:8080/api/n1-demo/good-entitygraph/1"  # Ver solução
```

### 📊 **Comparação entre Branches**

| Branch | Propósito | N+1 Problem | Paginação | BLOBs | Complexidade |
|--------|-----------|-------------|-----------|-------|--------------|
| `main` | ✅ Referência boa | ✅ Resolvido | ✅ Sempre | ✅ Optimizado | 🟡 Complexa |
| `badmain` | ❌ Anti-padrões | ❌ Sistemático | ❌ Nunca | ❌ Perigoso | 🟡 Complexa |
| `001-n1problem` | 🎓 Foco N+1 | 🎯 **FOCO** | ➖ Mínimo | ➖ Mínimo | 🟢 Simples |
| `002-pagination` | 🎓 Foco Paginação | ➖ Mínimo | 🎯 **FOCO** | ➖ Mínimo | 🟢 Simples |
| `003-blob-management` | 🎓 Foco BLOBs | ➖ Mínimo | ➖ Mínimo | 🎯 **FOCO** | 🟢 Simples |

### 🎯 **Plano de Estudos Sugerido**

#### 📚 **Nível Iniciante**
1. Ler `README-N1PROBLEM.md` no branch `001-n1problem`
2. Executar comparações simples de N+1
3. Compreender EntityGraphs básicos

#### 📈 **Nível Intermédio**
1. Dominar paginação no branch `002-pagination`
2. Praticar consultas optimizadas
3. Aprender gestão de BLOBs no branch `003-blob-management`

#### 🏆 **Nível Avançado**
1. Comparar `main` vs `badmain` integralmente
2. Medir performance real com ferramentas
3. Implementar optimizações próprias

---

**📚 Este projeto é uma ferramenta educativa completa para compreender profundamente as implicações de performance em JPA/Hibernate através de exemplos práticos, medições reais e aprendizado progressivo por conceitos específicos.**