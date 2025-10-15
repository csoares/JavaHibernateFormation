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

    003-blob-management)
        echo -e "\n${GREEN}═══════════════════════════════════════${NC}"
        echo -e "${GREEN}BLOB MANAGEMENT DEMO (003-blob-management)${NC}"
        echo -e "${GREEN}═══════════════════════════════════════${NC}\n"

        echo "This branch demonstrates BLOB (Binary Large Object) handling:"
        echo "  ❌ BAD: Loads all PDFs with orders (massive waste)"
        echo "  ✅ GOOD: Only metadata, lazy loading, separate endpoints"
        echo ""

        echo -e "${YELLOW}Select test scenario:${NC}"
        echo "  1) Problem vs Solution (All orders with/without BLOBs)"
        echo "  2) Check if order has PDF (without loading it)"
        echo "  3) Download single PDF (on-demand)"
        echo "  4) List orders with BLOB metadata"
        echo "  5) Batch processing comparison"
        echo ""
        read -p "Enter choice [1-5]: " BLOB_CHOICE

        case $BLOB_CHOICE in
            1)
                echo -e "\n${CYAN}══ Problem vs Solution ══${NC}"
                echo -e "${RED}WARNING: BAD endpoint may load large BLOBs!${NC}"
                echo ""
                compare_endpoints \
                    "❌ BAD: Load orders WITH PDF BLOBs (slow!)" \
                    "${APP_URL}/api/blob-demo/bad/all-with-blobs" \
                    "✅ GOOD: Load orders WITHOUT BLOBs (fast!)" \
                    "${APP_URL}/api/blob-demo/good/all-without-blobs"
                ;;

            2)
                echo -e "\n${CYAN}══ Check PDF Existence ══${NC}"
                echo "This checks if order has PDF WITHOUT loading the BLOB"
                echo ""
                test_endpoint "Check if order #1 has PDF" \
                    "${APP_URL}/api/blob-demo/order/1/has-pdf" $REPEAT_COUNT

                echo -e "${GREEN}✓ Only metadata checked, no BLOB loaded!${NC}"
                ;;

            3)
                echo -e "\n${CYAN}══ Download PDF On-Demand ══${NC}"
                echo "Downloads only when user explicitly requests it"
                echo ""
                read -p "Download PDF for order ID (e.g., 1)? " ORDER_ID
                if [ -n "$ORDER_ID" ]; then
                    echo "Downloading PDF for order #${ORDER_ID}..."
                    curl -s -o "/tmp/order_${ORDER_ID}.pdf" "${APP_URL}/api/blob-demo/order/${ORDER_ID}/pdf"
                    FILE_SIZE=$(ls -lh "/tmp/order_${ORDER_ID}.pdf" | awk '{print $5}')
                    echo -e "${GREEN}✓ PDF downloaded: /tmp/order_${ORDER_ID}.pdf (${FILE_SIZE})${NC}"
                fi
                ;;

            4)
                echo -e "\n${CYAN}══ List with Metadata ══${NC}"
                echo "Shows orders with 'has PDF' indicator without loading BLOBs"
                echo ""
                test_endpoint "List orders with BLOB metadata" \
                    "${APP_URL}/api/blob-demo/list-efficient" $REPEAT_COUNT

                echo -e "${GREEN}✓ Only metadata loaded, perfect for UI lists!${NC}"
                ;;

            5)
                echo -e "\n${CYAN}══ Batch Processing ══${NC}"
                echo "Compares processing many orders with/without BLOBs"
                echo ""
                test_endpoint "Batch process orders" \
                    "${APP_URL}/api/blob-demo/batch-process" 1

                echo -e "${YELLOW}Check response for detailed performance breakdown!${NC}"
                ;;
        esac
        ;;

    *)
        echo ""
        echo -e "${RED}════════════════════════════════════════════════════════${NC}"
        echo -e "${RED}ERROR: Unknown branch '${CURRENT_BRANCH}'${NC}"
        echo -e "${RED}════════════════════════════════════════════════════════${NC}"
        echo ""
        echo -e "${YELLOW}This script supports the following branches:${NC}"
        echo "  • main"
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
