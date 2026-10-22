-- ============================================================
-- 11.1. CATÉGORIES DE MATÉRIEL
-- ============================================================

CREATE TABLE item_category (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    archived_at TIMESTAMPTZ,
    UNIQUE(company_id, name)
);

-- ============================================================
-- 11.2. TYPES DE MATÉRIEL
-- ============================================================

CREATE TABLE item_type (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    category_id UUID REFERENCES item_category(id) ON DELETE SET NULL,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(180) NOT NULL,
    description TEXT,
    unit VARCHAR(50) NOT NULL DEFAULT 'piece',
    quantity_total NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (quantity_total >= 0),
    minimum_stock NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (minimum_stock >= 0),
    status item_status NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    archived_at TIMESTAMPTZ,
    UNIQUE(company_id, code)
);

-- ============================================================
-- 11.3. ENTREPÔTS
-- ============================================================

CREATE TABLE warehouse (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    branch_id UUID REFERENCES branch(id) ON DELETE SET NULL,
    name VARCHAR(150) NOT NULL,
    address TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    archived_at TIMESTAMPTZ
);

-- ============================================================
-- 11.4. ÉQUIPEMENTS INDIVIDUELS
-- Chaque ligne représente un équipement physique avec un QR Code.
-- ============================================================

CREATE TABLE item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    category_id UUID REFERENCES item_category(id) ON DELETE SET NULL,
    item_type_id UUID NOT NULL REFERENCES item_type(id) ON DELETE RESTRICT,
    qr_code VARCHAR(100) NOT NULL,
    description TEXT,
    status item_status NOT NULL DEFAULT 'AVAILABLE',
    current_warehouse_id UUID REFERENCES warehouse(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    archived_at TIMESTAMPTZ,
    archived_by UUID,
    last_inventory_at TIMESTAMPTZ,
    UNIQUE(company_id, qr_code)
);

-- ============================================================
-- 11.5. OPÉRATIONS MÉTIER
-- Une opération regroupe un ou plusieurs équipements scannés.
-- ============================================================

CREATE TABLE inventory_operation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES company(id) ON DELETE RESTRICT,
    operation_type VARCHAR(30) NOT NULL
        CHECK (operation_type IN (
            'RECEIPT',
            'ISSUE',
            'TRANSFER',
            'ASSIGNMENT',
            'RETURN',
            'ADJUSTMENT'
        )),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN (
            'PENDING',
            'COMPLETED',
            'CANCELLED',
            'FAILED'
        )),
    reference VARCHAR(150),
    idempotency_key VARCHAR(150),
    source_warehouse_id UUID REFERENCES warehouse(id) ON DELETE RESTRICT,
    destination_warehouse_id UUID REFERENCES warehouse(id) ON DELETE RESTRICT,
    employee_id UUID REFERENCES employee(id) ON DELETE RESTRICT,
    post_id UUID REFERENCES security_post(id) ON DELETE RESTRICT,
    performed_by UUID,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    CHECK (
        source_warehouse_id IS NULL
        OR destination_warehouse_id IS NULL
        OR source_warehouse_id <> destination_warehouse_id
    )
);

-- ============================================================
-- 11.6. MOUVEMENTS DE STOCK
-- Le journal des mouvements est conservé pour l'audit.
-- ============================================================

CREATE TABLE stock_movement (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    warehouse_id UUID NOT NULL REFERENCES warehouse(id) ON DELETE RESTRICT,
    item_id UUID NOT NULL REFERENCES item(id) ON DELETE RESTRICT,
    employee_id UUID REFERENCES employee(id) ON DELETE SET NULL,
    movement_type movement_type NOT NULL,
    reference VARCHAR(150),
    movement_date TIMESTAMPTZ NOT NULL DEFAULT now(),
    notes TEXT,
    source_warehouse_id UUID REFERENCES warehouse(id) ON DELETE RESTRICT,
    destination_warehouse_id UUID REFERENCES warehouse(id) ON DELETE RESTRICT,
    operation_id UUID REFERENCES inventory_operation(id) ON DELETE RESTRICT,
    quantity NUMERIC(12,2) NOT NULL DEFAULT 1 CHECK (quantity > 0),
    performed_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (
        source_warehouse_id IS NULL
        OR destination_warehouse_id IS NULL
        OR source_warehouse_id <> destination_warehouse_id
    )
);

