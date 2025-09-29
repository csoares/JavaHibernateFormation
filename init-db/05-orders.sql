-- Generate 100,000 orders with synthetic data
DO $$
DECLARE
    i INTEGER;
    user_id INTEGER;
    order_number TEXT;
    total_amount DECIMAL(10,2);
    order_date TIMESTAMP;
    status_options TEXT[] := ARRAY['PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED'];
    order_status TEXT;
    fake_pdf BYTEA;
    discount_percent DECIMAL(5,2);
    shipping_cost DECIMAL(8,2);
    tax_amount DECIMAL(8,2);
    order_type TEXT;
    payment_method TEXT;
    customer_notes TEXT;
BEGIN
    FOR i IN 1..100000 LOOP
        user_id := 1 + (RANDOM() * 99999)::INTEGER; -- Users 1-100000
        order_number := 'ORD-' || to_char(CURRENT_DATE, 'YYYY') || '-' || lpad(i::TEXT, 8, '0');

        -- Generate realistic order amounts with some variation
        total_amount := CASE (i % 5)
            WHEN 0 THEN 25.00 + (RANDOM() * 200)::DECIMAL(10,2)    -- Small orders
            WHEN 1 THEN 200.00 + (RANDOM() * 800)::DECIMAL(10,2)   -- Medium orders
            WHEN 2 THEN 1000.00 + (RANDOM() * 5000)::DECIMAL(10,2) -- Large orders
            WHEN 3 THEN 50.00 + (RANDOM() * 500)::DECIMAL(10,2)    -- Regular orders
            ELSE 100.00 + (RANDOM() * 2000)::DECIMAL(10,2)         -- Mixed orders
        END;

        order_date := CURRENT_TIMESTAMP - INTERVAL '1 day' * (RANDOM() * 1095)::INTEGER; -- Last 3 years
        order_status := status_options[1 + (RANDOM() * (array_length(status_options, 1) - 1))::INTEGER];

        discount_percent := (RANDOM() * 25)::DECIMAL(5,2); -- 0-25% discount
        shipping_cost := 5.00 + (RANDOM() * 45)::DECIMAL(8,2); -- $5-50 shipping
        tax_amount := total_amount * 0.08; -- 8% tax

        order_type := CASE (i % 4)
            WHEN 0 THEN 'ONLINE'
            WHEN 1 THEN 'IN_STORE'
            WHEN 2 THEN 'PHONE'
            ELSE 'MOBILE_APP'
        END;

        payment_method := CASE (i % 6)
            WHEN 0 THEN 'CREDIT_CARD'
            WHEN 1 THEN 'DEBIT_CARD'
            WHEN 2 THEN 'PIX'
            WHEN 3 THEN 'BOLETO'
            WHEN 4 THEN 'PAYPAL'
            ELSE 'BANK_TRANSFER'
        END;

        customer_notes := CASE (i % 10)
            WHEN 0 THEN 'Entrega urgente solicitada'
            WHEN 1 THEN 'Embalagem para presente'
            WHEN 2 THEN 'Entregar apenas em horário comercial'
            WHEN 3 THEN 'Produto frágil - cuidado especial'
            WHEN 4 THEN 'Cliente VIP - prioridade alta'
            ELSE NULL
        END;

        -- Create fake PDF data with varying sizes (2KB to 15KB)
        fake_pdf := decode(
            '255044462d312e340a25e2e3cfd30a0a0a312030206f626a0a3c3c2f547970652f436174616c6f672f50616765732032203020522f4c616e67286465292f537472756374547265655061726573' ||
            repeat('414243444546', (RANDOM() * 500)::INTEGER),
            'hex'
        );

        INSERT INTO orders (id, order_number, order_date, total_amount, status, user_id, invoice_pdf)
        VALUES (i, order_number, order_date, total_amount, order_status, user_id, fake_pdf);

        -- Progress logging every 5000 inserts
        IF i % 5000 = 0 THEN
            RAISE NOTICE 'Orders: % of 100000 created', i;
        END IF;
    END LOOP;
END $$;

-- Reset sequence
SELECT setval('orders_id_seq', 100000, true);