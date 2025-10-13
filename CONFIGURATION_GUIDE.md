# Configuration Files Guide

This document explains the purpose of each application properties file and when to use them.

## 📁 Configuration Files Overview

### 1. `application.properties` - Default Development Profile
**Purpose:** Local development with in-memory H2 database
**Use When:** Quick local testing without Docker

**Features:**
- H2 in-memory database (no setup required)
- H2 Console enabled at `/h2-console`
- DDL auto-generation (create-drop)
- Debug logging enabled
- Actuator endpoints exposed

**Run with:**
```bash
mvn spring-boot:run
# or
java -jar target/hibernate-performance.jar
```

**Access H2 Console:**
- URL: http://localhost:8080/h2-console
- JDBC URL: jdbc:h2:mem:testdb
- Username: sa
- Password: password

---

### 2. `application-docker.properties` - Docker PostgreSQL Development
**Purpose:** Development/demos with PostgreSQL in Docker
**Use When:** Running demos, testing with realistic database

**Features:**
- Connects to PostgreSQL in docker-compose
- SQL logging enabled (DEBUG) for learning
- Shows formatted SQL with comments
- Validates schema (no auto DDL)
- Full statistics and query logging

**Run with:**
```bash
# 1. Start PostgreSQL
docker-compose up -d postgresql

# 2. Run application
mvn spring-boot:run -Dspring-boot.run.profiles=docker

# 3. Application connects to postgres://localhost:5432/hibernate_formation
```

**Best for:**
- Running performance demos
- Teaching SQL query patterns
- Debugging Hibernate behavior
- Testing with realistic data volumes

---

### 3. `application-data-loader.properties` - Bulk Data Population
**Purpose:** Load 1M+ records into PostgreSQL for performance testing
**Use When:** Populating the demo database with massive datasets

**Features:**
- Optimized for bulk inserts
- Large connection pool (10 connections)
- High batch sizes (1000 per batch)
- Minimal logging (performance focus)
- Ordered inserts/updates

**Run with:**
```bash
# 1. Ensure PostgreSQL is running
docker-compose up -d postgresql

# 2. Run data loader
mvn spring-boot:run -Dspring-boot.run.profiles=data-loader

# 3. Populates:
#    - 1,100,000 users
#    - 600,000 products (10,000 with 1MB blobs)
#    - 600,000 orders with 2M items
#    - Total: ~11GB database
```

**Expected time:** 2-5 minutes depending on hardware

---

### 4. `application-bad-performance.properties` - Educational (Anti-Patterns)
**Purpose:** Demonstrate what NOT to do
**Use When:** Teaching students about performance pitfalls

**Features:**
- ❌ Minimal connection pool (2 connections max)
- ❌ No batching (batch_size=1)
- ❌ Excessive logging (DEBUG + TRACE)
- ❌ Poor fetch strategies
- ❌ Unordered inserts/updates

**Run with:**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=bad-performance
```

**Educational Value:**
- Shows 100-1000x performance degradation
- Each setting has detailed comments explaining WHY it's bad
- Real-world impact examples (e.g., "1M users = 12 hours")
- Perfect for side-by-side comparisons with good-performance

**Demo Scenario:**
1. Run with bad-performance profile
2. Time the operations (very slow!)
3. Switch to good-performance profile
4. Time again (dramatically faster!)
5. Discuss the differences

---

### 5. `application-good-performance.properties` - Production Best Practices
**Purpose:** Production-ready, optimized configuration
**Use When:** Production deployment or teaching optimization

**Features:**
- ✅ Proper connection pool (50 max, 10 idle)
- ✅ Optimal batching (batch_size=50)
- ✅ Efficient fetch strategies (fetch_size=100)
- ✅ Batch fetch size (16) to reduce N+1
- ✅ Ordered inserts/updates
- ✅ Minimal logging (WARN level)
- ✅ Connection leak detection

**Run with:**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=good-performance
```

**Educational Value:**
- Shows 100-1000x improvement over bad config
- Comprehensive comments explaining WHY each setting is optimal
- Real-world performance gains (e.g., "1M users = 30 seconds")
- Includes tuning guidelines for specific workloads

**Production Ready:**
- Can be used as template for real applications
- Includes monitoring and leak detection
- Balanced for both read and write operations
- Thoroughly documented with rules of thumb

---

## 🎓 Teaching Strategy

### For Students: Side-by-Side Comparison

