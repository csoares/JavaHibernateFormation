#!/bin/bash

# ===========================================
# Hibernate Performance Demo Testing Script
# ===========================================
# This script automatically detects your current branch
# and runs the appropriate performance tests
#
# USAGE:
#   1. Checkout the branch you want to test: git checkout <branch-name>
#   2. Start your application: mvn spring-boot:run -Dspring-boot.run.profiles=docker
#   3. In another terminal: ./demo-tests.sh
#
# Make sure database is loaded with test data first!
# ===========================================

set -e

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
APP_URL="http://localhost:8080"
REPEAT_COUNT=3

# Detect current branch
CURRENT_BRANCH=$(git branch --show-current)

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Hibernate Performance Demo Tests${NC}"
echo -e "${GREEN}========================================${NC}\n"
echo -e "${BLUE}Current branch: ${YELLOW}${CURRENT_BRANCH}${NC}\n"

# Check if application is running
echo -e "${BLUE}Checking if application is running...${NC}"
if ! curl -s "${APP_URL}/actuator/health" > /dev/null 2>&1; then
    if ! curl -s "${APP_URL}" > /dev/null 2>&1; then
        echo -e "${RED}ERROR: Application is not running!${NC}"
        echo "Start it with: mvn spring-boot:run -Dspring-boot.run.profiles=docker"
        exit 1
    fi
fi
echo -e "${GREEN}✓ Application is running${NC}\n"

# Check if PostgreSQL is running
echo -e "${BLUE}Checking if PostgreSQL is running...${NC}"
if ! docker ps | grep -q hibernate-postgres; then
    echo -e "${RED}ERROR: PostgreSQL is not running!${NC}"
    echo "Start it with: docker-compose up -d postgresql"
    exit 1
fi
echo -e "${GREEN}✓ PostgreSQL is running${NC}\n"

# Check database has data
echo -e "${BLUE}Checking database content...${NC}"
USER_COUNT=$(docker exec hibernate-postgres psql -U hibernate_user -d hibernate_formation -t -c "SELECT COUNT(*) FROM users;" 2>/dev/null | xargs || echo "0")

if [ "$USER_COUNT" -lt 10000 ]; then
    echo -e "${YELLOW}WARNING: Database appears empty or has insufficient data!${NC}"
    echo "Current user count: $USER_COUNT"
    echo -e "${YELLOW}Load data with: mvn spring-boot:run -Dspring-boot.run.profiles=data-loader${NC}"
    echo ""
    read -p "Continue anyway? (y/n): " CONTINUE
    if [ "$CONTINUE" != "y" ]; then
        exit 1
    fi
else
    echo -e "${GREEN}✓ Database ready with $USER_COUNT users${NC}\n"
fi

# Function to test endpoint with timing
test_endpoint() {
    local NAME="$1"
    local URL="$2"
    local REPEAT="${3:-1}"

    echo -e "${CYAN}Testing: $NAME${NC}"
    echo "URL: $URL"
    echo "Repeating: $REPEAT times"
    echo ""

    local TOTAL_TIME=0
    local SUCCESS_COUNT=0

    for i in $(seq 1 $REPEAT); do
        echo -n "  Run $i/$REPEAT: "

        START_TIME=$(date +%s%3N)
        RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}\nTIME_TOTAL:%{time_total}" "$URL" 2>/dev/null || echo "HTTP_CODE:000\nTIME_TOTAL:0")
        END_TIME=$(date +%s%3N)

        HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE:" | cut -d':' -f2)
        TIME_TOTAL=$(echo "$RESPONSE" | grep "TIME_TOTAL:" | cut -d':' -f2)

        if [ "$HTTP_CODE" = "200" ]; then
            echo -e "${GREEN}✓ ${TIME_TOTAL}s${NC}"
            SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
            TOTAL_TIME=$(echo "$TOTAL_TIME + $TIME_TOTAL" | bc)
        else
            echo -e "${RED}✗ HTTP $HTTP_CODE${NC}"
        fi
    done

    if [ $SUCCESS_COUNT -gt 0 ]; then
        AVG_TIME=$(echo "scale=3; $TOTAL_TIME / $SUCCESS_COUNT" | bc)
        echo -e "${GREEN}  Average: ${AVG_TIME}s (${SUCCESS_COUNT}/${REPEAT} successful)${NC}"
    else
        echo -e "${RED}  All requests failed!${NC}"
    fi

    echo ""
}

