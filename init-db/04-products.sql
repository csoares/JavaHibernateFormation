-- Generate 100,000 products with synthetic data
DO $$
DECLARE
    i INTEGER;
    adjectives TEXT[] := ARRAY[
        'Premium', 'Deluxe', 'Pro', 'Ultra', 'Smart', 'Eco', 'Digital', 'Professional',
        'Advanced', 'Compact', 'Wireless', 'Portable', 'Heavy Duty', 'Lightweight',
        'Ergonomic', 'Durable', 'Flexible', 'Powerful', 'Efficient', 'Innovative',
        'Classic', 'Modern', 'Vintage', 'Futuristic', 'Reliable', 'Precision',
        'High-Performance', 'Multi-Function', 'Universal', 'Specialized'
    ];
    nouns TEXT[] := ARRAY[
        'Notebook', 'Mouse', 'Teclado', 'Monitor', 'Smartphone', 'Tablet', 'Fone',
        'Camera', 'Impressora', 'Roteador', 'HD', 'Memoria', 'Processor', 'Placa',
        'Fonte', 'Gabinete', 'Cooler', 'Ventilador', 'Cabo', 'Adaptador',
        'Carregador', 'Bateria', 'Display', 'Sensor', 'Antena', 'Conversor',
        'Amplificador', 'Controlador', 'Interface', 'Module', 'Drive', 'Scanner'
    ];
    brands TEXT[] := ARRAY[
        'TechMax', 'ProLine', 'EliteGear', 'Innovatech', 'PowerCore', 'SmartFlow',
        'UltraVision', 'PrecisionPro', 'FlexiTech', 'NextGen', 'MegaForce', 'HyperLink'
    ];
    adjective TEXT;
    noun TEXT;
    brand TEXT;
    product_name TEXT;
    price DECIMAL(10,2);
    stock INTEGER;
    category_id INTEGER;
    fake_image BYTEA;
    description TEXT;
BEGIN
    FOR i IN 1..100000 LOOP
        adjective := adjectives[1 + (i % array_length(adjectives, 1))];
        noun := nouns[1 + ((i * 3) % array_length(nouns, 1))];
        brand := brands[1 + ((i * 5) % array_length(brands, 1))];

        product_name := brand || ' ' || adjective || ' ' || noun || ' ' || (i % 9999 + 1);
        price := 10.00 + (RANDOM() * 9990)::DECIMAL(10,2);
        stock := 1 + (RANDOM() * 999)::INTEGER;
        category_id := 1 + (i % 1000); -- Distribute across 1000 categories

        description := 'Produto ' || product_name || ' desenvolvido com tecnologia avançada para ' ||
                      'proporcionar máxima performance e durabilidade. Ideal para uso ' ||
                      CASE (i % 4)
                          WHEN 0 THEN 'profissional em ambientes corporativos.'
                          WHEN 1 THEN 'doméstico com foco em praticidade.'
                          WHEN 2 THEN 'industrial com alta resistência.'
                          ELSE 'educacional e acadêmico.'
                      END;

        -- Create fake image data (varying sizes from 1KB to 5KB)
        fake_image := decode(
            lpad(to_hex((RANDOM() * 16777215)::INTEGER), 6, '0') ||
            lpad(to_hex((RANDOM() * 16777215)::INTEGER), 6, '0'),
            'hex'
        );

        INSERT INTO products (id, name, description, price, stock_quantity, category_id, image_data)
        VALUES (i, product_name, description, price, stock, category_id, fake_image);

        -- Progress logging every 5000 inserts
        IF i % 5000 = 0 THEN
            RAISE NOTICE 'Products: % of 100000 created', i;
        END IF;
    END LOOP;
END $$;

-- Reset sequence
SELECT setval('products_id_seq', 100000, true);