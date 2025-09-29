package com.formation.hibernate.controller;

import com.formation.hibernate.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
public class TestController {

    private static final Logger logger = LoggerFactory.getLogger(TestController.class);

    private final DepartmentRepository departmentRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public TestController(DepartmentRepository departmentRepository,
                         CategoryRepository categoryRepository,
                         UserRepository userRepository,
                         ProductRepository productRepository,
                         OrderRepository orderRepository) {
        this.departmentRepository = departmentRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }


    @GetMapping("/quick-stats")
    @Transactional(readOnly = true)
    public ResponseEntity<String> getQuickStats() {
        long deptCount = departmentRepository.count();
        long catCount = categoryRepository.count();
        long userCount = userRepository.count();
        long productCount = productRepository.count();
        long orderCount = orderRepository.count();

        String stats = String.format(
            "📊 Estatísticas: %d departamentos, %d categorias, %d usuários, %d produtos, %d pedidos",
            deptCount, catCount, userCount, productCount, orderCount
        );

        logger.info(stats);
        return ResponseEntity.ok(stats);
    }

    @DeleteMapping("/clear")
    @Transactional
    public ResponseEntity<String> clearDatabase() {
        try {
            orderRepository.deleteAll();
            userRepository.deleteAll();
            productRepository.deleteAll();
            departmentRepository.deleteAll();
            categoryRepository.deleteAll();

            logger.info("🗑️ Base de dados limpa!");
            return ResponseEntity.ok("🗑️ Base de dados limpa com sucesso!");
        } catch (Exception e) {
            logger.error("❌ Erro ao limpar base de dados", e);
            return ResponseEntity.internalServerError()
                .body("❌ Erro: " + e.getMessage());
        }
    }
}