**Lesson 1: Connection Pool Impact**
```bash
# Terminal 1: Bad config
mvn spring-boot:run -Dspring-boot.run.profiles=bad-performance

# Terminal 2: Test endpoint
time curl "http://localhost:8080/api/users?limit=1000"
# Result: ~5000ms with 2 connections

# Stop and restart with good config
mvn spring-boot:run -Dspring-boot.run.profiles=good-performance

# Terminal 2: Same test
time curl "http://localhost:8080/api/users?limit=1000"
# Result: ~200ms with 50 connections
```

**Lesson 2: Batching Impact**
```bash
# Use data-loader to insert 10K users
# Bad config: ~2 minutes (10,000 individual inserts)
# Good config: ~3 seconds (200 batches of 50)
```

**Lesson 3: Logging Overhead**
```bash
# Compare log file sizes after same operations:
# Bad config: 500MB logs (DEBUG + TRACE)
# Good config: 5MB logs (WARN level)
```

---

## 📊 Performance Comparison Matrix

| Aspect | Default | Docker | Data Loader | Bad | Good |
|--------|---------|--------|-------------|-----|------|
| Database | H2 Memory | PostgreSQL | PostgreSQL | PostgreSQL | PostgreSQL |
| Purpose | Quick Dev | Demos | Bulk Load | Anti-Pattern | Production |
| Conn Pool | Default | Default | 10 | 2 | 50 |
| Batch Size | Default | Default | 1000 | 1 | 50 |
| Logging | DEBUG | DEBUG | WARN | TRACE | WARN |
| Use Case | Local Test | Teaching | Setup | Bad Example | Real App |

---

## 🚀 Quick Start Scenarios

### Scenario 1: Local Development
```bash
# Just code and test quickly
mvn spring-boot:run
# Uses H2, no setup needed
```

### Scenario 2: Performance Demo
```bash
# 1. Load massive data
mvn spring-boot:run -Dspring-boot.run.profiles=data-loader

# 2. Demo bad performance
mvn spring-boot:run -Dspring-boot.run.profiles=bad-performance

# 3. Demo good performance
mvn spring-boot:run -Dspring-boot.run.profiles=good-performance

# 4. Show the dramatic difference!
```

### Scenario 3: Teaching Session
```bash
# Use docker profile for SQL visibility
mvn spring-boot:run -Dspring-boot.run.profiles=docker

# Students can see:
# - Every SQL query in logs
# - Formatted SQL with comments
# - Hibernate statistics
# - Query execution times
```

### Scenario 4: Production Deployment
```bash
# Use good-performance as template
# Adjust based on your workload:
# - Update connection pool size
# - Tune batch sizes
# - Configure caching if needed
# - Set proper credentials

java -jar app.jar --spring.profiles.active=good-performance
```

---

## 🔧 Customization Guide

### For Your Specific Needs

**High Write Volume:**
```properties
# Increase batch sizes
spring.jpa.properties.hibernate.jdbc.batch_size=100
spring.jpa.properties.hibernate.order_inserts=true
```

**High Read Volume:**
```properties
# Enable second-level cache
spring.jpa.properties.hibernate.cache.use_second_level_cache=true
spring.jpa.properties.hibernate.cache.region.factory_class=...
```

**Low Memory Environment:**
```properties
# Reduce fetch size
spring.jpa.properties.hibernate.jdbc.fetch_size=50
spring.datasource.hikari.maximum-pool-size=20
```

**High Latency Network:**
```properties
# Increase batch sizes to reduce round-trips
spring.jpa.properties.hibernate.jdbc.batch_size=100
spring.jpa.properties.hibernate.jdbc.fetch_size=500
```

---

## 📚 Further Reading

Each configuration file contains:
- Detailed inline comments
- Real-world impact examples
- Performance calculations
- Tuning guidelines

**Read the files themselves for:**
- Deep understanding of each setting
- Mathematical explanations of improvements
- Specific tuning recommendations
- Common pitfalls and solutions

---

## 🎯 Summary

| File | Primary Audience | Key Benefit |
|------|-----------------|-------------|
| application.properties | Developers | Quick local development |
| application-docker.properties | Developers/Students | SQL visibility for learning |
| application-data-loader.properties | Demo Setup | Fast bulk data loading |
| application-bad-performance.properties | Students | Learn what NOT to do |
| application-good-performance.properties | Everyone | Production best practices |

**Remember:** The bad/good performance files are **educational tools** - use them to teach, compare, and understand performance optimization!
