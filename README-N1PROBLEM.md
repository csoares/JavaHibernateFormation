# 🎓 Branch 001: Problema N+1 - Tutorial Prático

## 🎯 Objectivo Educacional
Este branch foca **exclusivamente** no **problema N+1** - um dos problemas de performance mais comuns em aplicações JPA/Hibernate.

## ❓ O que é o Problema N+1?

O problema N+1 ocorre quando:
1. **1 query** para buscar uma lista de entidades principais
2. **N queries adicionais** para carregar relacionamentos de cada entidade

### 📊 Exemplo Prático
```java
// 1. Uma query para buscar 100 users
List<User> users = userRepository.findAll();

// 2. Para cada user, uma query extra para buscar o department (100 queries!)
for (User user : users) {
    String deptName = user.getDepartment().getName(); // ⚠️ LAZY LOADING!
}
```

**Resultado: 1 + 100 = 101 queries!** 🐌

## 🔍 Como Identificar

### Sinais no Log:
```sql
-- Query 1: Buscar users
SELECT u.* FROM users u

-- Queries 2-101: Para cada user, buscar department
SELECT d.* FROM departments d WHERE d.id = ?
SELECT d.* FROM departments d WHERE d.id = ?
SELECT d.* FROM departments d WHERE d.id = ?
-- ... 98 queries mais!
```

### Sinais na Performance:
- ⏱️ Tempo de resposta cresce linearmente com dados
- 🔄 Muitas queries pequenas em vez de poucas grandes
- 📈 Degradação exponencial com volume

## 🛠️ Soluções Demonstradas

### ❌ Versão com Problema (UserBadController)
```java
// Problema: findById() sem otimização
Optional<User> user = userRepository.findById(id);
String deptName = user.get().getDepartment().getName(); // ⚡ Query extra!
```

### ✅ Versão Otimizada (UserGoodController)
```java
// Solução: EntityGraph carrega tudo numa query
Optional<User> user = userRepository.findByIdWithDepartment(id);
String deptName = user.get().getDepartment().getName(); // ✅ Sem query extra!
```

## 🏗️ Implementações Demonstradas

### 1. EntityGraph (@NamedEntityGraph)
```java
@Entity
@NamedEntityGraph(
    name = "User.withDepartment",
    attributeNodes = @NamedAttributeNode("department")
)
public class User { ... }
```

### 2. Repository com EntityGraph
```java
@EntityGraph(value = "User.withDepartment")
Optional<User> findByIdWithDepartment(@Param("id") Long id);
```

### 3. JOIN FETCH Explícito
```java
@Query("SELECT u FROM User u JOIN FETCH u.department WHERE u.id = :id")
Optional<User> findByIdWithDepartmentJoinFetch(@Param("id") Long id);
```

## 🧪 Como Testar

### 1. Activar Logs SQL
```properties
# application.properties
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

### 2. Endpoints para Comparação
```bash
# ❌ Versão com problema N+1
curl http://localhost:8080/api/bad/users/1

# ✅ Versão otimizada
curl http://localhost:8080/api/good/users/1
```

### 3. Observar Logs
- **Versão má**: Múltiplas queries separadas
- **Versão boa**: Uma única query com JOIN

## 📈 Métricas de Performance

O sistema inclui `PerformanceMonitor` que mede:
- ⏱️ Tempo de execução
- 🔢 Número de queries
- 💾 Dados transferidos

### Exemplo de Output:
```
❌ BAD: getUserById-bad-1 executou 3 queries em 45ms
✅ GOOD: getUserById-good-1 executou 1 query em 12ms
```

## 🎓 Conceitos Aprendidos

1. **Lazy Loading**: Vantagens e armadilhas
2. **EntityGraph**: Controlo explícito de carregamento
3. **JOIN FETCH**: Optimização via JPQL
4. **Performance Monitoring**: Como medir e comparar
5. **Fetch Strategies**: EAGER vs LAZY vs EntityGraph

## 🚀 Próximos Passos

Após dominar o problema N+1:
- `002-pagination`: Paginação eficiente
- `003-blob-management`: Gestão de dados pesados
- `004-entitygraph`: EntityGraphs avançados
- `005-dto-projections`: Projecções optimizadas

---

💡 **Dica**: Use sempre o PerformanceMonitor para comparar as abordagens e ver a diferença real!