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
