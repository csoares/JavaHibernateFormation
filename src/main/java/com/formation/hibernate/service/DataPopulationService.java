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

@Service
public class DataPopulationService {

    private static final Logger logger = LoggerFactory.getLogger(DataPopulationService.class);

    private final DepartmentRepository departmentRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PerformanceMonitor performanceMonitor;

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

    public DataPopulationService(DepartmentRepository departmentRepository,
                               CategoryRepository categoryRepository,
                               UserRepository userRepository,
                               ProductRepository productRepository,
                               OrderRepository orderRepository,
                               PerformanceMonitor performanceMonitor) {
        this.departmentRepository = departmentRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.performanceMonitor = performanceMonitor;
    }

    @Transactional
    public void populateDatabase() {
        logger.info("🚀 Iniciando população da base de dados...");

        // 1. Criar departamentos
        List<Department> departments = createDepartments();

        // 2. Criar categorias
        List<Category> categories = createCategories();

        // 3. Criar usuários (5.000 - reduzido para evitar OutOfMemory)
        createUsersInBatches(departments, 5000);

        // 4. Criar produtos (2.000 - reduzido para evitar OutOfMemory)
        createProductsInBatches(categories, 2000);

        // 5. Criar pedidos (10.000 - reduzido para evitar OutOfMemory)
        createOrdersInBatches(5000, 10000);

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

    private void createUsersInBatches(List<Department> departments, int count) {
        performanceMonitor.measure("createUsers",
            "Criar " + count + " usuários em lotes",
            () -> {
                int batchSize = 500;
                for (int i = 0; i < count; i += batchSize) {
                    List<User> batch = new ArrayList<>();
                    int end = Math.min(i + batchSize, count);

                    for (int j = i; j < end; j++) {
                        String firstName = firstNames[random.nextInt(firstNames.length)];
                        String lastName = lastNames[random.nextInt(lastNames.length)];
                        String email = firstName.toLowerCase() + "." + lastName.toLowerCase() +
                                     "." + j + "@empresa.com";

                        Department randomDept = departments.get(random.nextInt(departments.size()));

                        User user = new User(firstName + " " + lastName, email, randomDept);
                        user.setCreatedAt(LocalDateTime.now().minusDays(random.nextInt(365)));
                        batch.add(user);
                    }

                    userRepository.saveAll(batch);
                    logger.info("Salvos {} usuários de {} total", end, count);

                    // Força garbage collection
                    batch.clear();
                    batch = null;
                    System.gc();
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

    @Transactional(readOnly = true)
    public void printDatabaseStatistics() {
        long departmentCount = departmentRepository.count();
        long categoryCount = categoryRepository.count();
        long userCount = userRepository.count();
        long productCount = productRepository.count();
        long orderCount = orderRepository.count();

        logger.info("📊 === ESTATÍSTICAS DA BASE DE DADOS ===");
        logger.info("   Departamentos: {}", departmentCount);
        logger.info("   Categorias: {}", categoryCount);
        logger.info("   Usuários: {}", userCount);
        logger.info("   Produtos: {}", productCount);
        logger.info("   Pedidos: {}", orderCount);
        logger.info("   Total de registros: {}", departmentCount + categoryCount + userCount + productCount + orderCount);
        logger.info("📊 === FIM DAS ESTATÍSTICAS ===");
    }

    @Transactional
    public void clearDatabase() {
        performanceMonitor.measure("clearDatabase",
            "Limpar toda a base de dados",
            () -> {
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