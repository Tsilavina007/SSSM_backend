-- 11. MATÉRIEL / INVENTAIRE / STOCK
-- ============================================================

CREATE TABLE item_category (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    UNIQUE(company_id, name)
);

CREATE TABLE item_type (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    category_id UUID REFERENCES item_category(id) ON DELETE SET NULL,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(180) NOT NULL,
    description TEXT,
    unit VARCHAR(50) NOT NULL DEFAULT 'piece',
    quantity_total NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK(quantity_total >= 0),
    minimum_stock NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK(minimum_stock >= 0),
    status item_status NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(company_id, code)
);

CREATE TABLE item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    category_id UUID REFERENCES item_category(id) ON DELETE SET NULL,
    item_type_id UUID NOT NULL REFERENCES item_type(id) ON DELETE SET NULL,
    qr_code VARCHAR(100) NOT NULL,
    description TEXT,
    status item_status NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(company_id, qr_code)
);

CREATE TABLE warehouse (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    branch_id UUID REFERENCES branch(id) ON DELETE SET NULL,
    name VARCHAR(150) NOT NULL,
    address TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE stock_movement (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    warehouse_id UUID NOT NULL REFERENCES warehouse(id) ON DELETE RESTRICT,
    item_id UUID NOT NULL REFERENCES item(id) ON DELETE RESTRICT,
    employee_id UUID REFERENCES employee(id) ON DELETE SET NULL,
    movement_type movement_type NOT NULL,
    reference VARCHAR(150),
    movement_date TIMESTAMPTZ NOT NULL DEFAULT now(),
    notes TEXT
);

CREATE TABLE item_assignment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_id UUID NOT NULL REFERENCES item(id) ON DELETE RESTRICT,
    employee_id UUID NOT NULL REFERENCES employee(id) ON DELETE RESTRICT,
    post_id UUID NOT NULL REFERENCES security_post(id) ON DELETE RESTRICT,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    returned_at TIMESTAMPTZ,
    condition_on_assignment TEXT,
    condition_on_return TEXT,
    notes TEXT
);

-- ============================================================
