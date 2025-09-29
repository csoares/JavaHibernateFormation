# 🎓 Branch 002: Paginação - Tutorial Prático

## 🎯 Objectivo Educacional
Este branch foca **exclusivamente** na **paginação eficiente** - essencial para aplicações que lidam com grandes volumes de dados.

## ❓ Por que Paginação é Crítica?

### 🚨 Problemas sem Paginação:
1. **OutOfMemoryError**: Carregar 100.000 registros na memória
2. **Timeout**: Queries que demoram minutos
3. **Rede Saturada**: Transferir GB de dados desnecessários
4. **UX Terrível**: Utilizador espera eternamente

### 📊 Exemplo Problemático:
```java
// ❌ PERIGO: Carrega TODOS os registos!
List<User> allUsers = userRepository.findAll(); // 100.000 users = CRASH!
```

## 🛠️ Soluções de Paginação

### ✅ Spring Data Pagination
```java
// Página 0, 20 itens, ordenado por nome
Pageable pageable = PageRequest.of(0, 20, Sort.by("name"));
Page<User> page = userRepository.findAll(pageable);
```

### 📄 Informações da Página:
```java
page.getContent();           // Lista de 20 users
page.getTotalElements();     // Total: 100.000
page.getTotalPages();        // Total: 5.000 páginas
page.getNumber();            // Página actual: 0
page.getSize();              // Tamanho: 20
page.isFirst();              // true
page.isLast();               // false
page.hasNext();              // true
```

## 🏗️ Implementações Demonstradas

### 1. Paginação Básica
```java
@GetMapping
public Page<UserDto> getUsers(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size) {
    
    Pageable pageable = PageRequest.of(page, size);
    return userRepository.findAll(pageable).map(userConverter::toDto);
}
```

### 2. Paginação com Ordenação
```java
// Ordenação múltipla: nome ASC, depois email DESC
Sort sort = Sort.by(
    Sort.Order.asc("name"),
    Sort.Order.desc("email")
);
Pageable pageable = PageRequest.of(page, size, sort);
```

### 3. Paginação com Filtros
```java
public Page<User> searchUsers(String name, Pageable pageable) {
    return userRepository.findByNameContainingIgnoreCase(name, pageable);
}
```

### 4. Paginação + EntityGraph (Evita N+1)
```java
@EntityGraph(value = "User.withDepartment")
Page<User> findAll(Pageable pageable);
```

## 📈 Comparação de Performance

### ❌ Sem Paginação:
```sql
-- Query única mas MASSIVA
SELECT * FROM users u 
LEFT JOIN departments d ON u.department_id = d.id;
-- Resultado: 100.000 registos, 500MB transferidos, 30 segundos
```

### ✅ Com Paginação:
```sql
-- Query pequena e rápida
SELECT * FROM users u 
LEFT JOIN departments d ON u.department_id = d.id
LIMIT 20 OFFSET 0;
-- Resultado: 20 registos, 10KB transferidos, 5ms
```

## 🧪 Como Testar

### 1. Popular Base de Dados
```bash
# Criar muitos dados para testar paginação
curl -X POST http://localhost:8080/api/data/populate
```

### 2. Endpoints para Comparação
```bash
# ❌ Versão sem paginação (PERIGOSO!)
curl "http://localhost:8080/api/pagination-demo/bad/all"

# ✅ Versão com paginação
curl "http://localhost:8080/api/pagination-demo/good?page=0&size=10"

# ✅ Com ordenação
curl "http://localhost:8080/api/pagination-demo/good?page=0&size=10&sortBy=name&sortDir=desc"

# ✅ Com filtro
curl "http://localhost:8080/api/pagination-demo/search?name=João&page=0&size=5"
```

### 3. Observar Diferenças
- **Sem paginação**: Logs mostram tempo alto e muitos dados
- **Com paginação**: Resposta rápida e controlada

## 📊 Tipos de Paginação

### 1. **Offset-based** (Padrão)
```java
// Página 10, 20 itens = OFFSET 200 LIMIT 20
PageRequest.of(10, 20)
```
**Vantagens**: Simples, permite saltar páginas
**Desvantagens**: Lento em páginas altas (OFFSET grande)

### 2. **Cursor-based** (Para datasets muito grandes)
```java
// Próxima página baseada no último ID visto
@Query("SELECT u FROM User u WHERE u.id > :lastId ORDER BY u.id")
List<User> findNextPage(@Param("lastId") Long lastId, Pageable pageable);
```
**Vantagens**: Performance constante
**Desvantagens**: Mais complexo, não permite saltar páginas

## 🎨 Frontend Integration

### React/JavaScript Example:
```javascript
const [currentPage, setCurrentPage] = useState(0);
const [users, setUsers] = useState({ content: [], totalPages: 0 });

// Fetch page
fetch(`/api/pagination-demo/good?page=${currentPage}&size=20`)
  .then(res => res.json())
  .then(data => setUsers(data));

// Navigation
const nextPage = () => setCurrentPage(prev => 
  prev < users.totalPages - 1 ? prev + 1 : prev
);
```

## 🔧 Configuração e Optimizações

### 1. Configuração Global
```properties
# application.properties
spring.data.web.pageable.default-page-size=20
spring.data.web.pageable.max-page-size=100
spring.data.web.pageable.page-parameter=page
spring.data.web.pageable.size-parameter=size
```

### 2. Validação de Entrada
```java
@GetMapping
public Page<UserDto> getUsers(
    @RequestParam(defaultValue = "0") @Min(0) int page,
    @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    // Implementação...
}
```

### 3. Cache de Contagens
```java
// Para contagens caras, usar cache
@Cacheable("user-count")
public long countUsers() {
    return userRepository.count();
}
```

## 🎓 Conceitos Aprendidos

1. **Page vs List**: Quando usar cada um
2. **Pageable**: Configuração flexível de paginação
3. **Sort**: Ordenação múltipla e direccional
4. **Performance**: OFFSET vs CURSOR pagination
5. **UX**: Como implementar navegação eficiente
6. **Segurança**: Validação de parâmetros de entrada

## 🚀 Próximos Passos

Após dominar paginação:
- `003-blob-management`: Gestão de dados pesados
- `004-entitygraph`: EntityGraphs avançados  
- `005-dto-projections`: Projecções optimizadas

---

💡 **Dica**: Sempre use paginação em produção! Mesmo que pareça desnecessário agora, os dados crescem rapidamente.