#!/bin/bash

# Demo Test Scripts for Hibernate Performance
# This script provides quick commands to test performance across branches

set -e

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Hibernate Performance Demo Tests${NC}"
echo -e "${GREEN}========================================${NC}\n"

# Check if PostgreSQL is running
if ! docker ps | grep -q hibernate-postgres; then
    echo -e "${RED}ERROR: PostgreSQL is not running!${NC}"
    echo "Start it with: docker-compose up -d postgresql"
    exit 1
fi

# Check database has data
USER_COUNT=$(docker exec hibernate-postgres psql -U hibernate_user -d hibernate_formation -t -c "SELECT COUNT(*) FROM users;" | xargs)

if [ "$USER_COUNT" -lt 100000 ]; then
    echo -e "${YELLOW}WARNING: Database appears empty or has insufficient data!${NC}"
    echo "Current user count: $USER_COUNT"
    echo "Load data with: mvn spring-boot:run -Dspring-boot.run.profiles=data-loader"
    exit 1
fi

echo -e "${GREEN}✓ Database ready with $USER_COUNT users${NC}\n"

# Function to test endpoint with timing
test_endpoint() {
    local NAME="$1"
    local URL="$2"

    echo -e "${YELLOW}Testing: $NAME${NC}"
    echo "URL: $URL"

    START_TIME=$(date +%s%3N)
    RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}\nTIME_TOTAL:%{time_total}" "$URL" || true)
    END_TIME=$(date +%s%3N)

    HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE:" | cut -d':' -f2)
    TIME_TOTAL=$(echo "$RESPONSE" | grep "TIME_TOTAL:" | cut -d':' -f2)

    if [ "$HTTP_CODE" = "200" ]; then
        echo -e "${GREEN}✓ Success - Time: ${TIME_TOTAL}s${NC}"
    else
        echo -e "${RED}✗ Failed - HTTP Code: ${HTTP_CODE}${NC}"
    fi

    echo ""
}

# Show menu
echo "Select which demo to run:"
echo "  1) N+1 Problem Demo (branch: 001-n1problem)"
echo "  2) Pagination Demo (branch: 002-pagination)"
echo "  3) BLOB Management Demo (branch: 003-blob-management)"
echo "  4) All demos"
echo ""
read -p "Enter choice [1-4]: " CHOICE

case $CHOICE in
    1)
        echo -e "\n${GREEN}=== N+1 PROBLEM DEMO ===${NC}\n"
        echo "This demonstrates the N+1 query problem:"
        echo "- BAD: Fetches user, then makes separate query for department"
        echo "- GOOD: Uses EntityGraph or JOIN FETCH to get everything in one query"
        echo ""

        echo "Testing single user access:"
        read -p "Press Enter to test BAD endpoint (N+1 problem)..."
        test_endpoint "BAD N+1 (single user)" "http://localhost:8080/api/n1-demo/bad/1"

        read -p "Press Enter to test GOOD with EntityGraph..."
        test_endpoint "GOOD with EntityGraph" "http://localhost:8080/api/n1-demo/good-entitygraph/1"

        read -p "Press Enter to test GOOD with JOIN FETCH..."
        test_endpoint "GOOD with JOIN FETCH" "http://localhost:8080/api/n1-demo/good-joinfetch/1"

        echo ""
        echo "Testing batch operations (this shows N+1 clearly):"
        read -p "Press Enter to test BAD batch (will execute many queries!)..."
        test_endpoint "BAD Batch (N+1 for all users!)" "http://localhost:8080/api/n1-demo/batch-bad"

        read -p "Press Enter to test GOOD batch..."
        test_endpoint "GOOD Batch (single query)" "http://localhost:8080/api/n1-demo/batch-good"

        echo -e "${YELLOW}Check the application logs to see the SQL queries!${NC}"
        echo "BAD version: 1 query for users + N queries for departments"
        echo "GOOD version: 1 query with JOIN"
        ;;

    2)
        echo -e "\n${GREEN}=== PAGINATION DEMO ===${NC}\n"
        echo "This demonstrates pagination strategies:"
        echo "- OFFSET: Performance degrades with higher page numbers"
        echo "- KEYSET: Consistent performance regardless of page"
        echo ""

        read -p "Press Enter to test OFFSET pagination (page 0)..."
        test_endpoint "OFFSET Page 0" "http://localhost:8080/api/pagination/offset?page=0&size=100"

        read -p "Press Enter to test OFFSET pagination (page 1000)..."
        test_endpoint "OFFSET Page 1000 (SLOW!)" "http://localhost:8080/api/pagination/offset?page=1000&size=100"

        read -p "Press Enter to test KEYSET pagination (start)..."
        test_endpoint "KEYSET Start" "http://localhost:8080/api/pagination/keyset?lastId=0&size=100"

        read -p "Press Enter to test KEYSET pagination (middle)..."
        test_endpoint "KEYSET Middle (FAST!)" "http://localhost:8080/api/pagination/keyset?lastId=100000&size=100"

        echo -e "${YELLOW}Notice: OFFSET gets slower, KEYSET stays fast!${NC}"
        ;;

    3)
        echo -e "\n${GREEN}=== BLOB MANAGEMENT DEMO ===${NC}\n"
        echo "This demonstrates BLOB loading strategies:"
        echo "- BAD: Eagerly loads all 1MB images (massive memory/network)"
        echo "- GOOD: Lazy loads, only fetches blobs when accessed"
        echo ""

        read -p "Press Enter to test BAD blob loading (WARNING: loads 100MB!)..."
        test_endpoint "BAD BLOB Loading" "http://localhost:8080/api/blob/bad/products?limit=100"

        read -p "Press Enter to test GOOD blob loading..."
        test_endpoint "GOOD BLOB Loading" "http://localhost:8080/api/blob/good/products?limit=100"

        echo -e "${YELLOW}BAD version: Transfers ~100MB of blob data${NC}"
        echo -e "${YELLOW}GOOD version: Only transfers metadata, no blobs${NC}"
        ;;

    4)
        echo -e "\n${GREEN}Running all demos...${NC}\n"
        echo "This will run all performance comparisons"
        echo "You'll need to switch branches manually between tests"
        echo ""
        echo -e "${RED}Feature not yet implemented - run tests individually${NC}"
        ;;

    *)
        echo -e "${RED}Invalid choice${NC}"
        exit 1
        ;;
esac

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Demo completed!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "Tips:"
echo "  - Watch SQL logs: docker-compose logs -f hibernate-app"
echo "  - Monitor database: docker exec hibernate-postgres pg_top -U hibernate_user -d hibernate_formation"
echo "  - Check memory: docker stats hibernate-postgres"
echo ""
