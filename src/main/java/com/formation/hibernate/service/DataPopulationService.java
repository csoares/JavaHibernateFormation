package com.formation.hibernate.service;

import com.formation.hibernate.entity.*;
import com.formation.hibernate.repository.*;
import com.formation.hibernate.util.PerformanceMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 🎓 DATA POPULATION SERVICE - Demonstração Completa da Camada de Serviço
 * 
 * Esta classe representa um serviço empresarial e demonstra:
 * ✅ Arquitectura correcta de camada de serviço Spring
 * ✅ Injecção de dependências por construtor (boa prática)
 * ✅ Gestão transaccional com @Transactional
 * ✅ Processamento em lotes para grandes volumes (batch processing)
 * ✅ Gestão inteligente de memória e garbage collection
 * ✅ Logging estruturado para monitorização
 * ✅ Integração de métricas de performance
 * ✅ Separação clara de responsabilidades
 * ✅ Tratamento de erro e recuperação
 */

// ✅ BOA PRÁTICA: @Service marca como component de serviço Spring
// Automaticamente detectado pelo component scan
// Representa a camada de lógica de negócio
@Service
public class DataPopulationService {

    // ✅ BOA PRÁTICA: Logger estático final para eficiência
    // SLF4J abstrai implementação (Logback, Log4j, etc.)
    private static final Logger logger = LoggerFactory.getLogger(DataPopulationService.class);

    /*
     * 🎓 INJECÇÃO DE DEPENDÊNCIAS - Constructor Injection (BOA PRÁTICA)
     * 
     * ✅ Campos final garantem imutabilidade após construção
     * ✅ Constructor injection é preferível a @Autowired em campos
     * ✅ Facilita testes unitários (pode injectar mocks facilmente)
     * ✅ Falha rapidamente se dependências estão em falta
     * ✅ Torna dependências explícitas e obrigatórias
     * ✅ Suporta objectos imutáveis (final fields)
     */
    private final DepartmentRepository departmentRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PerformanceMonitor performanceMonitor;

    // ✅ BOA PRÁTICA: Random como field para reutilização
    // Não criar new Random() em cada método (ineficiente)
    private final Random random = new Random();

    // Dados de exemplo para geração
    private final String[] departmentNames = {
        "Tecnologia", "Vendas", "Marketing", "Recursos Humanos", "Financeiro",
        "Operações", "Suporte", "Desenvolvimento", "Design", "Qualidade"
    };

    private final String[] categoryNames = {
        "Eletrônicos", "Informática", "Casa e Jardim", "Esportes", "Livros",
        "Roupas", "Acessórios", "Automotivo", "Ferramentas", "Saúde"
    };

    private final String[] firstNames = {
        "João", "Maria", "José", "Ana", "Pedro", "Carla", "Paulo", "Fernanda",
        "Carlos", "Juliana", "Ricardo", "Patricia", "Antonio", "Luciana", "Marcos"
    };

    private final String[] lastNames = {
        "Silva", "Santos", "Oliveira", "Sousa", "Lima", "Pereira", "Costa",
        "Rodrigues", "Martins", "Jesus", "Rocha", "Ribeiro", "Alves", "Monteiro"
    };

    private final String[] productAdjectives = {
        "Premium", "Deluxe", "Pro", "Ultra", "Smart", "Eco", "Digital",
        "Professional", "Advanced", "Compact", "Wireless", "Portable"
    };

    private final String[] productNouns = {
        "Notebook", "Mouse", "Teclado", "Monitor", "Smartphone", "Tablet",
        "Fone", "Camera", "Impressora", "Roteador", "HD", "Memoria"
    };

    // ✅ BOA PRÁTICA: Constructor Injection (Spring 4.3+ não precisa @Autowired)
    // Spring automaticamente injeta as dependências pelo construtor
    // Mais testável que @Autowired em fields
    public DataPopulationService(DepartmentRepository departmentRepository,
                               CategoryRepository categoryRepository,
                               UserRepository userRepository,
                               ProductRepository productRepository,
                               OrderRepository orderRepository,
                               OrderItemRepository orderItemRepository,
                               PerformanceMonitor performanceMonitor) {
        this.departmentRepository = departmentRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.performanceMonitor = performanceMonitor;
    }

