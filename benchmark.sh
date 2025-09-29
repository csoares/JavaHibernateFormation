#!/bin/bash

echo "🚀 Iniciando teste de performance..."

# Verificar se a aplicação está a correr
echo "📡 Verificando se a aplicação está a correr..."
if ! curl -s "http://localhost:8080/actuator/health" > /dev/null 2>&1; then
    echo "❌ Aplicação não está a correr em localhost:8080"
    echo "   Execute: mvn spring-boot:run"
    exit 1
fi
echo "✅ Aplicação está a correr"

# Verificar se os dados estão populados
echo "📊 Verificando se os dados estão populados..."
stats=$(curl -s "http://localhost:8080/api/data/statistics" 2>/dev/null)
if [[ $stats == *"users\":0"* ]]; then
    echo "📦 Populando dados de teste..."
    curl -s -X POST "http://localhost:8080/api/data/populate" > /dev/null
    echo "✅ Dados populados"
else
    echo "✅ Dados já existem"
fi

echo ""
echo "=================================="
echo "🏁 INICIANDO BENCHMARKS"
echo "=================================="
echo ""

# Função para medir tempo de resposta
measure_endpoint() {
    local name=$1
    local url=$2
    local iterations=${3:-5}
    
    echo "🔍 Testing: $name"
    echo "   URL: $url"
    
    # Array para armazenar tempos
    times=()
    total_time=0
    
    for i in $(seq 1 $iterations); do
        echo -n "   Request $i/$iterations... "
        
        # Medir tempo de resposta
        response_time=$(curl -o /dev/null -s -w '%{time_total}' "$url" 2>/dev/null)
        
        # Verificar se o curl foi bem-sucedido
        if [ $? -eq 0 ]; then
            times+=($response_time)
            total_time=$(echo "$total_time + $response_time" | bc -l)
            echo "${response_time}s"
        else
            echo "FALHOU"
            return 1
        fi
        
        # Pequena pausa entre requests
        sleep 0.5
    done
    
    # Calcular média
    avg_time=$(echo "scale=3; $total_time / $iterations" | bc -l)
    
    # Encontrar min e max
    min_time=${times[0]}
    max_time=${times[0]}
    for time in "${times[@]}"; do
        if (( $(echo "$time < $min_time" | bc -l) )); then
            min_time=$time
        fi
        if (( $(echo "$time > $max_time" | bc -l) )); then
            max_time=$time
        fi
    done
    
    echo "   📊 Resultados:"
    echo "      ⏱️  Tempo médio: ${avg_time}s"
    echo "      🏃 Mais rápido: ${min_time}s"
    echo "      🐌 Mais lento: ${max_time}s"
    echo ""
    
    # Retornar tempo médio para comparações
    echo "$avg_time"
}

# Armazenar resultados para comparação
declare -A results

echo "🔸 TESTE 1: Buscar usuário por ID"
echo "────────────────────────────────────"
good_user_time=$(measure_endpoint "GOOD - Buscar usuário" "http://localhost:8080/api/good/users/1")
bad_user_time=$(measure_endpoint "BAD - Buscar usuário" "http://localhost:8080/api/bad/users/1")

if [ -n "$good_user_time" ] && [ -n "$bad_user_time" ]; then
    ratio=$(echo "scale=2; $bad_user_time / $good_user_time" | bc -l)
    echo "📈 BAD é ${ratio}x mais lento que GOOD"
    echo ""
fi

echo "🔸 TESTE 2: Paginação vs Lista Completa"
echo "────────────────────────────────────────"
good_list_time=$(measure_endpoint "GOOD - Paginação (20 items)" "http://localhost:8080/api/good/users?page=0&size=20")
echo "⚠️  CUIDADO: O próximo teste pode ser muito lento..."
bad_list_time=$(measure_endpoint "BAD - Lista completa" "http://localhost:8080/api/bad/users" 3)

if [ -n "$good_list_time" ] && [ -n "$bad_list_time" ]; then
    ratio=$(echo "scale=2; $bad_list_time / $good_list_time" | bc -l)
    echo "📈 BAD é ${ratio}x mais lento que GOOD"
    echo ""
fi

echo "🔸 TESTE 3: Busca por Departamento"
echo "────────────────────────────────────"
good_dept_time=$(measure_endpoint "GOOD - Por Departamento (JOIN FETCH)" "http://localhost:8080/api/good/users/department/Tecnologia")
bad_dept_time=$(measure_endpoint "BAD - Por Departamento (N+1 Problem)" "http://localhost:8080/api/bad/users/department/Tecnologia")

if [ -n "$good_dept_time" ] && [ -n "$bad_dept_time" ]; then
    ratio=$(echo "scale=2; $bad_dept_time / $good_dept_time" | bc -l)
    echo "📈 BAD é ${ratio}x mais lento que GOOD"
    echo ""
fi

echo "🔸 TESTE 4: Sumários de Usuários"
echo "─────────────────────────────────"
good_summary_time=$(measure_endpoint "GOOD - Projeção JPQL" "http://localhost:8080/api/good/users/summaries")
bad_summary_time=$(measure_endpoint "BAD - Entidades completas" "http://localhost:8080/api/bad/users" 2)

if [ -n "$good_summary_time" ] && [ -n "$bad_summary_time" ]; then
    ratio=$(echo "scale=2; $bad_summary_time / $good_summary_time" | bc -l)
    echo "📈 BAD é ${ratio}x mais lento que GOOD"
    echo ""
fi

echo "=================================="
echo "📊 RESUMO DE PERFORMANCE"
echo "=================================="

echo ""
echo "🎯 Para ver logs detalhados e queries SQL:"
echo "   - Consulte os logs da aplicação"
echo "   - Acesse H2 Console: http://localhost:8080/h2-console"
echo ""

echo "📈 Para ver estatísticas da aplicação:"
echo "   curl http://localhost:8080/api/good/users/performance/summary"
echo "   curl http://localhost:8080/api/bad/users/performance/summary"
echo ""

echo "✅ Teste de benchmark concluído!"
echo ""
echo "💡 Dica: Execute o benchmark várias vezes para obter resultados mais consistentes"
echo "   (O JVM pode otimizar o código após algumas execuções - JIT compilation)"