-- Generate 1000 categories with synthetic data
DO $$
DECLARE
    i INTEGER;
    base_categories TEXT[] := ARRAY[
        'Eletrônicos', 'Informática', 'Casa e Jardim', 'Esportes', 'Livros',
        'Roupas', 'Acessórios', 'Automotivo', 'Ferramentas', 'Saúde',
        'Alimentação', 'Bebidas', 'Móveis', 'Decoração', 'Brinquedos',
        'Música', 'Filmes', 'Games', 'Beleza', 'Pet Shop',
        'Papelaria', 'Arte', 'Joias', 'Relógios', 'Perfumes'
    ];
    subcategories TEXT[] := ARRAY[
        'Premium', 'Básico', 'Professional', 'Home', 'Industrial', 'Infantil',
        'Feminino', 'Masculino', 'Unissex', 'Vintage', 'Moderno', 'Clássico',
        'Ecológico', 'Digital', 'Artesanal', 'Importado', 'Nacional'
    ];
    category_name TEXT;
    category_description TEXT;
BEGIN
    FOR i IN 1..1000 LOOP
        -- Create varied category names
        IF i <= 25 THEN
            category_name := base_categories[i];
        ELSE
            category_name := base_categories[((i - 1) % array_length(base_categories, 1)) + 1] || ' ' ||
                           subcategories[((i - 1) % array_length(subcategories, 1)) + 1] || ' ' || i;
        END IF;

        category_description := 'Categoria especializada em produtos de ' || category_name ||
                              ' com foco em qualidade e variedade para atender todos os públicos';

        INSERT INTO categories (id, name, description)
        VALUES (i, category_name, category_description);

        -- Progress logging every 100 inserts
        IF i % 100 = 0 THEN
            RAISE NOTICE 'Categories: % of 1000 created', i;
        END IF;
    END LOOP;
END $$;

-- Reset sequence
SELECT setval('categories_id_seq', 1000, true);