    /*
     * 🎓 MÉTODO PRINCIPAL - Demonstração Avançada de @Transactional
     */

    // ✅ BOA PRÁTICA: @Transactional no nível do método público
    // Garante que toda a operação está numa única transação (atomicidade)
    // Se qualquer parte falhar, tudo faz rollback automático
    // Spring cria proxy AOP que intercepta a chamada para gerir transacções
    // Isolation level padrão (READ_COMMITTED) é adequado para esta operação
    @Transactional
    public void populateDatabase() {
        logger.info("🚀 Iniciando população da base de dados...");

        // ✅ BOA PRÁTICA: Ordem correta para respeitar foreign keys
        // 1. Entidades sem dependências primeiro (Department, Category)
        // 2. Entidades que dependem das anteriores (User, Product)
        // 3. Entidades de relacionamento (Order, OrderItem)

        // 1. Criar departamentos (sem dependências)
        List<Department> departments = createDepartments();

        // 2. Criar categorias (sem dependências)
        List<Category> categories = createCategories();

        // 3. Criar usuários (depende de departments)
        createUsersInBatches(departments, 5000);

        // 4. Criar produtos (depende de categories)
        createProductsInBatches(categories, 2000);

        // 5. Criar pedidos (depende de users)
        createOrdersInBatches(5000, 10000);

        // 6. Criar itens dos pedidos (depende de orders e products)
        createOrderItemsInBatches();

        logger.info("✅ População da base de dados concluída!");
        performanceMonitor.printSummary();
    }

    private List<Department> createDepartments() {
        return performanceMonitor.measure("createDepartments",
            "Criar " + departmentNames.length + " departamentos",
            () -> {
                List<Department> departments = new ArrayList<>();
                for (String name : departmentNames) {
                    Department dept = new Department(
                        name,
                        "Departamento de " + name,
                        BigDecimal.valueOf(50000 + random.nextInt(450000)).doubleValue()
                    );
                    departments.add(dept);
                }
                return departmentRepository.saveAll(departments);
            });
    }

    private List<Category> createCategories() {
        return performanceMonitor.measure("createCategories",
            "Criar " + categoryNames.length + " categorias",
            () -> {
                List<Category> categories = new ArrayList<>();
                for (String name : categoryNames) {
                    Category category = new Category(
                        name,
                        "Categoria de produtos " + name
                    );
                    categories.add(category);
                }
                return categoryRepository.saveAll(categories);
            });
    }

    /*
     * 🎓 PROCESSAMENTO EM LOTES - Técnicas Profissionais de Batch Processing
     * 
     * Este método demonstra técnicas essenciais para processar grandes volumes:
     * ✅ Batch processing para eficiência de memória e base de dados
     * ✅ Logging de progresso para monitorização de operações longas
     * ✅ Gestão explícita de memória com garbage collection sugerido
     * ✅ Monitorização integrada de performance e métricas
     * ✅ Processamento resiliente com gestão de falhas
     * ✅ Optimização de transacções em lotes
     */
    private void createUsersInBatches(List<Department> departments, int count) {
        performanceMonitor.measure("createUsers",
            "Criar " + count + " usuários em lotes",
            () -> {
                // ✅ BOA PRÁTICA: Batch size otimizado
                // 500 é um bom equilíbrio entre performance e uso de memória
                // Muito pequeno: muitas transações
                // Muito grande: risco de OutOfMemoryError
                int batchSize = 500;
                
                // ✅ BOA PRÁTICA: Processamento em lotes
                for (int i = 0; i < count; i += batchSize) {
                    List<User> batch = new ArrayList<>();
                    int end = Math.min(i + batchSize, count);

                    // Criar objetos para este lote
                    for (int j = i; j < end; j++) {
                        String firstName = firstNames[random.nextInt(firstNames.length)];
                        String lastName = lastNames[random.nextInt(lastNames.length)];
                        
                        // ✅ BOA PRÁTICA: Garantir unicidade do email
                        String email = firstName.toLowerCase() + "." + lastName.toLowerCase() +
                                     "." + j + "@empresa.com";

                        // ✅ BOA PRÁTICA: Usar entidades já carregadas em memória
                        // Evita SELECT para cada department
                        Department randomDept = departments.get(random.nextInt(departments.size()));

                        User user = new User(firstName + " " + lastName, email, randomDept);
                        // ✅ BOA PRÁTICA: Dados realistas com variação temporal
                        user.setCreatedAt(LocalDateTime.now().minusDays(random.nextInt(365)));
                        batch.add(user);
                    }

                    // ✅ BOA PRÁTICA: saveAll() é mais eficiente que múltiplos save()
                    // Hibernate pode otimizar com batch inserts
                    userRepository.saveAll(batch);
                    
                    // ✅ BOA PRÁTICA: Logging de progresso para acompanhar operações longas
                    logger.info("Salvos {} usuários de {} total", end, count);

                    // ✅ BOA PRÁTICA: Gestão explícita de memória
                    // Liberta referências para permitir garbage collection
                    // Crítico em processamento de grandes volumes
                    batch.clear();
                    batch = null;
                    System.gc(); // Sugere GC (não força, mas ajuda)
                }
                return null;
            });
    }

