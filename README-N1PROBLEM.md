# 🎓 Branch 001: Problema N+1 - ULTRA SIMPLES

## 🎯 Objectivo
Demonstrar **APENAS** o problema N+1 da forma mais simples possível.

## 📦 O que contém este branch
- **2 entidades**: `User` e `Department`
- **2 repositórios**: `UserRepository` e `DepartmentRepository`  
- **1 controlador**: `N1ProblemController`
- **1 serviço**: `DataService` (apenas para criar dados)

**Removido**: DTOs, Converters, Performance Monitor, outras entidades, complexidades.

## ❓ O que é o Problema N+1?

```java
// 1. Uma query para buscar users
List<User> users = userRepository.findAll();

// 2. Para cada user, uma query extra para o department
for (User user : users) {
    String deptName = user.getDepartment().getName(); // ⚠️ QUERY EXTRA!
}
```

**Resultado: 1 + N queries!** 🐌

## 🚀 Como testar

### 1. Executar aplicação
```bash
mvn spring-boot:run
```

### 2. Popular dados
```bash
curl -X POST http://localhost:8080/api/data/populate
```

### 3. Ver logs SQL (IMPORTANTE!)
Adicione ao `application.properties`:
```properties
logging.level.org.hibernate.SQL=DEBUG
```

### 4. Testar problema N+1
```bash
# ❌ PROBLEMA: 2 queries para 1 user
curl "http://localhost:8080/api/n1-demo/bad/1"

# ❌ PROBLEMA: 1+N queries para N users  
curl "http://localhost:8080/api/n1-demo/batch-bad"
```

### 5. Testar soluções
```bash
# ✅ SOLUÇÃO EntityGraph: 1 query apenas
curl "http://localhost:8080/api/n1-demo/good-entitygraph/1"

# ✅ SOLUÇÃO JOIN FETCH: 1 query apenas
curl "http://localhost:8080/api/n1-demo/good-joinfetch/1"

# ✅ SOLUÇÃO em lote: 1 query para todos
curl "http://localhost:8080/api/n1-demo/batch-good"
```

## 🔍 Como identificar N+1 nos logs

### ❌ Problema (bad endpoints):
```sql
-- Query 1: Buscar user
SELECT u.* FROM users u WHERE u.id = ?

-- Query 2: Buscar department (EXTRA!)
SELECT d.* FROM departments d WHERE d.id = ?
```

### ✅ Solução (good endpoints):
```sql
-- Query ÚNICA com JOIN
SELECT u.*, d.* FROM users u 
LEFT JOIN departments d ON u.department_id = d.id 
WHERE u.id = ?
```

## 📚 Código-chave para estudar

### 1. Entidade User
```java
// EntityGraph para solução
@NamedEntityGraph(
    name = "User.withDepartment",
    attributeNodes = @NamedAttributeNode("department")
)

// Relacionamento LAZY (causa N+1)
@ManyToOne(fetch = FetchType.LAZY)
private Department department;
```

### 2. Repository com soluções
```java
// ❌ Problemático (herdado)
Optional<User> findById(Long id);

// ✅ Solução EntityGraph
@EntityGraph(value = "User.withDepartment")
Optional<User> findByIdWithDepartment(Long id);

// ✅ Solução JOIN FETCH
@Query("SELECT u FROM User u JOIN FETCH u.department WHERE u.id = :id")
Optional<User> findByIdWithDepartmentJoinFetch(Long id);
```

### 3. Controller com comparação directa
```java
// ❌ Problema
Optional<User> user = userRepository.findById(id);
user.get().getDepartment().getName(); // Query extra!

// ✅ Solução
Optional<User> user = userRepository.findByIdWithDepartment(id);
user.get().getDepartment().getName(); // Sem query extra!
```

## 🎯 Exercícios

1. **Execute** os endpoints e observe os logs SQL
2. **Conte** quantas queries são executadas em cada caso
3. **Compare** os tempos de resposta
4. **Teste** com mais dados (mude o DataService)

## 📊 Endpoints de teste

| Endpoint | Queries | Descrição |
|----------|---------|-----------|
| `/api/n1-demo/bad/1` | 2 | Problema N+1 básico |
| `/api/n1-demo/good-entitygraph/1` | 1 | Solução EntityGraph |
| `/api/n1-demo/good-joinfetch/1` | 1 | Solução JOIN FETCH |
| `/api/n1-demo/batch-bad` | 1+N | Problema em lote |
| `/api/n1-demo/batch-good` | 1 | Solução em lote |
| `/api/n1-demo/compare` | 0 | Resumo dos endpoints |

---

💡 **Conceito principal**: N+1 = 1 query inicial + N queries extras para relações LAZY