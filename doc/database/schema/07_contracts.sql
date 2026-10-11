-- 7. CONTRATS ET PRESTATIONS
-- ============================================================

CREATE TABLE service (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    code VARCHAR(80) NOT NULL,
    name VARCHAR(180) NOT NULL,
    description TEXT,
    unit VARCHAR(50) NOT NULL DEFAULT 'month',
    unit_price NUMERIC(18,2) NOT NULL DEFAULT 0 CHECK(unit_price >= 0),
    vat_rate NUMERIC(5,2) NOT NULL DEFAULT 0 CHECK(vat_rate >= 0 AND vat_rate <= 100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE(company_id, code)
);

CREATE TABLE contract (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES client(id) ON DELETE RESTRICT,
    contract_number VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    status contract_status NOT NULL DEFAULT 'DRAFT',
    billing_frequency VARCHAR(30) NOT NULL DEFAULT 'MONTHLY',
    currency_code CHAR(3) NOT NULL DEFAULT 'MGA',
    payment_terms_days INTEGER NOT NULL DEFAULT 30,
    total_amount NUMERIC(18,2) NOT NULL DEFAULT 0 CHECK(total_amount >= 0),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(company_id, contract_number),
    CHECK(end_date IS NULL OR end_date >= start_date)
);

CREATE TABLE contract_service (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_id UUID NOT NULL REFERENCES contract(id) ON DELETE CASCADE,
    service_id UUID NOT NULL REFERENCES service(id) ON DELETE RESTRICT,
    site_id UUID REFERENCES site(id) ON DELETE SET NULL,
    quantity NUMERIC(12,2) NOT NULL DEFAULT 1 CHECK(quantity > 0),
    unit_price NUMERIC(18,2) NOT NULL DEFAULT 0 CHECK(unit_price >= 0),
    vat_rate NUMERIC(5,2) NOT NULL DEFAULT 0 CHECK(vat_rate >= 0 AND vat_rate <= 100),
    start_date DATE,
    end_date DATE,
    notes TEXT
);

-- ============================================================