-- ============================================================
-- 11.7. DÉTAILS DES OPÉRATIONS
-- Un équipement ne peut apparaître qu'une fois par opération.
-- ============================================================

CREATE TABLE inventory_operation_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    operation_id UUID NOT NULL
        REFERENCES inventory_operation(id) ON DELETE RESTRICT,
    item_id UUID NOT NULL REFERENCES item(id) ON DELETE RESTRICT,
    movement_id UUID REFERENCES stock_movement(id) ON DELETE RESTRICT,
    quantity NUMERIC(12,2) NOT NULL DEFAULT 1 CHECK (quantity > 0),
    condition_before TEXT,
    condition_after TEXT,
    scanned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    notes TEXT,
    UNIQUE(operation_id, item_id)
);

-- ============================================================
-- 11.8. AFFECTATIONS AUX EMPLOYÉS ET POSTES
-- ============================================================

CREATE TABLE item_assignment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_id UUID NOT NULL REFERENCES item(id) ON DELETE RESTRICT,
    employee_id UUID NOT NULL REFERENCES employee(id) ON DELETE RESTRICT,
    post_id UUID NOT NULL REFERENCES security_post(id) ON DELETE RESTRICT,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    returned_at TIMESTAMPTZ,
    condition_on_assignment TEXT,
    condition_on_return TEXT,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    assigned_by UUID,
    returned_by UUID,
    expected_return_at TIMESTAMPTZ,
    CHECK (returned_at IS NULL OR returned_at >= assigned_at)
);

-- ============================================================
-- 11.9. CONFIGURATION DES SEUILS PAR ENTREPÔT ET TYPE
-- Permet un minimum différent selon l'agence / l'entrepôt.
-- ============================================================

CREATE TABLE warehouse_stock_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    warehouse_id UUID NOT NULL REFERENCES warehouse(id) ON DELETE RESTRICT,
    item_type_id UUID NOT NULL REFERENCES item_type(id) ON DELETE RESTRICT,
    minimum_stock NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (minimum_stock >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(warehouse_id, item_type_id)
);

-- ============================================================
-- 11.10. SESSIONS D'INVENTAIRE PHYSIQUE
-- ============================================================

CREATE TABLE inventory_count (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES company(id) ON DELETE RESTRICT,
    warehouse_id UUID NOT NULL REFERENCES warehouse(id) ON DELETE RESTRICT,
    reference VARCHAR(150) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS'
        CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    started_by UUID,
    completed_by UUID,
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    notes TEXT,
    UNIQUE(company_id, reference),
    CHECK (completed_at IS NULL OR completed_at >= started_at)
);

-- ============================================================
-- 11.11. SCANS ET ÉCARTS D'INVENTAIRE
-- Une ligne de résultat par équipement et par session.
-- ============================================================

CREATE TABLE inventory_count_scan (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inventory_count_id UUID NOT NULL
        REFERENCES inventory_count(id) ON DELETE RESTRICT,
    item_id UUID NOT NULL REFERENCES item(id) ON DELETE RESTRICT,
    scanned_by UUID,
    scanned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expected_warehouse_id UUID REFERENCES warehouse(id) ON DELETE RESTRICT,
    actual_warehouse_id UUID REFERENCES warehouse(id) ON DELETE RESTRICT,
    discrepancy_type VARCHAR(30)
        CHECK (discrepancy_type IN (
            'NONE',
            'MISSING',
            'UNEXPECTED',
            'WRONG_LOCATION',
            'DAMAGED'
        )),
    notes TEXT,
    UNIQUE(inventory_count_id, item_id)
);

-- ============================================================