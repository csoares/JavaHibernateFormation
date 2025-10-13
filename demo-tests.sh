#!/bin/bash

# ===========================================
# N+1 Problem Demo Testing Script
# ===========================================
# Branch: 001-n1problem
#
# This script demonstrates the N+1 query problem and its solutions
#
# USAGE:
#   1. Start your application: mvn spring-boot:run -Dspring-boot.run.profiles=docker
#   2. In another terminal: ./demo-tests.sh
#
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

# Check if application is running
if ! curl -s "${APP_URL}/actuator/health" > /dev/null 2>&1; then
    echo -e "${RED}Error: Application is not running at ${APP_URL}${NC}"
    echo -e "${YELLOW}Please start the application first:${NC}"
    echo -e "  mvn spring-boot:run -Dspring-boot.run.profiles=docker"
    exit 1
fi

echo -e "${GREEN}╔═══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║          N+1 PROBLEM DEMONSTRATION (001-n1problem)           ║${NC}"
echo -e "${GREEN}╚═══════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${CYAN}This branch demonstrates:${NC}"
echo "  • The N+1 query problem (lazy loading issue)"
echo "  • Solutions: EntityGraph and JOIN FETCH"
echo "  • Performance comparison at different scales"
echo ""

# Function to test a single endpoint
test_endpoint() {
    local NAME="$1"
    local URL="$2"
    local REPEAT="$3"

    echo -e "${CYAN}Testing: ${NAME}${NC}"
    echo -e "${BLUE}URL: ${URL}${NC}"
    echo -e "${BLUE}Repeating: ${REPEAT} times${NC}"
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

# Main loop
while true; do
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${YELLOW}SELECT TEST TYPE${NC}"
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    echo "  1) Single User Access - Quick demo (2 queries vs 1 query)"
    echo "  2) Batch Operations - Performance comparison"
    echo "  3) Compare Solutions - EntityGraph vs JOIN FETCH"
    echo "  4) Exit"
    echo ""
    read -p "Enter choice [1-4]: " TEST_CHOICE

    case $TEST_CHOICE in
    1)
        echo ""
        echo -e "${GREEN}═══════════════════════════════════════${NC}"
        echo -e "${GREEN}Single User Access Test${NC}"
        echo -e "${GREEN}═══════════════════════════════════════${NC}"
        echo ""

        compare_endpoints \
            "❌ BAD: N+1 (2 queries)" \
            "${APP_URL}/api/n1-demo/bad/1" \
            "✅ GOOD: EntityGraph (1 query)" \
            "${APP_URL}/api/n1-demo/good-entitygraph/1"

        echo ""
        echo -e "${YELLOW}💡 What happened?${NC}"
        echo "  ❌ BAD: First query fetches User, second query fetches Department"
        echo "  ✅ GOOD: Single query with EntityGraph fetches both"
        echo ""
        echo -e "${CYAN}Check application logs to see the SQL queries!${NC}"
        ;;

    2)
        echo ""
        echo -e "${GREEN}═══════════════════════════════════════${NC}"
        echo -e "${GREEN}Batch Operations Test${NC}"
        echo -e "${GREEN}═══════════════════════════════════════${NC}"
        echo ""
        echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
        echo -e "${YELLOW}Select Batch Test Size${NC}"
        echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
        echo ""
        echo -e "  ${GREEN}1) Medium (1000 users)${NC} - ⭐ ${GREEN}RECOMMENDED${NC} ⭐"
        echo -e "     ${GREEN}▶ BEST for demonstrations!${NC}"
        echo "     • GOOD is 5-10x faster (~250ms → ~50ms)"
        echo "     • Realistic business scenario"
        echo "     • Clear, visible performance improvement"
        echo "     • Query time is dominant factor"
        echo ""
        echo -e "  2) Small (10 users) - Educational"
        echo "     • GOOD is 4-5x faster (27ms → 6ms)"
        echo "     • Easy to count queries in logs"
        echo "     • Perfect for teaching the concept"
        echo ""
        echo -e "  ${YELLOW}3) Large (1.1M users)${NC} - Advanced/Academic"
        echo "     • Shows why N+1 hides in production metrics"
        echo "     • Network overhead dominates"
        echo "     • Demonstrates counter-intuitive behavior"
        echo ""
        read -p "Enter choice [1-3] (default: 1): " BATCH_CHOICE
        BATCH_CHOICE=${BATCH_CHOICE:-1}

        case $BATCH_CHOICE in
            1)
                echo ""
                echo -e "${CYAN}══ Medium Batch (1000 users) - REALISTIC Performance Demo ══${NC}"
                echo -e "${GREEN}⭐ This is the SWEET SPOT for demonstrating real improvement!${NC}"
                echo ""
                echo "Perfect balance for demonstrations:"
                echo "  ✅ Large enough to show real performance impact"
                echo "  ✅ Small enough that query time dominates"
                echo "  ✅ Represents typical business use case"
                echo ""
                echo "Expected results:"
                echo "  ❌ BAD: ~200-400ms (1001 queries)"
                echo "  ✅ GOOD: ~40-80ms (1 query)"
                echo "  🎯 GOOD should be 5-10x faster!"
                echo ""

                compare_endpoints \
                    "❌ BAD: Medium batch (1,001 queries!)" \
                    "${APP_URL}/api/n1-demo/batch-bad-medium" \
                    "✅ GOOD: Medium batch (1 query!)" \
                    "${APP_URL}/api/n1-demo/batch-good-medium"

                echo ""
                echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
                echo -e "${YELLOW}📊 ANALYSIS: Medium Batch Results${NC}"
                echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
                echo ""
                echo -e "${YELLOW}💡 Why this test shows clear improvement:${NC}"
                echo ""
                echo "  1️⃣  Query time is the dominant factor:"
                echo "      • BAD: ~200-300ms database time"
                echo "      • GOOD: ~40-60ms database time"
                echo "      • Network: ~50-100ms (much smaller impact)"
                echo ""
                echo "  2️⃣  Realistic scenario:"
                echo "      • Typical admin page: \"Show 1000 recent users\""
                echo "      • Export functionality"
                echo "      • Batch processing"
                echo ""
                echo "  3️⃣  Clear metrics:"
                echo "      • BAD: 1,001 JDBC statements"
                echo "      • GOOD: 1 JDBC statement"
                echo "      • 1000x reduction in database calls"
                echo ""
                echo -e "${GREEN}✅ This demonstrates the REAL VALUE of fixing N+1!${NC}"
                echo "   • Visible performance improvement (5-10x faster)"
                echo "   • Lower database load (1000x fewer queries)"
                echo "   • Better scalability under concurrent load"
                echo "   • Represents actual business scenarios"
                ;;

            2)
                echo ""
                echo -e "${CYAN}══ Small Batch (10 users) - N+1 Problem CLEARLY Visible ══${NC}"
                echo -e "${YELLOW}Best for teaching and understanding the concept${NC}"
                echo ""
                echo "With only 10 users, you'll clearly see:"
                echo "  ❌ BAD: 11 queries (1 for users + 10 for departments)"
                echo "  ✅ GOOD: 1 query (users + departments in single JOIN)"
                echo ""

                compare_endpoints \
                    "❌ BAD: Small batch (11 queries!)" \
                    "${APP_URL}/api/n1-demo/batch-bad-limited" \
                    "✅ GOOD: Small batch (1 query!)" \
                    "${APP_URL}/api/n1-demo/batch-good-limited"

                echo ""
                echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
                echo -e "${YELLOW}📊 ANALYSIS: Small Batch Results${NC}"
                echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
                echo ""
                echo -e "${YELLOW}💡 Look at:${NC}"
                echo -e "   1️⃣  Query count in HTTP response:"
                echo -e "      ${RED}BAD: 'Total queries: 11'${NC}"
                echo -e "      ${GREEN}GOOD: 'Total queries: 1'${NC}"
                echo ""
                echo -e "   2️⃣  Performance difference (GOOD should be 4-5x faster):"
                echo -e "      ${RED}BAD: ~20-30ms${NC} (11 round trips to database)"
                echo -e "      ${GREEN}GOOD: ~5-10ms${NC} (1 round trip to database)"
                echo ""
                echo -e "   3️⃣  Application logs (if SQL logging enabled):"
                echo -e "      ${RED}BAD: Count SELECT statements = 11${NC}"
                echo -e "      ${GREEN}GOOD: Count SELECT statements = 1${NC}"
                echo ""
                echo -e "${GREEN}✅ Perfect for understanding the N+1 concept!${NC}"
                ;;

            3)
                echo ""
                echo -e "${CYAN}══ Large Batch (1.1M users) - Advanced Academic Demo ══${NC}"
                echo -e "${RED}⚠️  WARNING: This will load 1.1 MILLION users!${NC}"
                echo -e "${RED}⚠️  Expected time: ~3.5 seconds per request${NC}"
                echo ""
                echo "This demonstrates N+1 at MASSIVE scale with counter-intuitive results:"
                echo "  ❌ BAD: ~1,001 queries (Hibernate batches department lookups)"
                echo "  ✅ GOOD: 1 query with LEFT JOIN on 1.1M rows"
                echo ""
                echo -e "${YELLOW}⚠️  SURPRISE: GOOD may actually be SLOWER!${NC}"
                echo "     Database JOINs on 1.1M rows can be slower than batched lookups"
                echo ""

                read -p "Continue with extreme test? (y/n): " CONFIRM
                if [ "$CONFIRM" = "y" ]; then
                    compare_endpoints \
                        "❌ BAD: Large batch (1,001 queries!)" \
                        "${APP_URL}/api/n1-demo/batch-bad" \
                        "✅ GOOD: Large batch (1 query!)" \
                        "${APP_URL}/api/n1-demo/batch-good"

                    echo ""
                    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
                    echo -e "${YELLOW}📊 ANALYSIS: Why N+1 Hides in Production${NC}"
                    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
                    echo ""
                    echo -e "${RED}❌ BAD (from Hibernate logs):${NC}"
                    echo "   • 1,001 JDBC statements"
                    echo "   • Database time: ~504ms"
                    echo ""
                    echo -e "${GREEN}✅ GOOD (from Hibernate logs):${NC}"
                    echo "   • 1 JDBC statement (LEFT JOIN)"
                    echo "   • Database time: ~670ms"
                    echo ""
                    echo -e "${YELLOW}⚠️  JOIN is SLOWER! (670ms vs 504ms)${NC}"
                    echo "   Why? Hibernate only fetches ~1,000 unique departments"
                    echo "   JOIN must process 1.1M rows vs 1,000 fast lookups"
                    echo ""
                    echo "  But HTTP response time is ~3.5s for BOTH:"
                    echo "  ┌─────────────────────────────────────────┐"
                    echo "  │ 🌐 Network (1.1M lines):  ~2.5s  (71%) │"
                    echo "  │ 🧵 String building:       ~0.6s  (17%) │"
                    echo "  │ 💾 Memory/JSON:           ~0.2s   (6%) │"
                    echo "  │ 🗃️  Database:              ~0.5s  (14%) │"
                    echo "  └─────────────────────────────────────────┘"
                    echo ""
                    echo -e "${RED}💡 KEY INSIGHTS:${NC}"
                    echo "  1️⃣  Database time (500ms) hidden by overhead (3000ms)"
                    echo "  2️⃣  Real problem: concurrent load (10 users × 1,001 = 10,010 queries)"
                    echo "  3️⃣  This is why N+1 is dangerous - hard to detect!"
                    echo ""
                    echo -e "${GREEN}✅ FOR CLEAR DEMO: Use option 1 (Medium - 1000 users)${NC}"
                    echo "   Shows 5-10x improvement with realistic scenario"
                fi
                ;;
        esac
        ;;

    3)
        echo ""
        echo -e "${GREEN}═══════════════════════════════════════${NC}"
        echo -e "${GREEN}Compare Solutions: EntityGraph vs JOIN FETCH${NC}"
        echo -e "${GREEN}═══════════════════════════════════════${NC}"
        echo ""
        echo "Both solutions resolve the N+1 problem, but in different ways:"
        echo ""

        echo -e "${CYAN}Testing EntityGraph solution:${NC}"
        test_endpoint "✅ EntityGraph (1 query)" \
            "${APP_URL}/api/n1-demo/good-entitygraph/1" $REPEAT_COUNT

        echo -e "${CYAN}Testing JOIN FETCH solution:${NC}"
        test_endpoint "✅ JOIN FETCH (1 query)" \
            "${APP_URL}/api/n1-demo/good-joinfetch/1" $REPEAT_COUNT

        echo ""
        echo -e "${YELLOW}💡 Comparison:${NC}"
        echo "  • EntityGraph: Annotation-based, reusable, cleaner code"
        echo "  • JOIN FETCH: Query-specific, explicit control, visible in JPQL"
        echo ""
        echo "  Both perform similarly - choose based on your needs:"
        echo "  - EntityGraph: When you want reusable loading strategies"
        echo "  - JOIN FETCH: When you need fine-grained query control"
        ;;

    4)
        echo ""
        echo -e "${GREEN}Thanks for using the N+1 Problem Demo!${NC}"
        echo ""
        echo -e "${CYAN}Remember:${NC}"
        echo "  • Always check for N+1 problems in production"
        echo "  • Use EntityGraph or JOIN FETCH to optimize"
        echo "  • Test with realistic data volumes"
        echo "  • Monitor database query counts, not just response times"
        echo ""
        exit 0
        ;;

    *)
        echo -e "${RED}Invalid choice. Please enter 1-4.${NC}"
        echo ""
        continue
        ;;
    esac

    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    read -p "Press Enter to return to main menu..."
    echo ""
done
