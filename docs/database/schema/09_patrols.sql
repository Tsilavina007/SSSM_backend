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
