# 🎓 Branch 003: Gestão de BLOBs - Tutorial Prático

## 🎯 Objectivo Educacional
Este branch foca **exclusivamente** na **gestão eficiente de BLOBs** (Binary Large Objects) - crucial para aplicações que lidam com ficheiros, imagens, documentos, etc.

## ❓ O que são BLOBs e Por que São Problemáticos?

### 📁 Tipos de BLOBs Comuns:
- **Imagens**: JPG, PNG, GIF (KB a MB)
- **Documentos**: PDF, Word, Excel (MB)
- **Vídeos**: MP4, AVI (GB)
- **Ficheiros**: ZIP, executáveis (variável)

### 🚨 Problemas com BLOBs:
1. **OutOfMemoryError**: 1000 PDFs de 1MB = 1GB na memória!
2. **Performance Degradada**: Transferir dados desnecessários
3. **Timeout**: Queries lentas por causa de dados pesados
4. **Rede Saturada**: Bandwidth desperdiçado

### 📊 Exemplo Problemático:
```java
// ❌ PERIGO: Carrega TODOS os Orders com PDFs!
List<Order> orders = orderRepository.findAll();
// Resultado: 10.000 orders × 1MB PDF = 10GB RAM!
```

## 🛠️ Estratégias de Gestão de BLOBs

### 1. **Lazy Loading** (Padrão)
```java
@Lob
@Basic(fetch = FetchType.LAZY)
private byte[] invoicePdf;
```
**Vantagem**: Só carrega quando acessado
**Desvantagem**: Pode causar queries extra

### 2. **Projecções Sem BLOBs**
```java
// Carrega tudo MENOS o BLOB
@Query("SELECT o.id, o.orderNumber, o.totalAmount FROM Order o")
List<Object[]> findOrdersWithoutBlobs();
```

### 3. **EntityGraphs Específicos**
```java
// Carrega relações mas NÃO BLOBs
@NamedEntityGraph(
    name = "Order.withoutBlobs",
    attributeNodes = {
        @NamedAttributeNode("user"),
        @NamedAttributeNode("orderItems")
    }
    // Nota: invoicePdf NÃO incluído
)
```

### 4. **Endpoints Dedicados para BLOBs**
```java
// Endpoint separado só para o PDF
@GetMapping("/{id}/pdf")
public ResponseEntity<byte[]> getOrderPdf(@PathVariable Long id) {
    // Carrega APENAS o BLOB quando necessário
}
```

## 🏗️ Implementações Demonstradas

### ❌ Versão Problemática
```java
// PROBLEMA: findAll() carrega TODOS os BLOBs
List<Order> orders = orderRepository.findAll();
for (Order order : orders) {
    // Cada PDF é carregado desnecessariamente
    boolean hasPdf = order.getInvoicePdf() != null;
}
```

### ✅ Versão Optimizada
```java
// SOLUÇÃO: Projecção sem BLOBs
List<Object[]> orders = orderRepository.findOrdersWithoutBlobData();
// Resultado: Dados essenciais sem desperdício de memória
```

## 📈 Comparação de Performance

### ❌ Com BLOBs Desnecessários:
```sql
-- Query carrega TUDO incluindo BLOBs
SELECT o.*, o.invoice_pdf FROM orders o;
-- Resultado: 10.000 rows × 1MB = 10GB transferidos
```

### ✅ Sem BLOBs:
```sql
-- Query exclui BLOBs explicitamente
SELECT o.id, o.order_number, o.total_amount FROM orders o;
-- Resultado: 10.000 rows × 100 bytes = 1MB transferidos
```

## 🧪 Como Testar

### 1. Popular com BLOBs
```bash
# Criar pedidos com PDFs para testar
curl -X POST http://localhost:8080/api/data/populate-with-blobs
```

