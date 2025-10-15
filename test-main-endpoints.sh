#!/bin/bash

# ===========================================
# Main Branch - Complete Endpoint Testing Script
# ===========================================
# Tests all endpoints on main branch to verify functionality
#
# USAGE:
#   1. Make sure application is running: mvn spring-boot:run
#   2. Run this script: ./test-main-endpoints.sh
# ===========================================

set -e

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m' # No Color

# Configuration
APP_URL="http://localhost:8080"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Main Branch - Endpoint Testing${NC}"
echo -e "${GREEN}========================================${NC}\n"

# Check if application is running
echo -e "${BLUE}Checking if application is running...${NC}"
if ! curl -s "${APP_URL}/actuator/health" > /dev/null 2>&1; then
    if ! curl -s "${APP_URL}" > /dev/null 2>&1; then
        echo -e "${RED}ERROR: Application is not running!${NC}"
        echo "Start it with: mvn spring-boot:run"
        exit 1
    fi
fi
echo -e "${GREEN}✓ Application is running${NC}\n"

# Helper function to test endpoint
test_endpoint() {
    local description="$1"
    local url="$2"
    local show_output="${3:-false}"

    echo -e "${CYAN}Testing: ${description}${NC}"
    echo -e "${BLUE}URL: ${url}${NC}"

    # Make request and capture response
    response=$(curl -s -w "\n%{http_code}" "${url}" 2>&1)
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)

    # Check status code
    if [ "$http_code" == "200" ]; then
        echo -e "${GREEN}✓ SUCCESS (HTTP $http_code)${NC}"
        if [ "$show_output" == "true" ]; then
            echo -e "${YELLOW}Response:${NC}"
            echo "$body" | python3 -m json.tool 2>/dev/null | head -30 || echo "$body" | head -30
        fi
    elif [ "$http_code" == "404" ]; then
        echo -e "${RED}✗ NOT FOUND (HTTP $http_code)${NC}"
    elif [ "$http_code" == "500" ]; then
        echo -e "${RED}✗ SERVER ERROR (HTTP $http_code)${NC}"
        echo -e "${RED}Response: $body${NC}"
    else
        echo -e "${YELLOW}⚠ UNEXPECTED (HTTP $http_code)${NC}"
    fi
    echo ""
}

# Helper function for section headers
section() {
    echo -e "\n${MAGENTA}═══════════════════════════════════════${NC}"
    echo -e "${MAGENTA}$1${NC}"
    echo -e "${MAGENTA}═══════════════════════════════════════${NC}\n"
}

# ===========================================
# BASIC ENDPOINTS
# ===========================================
section "1. BASIC ENDPOINTS (Simple Controllers)"

test_endpoint "Get single user" \
    "${APP_URL}/api/users/1"

test_endpoint "Get paginated users" \
    "${APP_URL}/api/users?page=0&size=10"

test_endpoint "Search users by name" \
    "${APP_URL}/api/users/search?name=Maria&page=0&size=5"

test_endpoint "Get users by department" \
    "${APP_URL}/api/users/department/2?page=0&size=5"

test_endpoint "Get single order" \
    "${APP_URL}/api/orders/599776"

test_endpoint "Get paginated orders" \
    "${APP_URL}/api/orders?page=0&size=10"

test_endpoint "Get orders by user" \
    "${APP_URL}/api/orders/user/1?page=0&size=5"

test_endpoint "Get orders by status" \
    "${APP_URL}/api/orders/status/PENDING?page=0&size=5"

# ===========================================
# GOOD ENDPOINTS (Best Practices)
# ===========================================
section "2. GOOD ENDPOINTS (Best Practices with EntityGraph)"

echo -e "${BLUE}--- User Endpoints (Optimized) ---${NC}\n"

test_endpoint "Get user with EntityGraph (optimized)" \
    "${APP_URL}/api/good/users/1"

test_endpoint "Get paginated users with EntityGraph" \
    "${APP_URL}/api/good/users?page=0&size=10"

test_endpoint "Get paginated users with custom sort" \
    "${APP_URL}/api/good/users?page=0&size=10&sortBy=name"

test_endpoint "Get user summaries (DTO projection)" \
    "${APP_URL}/api/good/users/summaries/paginated?page=0&size=20"

test_endpoint "Get users by department name" \
    "${APP_URL}/api/good/users/department/Vendas"

test_endpoint "Get user by email" \
    "${APP_URL}/api/good/users/email/maria.rodrigues.1@empresa.com"

test_endpoint "Count users by department" \
    "${APP_URL}/api/good/users/department/2/count"

test_endpoint "Get performance summary" \
    "${APP_URL}/api/good/users/performance/summary"

echo -e "${BLUE}--- Order Endpoints (Optimized) ---${NC}\n"

test_endpoint "Get order by ID (optimized with native query)" \
    "${APP_URL}/api/good/orders/599776"

test_endpoint "Get orders by status (paginated)" \
    "${APP_URL}/api/good/orders?status=PENDING&page=0&size=10"

test_endpoint "Get order summaries by user (DTO projection)" \
    "${APP_URL}/api/good/orders/user/1/summaries"

test_endpoint "Get order by number" \
    "${APP_URL}/api/good/orders/number/ORD-0000499776"

test_endpoint "Get order statistics" \
    "${APP_URL}/api/good/orders/statistics"

echo -e "${BLUE}--- BLOB/PDF Endpoints (Download & Metadata) ---${NC}\n"

test_endpoint "Check if order has PDF invoice" \
    "${APP_URL}/api/good/orders/599776/invoice/exists"

