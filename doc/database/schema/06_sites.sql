-- 6. SITES / POSTES
-- ============================================================

CREATE TABLE site (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    client_id UUID REFERENCES client(id) ON DELETE SET NULL,
    branch_id UUID REFERENCES branch(id) ON DELETE SET NULL,
    name VARCHAR(180) NOT NULL,
    code VARCHAR(80),
    address TEXT,
    city VARCHAR(100),
    latitude NUMERIC(10,7),
    longitude NUMERIC(10,7),
    status site_status NOT NULL DEFAULT 'ACTIVE',
    access_instructions TEXT,
    risk_level VARCHAR(30),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(company_id, code)
);


CREATE TABLE security_post (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    site_id UUID NOT NULL REFERENCES site(id) ON DELETE CASCADE,
    name VARCHAR(180) NOT NULL,
    code VARCHAR(80),
    description TEXT,
    required_agents INTEGER NOT NULL DEFAULT 1 CHECK(required_agents > 0),
    start_time TIME,
    end_time TIME,
    status post_status NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(site_id, code)
);

CREATE TABLE post_requirement (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL REFERENCES security_post(id) ON DELETE CASCADE,
    requirement VARCHAR(180) NOT NULL
);

-- ============================================================
