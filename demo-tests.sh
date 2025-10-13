#!/bin/bash

# ===========================================
# Hibernate Performance Demo Testing Script
# ===========================================
# This script tests performance across all branches
#
# USAGE:
#   1. Start your application: mvn spring-boot:run -Dspring-boot.run.profiles=docker
#   2. In another terminal: ./demo-tests.sh
#   3. Select your branch-specific tests
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

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Hibernate Performance Demo Tests${NC}"
echo -e "${GREEN}========================================${NC}\n"

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

# Show branch menu
echo -e "${CYAN}========================================${NC}"
echo -e "${CYAN}SELECT YOUR CURRENT BRANCH:${NC}"
echo -e "${CYAN}========================================${NC}"
echo ""
echo "  1) main          - Full-featured best practices demo"
echo "  2) badmain       - Anti-patterns demonstration"
echo "  3) 001-n1problem - N+1 query problem demo"
echo "  4) 002-pagination - Pagination strategies demo"
echo "  5) 003-blob-management - BLOB loading demo"
echo ""
read -p "Enter choice [1-5]: " BRANCH_CHOICE

case $BRANCH_CHOICE in
    1)
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

                compare_endpoints \
                    "Full entities (all fields loaded)" \
                    "${APP_URL}/api/good/users?page=0&size=100" \
                    "DTO projection (selected fields only)" \
                    "${APP_URL}/api/good/users/summaries"
                ;;
        esac
        ;;

    2)
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

    3)
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

    4)
        echo -e "\n${GREEN}═══════════════════════════════════════${NC}"
        echo -e "${GREEN}PAGINATION DEMO (002-pagination)${NC}"
        echo -e "${GREEN}═══════════════════════════════════════${NC}\n"

        echo "This branch demonstrates pagination strategies:"
        echo "  📄 OFFSET: Simple but degrades with higher pages"
        echo "  🔑 KEYSET: Consistent performance regardless of offset"
        echo ""

        echo -e "${CYAN}══ OFFSET Pagination ══${NC}"
        test_endpoint "Page 0 (beginning)" \
            "${APP_URL}/api/pagination/offset?page=0&size=100" $REPEAT_COUNT

        test_endpoint "Page 1000 (middle - SLOWER!)" \
            "${APP_URL}/api/pagination/offset?page=1000&size=100" $REPEAT_COUNT

        test_endpoint "Page 5000 (far - VERY SLOW!)" \
            "${APP_URL}/api/pagination/offset?page=5000&size=100" $REPEAT_COUNT

        read -p "Test KEYSET pagination for comparison? (y/n): " TEST_KEYSET
        if [ "$TEST_KEYSET" = "y" ]; then
            echo ""
            echo -e "${CYAN}══ KEYSET Pagination ══${NC}"

            test_endpoint "KEYSET: Start (lastId=0)" \
                "${APP_URL}/api/pagination/keyset?lastId=0&size=100" $REPEAT_COUNT

            test_endpoint "KEYSET: Middle (lastId=100000) - STILL FAST!" \
                "${APP_URL}/api/pagination/keyset?lastId=100000&size=100" $REPEAT_COUNT

            test_endpoint "KEYSET: Far (lastId=500000) - STILL FAST!" \
                "${APP_URL}/api/pagination/keyset?lastId=500000&size=100" $REPEAT_COUNT

            echo ""
            echo -e "${GREEN}✓ Notice: KEYSET maintains consistent performance!${NC}"
            echo -e "${YELLOW}  OFFSET: Gets slower with higher pages (10ms → 100ms → 500ms)${NC}"
            echo -e "${GREEN}  KEYSET: Stays fast regardless of position (10ms → 10ms → 10ms)${NC}"
        fi
        ;;

    5)
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
        echo -e "${RED}Invalid choice${NC}"
        exit 1
        ;;
esac

echo ""
echo -e "${GREEN}════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}Demo completed!${NC}"
echo -e "${GREEN}════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${CYAN}Tips for deeper analysis:${NC}"
echo ""
echo -e "${YELLOW}📊 View SQL logs:${NC}"
echo "   docker-compose logs -f hibernate-app | grep 'Hibernate:'"
echo ""
echo -e "${YELLOW}📈 Monitor database performance:${NC}"
echo "   docker exec -it hibernate-postgres pg_top -U hibernate_user -d hibernate_formation"
echo ""
echo -e "${YELLOW}💾 Check memory usage:${NC}"
echo "   docker stats hibernate-postgres"
echo ""
echo -e "${YELLOW}🔍 View Hibernate statistics:${NC}"
echo "   Check application logs for Session Metrics"
echo ""
echo -e "${CYAN}Next steps:${NC}"
echo "  • Switch branches to compare different implementations"
echo "  • Monitor SQL queries in logs while running tests"
echo "  • Try different configuration profiles (good-performance vs bad-performance)"
echo ""