### 2. Endpoints para Comparação
```bash
# ❌ Versão que carrega BLOBs (PERIGOSO!)
curl "http://localhost:8080/api/blob-demo/bad/all-with-blobs"

# ✅ Versão sem BLOBs
curl "http://localhost:8080/api/blob-demo/good/all-without-blobs"

# ✅ BLOB específico quando necessário
curl "http://localhost:8080/api/blob-demo/order/1/pdf" --output order1.pdf

# ✅ Listagem eficiente
curl "http://localhost:8080/api/blob-demo/list-efficient"
```

### 3. Monitorização
```bash
# Ver diferenças de performance
curl "http://localhost:8080/api/blob-demo/performance-summary"
```

## 🎨 Padrões de Design para BLOBs

### 1. **Separação de Responsabilidades**
```java
// Entidade principal SEM BLOB
public class OrderSummary {
    private Long id;
    private String orderNumber;
    private BigDecimal totalAmount;
    // Sem invoicePdf
}

// Entidade separada SÓ para BLOB
public class OrderDocument {
    private Long orderId;
    private byte[] invoicePdf;
    private String contentType;
}
```

### 2. **Lazy Loading Inteligente**
```java
@Entity
public class Order {
    // Dados essenciais (sempre carregados)
    private String orderNumber;
    private BigDecimal totalAmount;
    
    // BLOB (só carregado quando necessário)
    @Lob
    @Basic(fetch = FetchType.LAZY)
    private byte[] invoicePdf;
}
```

### 3. **Streaming para Ficheiros Grandes**
```java
@GetMapping("/{id}/pdf")
public ResponseEntity<Resource> streamOrderPdf(@PathVariable Long id) {
    byte[] pdf = orderService.getOrderPdf(id);
    ByteArrayResource resource = new ByteArrayResource(pdf);
    
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .contentLength(pdf.length)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=order.pdf")
        .body(resource);
}
```

## 🔧 Optimizações Avançadas

### 1. **Compressão**
```java
// Comprimir antes de guardar
public void saveOrderPdf(Long orderId, byte[] pdf) {
    byte[] compressed = compress(pdf);
    // Guardar compressed em vez de pdf original
}
```

### 2. **Armazenamento Externo**
```java
// Em vez de BLOB na BD, guardar em filesystem/cloud
public class Order {
    private String orderNumber;
    private String pdfPath; // Caminho para o ficheiro
    // Sem byte[] invoicePdf
}
```

### 3. **Cache Inteligente**
```java
@Cacheable("order-pdfs")
public byte[] getOrderPdf(Long orderId) {
    // Cache só para PDFs frequentemente acedidos
}
```

## 📊 Métricas e Monitorização

### Indicadores a Vigiar:
- **Heap Memory Usage**: Detectar memory leaks
- **Query Duration**: BLOBs deixam queries lentas
- **Network I/O**: Transferência desnecessária de dados
- **Database Size**: Crescimento descontrolado

### Alertas Críticos:
```java
// Detectar queries que carregam muitos BLOBs
if (queryResultSize > 50_000_000) { // 50MB
    logger.warn("Query carregou {}MB de BLOBs!", queryResultSize / 1_000_000);
}
```

## 🎓 Conceitos Aprendidos

1. **BLOB vs VARCHAR**: Quando usar cada um
2. **FetchType.LAZY**: Como funciona e quando falha
3. **Projecções**: Seleccionar dados específicos
4. **EntityGraph**: Controlo fino de carregamento
5. **Streaming**: Transferir ficheiros grandes eficientemente
6. **Resource Management**: Evitar memory leaks

## 🚀 Próximos Passos

Após dominar gestão de BLOBs:
- `004-entitygraph`: EntityGraphs avançados
- `005-dto-projections`: Projecções optimizadas
- `001-n1problem`: Revisitar com conhecimento de BLOBs

---

💡 **Dica Crucial**: Nunca carregue BLOBs desnecessariamente! Trate-os como recursos preciosos e caros.