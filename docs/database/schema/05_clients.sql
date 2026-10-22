-- 5. CLIENTS / CONTACTS
-- ============================================================

CREATE TABLE client (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    agency_id UUID REFERENCES agency(id) ON DELETE SET NULL,
    code VARCHAR(80) NOT NULL,
    name VARCHAR(180) NOT NULL,
    legal_name VARCHAR(255),
    client_type VARCHAR(80),
    registration_number VARCHAR(100),
    tax_number VARCHAR(100),
    email VARCHAR(180),
    phone VARCHAR(50),
    address TEXT,
    city VARCHAR(100),
    country VARCHAR(100) DEFAULT 'Madagascar',
    payment_terms_days INTEGER NOT NULL DEFAULT 30 CHECK(payment_terms_days >= 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(company_id, code)
);

CREATE TABLE client_contact (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL REFERENCES client(id) ON DELETE CASCADE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    function VARCHAR(100),
    email VARCHAR(180),
    phone VARCHAR(50),
    is_primary BOOLEAN NOT NULL DEFAULT FALSE
);

-- ============================================================