test_endpoint "Get PDF invoice metadata" \
    "${APP_URL}/api/good/orders/599776/invoice/metadata" true

echo -e "${CYAN}Testing: Download PDF invoice${NC}"
echo -e "${BLUE}URL: ${APP_URL}/api/good/orders/599776/invoice/download${NC}"
response=$(curl -s -w "\n%{http_code}" "${APP_URL}/api/good/orders/599776/invoice/download" -o /tmp/test-invoice.pdf 2>&1)
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" == "200" ]; then
    if [ -f /tmp/test-invoice.pdf ]; then
        file_size=$(stat -f%z /tmp/test-invoice.pdf 2>/dev/null || stat -c%s /tmp/test-invoice.pdf 2>/dev/null)
        echo -e "${GREEN}✓ SUCCESS - PDF downloaded (${file_size} bytes)${NC}"
        echo -e "${YELLOW}File saved to: /tmp/test-invoice.pdf${NC}"
        rm -f /tmp/test-invoice.pdf
    else
        echo -e "${GREEN}✓ SUCCESS (HTTP $http_code)${NC}"
    fi
elif [ "$http_code" == "204" ]; then
    echo -e "${YELLOW}⚠ NO CONTENT - Order has no PDF${NC}"
elif [ "$http_code" == "404" ]; then
    echo -e "${RED}✗ NOT FOUND (HTTP $http_code)${NC}"
else
    echo -e "${RED}✗ ERROR (HTTP $http_code)${NC}"
fi
echo ""

# ===========================================
# BAD ENDPOINTS (Anti-patterns for Comparison)
# ===========================================
section "3. BAD ENDPOINTS (Anti-patterns - N+1 Problems)"

echo -e "${YELLOW}⚠ WARNING: These endpoints demonstrate BAD practices intentionally!${NC}"
echo -e "${YELLOW}   They are slower and less efficient for educational comparison.${NC}\n"

echo -e "${BLUE}--- User Endpoints (N+1 Problems) ---${NC}\n"

test_endpoint "Get user WITHOUT optimization (N+1)" \
    "${APP_URL}/api/bad/users/1"

test_endpoint "Get users without pagination (DANGEROUS!)" \
    "${APP_URL}/api/bad/users?page=0&size=10"

test_endpoint "Get users by department (memory filtering)" \
    "${APP_URL}/api/bad/users/department/Vendas"

test_endpoint "Get user by email (loads all users!)" \
    "${APP_URL}/api/bad/users/email/maria.rodrigues.1@empresa.com"

test_endpoint "Count users by department (inefficient)" \
    "${APP_URL}/api/bad/users/department/2/count"

echo -e "${BLUE}--- Order Endpoints (N+1 Problems) ---${NC}\n"

test_endpoint "Get order WITHOUT optimization (N+1)" \
    "${APP_URL}/api/bad/orders/1"

test_endpoint "Get orders by status (memory filtering)" \
    "${APP_URL}/api/bad/orders/status/PENDING"

test_endpoint "Get order by number (loads all!)" \
    "${APP_URL}/api/bad/orders/number/ORD-0000499776"

# ===========================================
# DATA & STATISTICS ENDPOINTS
# ===========================================
section "4. DATA & STATISTICS ENDPOINTS"

test_endpoint "Get database statistics" \
    "${APP_URL}/api/data/statistics" true

# ===========================================
# PERFORMANCE COMPARISON
# ===========================================
section "5. PERFORMANCE COMPARISON (BAD vs GOOD)"

echo -e "${CYAN}Comparing response times for same operation...${NC}\n"

echo -e "${RED}❌ BAD: Get user with N+1 problem${NC}"
time curl -s "${APP_URL}/api/bad/users/1" > /dev/null
echo ""

echo -e "${GREEN}✅ GOOD: Get user with EntityGraph${NC}"
time curl -s "${APP_URL}/api/good/users/1" > /dev/null
echo ""

echo -e "${RED}❌ BAD: Get paginated users (N+1 for each)${NC}"
time curl -s "${APP_URL}/api/bad/users?page=0&size=20" > /dev/null
echo ""

echo -e "${GREEN}✅ GOOD: Get paginated users (optimized)${NC}"
time curl -s "${APP_URL}/api/good/users?page=0&size=20" > /dev/null
echo ""

echo -e "${RED}❌ BAD: Get order with nested N+1${NC}"
time curl -s "${APP_URL}/api/bad/orders/599776" > /dev/null
echo ""

echo -e "${GREEN}✅ GOOD: Get order with optimized query${NC}"
time curl -s "${APP_URL}/api/good/orders/599776" > /dev/null
echo ""

# ===========================================
# SUMMARY
# ===========================================
section "TESTING COMPLETE"

echo -e "${GREEN}All endpoint tests completed!${NC}\n"
echo -e "${BLUE}Summary:${NC}"
echo -e "  • Basic endpoints: /api/users, /api/orders"
echo -e "  • Good endpoints: /api/good/users, /api/good/orders"
echo -e "  • Bad endpoints: /api/bad/users, /api/bad/orders"
echo -e "  • Statistics: /api/data/statistics"
echo ""
echo -e "${YELLOW}Key Observations:${NC}"
echo -e "  • GOOD endpoints use EntityGraph, JOIN FETCH, and DTO projections"
echo -e "  • BAD endpoints demonstrate N+1 problems and memory filtering"
echo -e "  • Performance difference is visible even with small datasets"
echo ""
echo -e "${CYAN}For detailed performance metrics, check application logs!${NC}\n"
