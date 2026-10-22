-- 12. FACTURATION / TVA / PAIEMENTS
-- ============================================================

CREATE TABLE tax_rate (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    rate NUMERIC(5,2) NOT NULL CHECK(rate >= 0 AND rate <= 100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE(company_id, name)
);

CREATE TABLE invoice (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES client(id) ON DELETE RESTRICT,
    contract_id UUID REFERENCES contract(id) ON DELETE SET NULL,
    invoice_number VARCHAR(100) NOT NULL,
    issue_date DATE NOT NULL DEFAULT CURRENT_DATE,
    due_date DATE NOT NULL,
    status invoice_status NOT NULL DEFAULT 'DRAFT',
    currency_code CHAR(3) NOT NULL DEFAULT 'MGA',
    subtotal NUMERIC(18,2) NOT NULL DEFAULT 0 CHECK(subtotal >= 0),
    discount_amount NUMERIC(18,2) NOT NULL DEFAULT 0 CHECK(discount_amount >= 0),
    tax_amount NUMERIC(18,2) NOT NULL DEFAULT 0 CHECK(tax_amount >= 0),
    total_amount NUMERIC(18,2) NOT NULL DEFAULT 0 CHECK(total_amount >= 0),
    amount_paid NUMERIC(18,2) NOT NULL DEFAULT 0 CHECK(amount_paid >= 0),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(company_id, invoice_number)
);

CREATE TABLE invoice_line (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL REFERENCES invoice(id) ON DELETE CASCADE,
    service_id UUID REFERENCES service(id) ON DELETE SET NULL,
    description VARCHAR(255) NOT NULL,
    quantity NUMERIC(12,2) NOT NULL DEFAULT 1 CHECK(quantity > 0),
    unit_price NUMERIC(18,2) NOT NULL DEFAULT 0 CHECK(unit_price >= 0),
    discount_rate NUMERIC(5,2) NOT NULL DEFAULT 0 CHECK(discount_rate BETWEEN 0 AND 100),
    tax_rate NUMERIC(5,2) NOT NULL DEFAULT 0 CHECK(tax_rate BETWEEN 0 AND 100),
    line_subtotal NUMERIC(18,2) NOT NULL DEFAULT 0,
    line_tax NUMERIC(18,2) NOT NULL DEFAULT 0,
    line_total NUMERIC(18,2) NOT NULL DEFAULT 0
);

CREATE TABLE payment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL REFERENCES invoice(id) ON DELETE RESTRICT,
    payment_reference VARCHAR(150),
    payment_date DATE NOT NULL DEFAULT CURRENT_DATE,
    amount NUMERIC(18,2) NOT NULL CHECK(amount > 0),
    method payment_method NOT NULL,
    status payment_status NOT NULL DEFAULT 'PENDING',
    transaction_reference VARCHAR(180),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE payment_allocation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID NOT NULL REFERENCES payment(id) ON DELETE CASCADE,
    invoice_id UUID NOT NULL REFERENCES invoice(id) ON DELETE CASCADE,
    amount NUMERIC(18,2) NOT NULL CHECK(amount > 0),
    UNIQUE(payment_id, invoice_id)
);

-- ============================================================
