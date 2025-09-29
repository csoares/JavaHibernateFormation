#!/bin/bash

# Single Performance Comparison Script
echo "🔥 Hibernate Performance Comparison"
echo "======================================"

BASE_URL="http://localhost:8080"

# Test endpoints
ENDPOINTS=(
    "/api/data/statistics"
    "/api/users?page=0&size=100"
    "/api/products?page=0&size=100"
)

# Function to test and time requests
test_requests() {
    local profile=$1
    local results=()

    for endpoint in "${ENDPOINTS[@]}"; do
        local start=$(date +%s.%N)
        if curl -s -f "$BASE_URL$endpoint" > /dev/null 2>&1; then
            local end=$(date +%s.%N)
            local duration=$(echo "$end - $start" | bc -l 2>/dev/null || echo "1")
            results+=("$duration")
        else
            results+=("FAILED")
        fi
    done

    echo "${results[@]}"
}

# Test Bad Performance
echo ""
echo "🔴 Testing BAD performance..."
docker-compose down > /dev/null 2>&1
PERFORMANCE_PROFILE=bad-performance docker-compose up -d > /dev/null 2>&1
sleep 30

# Check if app is ready
if curl -s -f "$BASE_URL/actuator/health" > /dev/null 2>&1; then
    bad_results=($(test_requests "bad"))
else
    echo "❌ Bad performance app failed to start"
    exit 1
fi

# Test Good Performance
echo "🟢 Testing GOOD performance..."
docker-compose down > /dev/null 2>&1
PERFORMANCE_PROFILE=good-performance docker-compose up -d > /dev/null 2>&1
sleep 30

# Check if app is ready
if curl -s -f "$BASE_URL/actuator/health" > /dev/null 2>&1; then
    good_results=($(test_requests "good"))
else
    echo "❌ Good performance app failed to start"
    exit 1
fi

# Display Comparison
echo ""
echo "📊 PERFORMANCE COMPARISON RESULTS"
echo "=================================="
printf "%-40s %-12s %-12s %-15s\n" "ENDPOINT" "BAD (s)" "GOOD (s)" "IMPROVEMENT"
echo "--------------------------------------------------------------------------------"

for i in "${!ENDPOINTS[@]}"; do
    endpoint=${ENDPOINTS[$i]}
    bad_time=${bad_results[$i]}
    good_time=${good_results[$i]}

    if [[ "$bad_time" != "FAILED" && "$good_time" != "FAILED" ]]; then
        improvement=$(echo "scale=1; ($bad_time - $good_time) / $bad_time * 100" | bc -l 2>/dev/null || echo "0")
        printf "%-40s %-12.3f %-12.3f %-15s\n" "$endpoint" "$bad_time" "$good_time" "${improvement}%"
    else
        printf "%-40s %-12s %-12s %-15s\n" "$endpoint" "$bad_time" "$good_time" "N/A"
    fi
done

echo ""
echo "💡 Key Differences:"
echo "   Bad:  2 connections, batch size 1, verbose logging"
echo "   Good: 50 connections, batch size 50, minimal logging"

# Cleanup
docker-compose down > /dev/null 2>&1
echo ""
echo "✅ Comparison completed!"