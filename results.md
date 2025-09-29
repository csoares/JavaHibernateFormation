# 📊 Hibernate Performance Testing Results

## How to See Time Comparisons:

### 1. **Current Running Application (Good Performance)**
The application is currently running with **good-performance** profile.

Test it with:
```bash
time curl -s http://localhost:8080/api/data/statistics
```

### 2. **Manual Performance Comparison**

**Step 1 - Test Bad Performance:**
```bash
docker-compose down
PERFORMANCE_PROFILE=bad-performance docker-compose up -d
sleep 30
echo "🔥 BAD PERFORMANCE:"
time curl -s http://localhost:8080/api/data/statistics
time curl -s "http://localhost:8080/api/users?page=0&size=100"
```

**Step 2 - Test Good Performance:**
```bash
docker-compose down
PERFORMANCE_PROFILE=good-performance docker-compose up -d
sleep 30
echo "🚀 GOOD PERFORMANCE:"
time curl -s http://localhost:8080/api/data/statistics
time curl -s "http://localhost:8080/api/users?page=0&size=100"
```

### 3. **What to Look For:**

The `time` command shows three values:
- **real**: Total time (what users experience)
- **user**: CPU time in user mode
- **sys**: CPU time in system mode

**Expected Results:**
- **Bad Performance**: 0.5-3+ seconds real time
- **Good Performance**: 0.05-0.3 seconds real time

### 4. **Key Performance Differences:**

| Configuration | Connection Pool | Batch Size | Fetch Size | Logging |
|---------------|----------------|------------|------------|---------|
| Bad           | 2 connections | 1          | 1          | Verbose |
| Good          | 50 connections| 50         | 100        | Minimal |

### 5. **Database Statistics:**

Both configurations use the same massive dataset:
- **302,000+ total records**
- 1,000 departments
- 1,000 categories
- 100,000 users
- 100,000 products
- 100,000 orders

### 6. **Automated Scripts Available:**

- `./quick-test.sh` - Simple verification
- `./quick-comparison.sh` - Basic comparison
- `./manual-test.sh` - Step-by-step guide
- `./performance-test.sh` - Full automated testing (advanced)

### 7. **Example Results:**

```bash
# Bad Performance
$ time curl -s http://localhost:8080/api/data/statistics
real    0m0.083s
user    0m0.000s
sys     0m0.000s

# Good Performance
$ time curl -s http://localhost:8080/api/data/statistics
real    0m0.007s
user    0m0.000s
sys     0m0.000s
```

**Performance Improvement: ~91% faster!**