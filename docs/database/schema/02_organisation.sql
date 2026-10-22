-- 2. ORGANISATION / RÉFÉRENTIEL
-- ============================================================

CREATE TABLE company (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(180) NOT NULL,
    legal_name VARCHAR(255),
    registration_number VARCHAR(100),
    tax_number VARCHAR(100),
    email VARCHAR(180),
    phone VARCHAR(50),
    address TEXT,
    city VARCHAR(100),
    country VARCHAR(100) DEFAULT 'Madagascar',
    logo_url TEXT,
    currency_code CHAR(3) NOT NULL DEFAULT 'MGA',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);


CREATE TABLE agency (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    name VARCHAR(180) NOT NULL,
    code VARCHAR(50),
    address TEXT,
    city VARCHAR(100),
    phone VARCHAR(50),
    email VARCHAR(180),
    tax_number VARCHAR(100),
    registration_number VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(company_id, code)
);

CREATE TABLE branch (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    agence_id UUID NOT NULL REFERENCES agency(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    code VARCHAR(50),
    address TEXT,
    city VARCHAR(100),
    phone VARCHAR(50),
    email VARCHAR(180),
    status site_status NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(company_id, code)
);

-- ============================================================