    private void createProductsInBatches(List<Category> categories, int count) {
        performanceMonitor.measure("createProducts",
            "Criar " + count + " produtos com imagens em lotes",
            () -> {
                int batchSize = 250;
                for (int i = 0; i < count; i += batchSize) {
                    List<Product> batch = new ArrayList<>();
                    int end = Math.min(i + batchSize, count);

                    for (int j = i; j < end; j++) {
                        String adjective = productAdjectives[random.nextInt(productAdjectives.length)];
                        String noun = productNouns[random.nextInt(productNouns.length)];
                        String name = adjective + " " + noun + " " + (j + 1);

                        BigDecimal price = BigDecimal.valueOf(50 + random.nextInt(2000))
                            .add(BigDecimal.valueOf(random.nextInt(100)).divide(BigDecimal.valueOf(100)));

                        Category randomCategory = categories.get(random.nextInt(categories.size()));

                        Product product = new Product(
                            name,
                            "Descrição detalhada do produto " + name,
                            price,
                            10 + random.nextInt(500),
                            randomCategory
                        );

                        // Simular dados de imagem menores para evitar OutOfMemory
                        byte[] imageData = new byte[512 + random.nextInt(2048)]; // 0.5-2.5 KB
                        random.nextBytes(imageData);
                        product.setImageData(imageData);

                        batch.add(product);
                    }

                    productRepository.saveAll(batch);
                    logger.info("Salvos {} produtos de {} total", end, count);

                    // Força garbage collection
                    batch.clear();
                    batch = null;
                    System.gc();
                }
                return null;
            });
    }

    private void createOrdersInBatches(int userCount, int count) {
        performanceMonitor.measure("createOrders",
            "Criar " + count + " pedidos com PDFs em lotes",
            () -> {
                int batchSize = 200;
                for (int i = 0; i < count; i += batchSize) {
                    List<Order> batch = new ArrayList<>();
                    int end = Math.min(i + batchSize, count);

                    for (int j = i; j < end; j++) {
                        // Buscar usuário aleatório diretamente do banco para economizar memória
                        long randomUserId = 1 + random.nextInt(userCount);
                        User randomUser = userRepository.findById(randomUserId).orElse(null);

                        if (randomUser == null) continue;

                        Order order = new Order(
                            "ORD-" + String.format("%08d", j + 1),
                            BigDecimal.ZERO,
                            randomUser
                        );

                        order.setOrderDate(LocalDateTime.now().minusDays(random.nextInt(90)));
                        order.setStatus(Order.OrderStatus.values()[random.nextInt(Order.OrderStatus.values().length)]);

                        // Simular PDF menor para evitar OutOfMemory
                        byte[] pdfData = new byte[2048 + random.nextInt(8192)]; // 2-10 KB
                        random.nextBytes(pdfData);
                        order.setInvoicePdf(pdfData);

                        // Calcular total baseado em itens fictícios
                        BigDecimal total = BigDecimal.valueOf(100 + random.nextInt(2000))
                            .add(BigDecimal.valueOf(random.nextInt(100)).divide(BigDecimal.valueOf(100)));
                        order.setTotalAmount(total);

                        batch.add(order);
                    }

                    orderRepository.saveAll(batch);
                    logger.info("Salvos {} pedidos de {} total", end, count);

                    // Força garbage collection
                    batch.clear();
                    batch = null;
                    System.gc();
                }
                return null;
            });
    }

