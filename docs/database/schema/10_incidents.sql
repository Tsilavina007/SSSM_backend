-- 10. INCIDENTS / RAPPORTS
-- ============================================================

CREATE TABLE incident_type (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    UNIQUE(company_id, name)
);

CREATE TABLE incident (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    site_id UUID REFERENCES site(id) ON DELETE SET NULL,
    post_id UUID REFERENCES security_post(id) ON DELETE SET NULL,
    incident_type_id UUID REFERENCES incident_type(id) ON DELETE SET NULL,
    reported_by_employee_id UUID REFERENCES employee(id) ON DELETE SET NULL,
    assigned_to_employee_id UUID REFERENCES employee(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    severity incident_severity NOT NULL DEFAULT 'MEDIUM',
    status incident_status NOT NULL DEFAULT 'OPEN',
    occurred_at TIMESTAMPTZ,
    reported_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at TIMESTAMPTZ,
    latitude NUMERIC(10,7),
    longitude NUMERIC(10,7),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE incident_attachment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id UUID NOT NULL REFERENCES incident(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_url TEXT NOT NULL,
    mime_type VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE incident_action (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id UUID NOT NULL REFERENCES incident(id) ON DELETE CASCADE,
    employee_id UUID REFERENCES employee(id) ON DELETE SET NULL,
    action TEXT NOT NULL,
    action_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
