package com.formation.hibernate.util;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Massive Data Loader for Performance Demonstrations
 *
 * This loader creates a realistic dataset with:
 * - 1,000,000 users across multiple departments
 * - 500,000 products with 1MB blobs (images)
 * - 2,000,000 orders with order items
 * - Proper relationships and realistic data distribution
 *
 * Run with: --spring.profiles.active=data-loader
 */
@Component
@Profile("data-loader")
public class MassiveDataLoader implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final Random random = new Random(42); // Fixed seed for reproducibility
    private static final int BATCH_SIZE = 1000;

    // Data volume configuration
    private static final int DEPARTMENTS_COUNT = 100;
    private static final int USERS_COUNT = 1_000_000;
    private static final int CATEGORIES_COUNT = 50;
    private static final int PRODUCTS_COUNT = 500_000;
    private static final int PRODUCTS_WITH_BLOBS = 10_000; // Only 10K products with actual blobs to save space
    private static final int ORDERS_COUNT = 500_000;
    private static final int AVG_ITEMS_PER_ORDER = 4;

    // Blob configuration
    private static final int BLOB_SIZE = 1024 * 1024; // 1MB

    public MassiveDataLoader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("========================================");
        System.out.println("MASSIVE DATA LOADER STARTING");
        System.out.println("========================================");
        System.out.println("This will create:");
        System.out.println("- " + DEPARTMENTS_COUNT + " departments");
        System.out.println("- " + String.format("%,d", USERS_COUNT) + " users");
        System.out.println("- " + CATEGORIES_COUNT + " categories");
        System.out.println("- " + String.format("%,d", PRODUCTS_COUNT) + " products");
        System.out.println("- " + String.format("%,d", PRODUCTS_WITH_BLOBS) + " products with 1MB blobs");
        System.out.println("- " + String.format("%,d", ORDERS_COUNT) + " orders");
        System.out.println("========================================\n");

        long startTime = System.currentTimeMillis();

        loadDepartments();
        loadUsers();
        loadCategories();
        loadProducts();
        loadOrders();

        long endTime = System.currentTimeMillis();
        long duration = (endTime - startTime) / 1000;

        System.out.println("\n========================================");
        System.out.println("DATA LOADING COMPLETED!");
        System.out.println("Total time: " + duration + " seconds");
        System.out.println("========================================");

        printStatistics();
    }

    private void loadDepartments() {
        System.out.println("\n[1/5] Loading departments...");
        String sql = "INSERT INTO departments (name) VALUES (?)";

        jdbcTemplate.batchUpdate(sql,
            new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws java.sql.SQLException {
                    ps.setString(1, "Department " + (i + 1));
                }

                @Override
                public int getBatchSize() {
                    return DEPARTMENTS_COUNT;
                }
            });

        System.out.println("✓ Loaded " + DEPARTMENTS_COUNT + " departments");
    }

    @Transactional
    private void loadUsers() {
        System.out.println("\n[2/5] Loading users (this will take a while)...");
        String sql = "INSERT INTO users (name, email, department_id) VALUES (?, ?, ?)";

        AtomicInteger counter = new AtomicInteger(0);
        int batches = USERS_COUNT / BATCH_SIZE;

        for (int batch = 0; batch < batches; batch++) {
            final int batchNum = batch;

            jdbcTemplate.batchUpdate(sql,
                new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws java.sql.SQLException {
                        int userNum = batchNum * BATCH_SIZE + i + 1;
                        ps.setString(1, "User " + userNum);
                        ps.setString(2, "user" + userNum + "@formation.com");
                        ps.setLong(3, (userNum % DEPARTMENTS_COUNT) + 1);
                    }

                    @Override
                    public int getBatchSize() {
                        return BATCH_SIZE;
                    }
                });

            counter.addAndGet(BATCH_SIZE);
            if (counter.get() % 10000 == 0) {
                System.out.println("  Progress: " + String.format("%,d", counter.get()) + " / " + String.format("%,d", USERS_COUNT) + " users");
            }
        }

        System.out.println("✓ Loaded " + String.format("%,d", USERS_COUNT) + " users");
    }

    private void loadCategories() {
        System.out.println("\n[3/5] Loading categories...");
        String sql = "INSERT INTO categories (name, description) VALUES (?, ?)";

        jdbcTemplate.batchUpdate(sql,
            new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws java.sql.SQLException {
                    ps.setString(1, "Category " + (i + 1));
                    ps.setString(2, "Description for category " + (i + 1));
                }

                @Override
                public int getBatchSize() {
                    return CATEGORIES_COUNT;
                }
            });

        System.out.println("✓ Loaded " + CATEGORIES_COUNT + " categories");
    }

    @Transactional
    private void loadProducts() {
        System.out.println("\n[4/5] Loading products...");
        String sql = "INSERT INTO products (name, description, price, stock_quantity, category_id, image_data) VALUES (?, ?, ?, ?, ?, ?)";

        AtomicInteger counter = new AtomicInteger(0);
        int batches = PRODUCTS_COUNT / BATCH_SIZE;

        // Pre-generate a 1MB blob to reuse
        byte[] blobData = new byte[BLOB_SIZE];
        random.nextBytes(blobData);

        for (int batch = 0; batch < batches; batch++) {
            final int batchNum = batch;

            jdbcTemplate.batchUpdate(sql,
                new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws java.sql.SQLException {
                        int productNum = batchNum * BATCH_SIZE + i + 1;
                        ps.setString(1, "Product " + productNum);
                        ps.setString(2, "Description for product " + productNum);
                        ps.setBigDecimal(3, BigDecimal.valueOf(10 + random.nextDouble() * 990));
                        ps.setInt(4, random.nextInt(1000) + 1);
                        ps.setLong(5, (random.nextInt(CATEGORIES_COUNT)) + 1);

                        // Only add blob to first PRODUCTS_WITH_BLOBS products
                        if (productNum <= PRODUCTS_WITH_BLOBS) {
                            ps.setBytes(6, blobData);
                        } else {
                            ps.setBytes(6, null);
                        }
                    }

                    @Override
                    public int getBatchSize() {
                        return BATCH_SIZE;
                    }
                });

            counter.addAndGet(BATCH_SIZE);
            if (counter.get() % 10000 == 0) {
                System.out.println("  Progress: " + String.format("%,d", counter.get()) + " / " + String.format("%,d", PRODUCTS_COUNT) + " products");
            }
        }

        System.out.println("✓ Loaded " + String.format("%,d", PRODUCTS_COUNT) + " products");
        System.out.println("  (with " + String.format("%,d", PRODUCTS_WITH_BLOBS) + " products containing 1MB blobs)");
    }

    @Transactional
    private void loadOrders() {
        System.out.println("\n[5/5] Loading orders with items...");
        String orderSql = "INSERT INTO orders (order_number, order_date, status, total_amount, user_id) VALUES (?, ?, ?, ?, ?)";
        String itemSql = "INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?)";

        AtomicInteger counter = new AtomicInteger(0);
        String[] statuses = {"PENDING", "COMPLETED", "SHIPPED", "CANCELLED"};

        int batches = ORDERS_COUNT / BATCH_SIZE;

        for (int batch = 0; batch < batches; batch++) {
            final int batchNum = batch;

            // Insert orders
            jdbcTemplate.batchUpdate(orderSql,
                new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws java.sql.SQLException {
                        int orderNum = batchNum * BATCH_SIZE + i + 1;
                        ps.setString(1, "ORD-" + String.format("%010d", orderNum));
                        ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().minusDays(random.nextInt(365))));
                        ps.setString(3, statuses[random.nextInt(statuses.length)]);
                        ps.setBigDecimal(4, BigDecimal.valueOf(50 + random.nextDouble() * 950));
                        ps.setLong(5, (random.nextInt(USERS_COUNT)) + 1);
                    }

                    @Override
                    public int getBatchSize() {
                        return BATCH_SIZE;
                    }
                });

            // Insert order items for this batch
            int itemsPerBatch = BATCH_SIZE * AVG_ITEMS_PER_ORDER;
            jdbcTemplate.batchUpdate(itemSql,
                new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws java.sql.SQLException {
                        int orderIndex = i / AVG_ITEMS_PER_ORDER;
                        long orderId = batchNum * BATCH_SIZE + orderIndex + 1;

                        ps.setLong(1, orderId);
                        ps.setLong(2, (random.nextInt(PRODUCTS_COUNT)) + 1);
                        ps.setInt(3, random.nextInt(5) + 1);
                        ps.setBigDecimal(4, BigDecimal.valueOf(10 + random.nextDouble() * 90));
                    }

                    @Override
                    public int getBatchSize() {
                        return itemsPerBatch;
                    }
                });

            counter.addAndGet(BATCH_SIZE);
            if (counter.get() % 10000 == 0) {
                System.out.println("  Progress: " + String.format("%,d", counter.get()) + " / " + String.format("%,d", ORDERS_COUNT) + " orders");
            }
        }

        System.out.println("✓ Loaded " + String.format("%,d", ORDERS_COUNT) + " orders with ~" + String.format("%,d", ORDERS_COUNT * AVG_ITEMS_PER_ORDER) + " items");
    }

    private void printStatistics() {
        System.out.println("\nDatabase Statistics:");
        System.out.println("-------------------");

        printTableCount("departments");
        printTableCount("users");
        printTableCount("categories");
        printTableCount("products");
        printTableCount("orders");
        printTableCount("order_items");

        // Print database size
        Long dbSize = jdbcTemplate.queryForObject(
            "SELECT pg_database_size('hibernate_formation')", Long.class);
        if (dbSize != null) {
            System.out.println("\nDatabase size: " + formatBytes(dbSize));
        }

        // Print blob statistics
        Long blobSize = jdbcTemplate.queryForObject(
            "SELECT SUM(pg_column_size(image_data)) FROM products WHERE image_data IS NOT NULL",
            Long.class);
        if (blobSize != null) {
            System.out.println("Total BLOB storage: " + formatBytes(blobSize));
        }
    }

    private void printTableCount(String tableName) {
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + tableName, Long.class);
        System.out.println("  " + tableName + ": " + String.format("%,d", count) + " records");
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
