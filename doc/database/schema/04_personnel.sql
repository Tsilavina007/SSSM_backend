-- 4. PERSONNEL / AGENTS DE SÉCURITÉ
-- ============================================================

CREATE TABLE employee (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    agency_id UUID REFERENCES agency(id) ON DELETE SET NULL,
    user_id UUID UNIQUE REFERENCES app_user(id) ON DELETE SET NULL,
    employee_number VARCHAR(80) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    gender VARCHAR(30),
    date_of_birth DATE,
    phone VARCHAR(50),
    email VARCHAR(180),
    address TEXT,
    national_id VARCHAR(100),
    hire_date DATE,
    end_date DATE,
    position VARCHAR(100),
    status employee_status NOT NULL DEFAULT 'ACTIVE',
    photo_url TEXT,
    emergency_contact_name VARCHAR(180),
    emergency_contact_phone VARCHAR(50),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(company_id, employee_number)
);

CREATE TABLE employee_document (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES employee(id) ON DELETE CASCADE,
    document_type VARCHAR(100) NOT NULL,
    document_number VARCHAR(150),
    issue_date DATE,
    expiry_date DATE,
    file_url TEXT,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
