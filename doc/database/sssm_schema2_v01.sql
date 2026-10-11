BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE SCHEMA IF NOT EXISTS security;
SET search_path TO security, public;

-- ============================================================
-- 1. TYPES ENUM
-- ============================================================

CREATE TYPE user_status AS ENUM ('ACTIVE', 'INACTIVE', 'LOCKED');
CREATE TYPE employee_status AS ENUM ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'LEFT');
CREATE TYPE site_status AS ENUM ('ACTIVE', 'INACTIVE');
CREATE TYPE post_status AS ENUM ('ACTIVE', 'INACTIVE');
CREATE TYPE shift_status AS ENUM ('PLANNED', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'ABSENT');
CREATE TYPE assignment_status AS ENUM ('PLANNED', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED');
CREATE TYPE patrol_status AS ENUM ('PLANNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'FAILED');
CREATE TYPE incident_status AS ENUM ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'CANCELLED');
CREATE TYPE incident_severity AS ENUM ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL');
CREATE TYPE contract_status AS ENUM ('DRAFT', 'PENDING', 'ACTIVE', 'SUSPENDED', 'EXPIRED', 'TERMINATED', 'CANCELLED');
CREATE TYPE invoice_status AS ENUM ('DRAFT', 'ISSUED', 'PARTIALLY_PAID', 'PAID', 'OVERDUE', 'CANCELLED');
CREATE TYPE payment_status AS ENUM ('PENDING', 'CONFIRMED', 'FAILED', 'CANCELLED', 'REFUNDED');
CREATE TYPE payment_method AS ENUM ('CASH', 'BANK_TRANSFER', 'MOBILE_MONEY', 'CHEQUE', 'CARD', 'OTHER');
CREATE TYPE item_status AS ENUM ('AVAILABLE', 'ASSIGNED', 'DAMAGED', 'LOST', 'REPAIR', 'RETIRED');
CREATE TYPE movement_type AS ENUM ('IN', 'OUT', 'TRANSFER', 'RETURN', 'ADJUSTMENT');
CREATE TYPE notification_channel AS ENUM ('EMAIL', 'SMS', 'PUSH', 'IN_APP');
CREATE TYPE notification_status AS ENUM ('PENDING', 'SENT', 'FAILED', 'READ');
CREATE TYPE document_type AS ENUM ('CONTRACT', 'INVOICE', 'RECEIPT', 'REPORT', 'CERTIFICATE', 'OTHER');

-- ============================================================
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
-- 3. AUTHENTIFICATION / RBAC
-- ============================================================

CREATE TABLE app_user (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID REFERENCES company(id) ON DELETE SET NULL,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(180) UNIQUE,
    password_hash TEXT NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(50),
    status user_status NOT NULL DEFAULT 'ACTIVE',
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE role (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID REFERENCES company(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    UNIQUE(company_id, name)
);

CREATE TABLE permission (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(150) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description TEXT
);

CREATE TABLE role_permission (
    role_id UUID NOT NULL REFERENCES role(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permission(id) ON DELETE CASCADE,
    PRIMARY KEY(role_id, permission_id)
);

CREATE TABLE user_role (
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES role(id) ON DELETE CASCADE,
    PRIMARY KEY(user_id, role_id)
);

-- ============================================================
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
-- 9. PATROUILLES / RONDES / CHECKPOINTS / GPS
-- ============================================================

CREATE TABLE patrol_route (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    site_id UUID NOT NULL REFERENCES site(id) ON DELETE CASCADE,
    name VARCHAR(180) NOT NULL,
    description TEXT,
    estimated_duration_minutes INTEGER,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE checkpoint (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patrol_route_id UUID NOT NULL REFERENCES patrol_route(id) ON DELETE CASCADE,
    name VARCHAR(180) NOT NULL,
    code VARCHAR(80),
    sequence_number INTEGER NOT NULL CHECK(sequence_number > 0),
    latitude NUMERIC(10,7),
    longitude NUMERIC(10,7),
    radius_meters INTEGER NOT NULL DEFAULT 50 CHECK(radius_meters > 0),
    qr_code VARCHAR(255),
    nfc_code VARCHAR(255),
    UNIQUE(patrol_route_id, sequence_number),
    UNIQUE(patrol_route_id, code)
);

CREATE TABLE patrol (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patrol_route_id UUID NOT NULL REFERENCES patrol_route(id) ON DELETE RESTRICT,
    employee_id UUID NOT NULL REFERENCES employee(id) ON DELETE RESTRICT,
    site_id UUID NOT NULL REFERENCES site(id) ON DELETE RESTRICT,
    scheduled_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    status patrol_status NOT NULL DEFAULT 'PLANNED',
    notes TEXT
);

CREATE TABLE patrol_checkpoint (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patrol_id UUID NOT NULL REFERENCES patrol(id) ON DELETE CASCADE,
    checkpoint_id UUID NOT NULL REFERENCES checkpoint(id) ON DELETE RESTRICT,
    scanned_at TIMESTAMPTZ,
    latitude NUMERIC(10,7),
    longitude NUMERIC(10,7),
    distance_meters NUMERIC(10,2),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    notes TEXT,
    UNIQUE(patrol_id, checkpoint_id)
);

CREATE TABLE gps_tracking (
    id BIGSERIAL PRIMARY KEY,
    employee_id UUID NOT NULL REFERENCES employee(id) ON DELETE CASCADE,
    patrol_id UUID REFERENCES patrol(id) ON DELETE CASCADE,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    latitude NUMERIC(10,7) NOT NULL,
    longitude NUMERIC(10,7) NOT NULL,
    accuracy_meters NUMERIC(10,2),
    speed_kmh NUMERIC(10,2),
    heading NUMERIC(6,2),
    battery_level INTEGER CHECK(battery_level IS NULL OR battery_level BETWEEN 0 AND 100)
);

-- ============================================================
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
    item_type_id UUID NOT NULL REFERENCES company(id) ON DELETE SET NULL,
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
-- 13. DOCUMENTS / COMMUNICATIONS
-- ============================================================

CREATE TABLE document (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    client_id UUID REFERENCES client(id) ON DELETE SET NULL,
    employee_id UUID REFERENCES employee(id) ON DELETE SET NULL,
    contract_id UUID REFERENCES contract(id) ON DELETE SET NULL,
    invoice_id UUID REFERENCES invoice(id) ON DELETE SET NULL,
    document_type document_type NOT NULL,
    title VARCHAR(255) NOT NULL,
    file_name VARCHAR(255),
    file_url TEXT NOT NULL,
    mime_type VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE notification (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES app_user(id) ON DELETE CASCADE,
    employee_id UUID REFERENCES employee(id) ON DELETE CASCADE,
    channel notification_channel NOT NULL,
    status notification_status NOT NULL DEFAULT 'PENDING',
    subject VARCHAR(255),
    message TEXT NOT NULL,
    scheduled_at TIMESTAMPTZ,
    sent_at TIMESTAMPTZ,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE email_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_email VARCHAR(180) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body TEXT,
    sent_at TIMESTAMPTZ,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- 14. AUDIT
-- ============================================================

CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID REFERENCES app_user(id) ON DELETE SET NULL,
    action VARCHAR(80) NOT NULL,
    entity_name VARCHAR(100) NOT NULL,
    entity_id UUID,
    old_data JSONB,
    new_data JSONB,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- 15. INDEX
-- ============================================================

-- Company
CREATE INDEX idx_company_created_at ON company(created_at DESC);

-- Branch
CREATE INDEX idx_branch_company ON branch(company_id);
CREATE INDEX idx_branch_agency ON branch(agence_id);
CREATE INDEX idx_branch_code ON branch(code);
CREATE INDEX idx_branch_status ON branch(status);

-- Agency
CREATE INDEX idx_agency_company ON agency(company_id);
CREATE INDEX idx_agency_code ON agency(code);
CREATE INDEX idx_agency_active ON agency(active);

-- App User
CREATE INDEX idx_user_company ON app_user(company_id);
CREATE INDEX idx_user_status ON app_user(status);
CREATE INDEX idx_user_email ON app_user(email);
CREATE INDEX idx_user_username ON app_user(username);
CREATE INDEX idx_user_last_login ON app_user(last_login_at DESC);

-- Role & Permissions
CREATE INDEX idx_role_company ON role(company_id);
CREATE INDEX idx_permission_code ON permission(code);
CREATE INDEX idx_role_permission_role ON role_permission(role_id);
CREATE INDEX idx_role_permission_permission ON role_permission(permission_id);
CREATE INDEX idx_user_role_user ON user_role(user_id);
CREATE INDEX idx_user_role_role ON user_role(role_id);

-- Employee
CREATE INDEX idx_employee_company ON employee(company_id);
CREATE INDEX idx_employee_agency ON employee(agency_id);
CREATE INDEX idx_employee_user ON employee(user_id);
CREATE INDEX idx_employee_status ON employee(status);
CREATE INDEX idx_employee_number ON employee(employee_number);
CREATE INDEX idx_employee_email ON employee(email);
CREATE INDEX idx_employee_national_id ON employee(national_id);
CREATE INDEX idx_employee_hire_date ON employee(hire_date);

-- Employee Document
CREATE INDEX idx_employee_doc_employee ON employee_document(employee_id);
CREATE INDEX idx_employee_doc_type ON employee_document(document_type);

-- Client
CREATE INDEX idx_client_company ON client(company_id);
CREATE INDEX idx_client_agency ON client(agency_id);
CREATE INDEX idx_client_code ON client(code);
CREATE INDEX idx_client_active ON client(active);
CREATE INDEX idx_client_email ON client(email);

-- Client Contact
CREATE INDEX idx_client_contact_client ON client_contact(client_id);
CREATE INDEX idx_client_contact_primary ON client_contact(is_primary);

-- Site
CREATE INDEX idx_site_company ON site(company_id);
CREATE INDEX idx_site_client ON site(client_id);
CREATE INDEX idx_site_branch ON site(branch_id);
CREATE INDEX idx_site_code ON site(code);
CREATE INDEX idx_site_city ON site(city);
CREATE INDEX idx_site_status ON site(status);
CREATE INDEX idx_site_location ON site(latitude, longitude);

-- Security Post
CREATE INDEX idx_post_site ON security_post(site_id);
CREATE INDEX idx_post_code ON security_post(code);
CREATE INDEX idx_post_status ON security_post(status);

-- Post Requirement
CREATE INDEX idx_post_requirement_post ON post_requirement(post_id);

-- Service
CREATE INDEX idx_service_company ON service(company_id);
CREATE INDEX idx_service_code ON service(code);
CREATE INDEX idx_service_active ON service(active);

-- Contract
CREATE INDEX idx_contract_company ON contract(company_id);
CREATE INDEX idx_contract_client ON contract(client_id);
CREATE INDEX idx_contract_number ON contract(contract_number);
CREATE INDEX idx_contract_status ON contract(status);
CREATE INDEX idx_contract_dates ON contract(start_date, end_date);

-- Contract Service
CREATE INDEX idx_contract_service_contract ON contract_service(contract_id);
CREATE INDEX idx_contract_service_service ON contract_service(service_id);
CREATE INDEX idx_contract_service_site ON contract_service(site_id);

-- Shift
CREATE INDEX idx_shift_company ON shift(company_id);

-- Schedule
CREATE INDEX idx_schedule_company ON schedule(company_id);
CREATE INDEX idx_schedule_site ON schedule(site_id);
CREATE INDEX idx_schedule_post ON schedule(post_id);
CREATE INDEX idx_schedule_shift ON schedule(shift_id);
CREATE INDEX idx_schedule_date ON schedule(schedule_date);
CREATE INDEX idx_schedule_site_date ON schedule(site_id, schedule_date);
CREATE INDEX idx_schedule_status ON schedule(status);

-- Assignment
CREATE INDEX idx_assignment_schedule ON assignment(schedule_id);
CREATE INDEX idx_assignment_employee ON assignment(employee_id);
CREATE INDEX idx_assignment_status ON assignment(status);

-- Attendance
CREATE INDEX idx_attendance_employee ON attendance(employee_id);
CREATE INDEX idx_attendance_site ON attendance(site_id);
CREATE INDEX idx_attendance_assignment ON attendance(assignment_id);
CREATE INDEX idx_attendance_employee_date ON attendance(employee_id, attendance_date);
CREATE INDEX idx_attendance_date ON attendance(attendance_date);
CREATE INDEX idx_attendance_status ON attendance(status);

-- Patrol Route
CREATE INDEX idx_patrol_route_site ON patrol_route(site_id);
CREATE INDEX idx_patrol_route_active ON patrol_route(active);

-- Checkpoint
CREATE INDEX idx_checkpoint_route ON checkpoint(patrol_route_id);
CREATE INDEX idx_checkpoint_code ON checkpoint(code);
CREATE INDEX idx_checkpoint_qr ON checkpoint(qr_code);
CREATE INDEX idx_checkpoint_nfc ON checkpoint(nfc_code);

-- Patrol
CREATE INDEX idx_patrol_route ON patrol(patrol_route_id);
CREATE INDEX idx_patrol_employee ON patrol(employee_id);
CREATE INDEX idx_patrol_site ON patrol(site_id);
CREATE INDEX idx_patrol_status ON patrol(status);
CREATE INDEX idx_patrol_scheduled_at ON patrol(scheduled_at);
CREATE INDEX idx_patrol_started_at ON patrol(started_at DESC);

-- Patrol Checkpoint
CREATE INDEX idx_patrol_checkpoint_patrol ON patrol_checkpoint(patrol_id);
CREATE INDEX idx_patrol_checkpoint_checkpoint ON patrol_checkpoint(checkpoint_id);
CREATE INDEX idx_patrol_checkpoint_scanned_at ON patrol_checkpoint(scanned_at);

-- GPS Tracking
CREATE INDEX idx_gps_employee_time ON gps_tracking(employee_id, recorded_at DESC);
CREATE INDEX idx_gps_patrol_time ON gps_tracking(patrol_id, recorded_at DESC);
CREATE INDEX idx_gps_recorded_at ON gps_tracking(recorded_at DESC);

-- Incident Type
CREATE INDEX idx_incident_type_company ON incident_type(company_id);

-- Incident
CREATE INDEX idx_incident_company ON incident(company_id);
CREATE INDEX idx_incident_site ON incident(site_id);
CREATE INDEX idx_incident_post ON incident(post_id);
CREATE INDEX idx_incident_type ON incident(incident_type_id);
CREATE INDEX idx_incident_reported_by ON incident(reported_by_employee_id);
CREATE INDEX idx_incident_assigned_to ON incident(assigned_to_employee_id);
CREATE INDEX idx_incident_status ON incident(status);
CREATE INDEX idx_incident_severity ON incident(severity);
CREATE INDEX idx_incident_reported_at ON incident(reported_at DESC);
CREATE INDEX idx_incident_occurred_at ON incident(occurred_at DESC);

-- Incident Attachment
CREATE INDEX idx_incident_attachment_incident ON incident_attachment(incident_id);

-- Incident Action
CREATE INDEX idx_incident_action_incident ON incident_action(incident_id);
CREATE INDEX idx_incident_action_employee ON incident_action(employee_id);

-- Item Category
CREATE INDEX idx_item_category_company ON item_category(company_id);

-- Item Type
CREATE INDEX idx_item_type_company ON item_type(company_id);
CREATE INDEX idx_item_type_category ON item_type(category_id);
CREATE INDEX idx_item_type_code ON item_type(code);
CREATE INDEX idx_item_type_status ON item_type(status);

-- Item
CREATE INDEX idx_item_company ON item(company_id);
CREATE INDEX idx_item_category ON item(category_id);
CREATE INDEX idx_item_type ON item(item_type_id);
CREATE INDEX idx_item_qr_code ON item(qr_code);
CREATE INDEX idx_item_status ON item(status);

-- Warehouse
CREATE INDEX idx_warehouse_company ON warehouse(company_id);
CREATE INDEX idx_warehouse_branch ON warehouse(branch_id);
CREATE INDEX idx_warehouse_active ON warehouse(active);

-- Stock Movement
CREATE INDEX idx_stock_warehouse ON stock_movement(warehouse_id);
CREATE INDEX idx_stock_item ON stock_movement(item_id);
CREATE INDEX idx_stock_employee ON stock_movement(employee_id);
CREATE INDEX idx_stock_movement_type ON stock_movement(movement_type);
CREATE INDEX idx_stock_movement_date ON stock_movement(movement_date DESC);
CREATE INDEX idx_stock_reference ON stock_movement(reference);

-- Item Assignment
CREATE INDEX idx_item_assignment_item ON item_assignment(item_id);
CREATE INDEX idx_item_assignment_employee ON item_assignment(employee_id);
CREATE INDEX idx_item_assignment_post ON item_assignment(post_id);
CREATE INDEX idx_item_assignment_assigned_at ON item_assignment(assigned_at DESC);

-- Tax Rate
CREATE INDEX idx_tax_rate_company ON tax_rate(company_id);
CREATE INDEX idx_tax_rate_active ON tax_rate(active);

-- Invoice
CREATE INDEX idx_invoice_company ON invoice(company_id);
CREATE INDEX idx_invoice_client ON invoice(client_id);
CREATE INDEX idx_invoice_contract ON invoice(contract_id);
CREATE INDEX idx_invoice_number ON invoice(invoice_number);
CREATE INDEX idx_invoice_status ON invoice(status);
CREATE INDEX idx_invoice_issue_date ON invoice(issue_date DESC);
CREATE INDEX idx_invoice_due_date ON invoice(due_date);

-- Invoice Line
CREATE INDEX idx_invoice_line_invoice ON invoice_line(invoice_id);
CREATE INDEX idx_invoice_line_service ON invoice_line(service_id);

-- Payment
CREATE INDEX idx_payment_invoice ON payment(invoice_id);
CREATE INDEX idx_payment_date ON payment(payment_date DESC);
CREATE INDEX idx_payment_status ON payment(status);
CREATE INDEX idx_payment_method ON payment(method);
CREATE INDEX idx_payment_reference ON payment(payment_reference);

-- Payment Allocation
CREATE INDEX idx_payment_alloc_payment ON payment_allocation(payment_id);
CREATE INDEX idx_payment_alloc_invoice ON payment_allocation(invoice_id);

-- Document
CREATE INDEX idx_document_company ON document(company_id);
CREATE INDEX idx_document_client ON document(client_id);
CREATE INDEX idx_document_employee ON document(employee_id);
CREATE INDEX idx_document_contract ON document(contract_id);
CREATE INDEX idx_document_invoice ON document(invoice_id);
CREATE INDEX idx_document_type ON document(document_type);

-- Notification
CREATE INDEX idx_notification_user ON notification(user_id);
CREATE INDEX idx_notification_employee ON notification(employee_id);
CREATE INDEX idx_notification_status ON notification(status);
CREATE INDEX idx_notification_channel ON notification(channel);
CREATE INDEX idx_notification_created_at ON notification(created_at DESC);

-- Email Log
CREATE INDEX idx_email_log_recipient ON email_log(recipient_email);
CREATE INDEX idx_email_log_status ON email_log(status);
CREATE INDEX idx_email_log_sent_at ON email_log(sent_at DESC);

-- Audit Log
CREATE INDEX idx_audit_user ON audit_log(user_id);
CREATE INDEX idx_audit_entity ON audit_log(entity_name, entity_id);
CREATE INDEX idx_audit_created_at ON audit_log(created_at DESC);

-- ============================================================
-- 16. TRIGGER updated_at
-- ============================================================

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_company_updated_at
BEFORE UPDATE ON company
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_branch_updated_at
BEFORE UPDATE ON branch
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_agency_updated_at
BEFORE UPDATE ON agency
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_user_updated_at
BEFORE UPDATE ON app_user
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_employee_updated_at
BEFORE UPDATE ON employee
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_client_updated_at
BEFORE UPDATE ON client
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_site_updated_at
BEFORE UPDATE ON site
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_contract_updated_at
BEFORE UPDATE ON contract
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_incident_updated_at
BEFORE UPDATE ON incident
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_item_type_updated_at
BEFORE UPDATE ON item_type
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_item_updated_at
BEFORE UPDATE ON item
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_invoice_updated_at
BEFORE UPDATE ON invoice
FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ============================================================
-- 17. DONNÉES INITIALES : permissions de base
-- ============================================================

INSERT INTO permission(code, name, description) VALUES
('dashboard.view', 'Voir le tableau de bord', 'Accès au dashboard'),
('users.manage', 'Gérer les utilisateurs', 'Création/modification des comptes'),
('roles.manage', 'Gérer les rôles', 'Gestion RBAC'),
('employees.view', 'Voir les agents', 'Consultation du personnel'),
('employees.manage', 'Gérer les agents', 'Gestion du personnel'),
('sites.manage', 'Gérer les sites', 'Gestion des sites clients'),
('posts.manage', 'Gérer les postes', 'Gestion des postes de sécurité'),
('planning.manage', 'Gérer le planning', 'Création et modification du planning'),
('patrols.manage', 'Gérer les patrouilles', 'Rondes et checkpoints'),
('tracking.view', 'Voir le suivi GPS', 'Géolocalisation'),
('incidents.manage', 'Gérer les incidents', 'Déclaration et traitement'),
('inventory.manage', 'Gérer le stock', 'Matériel et mouvements'),
('clients.manage', 'Gérer les clients', 'CRM clients'),
('contracts.manage', 'Gérer les contrats', 'Contrats et prestations'),
('invoices.manage', 'Gérer les factures', 'Facturation'),
('payments.manage', 'Gérer les paiements', 'Encaissements'),
('reports.view', 'Voir les rapports', 'Rapports et statistiques')
ON CONFLICT (code) DO NOTHING;

COMMIT;


-- INSERT INTO security.company(
-- 	name, legal_name, registration_number, tax_number, email, phone, address, city, country, logo_url, currency_code, created_at, updated_at)
-- 	VALUES ('SSSM', 'SSSM Security', 'sssm registration_number', 'tax_number', 'sssm@email.com', '0320032523', 'sssm adress', 'Antananarivo', 'MADAGASCAR', 'logo.png', 'MGA', '2000-01-15', '2000-01-15');