    private void createOrderItemsInBatches() {
        performanceMonitor.measure("createOrderItems",
            "Criar itens para todos os pedidos",
            () -> {
                long totalOrders = orderRepository.count();
                long totalProducts = productRepository.count();
                
                if (totalOrders == 0 || totalProducts == 0) {
                    logger.warn("⚠️ Não é possível criar order items: pedidos={}, produtos={}", totalOrders, totalProducts);
                    return null;
                }

                int batchSize = 200;
                int totalOrderItems = 0;
                
                // Processar pedidos em lotes
                for (int offset = 0; offset < totalOrders; offset += batchSize) {
                    List<Order> orders = orderRepository.findAll(
                        org.springframework.data.domain.PageRequest.of(offset / batchSize, batchSize)
                    ).getContent();
                    
                    List<OrderItem> orderItemsBatch = new ArrayList<>();
                    
                    for (Order order : orders) {
                        // Criar 1-5 itens por pedido
                        int itemCount = 1 + random.nextInt(5);
                        BigDecimal orderTotal = BigDecimal.ZERO;
                        
                        for (int i = 0; i < itemCount; i++) {
                            // Buscar produto aleatório
                            long randomProductId = 1 + random.nextInt((int) totalProducts);
                            Product randomProduct = productRepository.findById(randomProductId).orElse(null);
                            
                            if (randomProduct == null) continue;
                            
                            int quantity = 1 + random.nextInt(3); // 1-3 unidades
                            BigDecimal unitPrice = randomProduct.getPrice();
                            
                            OrderItem orderItem = new OrderItem(quantity, unitPrice, order, randomProduct);
                            orderItemsBatch.add(orderItem);
                            
                            orderTotal = orderTotal.add(orderItem.getTotalPrice());
                        }
                        
                        // Atualizar total do pedido baseado nos itens reais
                        order.setTotalAmount(orderTotal);
                    }
                    
                    // Salvar lote de order items
                    if (!orderItemsBatch.isEmpty()) {
                        orderItemRepository.saveAll(orderItemsBatch);
                        orderRepository.saveAll(orders); // Atualizar totais dos pedidos
                        totalOrderItems += orderItemsBatch.size();
                        
                        logger.info("Salvos {} order items (lote {}/{})", 
                            orderItemsBatch.size(), 
                            (offset / batchSize) + 1, 
                            (totalOrders + batchSize - 1) / batchSize);
                    }
                    
                    // Limpar memória
                    orderItemsBatch.clear();
                    orders.clear();
                    System.gc();
                }
                
                logger.info("✅ Criados {} order items no total", totalOrderItems);
                return null;
            });
    }

    @Transactional(readOnly = true)
    public void printDatabaseStatistics() {
        long departmentCount = departmentRepository.count();
        long categoryCount = categoryRepository.count();
        long userCount = userRepository.count();
        long productCount = productRepository.count();
        long orderCount = orderRepository.count();
        long orderItemCount = orderItemRepository.count();

        logger.info("📊 === ESTATÍSTICAS DA BASE DE DADOS ===");
        logger.info("   Departamentos: {}", departmentCount);
        logger.info("   Categorias: {}", categoryCount);
        logger.info("   Usuários: {}", userCount);
        logger.info("   Produtos: {}", productCount);
        logger.info("   Pedidos: {}", orderCount);
        logger.info("   Itens de Pedidos: {}", orderItemCount);
        logger.info("   Total de registros: {}", departmentCount + categoryCount + userCount + productCount + orderCount + orderItemCount);
        logger.info("📊 === FIM DAS ESTATÍSTICAS ===");
    }

    @Transactional
    public void clearDatabase() {
        performanceMonitor.measure("clearDatabase",
            "Limpar toda a base de dados",
            () -> {
                // Ordem importante: primeiro order_items, depois orders (foreign key)
                orderItemRepository.deleteAll();
                orderRepository.deleteAll();
                userRepository.deleteAll();
                productRepository.deleteAll();
                departmentRepository.deleteAll();
                categoryRepository.deleteAll();
                logger.info("🗑️ Base de dados limpa!");
                return null;
            });
    }
}