# Function to compare two endpoints
compare_endpoints() {
    local BAD_NAME="$1"
    local BAD_URL="$2"
    local GOOD_NAME="$3"
    local GOOD_URL="$4"

    echo -e "${YELLOW}╔═══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${YELLOW}║             PERFORMANCE COMPARISON                            ║${NC}"
    echo -e "${YELLOW}╚═══════════════════════════════════════════════════════════════╝${NC}"
    echo ""

    test_endpoint "$BAD_NAME" "$BAD_URL" $REPEAT_COUNT
    read -p "Press Enter to test optimized version..."
    test_endpoint "$GOOD_NAME" "$GOOD_URL" $REPEAT_COUNT

    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${YELLOW}💡 Check application logs to see SQL query differences!${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
}

# Main logic - auto-detect branch and run appropriate tests
run_tests() {
    case $CURRENT_BRANCH in
    main)
        echo -e "\n${GREEN}═══════════════════════════════════════${NC}"
        echo -e "${GREEN}MAIN BRANCH - Best Practices Demo${NC}"
        echo -e "${GREEN}═══════════════════════════════════════${NC}\n"

        echo "This branch demonstrates production-ready best practices:"
        echo "  ✅ EntityGraph for N+1 resolution"
        echo "  ✅ JOIN FETCH for explicit optimization"
        echo "  ✅ DTO projections for efficiency"
        echo "  ✅ Pagination for scalability"
        echo "  ✅ Proper transaction management"
        echo ""

        echo -e "${YELLOW}Select test scenario:${NC}"
        echo "  1) User queries (BAD vs GOOD comparison)"
        echo "  2) Order queries (nested relationships)"
        echo "  3) Pagination tests"
        echo "  4) DTO projection tests"
        echo ""
        read -p "Enter choice [1-4]: " TEST_CHOICE

        case $TEST_CHOICE in
            1)
                echo -e "\n${CYAN}═══ USER QUERIES COMPARISON ═══${NC}\n"
                compare_endpoints \
                    "❌ BAD: User with N+1 problem" \
                    "${APP_URL}/api/bad/users/1" \
                    "✅ GOOD: User with EntityGraph" \
                    "${APP_URL}/api/good/users/1"

                read -p "Test batch operations (100 users)? (y/n): " TEST_BATCH
                if [ "$TEST_BATCH" = "y" ]; then
                    compare_endpoints \
                        "❌ BAD: List users (N+1 for each)" \
                        "${APP_URL}/api/bad/users?page=0&size=100" \
                        "✅ GOOD: List users (optimized)" \
                        "${APP_URL}/api/good/users?page=0&size=100"
                fi
                ;;

            2)
                echo -e "\n${CYAN}═══ ORDER QUERIES (Nested Relationships) ═══${NC}\n"
                echo "Orders have: Order → User → Department (3 levels)"
                echo ""

                compare_endpoints \
                    "❌ BAD: Order with nested N+1" \
                    "${APP_URL}/api/bad/orders/1" \
                    "✅ GOOD: Order with nested EntityGraph" \
                    "${APP_URL}/api/good/orders/1"
                ;;

            3)
                echo -e "\n${CYAN}═══ PAGINATION PERFORMANCE ═══${NC}\n"
                echo "Testing pagination at different offsets"
                echo ""

                test_endpoint "Page 0 (first 20 users)" \
                    "${APP_URL}/api/good/users?page=0&size=20" $REPEAT_COUNT

                test_endpoint "Page 100 (users 2000-2020)" \
                    "${APP_URL}/api/good/users?page=100&size=20" $REPEAT_COUNT

                test_endpoint "Page 1000 (users 20000-20020)" \
                    "${APP_URL}/api/good/users?page=1000&size=20" $REPEAT_COUNT

                echo -e "${YELLOW}Note: OFFSET pagination gets slower with higher page numbers${NC}"
                echo -e "${YELLOW}Consider keyset pagination for very large offsets${NC}"
                ;;

            4)
                echo -e "\n${CYAN}═══ DTO PROJECTION EFFICIENCY ═══${NC}\n"
                echo "DTO projections load only required fields"
                echo ""
                echo -e "${YELLOW}Note:${NC} Performance may be similar for small datasets (100 users)"
                echo "DTO benefits scale with volume: larger payloads = bigger difference"
                echo ""
                echo "Full entities load: User + complete Department (id, name, description, budget)"
                echo "DTO projection loads: User + only department.name (~25% less data)"
                echo ""

                compare_endpoints \
                    "Full entities (all fields loaded)" \
                    "${APP_URL}/api/good/users?page=0&size=100" \
                    "DTO projection (selected fields only)" \
                    "${APP_URL}/api/good/users/summaries/paginated?page=0&size=100"
                ;;
        esac
        ;;

    badmain)
        echo -e "\n${RED}═══════════════════════════════════════${NC}"
        echo -e "${RED}BADMAIN BRANCH - Anti-Patterns Demo${NC}"
        echo -e "${RED}═══════════════════════════════════════${NC}\n"

        echo "This branch demonstrates common performance mistakes!"
        echo "Compare these results with 'main' branch performance."
        echo ""

        echo -e "${RED}Testing anti-patterns (expect SLOW performance):${NC}"
        echo ""

        test_endpoint "❌ User query (bad practices)" \
            "${APP_URL}/api/bad/users/1" $REPEAT_COUNT

        read -p "Test batch operations (warning: may be VERY slow)? (y/n): " TEST_BATCH
        if [ "$TEST_BATCH" = "y" ]; then
            test_endpoint "❌ List 100 users (N+1 problem!)" \
                "${APP_URL}/api/bad/users?page=0&size=100" 1

            echo -e "${RED}⚠️  Check logs: you'll see 100+ separate SQL queries!${NC}"
        fi
        ;;

    001-n1problem)
        echo -e "\n${GREEN}═══════════════════════════════════════${NC}"
        echo -e "${GREEN}N+1 PROBLEM DEMO (001-n1problem)${NC}"
        echo -e "${GREEN}═══════════════════════════════════════${NC}\n"

        echo "This branch focuses specifically on the N+1 query problem:"
        echo "  ❌ BAD: Fetches user, then separate query for department"
        echo "  ✅ GOOD: Uses EntityGraph or JOIN FETCH for single query"
        echo ""

        echo -e "${CYAN}══ Single User Access ══${NC}"
        compare_endpoints \
            "❌ BAD: N+1 (2 queries)" \
            "${APP_URL}/api/n1-demo/bad/1" \
            "✅ GOOD: EntityGraph (1 query)" \
            "${APP_URL}/api/n1-demo/good-entitygraph/1"

        read -p "Test JOIN FETCH alternative? (y/n): " TEST_JOIN
        if [ "$TEST_JOIN" = "y" ]; then
            test_endpoint "✅ GOOD: JOIN FETCH (1 query)" \
                "${APP_URL}/api/n1-demo/good-joinfetch/1" $REPEAT_COUNT
        fi

        read -p "Test batch operations (THIS shows N+1 clearly!)? (y/n): " TEST_BATCH
        if [ "$TEST_BATCH" = "y" ]; then
            echo ""
            echo -e "${CYAN}══ Batch Operations (100 users) ══${NC}"
            echo -e "${YELLOW}Watch the logs carefully!${NC}"
            echo ""

            compare_endpoints \
                "❌ BAD: Batch (101 queries!)" \
                "${APP_URL}/api/n1-demo/batch-bad" \
                "✅ GOOD: Batch (1 query!)" \
                "${APP_URL}/api/n1-demo/batch-good"

            echo -e "${RED}BAD version: 1 query for users + 100 queries for departments${NC}"
            echo -e "${GREEN}GOOD version: 1 query with JOIN${NC}"
        fi
        ;;

    002-pagination)
        echo -e "\n${GREEN}═══════════════════════════════════════${NC}"
        echo -e "${GREEN}PAGINATION DEMO (002-pagination)${NC}"
        echo -e "${GREEN}═══════════════════════════════════════${NC}\n"

        echo "This branch demonstrates pagination best practices:"
        echo "  ❌ BAD: Loading all records without pagination"
        echo "  ✅ GOOD: Using Spring Data Pageable"
        echo "  ✅ ADVANCED: Pagination with sorting and filtering"
        echo ""

        echo -e "${YELLOW}Select test scenario:${NC}"
        echo "  1) Problem vs Solution (all users vs paginated)"
        echo "  2) Basic Pagination (different page sizes)"
        echo "  3) Pagination with Sorting"
        echo "  4) Pagination with Search/Filter"
        echo "  5) Optimized Pagination (with EntityGraph)"
        echo "  6) Stress Test (multiple pages)"
        echo ""
        read -p "Enter choice [1-6]: " PAGINATION_CHOICE

        case $PAGINATION_CHOICE in
            1)
                echo -e "\n${CYAN}══ Problem vs Solution ══${NC}"
                echo -e "${RED}WARNING: /bad/all endpoint loads ALL users - may be slow!${NC}"
                echo ""
                compare_endpoints \
                    "❌ BAD: All users (no pagination)" \
                    "${APP_URL}/api/pagination-demo/bad/all" \
                    "✅ GOOD: Paginated (page 0, size 20)" \
                    "${APP_URL}/api/pagination-demo/good?page=0&size=20"
                ;;

            2)
                echo -e "\n${CYAN}══ Basic Pagination ══${NC}"
                test_endpoint "Page 0 (first 20 users)" \
                    "${APP_URL}/api/pagination-demo/good?page=0&size=20" $REPEAT_COUNT

                test_endpoint "Page 10 (users 200-220)" \
                    "${APP_URL}/api/pagination-demo/good?page=10&size=20" $REPEAT_COUNT

                test_endpoint "Page 50 (users 1000-1020)" \
                    "${APP_URL}/api/pagination-demo/good?page=50&size=20" $REPEAT_COUNT

                echo -e "${YELLOW}Note: OFFSET pagination may degrade with very high page numbers${NC}"
                ;;

            3)
                echo -e "\n${CYAN}══ Pagination with Sorting ══${NC}"
                test_endpoint "Sort by name (ASC)" \
                    "${APP_URL}/api/pagination-demo/good-with-sort?page=0&size=10&sortBy=name&sortDir=asc" $REPEAT_COUNT

                test_endpoint "Sort by email (DESC)" \
                    "${APP_URL}/api/pagination-demo/good-with-sort?page=0&size=10&sortBy=email&sortDir=desc" $REPEAT_COUNT
                ;;

            4)
                echo -e "\n${CYAN}══ Pagination with Search ══${NC}"
                echo "Searching for users with 'User' in their name"
                echo ""
                test_endpoint "Search: 'User' (page 0)" \
                    "${APP_URL}/api/pagination-demo/search?name=User&page=0&size=20" $REPEAT_COUNT

                test_endpoint "Search: 'User' (page 5)" \
                    "${APP_URL}/api/pagination-demo/search?name=User&page=5&size=20" $REPEAT_COUNT
                ;;

            5)
                echo -e "\n${CYAN}══ Optimized Pagination (with EntityGraph) ══${NC}"
                echo "Compare pagination WITH and WITHOUT EntityGraph optimization"
                echo ""
                echo -e "${YELLOW}What to expect:${NC}"
                echo "  ⚠️  Basic: May trigger N+1 queries (1 + 20 queries for 20 users)"
                echo "  ✅ Optimized: Single query with JOIN (fast!)"
                echo ""
                echo -e "${YELLOW}💡 Watch the SQL logs to see the difference!${NC}"
                echo ""
                compare_endpoints \
                    "⚠️  Basic pagination (WITHOUT EntityGraph - N+1 problem!)" \
                    "${APP_URL}/api/pagination-demo/good?page=0&size=20" \
                    "✅ Optimized pagination (WITH EntityGraph - single query)" \
                    "${APP_URL}/api/pagination-demo/optimized?page=0&size=20"
                ;;

            6)
                echo -e "\n${CYAN}══ Stress Test ══${NC}"
                echo "Testing 10 sequential pages to verify consistent performance"
                echo ""
                echo -e "${YELLOW}What this tests:${NC}"
                echo "  • Performance consistency across different page numbers"
                echo "  • Whether pagination degrades with higher offsets"
                echo "  • Memory and query optimization effectiveness"
                echo ""

                echo -e "${BLUE}Running stress test (fetching 10 pages of 50 users each)...${NC}"
                echo ""

                STRESS_RESULT=$(curl -s "${APP_URL}/api/pagination-demo/stress-test")
                echo "$STRESS_RESULT"
                echo ""

                echo -e "${YELLOW}💡 Interpretation:${NC}"
                echo "  ✅ Good: Similar times across all pages (20-30ms each)"
                echo "  ⚠️  Bad: Times increase with page number (20ms → 50ms → 100ms)"
                echo ""
                echo -e "${CYAN}Why this matters:${NC}"
                echo "  OFFSET pagination skips rows, so page 9 skips 450 rows."
                echo "  With proper indexing and EntityGraph, performance stays consistent!"
                ;;
        esac
        ;;

    003-blob-management)
        echo -e "\n${GREEN}═══════════════════════════════════════${NC}"
        echo -e "${GREEN}BLOB MANAGEMENT DEMO (003-blob-management)${NC}"
        echo -e "${GREEN}═══════════════════════════════════════${NC}\n"

        echo "This branch demonstrates BLOB (Binary Large Object) handling:"
        echo "  ❌ BAD: Eagerly loads all images/PDFs (massive waste)"
        echo "  ✅ GOOD: Lazy loading, only fetches when needed"
        echo ""

        echo -e "${YELLOW}Products have 1MB images attached${NC}"
        echo ""

        read -p "Test GOOD version first (safe, fast)? (y/n): " TEST_GOOD_FIRST
        if [ "$TEST_GOOD_FIRST" = "y" ]; then
            test_endpoint "✅ GOOD: 100 products WITHOUT images" \
                "${APP_URL}/api/blob/good/products?limit=100" $REPEAT_COUNT

            echo -e "${GREEN}Only metadata loaded, no BLOB data transferred${NC}"
            echo ""
        fi

        read -p "Test BAD version (WARNING: loads 100MB!)? (y/n): " TEST_BAD
        if [ "$TEST_BAD" = "y" ]; then
            echo -e "${RED}⚠️  WARNING: This will transfer ~100MB of data!${NC}"
            read -p "Are you sure? (yes/no): " CONFIRM

            if [ "$CONFIRM" = "yes" ]; then
                test_endpoint "❌ BAD: 100 products WITH images (100MB!)" \
                    "${APP_URL}/api/blob/bad/products?limit=100" 1

                echo ""
                echo -e "${RED}BAD version: ~100MB transferred (1MB × 100 products)${NC}"
                echo -e "${GREEN}GOOD version: ~10KB transferred (only metadata)${NC}"
                echo -e "${YELLOW}Difference: 10,000x more data in BAD version!${NC}"
            fi
        fi
        ;;

    *)
        echo ""
        echo -e "${RED}════════════════════════════════════════════════════════${NC}"
        echo -e "${RED}ERROR: Unknown branch '${CURRENT_BRANCH}'${NC}"
        echo -e "${RED}════════════════════════════════════════════════════════${NC}"
        echo ""
        echo -e "${YELLOW}This script supports the following branches:${NC}"
        echo "  • main"
        echo "  • badmain"
        echo "  • 001-n1problem"
        echo "  • 002-pagination"
        echo "  • 003-blob-management"
        echo ""
        echo -e "${CYAN}To test a different branch:${NC}"
        echo "  1. git checkout <branch-name>"
        echo "  2. ./demo-tests.sh"
        echo ""
        exit 1
        ;;
    esac
}

# Run tests for current branch
run_tests

# Show completion message
echo ""
echo -e "${GREEN}════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}Test completed!${NC}"
echo -e "${GREEN}════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${CYAN}Tips for deeper analysis:${NC}"
echo ""
echo -e "${YELLOW}📊 View SQL logs:${NC}"
echo "   tail -f spring-boot.log | grep 'org.hibernate.SQL'"
echo ""
echo -e "${YELLOW}📈 Monitor database performance:${NC}"
echo "   docker exec -it hibernate-postgres psql -U hibernate_user -d hibernate_formation"
echo ""
echo -e "${YELLOW}💾 Check memory usage:${NC}"
echo "   docker stats hibernate-postgres"
echo ""
echo -e "${YELLOW}🔍 View Hibernate statistics:${NC}"
echo "   Check application logs for Session Metrics"
echo ""
echo -e "${CYAN}Next steps:${NC}"
echo "  • Switch branches to compare different implementations: git checkout <branch>"
echo "  • Monitor SQL queries in logs while running tests"
echo "  • Run this script again on a different branch"
echo ""
