# Performance Demonstration Setup Guide

This guide shows how to set up a PostgreSQL database with massive datasets (1M+ records) to demonstrate Hibernate performance issues across different branches.

## Overview

The setup includes:
- **PostgreSQL** database in Docker
- **1,000,000 users** distributed across 100 departments
- **500,000 products** (10,000 with 1MB blobs/images)
- **500,000 orders** with ~2,000,000 order items
- Proper indexes and relationships for realistic scenarios

## Quick Start

### 1. Start PostgreSQL

```bash
docker-compose up -d postgresql
```

Wait for PostgreSQL to be healthy:
```bash
docker-compose ps
```

### 2. Load the Massive Dataset

This will populate the database with 1M+ records:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=data-loader
```

**Note:** This process will take several minutes (typically 5-15 minutes depending on your machine). You'll see progress updates during loading.

### 3. Verify the Data

Connect to PostgreSQL:
```bash
docker exec -it hibernate-postgres psql -U hibernate_user -d hibernate_formation
```

Check record counts:
```sql
SELECT 'users' as table_name, COUNT(*) as count FROM users
UNION ALL
SELECT 'departments', COUNT(*) FROM departments
UNION ALL
SELECT 'products', COUNT(*) FROM products
UNION ALL
SELECT 'orders', COUNT(*) FROM orders
UNION ALL
SELECT 'order_items', COUNT(*) FROM order_items;
```

Check database size:
```sql
SELECT pg_size_pretty(pg_database_size('hibernate_formation')) as db_size;
```

## Demonstrating Performance Issues

### Branch: 001-n1problem (N+1 Query Problem)

Show the N+1 problem with a small dataset first:

```bash
git checkout 001-n1problem
mvn spring-boot:run -Dspring-boot.run.profiles=docker
```

Test endpoints:
```bash
# Bad: Will trigger N+1 queries (1 + 100 queries for 100 users)
curl "http://localhost:8080/api/n1/bad?limit=100"

# Good: Single query with JOIN FETCH
curl "http://localhost:8080/api/n1/good?limit=100"
```

With 1M users in database, the N+1 problem becomes catastrophic!

### Branch: 002-pagination (Pagination Strategies)

```bash
git checkout 002-pagination
mvn spring-boot:run -Dspring-boot.run.profiles=docker
```

Test pagination performance:
```bash
# Offset-based pagination (gets slower with higher page numbers)
curl "http://localhost:8080/api/pagination/offset?page=0&size=100"
curl "http://localhost:8080/api/pagination/offset?page=1000&size=100"  # Much slower!

# Keyset/cursor-based pagination (consistent performance)
curl "http://localhost:8080/api/pagination/keyset?lastId=0&size=100"
curl "http://localhost:8080/api/pagination/keyset?lastId=100000&size=100"  # Same speed!
```

### Branch: 003-blob-management (BLOB Handling)

```bash
git checkout 003-blob-management
mvn spring-boot:run -Dspring-boot.run.profiles=docker
```

Test BLOB loading:
```bash
# Bad: Loads all 1MB images eagerly (10GB+ for 10,000 products!)
curl "http://localhost:8080/api/blob/bad/products?limit=100"

# Good: Lazy loading, only loads blobs when needed
curl "http://localhost:8080/api/blob/good/products?limit=100"
```

## Performance Monitoring

The application includes performance metrics. Check them:

```bash
curl http://localhost:8080/actuator/metrics/hibernate.query.execution
```

Watch SQL queries in logs:
```bash
# The application logs all SQL queries with timing
docker-compose logs -f hibernate-app
```

## Database Connection

If you need to connect with a database tool:
- **Host:** localhost
- **Port:** 5432
- **Database:** hibernate_formation
- **Username:** hibernate_user
- **Password:** hibernate_password

## Cleanup

### Stop services:
```bash
docker-compose down
```

### Remove data volume (complete cleanup):
```bash
docker-compose down -v
```

### Start fresh:
```bash
docker-compose down -v
docker-compose up -d postgresql
mvn spring-boot:run -Dspring-boot.run.profiles=data-loader
```

## Tips for Students

1. **Start Small:** First demonstrate with `limit=10`, then `limit=100`, then show what happens with larger datasets
2. **Compare Queries:** Show the SQL logs side-by-side for "bad" vs "good" implementations
3. **Measure Time:** Use browser dev tools or `curl` with `-w "@curl-format.txt"` to show response times
4. **Database Load:** Show `pg_stat_activity` to demonstrate concurrent query load
5. **Memory Usage:** Monitor application memory with `docker stats hibernate-app`

## Create curl timing template:

```bash
cat > curl-format.txt << 'EOF'
    time_namelookup:  %{time_namelookup}\n
       time_connect:  %{time_connect}\n
    time_appconnect:  %{time_appconnect}\n
   time_pretransfer:  %{time_pretransfer}\n
      time_redirect:  %{time_redirect}\n
 time_starttransfer:  %{time_starttransfer}\n
                    ----------\n
         time_total:  %{time_total}\n
EOF
```

Then use: `curl -w "@curl-format.txt" <url>`

## Troubleshooting

### PostgreSQL won't start
```bash
docker-compose logs postgresql
```

### Data loader fails
- Ensure PostgreSQL is running and healthy
- Check connection settings in `application-data-loader.properties`
- Verify schema was created: Check `init-db/01-schema.sql` was executed

### Out of memory during data loading
- Increase Docker memory allocation (recommended: 4GB+)
- Reduce batch sizes in `MassiveDataLoader.java`

### Application won't connect
- Verify PostgreSQL is accepting connections: `docker-compose ps`
- Check if port 5432 is available: `lsof -i :5432`
- Use `application-docker.properties` profile for connecting to Docker PostgreSQL
