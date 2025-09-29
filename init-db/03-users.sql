-- Generate 100,000 users with synthetic data
DO $$
DECLARE
    i INTEGER;
    first_names TEXT[] := ARRAY[
        'João', 'Maria', 'José', 'Ana', 'Pedro', 'Carla', 'Paulo', 'Fernanda',
        'Carlos', 'Juliana', 'Ricardo', 'Patricia', 'Antonio', 'Luciana', 'Marcos',
        'Roberto', 'Silvia', 'Fernando', 'Camila', 'Eduardo', 'Beatriz', 'Rafael',
        'Daniela', 'Gabriel', 'Larissa', 'Diego', 'Tatiana', 'Bruno', 'Adriana',
        'Gustavo', 'Priscila', 'Rodrigo', 'Vanessa', 'Alexandre', 'Mônica'
    ];
    last_names TEXT[] := ARRAY[
        'Silva', 'Santos', 'Oliveira', 'Sousa', 'Lima', 'Pereira', 'Costa',
        'Rodrigues', 'Martins', 'Jesus', 'Rocha', 'Ribeiro', 'Alves', 'Monteiro',
        'Ferreira', 'Barbosa', 'Cardoso', 'Correia', 'Teixeira', 'Morais',
        'Freitas', 'Gomes', 'Carvalho', 'Fernandes', 'Dias', 'Pinto', 'Araújo',
        'Neves', 'Soares', 'Castro', 'Lopes', 'Ramos', 'Miranda', 'Machado'
    ];
    first_name TEXT;
    last_name TEXT;
    dept_id INTEGER;
    created_date TIMESTAMP;
    full_name TEXT;
    email_addr TEXT;
BEGIN
    FOR i IN 1..100000 LOOP
        first_name := first_names[1 + (i % array_length(first_names, 1))];
        last_name := last_names[1 + ((i * 7) % array_length(last_names, 1))];
        dept_id := 1 + (i % 1000); -- Distribute across 1000 departments
        created_date := CURRENT_TIMESTAMP - INTERVAL '1 day' * (RANDOM() * 1095)::INTEGER; -- Random date in last 3 years

        full_name := first_name || ' ' || last_name;
        email_addr := lower(first_name) || '.' || lower(last_name) || '.' || i || '@empresa.com';

        INSERT INTO users (id, name, email, created_at, department_id)
        VALUES (i, full_name, email_addr, created_date, dept_id);

        -- Progress logging every 5000 inserts
        IF i % 5000 = 0 THEN
            RAISE NOTICE 'Users: % of 100000 created', i;
        END IF;
    END LOOP;
END $$;

-- Reset sequence
SELECT setval('users_id_seq', 100000, true);