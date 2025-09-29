-- Generate 1000 departments with synthetic data
DO $$
DECLARE
    i INTEGER;
    dept_types TEXT[] := ARRAY[
        'Tecnologia', 'Vendas', 'Marketing', 'Recursos Humanos', 'Financeiro',
        'Operações', 'Suporte', 'Desenvolvimento', 'Design', 'Qualidade',
        'Produção', 'Logística', 'Compras', 'Jurídico', 'Auditoria',
        'Comunicação', 'Treinamento', 'Segurança', 'Manutenção', 'Pesquisa'
    ];
    regions TEXT[] := ARRAY[
        'Norte', 'Sul', 'Leste', 'Oeste', 'Centro', 'Nordeste', 'Sudeste',
        'Internacional', 'Regional', 'Nacional', 'Local', 'Global'
    ];
    dept_name TEXT;
    dept_description TEXT;
    budget_amount DECIMAL(12,2);
BEGIN
    FOR i IN 1..1000 LOOP
        -- Create varied department names
        IF i <= 20 THEN
            dept_name := dept_types[((i - 1) % array_length(dept_types, 1)) + 1];
        ELSE
            dept_name := dept_types[((i - 1) % array_length(dept_types, 1)) + 1] || ' ' ||
                        regions[((i - 1) % array_length(regions, 1)) + 1] || ' ' || i;
        END IF;

        dept_description := 'Departamento de ' || dept_name || ' responsável por atividades estratégicas da empresa';
        budget_amount := 50000.00 + (RANDOM() * 2000000)::DECIMAL(12,2);

        INSERT INTO departments (id, name, description, budget)
        VALUES (i, dept_name, dept_description, budget_amount);

        -- Progress logging every 100 inserts
        IF i % 100 = 0 THEN
            RAISE NOTICE 'Departments: % of 1000 created', i;
        END IF;
    END LOOP;
END $$;

-- Reset sequence
SELECT setval('departments_id_seq', 1000, true);