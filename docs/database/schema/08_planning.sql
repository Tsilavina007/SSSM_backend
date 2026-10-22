-- 8. PLANNING / HORAIRES / AFFECTATIONS
-- ============================================================

CREATE TABLE shift (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    name VARCHAR(120),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    duration_hours NUMERIC(6,2),
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE schedule (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    site_id UUID NOT NULL REFERENCES site(id) ON DELETE CASCADE,
    post_id UUID REFERENCES security_post(id) ON DELETE SET NULL,
    shift_id UUID NOT NULL REFERENCES shift(id) ON DELETE RESTRICT,
    schedule_date DATE NOT NULL,
    status shift_status NOT NULL DEFAULT 'PLANNED',
    notes TEXT,
    UNIQUE(site_id, post_id, shift_id, schedule_date)
);

CREATE TABLE assignment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    schedule_id UUID NOT NULL REFERENCES schedule(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES employee(id) ON DELETE RESTRICT,
    status assignment_status NOT NULL DEFAULT 'PLANNED',
    check_in_at TIMESTAMPTZ,
    check_out_at TIMESTAMPTZ,
    notes TEXT,
    UNIQUE(schedule_id, employee_id)
);

CREATE TABLE attendance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES employee(id) ON DELETE CASCADE,
    site_id UUID REFERENCES site(id) ON DELETE SET NULL,
    assignment_id UUID REFERENCES assignment(id) ON DELETE SET NULL,
    attendance_date DATE NOT NULL,
    check_in_at TIMESTAMPTZ,
    check_out_at TIMESTAMPTZ,
    status VARCHAR(30) NOT NULL DEFAULT 'PRESENT',
    notes TEXT,
    UNIQUE(employee_id, attendance_date, assignment_id)
);

-- ============================================================
