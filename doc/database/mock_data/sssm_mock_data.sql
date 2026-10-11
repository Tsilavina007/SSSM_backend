\encoding UTF8
\set ON_ERROR_STOP on

BEGIN;

SET search_path TO security, public;

-- =====================================================================
-- PARTIE 1 : DONNÉES DE BASE (script d'origine)
-- =====================================================================

INSERT INTO company (
    name,
    legal_name,
    registration_number,
    tax_number,
    email,
    phone,
    address,
    city,
    country,
    currency_code
) VALUES (
    'SSSM Madagascar',
    'Société de Sécurité et Services Madagascar',
    'RC-SSSM-001',
    'NIF-SSSM-001',
    'contact@sssm.test',
    '+261 34 00 000 01',
    'Lot II M 45',
    'Antananarivo',
    'Madagascar',
    'MGA'
) RETURNING id \gset company_

INSERT INTO agency (
    company_id,
    name,
    code,
    address,
    city,
    phone,
    email,
    active
) VALUES (
    :'company_id',
    'Agence Centrale',
    'AC',
    'Analakely',
    'Antananarivo',
    '+261 34 00 000 11',
    'agence.centrale@sssm.test',
    TRUE
) RETURNING id \gset agency_

INSERT INTO branch (
    company_id,
    agence_id,
    name,
    code,
    address,
    city,
    status
) VALUES (
    :'company_id',
    :'agency_id',
    'Branche Antananarivo',
    'TANA',
    'Ivandry',
    'Antananarivo',
    'ACTIVE'
) RETURNING id \gset branch_

INSERT INTO permission (
    code,
    name,
    description
) VALUES (
    'demo.read',
    'Lecture démo',
    'Permission de lecture pour les données de démonstration'
) RETURNING id \gset permission_read_

INSERT INTO permission (
    code,
    name,
    description
) VALUES (
    'demo.manage',
    'Gestion démo',
    'Permission de gestion pour les données de démonstration'
) RETURNING id \gset permission_manage_

INSERT INTO role (
    company_id,
    name,
    description
) VALUES (
    :'company_id',
    'Administrateur démo',
    'Administrateur de démonstration'
) RETURNING id \gset role_

INSERT INTO app_user (
    company_id,
    username,
    email,
    password_hash,
    first_name,
    last_name,
    phone,
    status
) VALUES (
    :'company_id',
    'demo.admin',
    'demo.admin@sssm.test',
    '$2a$10$demo-password-hash',
    'Demo',
    'Admin',
    '+261 34 00 000 51',
    'ACTIVE'
) RETURNING id \gset user_

INSERT INTO role_permission (
    role_id,
    permission_id
) VALUES (
    :'role_id',
    :'permission_read_id'
);

INSERT INTO role_permission (
    role_id,
    permission_id
) VALUES (
    :'role_id',
    :'permission_manage_id'
);

INSERT INTO user_role (
    user_id,
    role_id
) VALUES (
    :'user_id',
    :'role_id'
);

INSERT INTO employee (
    company_id,
    agency_id,
    user_id,
    employee_number,
    first_name,
    last_name,
    gender,
    date_of_birth,
    phone,
    email,
    address,
    national_id,
    hire_date,
    position,
    status,
    emergency_contact_name,
    emergency_contact_phone,
    notes
) VALUES (
    :'company_id',
    :'agency_id',
    :'user_id',
    'EMP-0001',
    'Jean',
    'Rakoto',
    'M',
    '1990-05-15',
    '+261 34 00 000 61',
    'jean.rakoto@sssm.test',
    'Ambohimanarina',
    'NID-0001',
    '2024-01-15',
    'Chef de site',
    'ACTIVE',
    'Marie Rakoto',
    '+261 34 00 000 62',
    'Agent principal de démonstration'
) RETURNING id \gset employee_primary_

INSERT INTO employee (
    company_id,
    agency_id,
    employee_number,
    first_name,
    last_name,
    gender,
    date_of_birth,
    phone,
    email,
    address,
    national_id,
    hire_date,
    position,
    status,
    emergency_contact_name,
    emergency_contact_phone
) VALUES (
    :'company_id',
    :'agency_id',
    'EMP-0002',
    'Hery',
    'Rasoanaivo',
    'M',
    '1995-08-20',
    '+261 34 00 000 63',
    'hery.rasoanaivo@sssm.test',
    'Behoririka',
    'NID-0002',
    '2024-02-01',
    'Agent de sécurité',
    'ACTIVE',
    'Soa Rasoanaivo',
    '+261 34 00 000 64'
) RETURNING id \gset employee_secondary_

INSERT INTO employee_document (
    employee_id,
    document_type,
    document_number,
    issue_date,
    expiry_date,
    file_url,
    notes
) VALUES (
    :'employee_primary_id',
    'Carte professionnelle',
    'DOC-EMP-0001',
    '2024-01-10',
    '2027-01-10',
    'https://example.test/documents/emp-0001.pdf',
    'Document de démonstration'
);

INSERT INTO client (
    company_id,
    agency_id,
    code,
    name,
    legal_name,
    client_type,
    registration_number,
    tax_number,
    email,
    phone,
    address,
    city,
    country,
    payment_terms_days,
    active
) VALUES (
    :'company_id',
    :'agency_id',
    'CLI-001',
    'Client Démo',
    'Client Démo SARL',
    'Entreprise',
    'RC-CLI-001',
    'NIF-CLI-001',
    'contact@client-demo.test',
    '+261 34 00 000 81',
    'Zone industrielle',
    'Antananarivo',
    'Madagascar',
    30,
    TRUE
) RETURNING id \gset client_

INSERT INTO client_contact (
    client_id,
    first_name,
    last_name,
    function,
    email,
    phone,
    is_primary
) VALUES (
    :'client_id',
    'Fara',
    'Andriamihaja',
    'Responsable administratif',
    'fara@client-demo.test',
    '+261 34 00 000 91',
    TRUE
);

INSERT INTO site (
    company_id,
    client_id,
    branch_id,
    name,
    code,
    address,
    city,
    latitude,
    longitude,
    status,
    access_instructions,
    risk_level
) VALUES (
    :'company_id',
    :'client_id',
    :'branch_id',
    'Site Client Démo',
    'SITE-001',
    'Zone industrielle',
    'Antananarivo',
    -18.8792000,
    47.5079000,
    'ACTIVE',
    'Accès principal par le portail nord',
    'MEDIUM'
) RETURNING id \gset site_

INSERT INTO security_post (
    site_id,
    name,
    code,
    description,
    required_agents,
    start_time,
    end_time,
    status
) VALUES (
    :'site_id',
    'Poste principal',
    'POST-001',
    'Contrôle du portail principal',
    2,
    '06:00',
    '18:00',
    'ACTIVE'
) RETURNING id \gset post_

INSERT INTO post_requirement (
    post_id,
    requirement
) VALUES (
    :'post_id',
    'Téléphone professionnel'
);

INSERT INTO post_requirement (
    post_id,
    requirement
) VALUES (
    :'post_id',
    'Lampe torche'
);

INSERT INTO service (
    company_id,
    code,
    name,
    description,
    unit,
    unit_price,
    vat_rate,
    active
) VALUES (
    :'company_id',
    'SEC-24H',
    'Sécurité 24 heures',
    'Surveillance permanente du site',
    'month',
    2500000,
    20,
    TRUE
) RETURNING id \gset service_

INSERT INTO contract (
    company_id,
    client_id,
    contract_number,
    title,
    start_date,
    end_date,
    status,
    billing_frequency,
    currency_code,
    payment_terms_days,
    total_amount,
    notes
) VALUES (
    :'company_id',
    :'client_id',
    'CTR-2024-001',
    'Contrat de sécurité du site démo',
    '2024-01-01',
    '2024-12-31',
    'ACTIVE',
    'MONTHLY',
    'MGA',
    30,
    30000000,
    'Contrat de démonstration'
) RETURNING id \gset contract_

INSERT INTO contract_service (
    contract_id,
    service_id,
    site_id,
    quantity,
    unit_price,
    vat_rate,
    start_date,
    end_date,
    notes
) VALUES (
    :'contract_id',
    :'service_id',
    :'site_id',
    12,
    2500000,
    20,
    '2024-01-01',
    '2024-12-31',
    'Prestation principale'
);

INSERT INTO shift (
    company_id,
    name,
    start_time,
    end_time,
    duration_hours,
    description
) VALUES (
    :'company_id',
    'Équipe jour',
    '06:00',
    '18:00',
    12,
    'Équipe de jour'
) RETURNING id \gset shift_

INSERT INTO schedule (
    company_id,
    site_id,
    post_id,
    shift_id,
    schedule_date,
    status,
    notes
) VALUES (
    :'company_id',
    :'site_id',
    :'post_id',
    :'shift_id',
    '2024-06-15',
    'CONFIRMED',
    'Planning de démonstration'
) RETURNING id \gset schedule_

INSERT INTO assignment (
    schedule_id,
    employee_id,
    status,
    check_in_at,
    notes
) VALUES (
    :'schedule_id',
    :'employee_primary_id',
    'CONFIRMED',
    '2024-06-15 06:00:00+03',
    'Affectation principale'
) RETURNING id \gset assignment_

INSERT INTO attendance (
    employee_id,
    site_id,
    assignment_id,
    attendance_date,
    check_in_at,
    check_out_at,
    status
) VALUES (
    :'employee_primary_id',
    :'site_id',
    :'assignment_id',
    '2024-06-15',
    '2024-06-15 05:55:00+03',
    '2024-06-15 18:05:00+03',
    'PRESENT'
);

INSERT INTO patrol_route (
    site_id,
    name,
    description,
    estimated_duration_minutes,
    active
) VALUES (
    :'site_id',
    'Ronde périmétrique',
    'Ronde autour du site client',
    45,
    TRUE
) RETURNING id \gset patrol_route_

INSERT INTO checkpoint (
    patrol_route_id,
    name,
    code,
    sequence_number,
    latitude,
    longitude,
    radius_meters,
    qr_code
) VALUES (
    :'patrol_route_id',
    'Portail nord',
    'CP-001',
    1,
    -18.8789000,
    47.5076000,
    50,
    'QR-CP-001'
) RETURNING id \gset checkpoint_

INSERT INTO patrol (
    patrol_route_id,
    employee_id,
    site_id,
    scheduled_at,
    started_at,
    ended_at,
    status,
    notes
) VALUES (
    :'patrol_route_id',
    :'employee_primary_id',
    :'site_id',
    '2024-06-15 10:00:00+03',
    '2024-06-15 10:02:00+03',
    '2024-06-15 10:43:00+03',
    'COMPLETED',
    'Ronde réussie'
) RETURNING id \gset patrol_

INSERT INTO patrol_checkpoint (
    patrol_id,
    checkpoint_id,
    scanned_at,
    latitude,
    longitude,
    distance_meters,
    status
) VALUES (
    :'patrol_id',
    :'checkpoint_id',
    '2024-06-15 10:15:00+03',
    -18.8789000,
    47.5076000,
    3.2,
    'SCANNED'
);

INSERT INTO gps_tracking (
    employee_id,
    patrol_id,
    recorded_at,
    latitude,
    longitude,
    accuracy_meters,
    speed_kmh,
    heading,
    battery_level
) VALUES (
    :'employee_primary_id',
    :'patrol_id',
    '2024-06-15 10:20:00+03',
    -18.8791000,
    47.5078000,
    5.2,
    3.5,
    90,
    85
);

INSERT INTO incident_type (
    company_id,
    name,
    description
) VALUES (
    :'company_id',
    'Accès non autorisé',
    'Tentative d''accès non autorisé'
) RETURNING id \gset incident_type_

INSERT INTO incident (
    company_id,
    site_id,
    post_id,
    incident_type_id,
    reported_by_employee_id,
    title,
    description,
    severity,
    status,
    occurred_at,
    reported_at,
    latitude,
    longitude
) VALUES (
    :'company_id',
    :'site_id',
    :'post_id',
    :'incident_type_id',
    :'employee_primary_id',
    'Tentative d''accès détectée',
    'Un accès inhabituel a été détecté au portail nord.',
    'MEDIUM',
    'RESOLVED',
    '2024-06-15 14:00:00+03',
    '2024-06-15 14:05:00+03',
    -18.8789000,
    47.5076000
) RETURNING id \gset incident_

INSERT INTO incident_attachment (
    incident_id,
    file_name,
    file_url,
    mime_type
) VALUES (
    :'incident_id',
    'rapport-acces.jpg',
    'https://example.test/incidents/rapport-acces.jpg',
    'image/jpeg'
);

INSERT INTO incident_action (
    incident_id,
    employee_id,
    action,
    action_at
) VALUES (
    :'incident_id',
    :'employee_primary_id',
    'Vérification du registre visiteurs et sécurisation du portail',
    '2024-06-15 14:10:00+03'
);

INSERT INTO item_category (
    company_id,
    name,
    description
) VALUES (
    :'company_id',
    'Équipements de sécurité',
    'Équipements utilisés par les agents'
) RETURNING id \gset item_category_

INSERT INTO item_type (
    company_id,
    category_id,
    code,
    name,
    description,
    unit,
    quantity_total,
    minimum_stock,
    status
) VALUES (
    :'company_id',
    :'item_category_id',
    'RADIO',
    'Radio portable',
    'Radio de communication',
    'piece',
    10,
    2,
    'AVAILABLE'
) RETURNING id \gset item_type_

INSERT INTO item (
    company_id,
    category_id,
    item_type_id,
    qr_code,
    description,
    status
) VALUES (
    :'company_id',
    :'item_category_id',
    :'item_type_id',
    'QR-ITEM-001',
    'Radio affectée au chef de site',
    'ASSIGNED'
) RETURNING id \gset item_

INSERT INTO warehouse (
    company_id,
    branch_id,
    name,
    address,
    active
) VALUES (
    :'company_id',
    :'branch_id',
    'Entrepôt principal',
    'Ivandry',
    TRUE
) RETURNING id \gset warehouse_

INSERT INTO stock_movement (
    warehouse_id,
    item_id,
    employee_id,
    movement_type,
    reference,
    movement_date,
    notes
) VALUES (
    :'warehouse_id',
    :'item_id',
    :'employee_primary_id',
    'OUT',
    'MVT-001',
    '2024-06-15 05:30:00+03',
    'Sortie pour affectation'
);

INSERT INTO item_assignment (
    item_id,
    employee_id,
    post_id,
    assigned_at,
    condition_on_assignment,
    notes
) VALUES (
    :'item_id',
    :'employee_primary_id',
    (SELECT id FROM security_post WHERE code = 'POST-001' LIMIT 1),
    '2024-06-15 05:45:00+03',
    'Bon état',
    'Affectation permanente pendant le service'
);

INSERT INTO tax_rate (
    company_id,
    name,
    rate,
    active
) VALUES (
    :'company_id',
    'TVA standard',
    20,
    TRUE
) RETURNING id \gset tax_rate_

INSERT INTO invoice (
    company_id,
    client_id,
    contract_id,
    invoice_number,
    issue_date,
    due_date,
    status,
    currency_code,
    subtotal,
    discount_amount,
    tax_amount,
    total_amount,
    amount_paid,
    notes
) VALUES (
    :'company_id',
    :'client_id',
    :'contract_id',
    'FAC-2024-001',
    '2024-06-01',
    '2024-06-30',
    'ISSUED',
    'MGA',
    2500000,
    0,
    500000,
    3000000,
    1000000,
    'Facture de démonstration'
) RETURNING id \gset invoice_

INSERT INTO invoice_line (
    invoice_id,
    service_id,
    description,
    quantity,
    unit_price,
    discount_rate,
    tax_rate,
    line_subtotal,
    line_tax,
    line_total
) VALUES (
    :'invoice_id',
    :'service_id',
    'Sécurité 24 heures - juin 2024',
    1,
    2500000,
    0,
    20,
    2500000,
    500000,
    3000000
);

INSERT INTO payment (
    invoice_id,
    payment_reference,
    payment_date,
    amount,
    method,
    status,
    transaction_reference,
    notes
) VALUES (
    :'invoice_id',
    'PAY-2024-001',
    '2024-06-10',
    1000000,
    'BANK_TRANSFER',
    'CONFIRMED',
    'TRX-DEMO-001',
    'Acompte de démonstration'
) RETURNING id \gset payment_

INSERT INTO payment_allocation (
    payment_id,
    invoice_id,
    amount
) VALUES (
    :'payment_id',
    :'invoice_id',
    1000000
);

INSERT INTO document (
    company_id,
    client_id,
    employee_id,
    contract_id,
    invoice_id,
    document_type,
    title,
    file_name,
    file_url,
    mime_type
) VALUES (
    :'company_id',
    :'client_id',
    :'employee_primary_id',
    :'contract_id',
    :'invoice_id',
    'CONTRACT',
    'Contrat signé démo',
    'contrat-2024-001.pdf',
    'https://example.test/documents/contrat-2024-001.pdf',
    'application/pdf'
);

INSERT INTO notification (
    user_id,
    employee_id,
    channel,
    status,
    subject,
    message,
    scheduled_at,
    sent_at
) VALUES (
    :'user_id',
    :'employee_primary_id',
    'IN_APP',
    'SENT',
    'Nouvelle affectation',
    'Une nouvelle affectation vous a été attribuée.',
    '2024-06-15 05:30:00+03',
    '2024-06-15 05:30:05+03'
);

INSERT INTO email_log (
    recipient_email,
    subject,
    body,
    sent_at,
    status
) VALUES (
    'demo.admin@sssm.test',
    'Bienvenue dans SSSM',
    'Votre compte de démonstration est prêt.',
    '2024-01-15 08:00:00+03',
    'SENT'
);

INSERT INTO audit_log (
    user_id,
    action,
    entity_name,
    entity_id,
    old_data,
    new_data,
    ip_address,
    user_agent
) VALUES (
    :'user_id',
    'CREATE',
    'company',
    :'company_id',
    NULL,
    '{"name": "SSSM Madagascar"}'::jsonb,
    '127.0.0.1',
    'SSSM mock data script'
);

-- =====================================================================
-- PARTIE 2 : DONNÉES SUPPLÉMENTAIRES
-- =====================================================================

-- ---------------------------------------------------------------------
-- 2.1 Agences et branches
-- ---------------------------------------------------------------------

INSERT INTO agency (company_id, name, code, address, city, phone, email, active) VALUES (
    :'company_id', 'Agence Toamasina', 'AT', 'Boulevard Ratsimilaho', 'Toamasina',
    '+261 34 00 000 12', 'agence.toamasina@sssm.test', TRUE
) RETURNING id \gset agency_toa_

INSERT INTO agency (company_id, name, code, address, city, phone, email, active) VALUES (
    :'company_id', 'Agence Antsirabe', 'AA', 'Avenue de l''Indépendance', 'Antsirabe',
    '+261 34 00 000 13', 'agence.antsirabe@sssm.test', TRUE
) RETURNING id \gset agency_ant_

INSERT INTO branch (company_id, agence_id, name, code, address, city, status) VALUES (
    :'company_id', :'agency_toa_id', 'Branche Toamasina', 'TOAM', 'Tanambao V', 'Toamasina', 'ACTIVE'
) RETURNING id \gset branch_toa_

INSERT INTO branch (company_id, agence_id, name, code, address, city, status) VALUES (
    :'company_id', :'agency_ant_id', 'Branche Antsirabe', 'ANTS', 'Ambohimena', 'Antsirabe', 'ACTIVE'
) RETURNING id \gset branch_ant_

-- ---------------------------------------------------------------------
-- 2.2 Permissions, rôles, utilisateurs
-- ---------------------------------------------------------------------

INSERT INTO permission (code, name, description) VALUES
    ('employee.read',    'Lecture employés',      'Consulter les fiches employés'),
    ('employee.manage',  'Gestion employés',      'Créer et modifier les employés'),
    ('site.read',        'Lecture sites',         'Consulter les sites et postes'),
    ('site.manage',      'Gestion sites',         'Créer et modifier les sites et postes'),
    ('incident.read',    'Lecture incidents',     'Consulter les incidents'),
    ('incident.report',  'Déclaration incidents', 'Déclarer et traiter un incident'),
    ('invoice.read',     'Lecture factures',      'Consulter les factures et paiements'),
    ('invoice.manage',   'Gestion facturation',   'Créer des factures et enregistrer des paiements'),
    ('inventory.manage', 'Gestion inventaire',    'Gérer le matériel et les mouvements de stock');

INSERT INTO role (company_id, name, description) VALUES
    (:'company_id', 'Superviseur',       'Supervision des sites, des agents et des incidents'),
    (:'company_id', 'Agent de sécurité', 'Agent de terrain'),
    (:'company_id', 'Comptable',         'Facturation et paiements');

SELECT
    (SELECT id FROM role WHERE name = 'Superviseur'       AND company_id = :'company_id') AS role_sup_id,
    (SELECT id FROM role WHERE name = 'Agent de sécurité' AND company_id = :'company_id') AS role_agent_id,
    (SELECT id FROM role WHERE name = 'Comptable'         AND company_id = :'company_id') AS role_compta_id
\gset

-- L'administrateur démo reçoit toutes les nouvelles permissions
INSERT INTO role_permission (role_id, permission_id)
SELECT :'role_id', id FROM permission
WHERE code IN ('employee.read','employee.manage','site.read','site.manage','incident.read',
               'incident.report','invoice.read','invoice.manage','inventory.manage');

INSERT INTO role_permission (role_id, permission_id)
SELECT :'role_sup_id', id FROM permission
WHERE code IN ('employee.read','site.read','site.manage','incident.read','incident.report','inventory.manage');

INSERT INTO role_permission (role_id, permission_id)
SELECT :'role_agent_id', id FROM permission
WHERE code IN ('site.read','incident.report');

INSERT INTO role_permission (role_id, permission_id)
SELECT :'role_compta_id', id FROM permission
WHERE code IN ('invoice.read','invoice.manage');

INSERT INTO app_user (company_id, username, email, password_hash, first_name, last_name, phone, status) VALUES
    (:'company_id', 'lalao.superviseur', 'lalao.randriamanana@sssm.test', '$2a$10$demo-password-hash',
     'Lalao', 'Randriamanana', '+261 34 00 000 52', 'ACTIVE'),
    (:'company_id', 'voahangy.compta',    'voahangy.rakotomalala@sssm.test', '$2a$10$demo-password-hash',
     'Voahangy', 'Rakotomalala', '+261 34 00 000 53', 'ACTIVE'),
    (:'company_id', 'tahina.agent',       'tahina.razafindrabe@sssm.test', '$2a$10$demo-password-hash',
     'Tahina', 'Razafindrabe', '+261 34 00 000 54', 'ACTIVE'),
    (:'company_id', 'tsiky.agent',        'tsiky.ravelomanana@sssm.test', '$2a$10$demo-password-hash',
     'Tsiky', 'Ravelomanana', '+261 34 00 000 55', 'ACTIVE');

SELECT
    (SELECT id FROM app_user WHERE username = 'lalao.superviseur') AS user_sup_id,
    (SELECT id FROM app_user WHERE username = 'voahangy.compta')   AS user_compta_id,
    (SELECT id FROM app_user WHERE username = 'tahina.agent')      AS user_agent1_id,
    (SELECT id FROM app_user WHERE username = 'tsiky.agent')       AS user_agent2_id
\gset

INSERT INTO user_role (user_id, role_id) VALUES
    (:'user_sup_id',    :'role_sup_id'),
    (:'user_compta_id', :'role_compta_id'),
    (:'user_agent1_id', :'role_agent_id'),
    (:'user_agent2_id', :'role_agent_id');

-- ---------------------------------------------------------------------
-- 2.3 Employés et documents
-- ---------------------------------------------------------------------

INSERT INTO employee (
    company_id, agency_id, user_id, employee_number, first_name, last_name, gender, date_of_birth,
    phone, email, address, national_id, hire_date, position, status,
    emergency_contact_name, emergency_contact_phone
) VALUES
    (:'company_id', :'agency_id',     :'user_sup_id',    'EMP-0003', 'Lalao',    'Randriamanana', 'F', '1988-03-12',
     '+261 34 00 000 65', 'lalao.randriamanana@sssm.test', 'Ankadifotsy', 'NID-0003', '2023-09-01',
     'Superviseur', 'ACTIVE', 'Rado Randriamanana', '+261 34 00 000 66'),
    (:'company_id', :'agency_id',     :'user_agent1_id', 'EMP-0004', 'Tahina',   'Razafindrabe',  'M', '1993-11-02',
     '+261 34 00 000 67', 'tahina.razafindrabe@sssm.test', 'Ankorondrano', 'NID-0004', '2024-02-15',
     'Agent de sécurité', 'ACTIVE', 'Naina Razafindrabe', '+261 34 00 000 68'),
    (:'company_id', :'agency_id',     :'user_compta_id', 'EMP-0005', 'Voahangy', 'Rakotomalala',  'F', '1991-07-25',
     '+261 34 00 000 69', 'voahangy.rakotomalala@sssm.test', 'Isotry', 'NID-0005', '2023-06-01',
     'Comptable', 'ACTIVE', 'Hanta Rakotomalala', '+261 34 00 000 70'),
    (:'company_id', :'agency_toa_id', NULL,              'EMP-0006', 'Mamy',     'Andrianarivo',  'M', '1994-01-18',
     '+261 34 00 000 71', 'mamy.andrianarivo@sssm.test', 'Ambodimanga', 'NID-0006', '2024-03-01',
     'Agent de sécurité', 'ACTIVE', 'Lanto Andrianarivo', '+261 34 00 000 72'),
    (:'company_id', :'agency_toa_id', NULL,              'EMP-0007', 'Njaka',    'Rabemananjara', 'M', '1996-09-09',
     '+261 34 00 000 73', 'njaka.rabemananjara@sssm.test', 'Tanambao II', 'NID-0007', '2024-03-01',
     'Agent de sécurité', 'ACTIVE', 'Vola Rabemananjara', '+261 34 00 000 74'),
    (:'company_id', :'agency_id',     :'user_agent2_id', 'EMP-0008', 'Tsiky',    'Ravelomanana',  'F', '1997-04-30',
     '+261 34 00 000 75', 'tsiky.ravelomanana@sssm.test', 'Andohalo', 'NID-0008', '2024-02-20',
     'Agent de sécurité', 'ACTIVE', 'Zo Ravelomanana', '+261 34 00 000 76'),
    (:'company_id', :'agency_ant_id', NULL,              'EMP-0009', 'Fidy',     'Randrianasolo', 'M', '1989-12-05',
     '+261 34 00 000 77', 'fidy.randrianasolo@sssm.test', 'Ambohimena', 'NID-0009', '2023-11-01',
     'Chef d''équipe', 'ACTIVE', 'Tiana Randrianasolo', '+261 34 00 000 78'),
    (:'company_id', :'agency_ant_id', NULL,              'EMP-0010', 'Miora',    'Rasolofo',      'F', '1998-06-14',
     '+261 34 00 000 79', 'miora.rasolofo@sssm.test', 'Mahazoarivo', 'NID-0010', '2024-04-01',
     'Agent de sécurité', 'ACTIVE', 'Haja Rasolofo', '+261 34 00 000 80');

SELECT
    (SELECT id FROM employee WHERE employee_number = 'EMP-0003') AS emp3_id,
    (SELECT id FROM employee WHERE employee_number = 'EMP-0004') AS emp4_id,
    (SELECT id FROM employee WHERE employee_number = 'EMP-0005') AS emp5_id,
    (SELECT id FROM employee WHERE employee_number = 'EMP-0006') AS emp6_id,
    (SELECT id FROM employee WHERE employee_number = 'EMP-0007') AS emp7_id,
    (SELECT id FROM employee WHERE employee_number = 'EMP-0008') AS emp8_id,
    (SELECT id FROM employee WHERE employee_number = 'EMP-0009') AS emp9_id,
    (SELECT id FROM employee WHERE employee_number = 'EMP-0010') AS emp10_id
\gset

INSERT INTO employee_document (employee_id, document_type, document_number, issue_date, expiry_date, file_url, notes) VALUES
    (:'employee_secondary_id', 'Carte professionnelle', 'DOC-EMP-0002', '2024-01-25', '2027-01-25',
     'https://example.test/documents/emp-0002.pdf', 'Carte professionnelle agent'),
    (:'emp3_id',  'Carte professionnelle', 'DOC-EMP-0003', '2023-08-20', '2026-08-20',
     'https://example.test/documents/emp-0003.pdf', 'Carte superviseur'),
    (:'emp4_id',  'Carte professionnelle', 'DOC-EMP-0004', '2024-02-10', '2027-02-10',
     'https://example.test/documents/emp-0004.pdf', NULL),
    (:'emp4_id',  'Certificat médical',    'MED-EMP-0004', '2024-02-05', '2025-02-05',
     'https://example.test/documents/med-emp-0004.pdf', 'Aptitude physique validée'),
    (:'emp6_id',  'Carte professionnelle', 'DOC-EMP-0006', '2024-02-25', '2027-02-25',
     'https://example.test/documents/emp-0006.pdf', NULL),
    (:'emp8_id',  'Permis de conduire',    'PERM-EMP-0008', '2022-05-10', '2032-05-10',
     'https://example.test/documents/perm-emp-0008.pdf', 'Catégorie B'),
    (:'emp9_id',  'Carte professionnelle', 'DOC-EMP-0009', '2023-10-20', '2026-10-20',
     'https://example.test/documents/emp-0009.pdf', 'Expire bientôt'),
    (:'emp10_id', 'Certificat médical',    'MED-EMP-0010', '2024-03-20', '2025-03-20',
     'https://example.test/documents/med-emp-0010.pdf', NULL);

-- ---------------------------------------------------------------------
-- 2.4 Clients et contacts
-- ---------------------------------------------------------------------

INSERT INTO client (
    company_id, agency_id, code, name, legal_name, client_type, registration_number, tax_number,
    email, phone, address, city, country, payment_terms_days, active
) VALUES
    (:'company_id', :'agency_id',     'CLI-002', 'Banque Océan',     'Banque Océan Madagascar SA', 'Entreprise',
     'RC-CLI-002', 'NIF-CLI-002', 'contact@banque-ocean.test', '+261 34 00 000 82',
     'Analakely', 'Antananarivo', 'Madagascar', 15, TRUE),
    (:'company_id', :'agency_toa_id', 'CLI-003', 'Hôtel Le Baobab',  'Hôtel Le Baobab SARL',       'Entreprise',
     'RC-CLI-003', 'NIF-CLI-003', 'direction@hotel-baobab.test', '+261 34 00 000 83',
     'Boulevard Joffre', 'Toamasina', 'Madagascar', 30, TRUE),
    (:'company_id', :'agency_ant_id', 'CLI-004', 'Mada Market',      'Mada Market SA',             'Entreprise',
     'RC-CLI-004', 'NIF-CLI-004', 'achats@mada-market.test', '+261 34 00 000 84',
     'Route d''Ambositra', 'Antsirabe', 'Madagascar', 45, TRUE);

SELECT
    (SELECT id FROM client WHERE code = 'CLI-002') AS client2_id,
    (SELECT id FROM client WHERE code = 'CLI-003') AS client3_id,
    (SELECT id FROM client WHERE code = 'CLI-004') AS client4_id
\gset

INSERT INTO client_contact (client_id, first_name, last_name, function, email, phone, is_primary) VALUES
    (:'client_id',  'Tojo',   'Rabary',        'Directeur des opérations', 'tojo@client-demo.test',      '+261 34 00 000 92', FALSE),
    (:'client2_id', 'Nirina', 'Rakotondrabe',  'Responsable sécurité',     'nirina@banque-ocean.test',   '+261 34 00 000 93', TRUE),
    (:'client2_id', 'Aina',   'Razanamahefa',  'Directrice d''agence',     'aina@banque-ocean.test',     '+261 34 00 000 94', FALSE),
    (:'client3_id', 'Sarobidy','Andrianjafy',  'Directeur général',        'sarobidy@hotel-baobab.test', '+261 34 00 000 95', TRUE),
    (:'client3_id', 'Lova',   'Ramanantsoa',   'Chef de réception',        'lova@hotel-baobab.test',     '+261 34 00 000 96', FALSE),
    (:'client4_id', 'Faly',   'Rasamoely',     'Responsable magasin',      'faly@mada-market.test',      '+261 34 00 000 97', TRUE);

-- ---------------------------------------------------------------------
-- 2.5 Sites, postes et exigences
-- ---------------------------------------------------------------------

INSERT INTO site (
    company_id, client_id, branch_id, name, code, address, city,
    latitude, longitude, status, access_instructions, risk_level
) VALUES
    (:'company_id', :'client2_id', :'branch_id',     'Banque Océan - Agence Analakely', 'SITE-002',
     'Rue Rainandriamampandry, Analakely', 'Antananarivo', -18.9096000, 47.5256000, 'ACTIVE',
     'Entrée par la façade principale, badge obligatoire', 'HIGH'),
    (:'company_id', :'client3_id', :'branch_toa_id', 'Hôtel Le Baobab', 'SITE-003',
     'Boulevard Joffre', 'Toamasina', -18.1500000, 49.4000000, 'ACTIVE',
     'Entrée du personnel côté jardin', 'MEDIUM'),
    (:'company_id', :'client4_id', :'branch_ant_id', 'Mada Market Antsirabe', 'SITE-004',
     'Route d''Ambositra', 'Antsirabe', -19.8650000, 47.0340000, 'ACTIVE',
     'Livraisons uniquement par l''entrée arrière', 'LOW'),
    (:'company_id', :'client_id',  :'branch_id',     'Entrepôt Client Démo', 'SITE-005',
     'Route Digue, Anosizato', 'Antananarivo', -18.8810000, 47.5100000, 'ACTIVE',
     'Contacter le gardien avant toute entrée', 'LOW');

SELECT
    (SELECT id FROM site WHERE code = 'SITE-002') AS site2_id,
    (SELECT id FROM site WHERE code = 'SITE-003') AS site3_id,
    (SELECT id FROM site WHERE code = 'SITE-004') AS site4_id,
    (SELECT id FROM site WHERE code = 'SITE-005') AS site5_id
\gset

INSERT INTO security_post (site_id, name, code, description, required_agents, start_time, end_time, status) VALUES
    (:'site2_id', 'Accueil banque',      'POST-002', 'Contrôle des entrées et du hall',         2, '06:00', '18:00', 'ACTIVE'),
    (:'site2_id', 'Distributeurs',       'POST-003', 'Surveillance des distributeurs',          1, '06:00', '18:00', 'ACTIVE'),
    (:'site3_id', 'Entrée hôtel',        'POST-004', 'Accueil et contrôle de l''entrée',        1, '06:00', '14:00', 'ACTIVE'),
    (:'site3_id', 'Jardin et piscine',   'POST-005', 'Surveillance du jardin et de la piscine', 1, '14:00', '22:00', 'ACTIVE'),
    (:'site4_id', 'Caisses et entrée',   'POST-006', 'Contrôle clientèle et caisses',           2, '06:00', '18:00', 'ACTIVE'),
    (:'site5_id', 'Portail entrepôt',    'POST-007', 'Contrôle des camions et visiteurs',       1, '06:00', '18:00', 'ACTIVE');

SELECT
    (SELECT id FROM security_post WHERE code = 'POST-002') AS post2_id,
    (SELECT id FROM security_post WHERE code = 'POST-003') AS post3_id,
    (SELECT id FROM security_post WHERE code = 'POST-004') AS post4_id,
    (SELECT id FROM security_post WHERE code = 'POST-005') AS post5_id,
    (SELECT id FROM security_post WHERE code = 'POST-006') AS post6_id,
    (SELECT id FROM security_post WHERE code = 'POST-007') AS post7_id
\gset

INSERT INTO post_requirement (post_id, requirement) VALUES
    (:'post2_id', 'Tenue complète'),
    (:'post2_id', 'Radio portable'),
    (:'post2_id', 'Gilet de protection'),
    (:'post3_id', 'Radio portable'),
    (:'post4_id', 'Tenue complète'),
    (:'post4_id', 'Lampe torche'),
    (:'post5_id', 'Lampe torche'),
    (:'post5_id', 'Téléphone professionnel'),
    (:'post6_id', 'Tenue complète'),
    (:'post6_id', 'Radio portable'),
    (:'post7_id', 'Registre des visiteurs');

-- ---------------------------------------------------------------------
-- 2.6 Services, contrats, prestations
-- ---------------------------------------------------------------------

INSERT INTO service (company_id, code, name, description, unit, unit_price, vat_rate, active) VALUES
    (:'company_id', 'SEC-12H',     'Sécurité 12 heures',   'Surveillance de jour ou de nuit',             'month', 1400000, 20, TRUE),
    (:'company_id', 'CTRL-ACCES',  'Contrôle d''accès',    'Gestion des entrées, badges et registres',    'month', 1800000, 20, TRUE),
    (:'company_id', 'RONDE',       'Ronde de surveillance','Ronde avec pointage de checkpoints',          'visit',   25000, 20, TRUE),
    (:'company_id', 'INTERVENTION','Intervention urgente', 'Intervention d''une équipe mobile',            'hour',    60000, 20, TRUE);

SELECT
    (SELECT id FROM service WHERE code = 'SEC-12H')     AS service_12h_id,
    (SELECT id FROM service WHERE code = 'CTRL-ACCES')  AS service_acces_id,
    (SELECT id FROM service WHERE code = 'RONDE')       AS service_ronde_id
\gset

INSERT INTO contract (
    company_id, client_id, contract_number, title, start_date, end_date, status,
    billing_frequency, currency_code, payment_terms_days, total_amount, notes
) VALUES
    (:'company_id', :'client2_id', 'CTR-2024-002', 'Sécurité agence Banque Océan Analakely',
     '2024-03-01', '2025-02-28', 'ACTIVE', 'MONTHLY', 'MGA', 15, 38400000, 'Contrôle d''accès et sécurité de jour'),
    (:'company_id', :'client3_id', 'CTR-2024-003', 'Sécurité 24h Hôtel Le Baobab',
     '2024-02-01', '2025-01-31', 'ACTIVE', 'MONTHLY', 'MGA', 30, 30000000, 'Surveillance permanente de l''hôtel'),
    (:'company_id', :'client4_id', 'CTR-2024-004', 'Sécurité magasin Mada Market Antsirabe',
     '2024-04-01', '2025-03-31', 'ACTIVE', 'MONTHLY', 'MGA', 45, 25800000, 'Sécurité de jour et rondes'),
    (:'company_id', :'client_id',  'CTR-2024-005', 'Gardiennage entrepôt Client Démo',
     '2024-07-01', '2024-12-31', 'ACTIVE', 'MONTHLY', 'MGA', 30, 8400000, 'Contrat additionnel pour l''entrepôt');

SELECT
    (SELECT id FROM contract WHERE contract_number = 'CTR-2024-002') AS contract2_id,
    (SELECT id FROM contract WHERE contract_number = 'CTR-2024-003') AS contract3_id,
    (SELECT id FROM contract WHERE contract_number = 'CTR-2024-004') AS contract4_id,
    (SELECT id FROM contract WHERE contract_number = 'CTR-2024-005') AS contract5_id
\gset

INSERT INTO contract_service (
    contract_id, service_id, site_id, quantity, unit_price, vat_rate, start_date, end_date, notes
) VALUES
    (:'contract2_id', :'service_acces_id', :'site2_id', 12, 1800000, 20, '2024-03-01', '2025-02-28', 'Contrôle d''accès'),
    (:'contract2_id', :'service_12h_id',   :'site2_id', 12, 1400000, 20, '2024-03-01', '2025-02-28', 'Sécurité de jour'),
    (:'contract3_id', :'service_id',       :'site3_id', 12, 2500000, 20, '2024-02-01', '2025-01-31', 'Sécurité 24h'),
    (:'contract4_id', :'service_12h_id',   :'site4_id', 12, 1400000, 20, '2024-04-01', '2025-03-31', 'Sécurité de jour'),
    (:'contract4_id', :'service_ronde_id', :'site4_id', 360,  25000, 20, '2024-04-01', '2025-03-31', 'Rondes quotidiennes'),
    (:'contract5_id', :'service_12h_id',   :'site5_id',  6, 1400000, 20, '2024-07-01', '2024-12-31', 'Gardiennage de jour');

-- ---------------------------------------------------------------------
-- 2.7 Équipes, plannings, affectations, présences
-- ---------------------------------------------------------------------

INSERT INTO shift (company_id, name, start_time, end_time, duration_hours, description) VALUES
    (:'company_id', 'Équipe matin',      '06:00', '14:00', 8, 'Équipe du matin'),
    (:'company_id', 'Équipe après-midi', '14:00', '22:00', 8, 'Équipe de l''après-midi');

SELECT
    (SELECT id FROM shift WHERE name = 'Équipe matin'      AND company_id = :'company_id') AS shift_morning_id,
    (SELECT id FROM shift WHERE name = 'Équipe après-midi' AND company_id = :'company_id') AS shift_afternoon_id
\gset

INSERT INTO schedule (company_id, site_id, post_id, shift_id, schedule_date, status, notes) VALUES
    (:'company_id', :'site_id',  :'post_id',  :'shift_id',           '2024-06-16', 'CONFIRMED', 'Planning poste principal'),
    (:'company_id', :'site_id',  :'post_id',  :'shift_id',           '2024-06-17', 'CONFIRMED', 'Planning poste principal'),
    (:'company_id', :'site2_id', :'post2_id', :'shift_id',           '2024-06-16', 'CONFIRMED', 'Planning accueil banque'),
    (:'company_id', :'site2_id', :'post2_id', :'shift_id',           '2024-06-17', 'CONFIRMED', 'Planning accueil banque'),
    (:'company_id', :'site3_id', :'post4_id', :'shift_morning_id',   '2024-06-16', 'CONFIRMED', 'Entrée hôtel - matin'),
    (:'company_id', :'site3_id', :'post4_id', :'shift_morning_id',   '2024-06-17', 'CONFIRMED', 'Entrée hôtel - matin'),
    (:'company_id', :'site3_id', :'post5_id', :'shift_afternoon_id', '2024-06-16', 'CONFIRMED', 'Jardin hôtel - après-midi'),
    (:'company_id', :'site3_id', :'post5_id', :'shift_afternoon_id', '2024-06-17', 'CONFIRMED', 'Jardin hôtel - après-midi'),
    (:'company_id', :'site4_id', :'post6_id', :'shift_id',           '2024-06-16', 'CONFIRMED', 'Planning Mada Market'),
    (:'company_id', :'site4_id', :'post6_id', :'shift_id',           '2024-06-17', 'CONFIRMED', 'Planning Mada Market');

INSERT INTO assignment (schedule_id, employee_id, status, check_in_at, notes)
SELECT s.id, e.id, 'CONFIRMED', v.check_in::timestamptz, v.notes
FROM (VALUES
    ('POST-001', '2024-06-16', 'EMP-0002', '2024-06-16 06:00:00+03', 'Agent portail'),
    ('POST-001', '2024-06-16', 'EMP-0008', '2024-06-16 06:00:00+03', 'Agent portail'),
    ('POST-001', '2024-06-17', 'EMP-0001', '2024-06-17 06:00:00+03', 'Chef de site'),
    ('POST-001', '2024-06-17', 'EMP-0008', '2024-06-17 06:00:00+03', 'Agent portail'),
    ('POST-002', '2024-06-16', 'EMP-0004', '2024-06-16 06:00:00+03', 'Contrôle hall'),
    ('POST-002', '2024-06-16', 'EMP-0001', '2024-06-16 06:00:00+03', 'Contrôle hall'),
    ('POST-002', '2024-06-17', 'EMP-0004', '2024-06-17 06:00:00+03', 'Contrôle hall'),
    ('POST-002', '2024-06-17', 'EMP-0002', '2024-06-17 06:00:00+03', 'Contrôle hall'),
    ('POST-004', '2024-06-16', 'EMP-0006', '2024-06-16 06:00:00+03', 'Entrée hôtel'),
    ('POST-004', '2024-06-17', 'EMP-0007', '2024-06-17 06:00:00+03', 'Entrée hôtel'),
    ('POST-005', '2024-06-16', 'EMP-0007', '2024-06-16 14:00:00+03', 'Jardin et piscine'),
    ('POST-005', '2024-06-17', 'EMP-0006', '2024-06-17 14:00:00+03', 'Jardin et piscine'),
    ('POST-006', '2024-06-16', 'EMP-0010', '2024-06-16 06:00:00+03', 'Caisses'),
    ('POST-006', '2024-06-16', 'EMP-0009', '2024-06-16 06:00:00+03', 'Chef d''équipe'),
    ('POST-006', '2024-06-17', 'EMP-0010', '2024-06-17 06:00:00+03', 'Caisses'),
    ('POST-006', '2024-06-17', 'EMP-0009', '2024-06-17 06:00:00+03', 'Chef d''équipe')
) AS v(post_code, sdate, emp, check_in, notes)
JOIN security_post p ON p.code = v.post_code
JOIN schedule s      ON s.post_id = p.id AND s.schedule_date = v.sdate::date
JOIN employee e      ON e.employee_number = v.emp;

-- Présences générées à partir des affectations (16 et 17 juin 2024)
INSERT INTO attendance (employee_id, site_id, assignment_id, attendance_date, check_in_at, check_out_at, status)
SELECT
    a.employee_id,
    s.site_id,
    a.id,
    s.schedule_date,
    a.check_in_at - INTERVAL '5 minutes',
    a.check_in_at + (sh.duration_hours * INTERVAL '1 hour') + INTERVAL '5 minutes',
    'PRESENT'
FROM assignment a
JOIN schedule s ON s.id = a.schedule_id
JOIN shift sh   ON sh.id = s.shift_id
WHERE s.schedule_date >= '2024-06-16';

-- ---------------------------------------------------------------------
-- 2.8 Rondes, checkpoints, patrouilles, GPS
-- ---------------------------------------------------------------------

INSERT INTO patrol_route (site_id, name, description, estimated_duration_minutes, active) VALUES (
    :'site3_id', 'Ronde hôtel', 'Entrée, piscine et plage de l''hôtel', 40, TRUE
) RETURNING id \gset patrol_route_hotel_

INSERT INTO patrol_route (site_id, name, description, estimated_duration_minutes, active) VALUES (
    :'site2_id', 'Ronde banque', 'Hall d''accueil et salle des coffres', 30, TRUE
) RETURNING id \gset patrol_route_bank_

INSERT INTO patrol_route (site_id, name, description, estimated_duration_minutes, active) VALUES (
    :'site4_id', 'Ronde magasin', 'Entrée clients, réserve et parking', 35, TRUE
) RETURNING id \gset patrol_route_market_

INSERT INTO checkpoint (patrol_route_id, name, code, sequence_number, latitude, longitude, radius_meters, qr_code) VALUES
    (:'patrol_route_id',        'Parking',            'CP-002', 2, -18.8795000, 47.5082000, 50, 'QR-CP-002'),
    (:'patrol_route_id',        'Local technique',    'CP-003', 3, -18.8799000, 47.5071000, 40, 'QR-CP-003'),
    (:'patrol_route_hotel_id',  'Entrée principale',  'CP-004', 1, -18.1499000, 49.4001000, 30, 'QR-CP-004'),
    (:'patrol_route_hotel_id',  'Piscine',            'CP-005', 2, -18.1503000, 49.4006000, 30, 'QR-CP-005'),
    (:'patrol_route_hotel_id',  'Plage',              'CP-006', 3, -18.1508000, 49.4012000, 40, 'QR-CP-006'),
    (:'patrol_route_bank_id',   'Hall d''accueil',    'CP-007', 1, -18.9100000, 47.5255000, 20, 'QR-CP-007'),
    (:'patrol_route_bank_id',   'Salle des coffres',  'CP-008', 2, -18.9102000, 47.5257000, 20, 'QR-CP-008'),
    (:'patrol_route_market_id', 'Entrée clients',     'CP-009', 1, -19.8650000, 47.0340000, 30, 'QR-CP-009'),
    (:'patrol_route_market_id', 'Réserve',            'CP-010', 2, -19.8653000, 47.0344000, 30, 'QR-CP-010'),
    (:'patrol_route_market_id', 'Parking',            'CP-011', 3, -19.8656000, 47.0338000, 40, 'QR-CP-011');

SELECT
    (SELECT id FROM checkpoint WHERE code = 'CP-002') AS cp2_id,
    (SELECT id FROM checkpoint WHERE code = 'CP-003') AS cp3_id,
    (SELECT id FROM checkpoint WHERE code = 'CP-004') AS cp4_id,
    (SELECT id FROM checkpoint WHERE code = 'CP-005') AS cp5_id,
    (SELECT id FROM checkpoint WHERE code = 'CP-006') AS cp6_id,
    (SELECT id FROM checkpoint WHERE code = 'CP-007') AS cp7_id,
    (SELECT id FROM checkpoint WHERE code = 'CP-008') AS cp8_id,
    (SELECT id FROM checkpoint WHERE code = 'CP-009') AS cp9_id,
    (SELECT id FROM checkpoint WHERE code = 'CP-010') AS cp10_id,
    (SELECT id FROM checkpoint WHERE code = 'CP-011') AS cp11_id
\gset

INSERT INTO patrol (patrol_route_id, employee_id, site_id, scheduled_at, started_at, ended_at, status, notes) VALUES (
    :'patrol_route_id', :'emp8_id', :'site_id',
    '2024-06-16 10:00:00+03', '2024-06-16 10:01:00+03', '2024-06-16 10:44:00+03',
    'COMPLETED', 'Ronde périmétrique - RAS'
) RETURNING id \gset patrol_2_

INSERT INTO patrol (patrol_route_id, employee_id, site_id, scheduled_at, started_at, ended_at, status, notes) VALUES (
    :'patrol_route_hotel_id', :'emp6_id', :'site3_id',
    '2024-06-16 09:00:00+03', '2024-06-16 09:03:00+03', '2024-06-16 09:42:00+03',
    'COMPLETED', 'Ronde hôtel - RAS'
) RETURNING id \gset patrol_3_

INSERT INTO patrol (patrol_route_id, employee_id, site_id, scheduled_at, started_at, ended_at, status, notes) VALUES (
    :'patrol_route_bank_id', :'emp4_id', :'site2_id',
    '2024-06-16 11:00:00+03', '2024-06-16 11:01:00+03', '2024-06-16 11:31:00+03',
    'COMPLETED', 'Ronde banque - RAS'
) RETURNING id \gset patrol_4_

INSERT INTO patrol (patrol_route_id, employee_id, site_id, scheduled_at, started_at, ended_at, status, notes) VALUES (
    :'patrol_route_market_id', :'emp10_id', :'site4_id',
    '2024-06-16 15:00:00+03', '2024-06-16 15:02:00+03', '2024-06-16 15:38:00+03',
    'COMPLETED', 'Ronde magasin - extincteurs vérifiés'
) RETURNING id \gset patrol_5_

INSERT INTO patrol_checkpoint (patrol_id, checkpoint_id, scanned_at, latitude, longitude, distance_meters, status) VALUES
    (:'patrol_2_id', :'checkpoint_id', '2024-06-16 10:08:00+03', -18.8789100, 47.5076100,  4.1, 'SCANNED'),
    (:'patrol_2_id', :'cp2_id',        '2024-06-16 10:22:00+03', -18.8795100, 47.5082200,  3.0, 'SCANNED'),
    (:'patrol_2_id', :'cp3_id',        '2024-06-16 10:36:00+03', -18.8799200, 47.5071100,  5.6, 'SCANNED'),
    (:'patrol_3_id', :'cp4_id',        '2024-06-16 09:08:00+03', -18.1499100, 49.4001100,  2.8, 'SCANNED'),
    (:'patrol_3_id', :'cp5_id',        '2024-06-16 09:21:00+03', -18.1503100, 49.4006200,  3.9, 'SCANNED'),
    (:'patrol_3_id', :'cp6_id',        '2024-06-16 09:36:00+03', -18.1508200, 49.4012100,  6.2, 'SCANNED'),
    (:'patrol_4_id', :'cp7_id',        '2024-06-16 11:08:00+03', -18.9100100, 47.5255100,  1.9, 'SCANNED'),
    (:'patrol_4_id', :'cp8_id',        '2024-06-16 11:22:00+03', -18.9102100, 47.5257100,  2.4, 'SCANNED'),
    (:'patrol_5_id', :'cp9_id',        '2024-06-16 15:08:00+03', -19.8650100, 47.0340100,  3.5, 'SCANNED'),
    (:'patrol_5_id', :'cp10_id',       '2024-06-16 15:20:00+03', -19.8653100, 47.0344200,  4.8, 'SCANNED'),
    (:'patrol_5_id', :'cp11_id',       '2024-06-16 15:32:00+03', -19.8656100, 47.0338100,  5.0, 'SCANNED');

INSERT INTO gps_tracking (
    employee_id, patrol_id, recorded_at, latitude, longitude, accuracy_meters, speed_kmh, heading, battery_level
) VALUES
    (:'emp8_id',  :'patrol_2_id', '2024-06-16 10:10:00+03', -18.8791000, 47.5078000, 4.8, 3.2,  90, 92),
    (:'emp8_id',  :'patrol_2_id', '2024-06-16 10:25:00+03', -18.8796000, 47.5080000, 5.1, 3.8, 180, 90),
    (:'emp6_id',  :'patrol_3_id', '2024-06-16 09:10:00+03', -18.1500000, 49.4003000, 3.9, 3.0,  45, 78),
    (:'emp6_id',  :'patrol_3_id', '2024-06-16 09:25:00+03', -18.1505000, 49.4008000, 4.2, 3.4, 120, 76),
    (:'emp4_id',  :'patrol_4_id', '2024-06-16 11:10:00+03', -18.9101000, 47.5256000, 3.5, 2.1,  60, 88),
    (:'emp10_id', :'patrol_5_id', '2024-06-16 15:10:00+03', -19.8651000, 47.0342000, 5.5, 3.6, 135, 81),
    (:'emp10_id', :'patrol_5_id', '2024-06-16 15:25:00+03', -19.8654000, 47.0341000, 5.9, 3.3, 200, 79),
    (:'emp8_id',  :'patrol_2_id', '2024-06-16 10:38:00+03', -18.8798000, 47.5072000, 4.4, 3.9, 270, 88);

-- ---------------------------------------------------------------------
-- 2.9 Incidents
-- ---------------------------------------------------------------------

INSERT INTO incident_type (company_id, name, description) VALUES
    (:'company_id', 'Vol ou tentative de vol', 'Vol constaté ou tentative sur un site'),
    (:'company_id', 'Alarme incendie',         'Déclenchement d''une alarme incendie'),
    (:'company_id', 'Trouble à l''ordre public','Bagarre ou comportement agressif'),
    (:'company_id', 'Dégradation de matériel', 'Dégradation de biens du client');

SELECT
    (SELECT id FROM incident_type WHERE name = 'Vol ou tentative de vol'   AND company_id = :'company_id') AS it_theft_id,
    (SELECT id FROM incident_type WHERE name = 'Alarme incendie'           AND company_id = :'company_id') AS it_fire_id,
    (SELECT id FROM incident_type WHERE name = 'Trouble à l''ordre public' AND company_id = :'company_id') AS it_disorder_id,
    (SELECT id FROM incident_type WHERE name = 'Dégradation de matériel'   AND company_id = :'company_id') AS it_damage_id
\gset

INSERT INTO incident (
    company_id, site_id, post_id, incident_type_id, reported_by_employee_id, title, description,
    severity, status, occurred_at, reported_at, latitude, longitude
) VALUES
    (:'company_id', :'site4_id', :'post6_id', :'it_theft_id', :'emp10_id',
     'Vol à l''étalage',
     'Un client a été surpris en train de dissimuler des produits. Remise à la direction du magasin.',
     'LOW', 'RESOLVED', '2024-06-16 16:20:00+03', '2024-06-16 16:25:00+03', -19.8650000, 47.0340000),
    (:'company_id', :'site3_id', :'post5_id', :'it_fire_id', :'emp7_id',
     'Alarme incendie en cuisine',
     'Déclenchement de l''alarme dans la cuisine. Évacuation partielle, fausse alerte due à la fumée.',
     'HIGH', 'RESOLVED', '2024-06-16 19:10:00+03', '2024-06-16 19:12:00+03', -18.1501000, 49.4003000),
    (:'company_id', :'site2_id', :'post2_id', :'it_disorder_id', :'emp4_id',
     'Client agressif au guichet',
     'Un client a haussé le ton et refusé de quitter le hall. Calmé puis raccompagné à l''extérieur.',
     'MEDIUM', 'RESOLVED', '2024-06-17 10:30:00+03', '2024-06-17 10:35:00+03', -18.9100000, 47.5255000),
    (:'company_id', :'site_id',  :'post_id',  :'it_damage_id', :'emp8_id',
     'Barrière du parking endommagée',
     'La barrière du parking a été percutée par un camion de livraison. Constat établi.',
     'MEDIUM', 'OPEN', '2024-06-17 09:15:00+03', '2024-06-17 09:20:00+03', -18.8795000, 47.5082000);

SELECT
    (SELECT id FROM incident WHERE title = 'Vol à l''étalage')                AS inc2_id,
    (SELECT id FROM incident WHERE title = 'Alarme incendie en cuisine')      AS inc3_id,
    (SELECT id FROM incident WHERE title = 'Client agressif au guichet')      AS inc4_id,
    (SELECT id FROM incident WHERE title = 'Barrière du parking endommagée')  AS inc5_id
\gset

INSERT INTO incident_attachment (incident_id, file_name, file_url, mime_type) VALUES
    (:'inc2_id', 'video-etalage.mp4',      'https://example.test/incidents/video-etalage.mp4',      'video/mp4'),
    (:'inc3_id', 'photo-cuisine.jpg',      'https://example.test/incidents/photo-cuisine.jpg',       'image/jpeg'),
    (:'inc3_id', 'rapport-alarme.pdf',     'https://example.test/incidents/rapport-alarme.pdf',      'application/pdf'),
    (:'inc5_id', 'photo-barriere.jpg',     'https://example.test/incidents/photo-barriere.jpg',      'image/jpeg'),
    (:'inc5_id', 'constat-livraison.pdf',  'https://example.test/incidents/constat-livraison.pdf',   'application/pdf');

INSERT INTO incident_action (incident_id, employee_id, action, action_at) VALUES
    (:'inc2_id', :'emp10_id', 'Interpellation du client et remise à la direction du magasin', '2024-06-16 16:30:00+03'),
    (:'inc2_id', :'emp9_id',  'Rédaction du rapport et archivage de la vidéo',               '2024-06-16 17:15:00+03'),
    (:'inc3_id', :'emp7_id',  'Évacuation de la cuisine et appel des pompiers',              '2024-06-16 19:13:00+03'),
    (:'inc3_id', :'emp7_id',  'Levée de doute et réarmement du système avec le technicien',  '2024-06-16 19:50:00+03'),
    (:'inc4_id', :'emp4_id',  'Dialogue avec le client et raccompagnement à la sortie',      '2024-06-17 10:40:00+03'),
    (:'inc4_id', :'emp3_id',  'Information de la directrice d''agence',                      '2024-06-17 11:10:00+03'),
    (:'inc5_id', :'emp8_id',  'Balisage de la zone et constat avec le chauffeur',            '2024-06-17 09:30:00+03');

-- ---------------------------------------------------------------------
-- 2.10 Inventaire, entrepôts, mouvements, affectations de matériel
-- ---------------------------------------------------------------------

INSERT INTO item_category (company_id, name, description) VALUES
    (:'company_id', 'Éclairage',     'Lampes et torches'),
    (:'company_id', 'Protection',    'Gilets et tenues de protection'),
    (:'company_id', 'Communication', 'Téléphones et accessoires');

SELECT
    (SELECT id FROM item_category WHERE name = 'Éclairage'     AND company_id = :'company_id') AS cat_light_id,
    (SELECT id FROM item_category WHERE name = 'Protection'    AND company_id = :'company_id') AS cat_protect_id,
    (SELECT id FROM item_category WHERE name = 'Communication' AND company_id = :'company_id') AS cat_comm_id
\gset

INSERT INTO item_type (
    company_id, category_id, code, name, description, unit, quantity_total, minimum_stock, status
) VALUES
    (:'company_id', :'cat_light_id',   'TORCH', 'Lampe torche',        'Lampe torche rechargeable',        'piece', 25,  5, 'AVAILABLE'),
    (:'company_id', :'cat_protect_id', 'VEST',  'Gilet de protection', 'Gilet pare-coups',                  'piece', 15,  3, 'AVAILABLE'),
    (:'company_id', :'cat_protect_id', 'UNIF',  'Tenue complète',      'Uniforme complet agent',            'piece', 40, 10, 'AVAILABLE'),
    (:'company_id', :'cat_comm_id',    'PHONE', 'Téléphone pro',       'Téléphone professionnel',           'piece', 12,  2, 'AVAILABLE');

SELECT
    (SELECT id FROM item_type WHERE code = 'TORCH') AS it_torch_id,
    (SELECT id FROM item_type WHERE code = 'VEST')  AS it_vest_id,
    (SELECT id FROM item_type WHERE code = 'UNIF')  AS it_unif_id,
    (SELECT id FROM item_type WHERE code = 'PHONE') AS it_phone_id
\gset

INSERT INTO item (company_id, category_id, item_type_id, qr_code, description, status) VALUES
    (:'company_id', :'item_category_id', :'item_type_id', 'QR-ITEM-002', 'Radio affectée à l''agent du portail', 'ASSIGNED'),
    (:'company_id', :'item_category_id', :'item_type_id', 'QR-ITEM-003', 'Radio de réserve',                     'AVAILABLE'),
    (:'company_id', :'cat_light_id',     :'it_torch_id',  'QR-ITEM-004', 'Lampe torche poste principal',         'ASSIGNED'),
    (:'company_id', :'cat_light_id',     :'it_torch_id',  'QR-ITEM-005', 'Lampe torche accueil banque',          'ASSIGNED'),
    (:'company_id', :'cat_protect_id',   :'it_vest_id',   'QR-ITEM-006', 'Gilet de protection en réserve',       'AVAILABLE'),
    (:'company_id', :'cat_comm_id',      :'it_phone_id',  'QR-ITEM-007', 'Téléphone du superviseur',             'ASSIGNED');

SELECT
    (SELECT id FROM item WHERE qr_code = 'QR-ITEM-002') AS item2_id,
    (SELECT id FROM item WHERE qr_code = 'QR-ITEM-003') AS item3_id,
    (SELECT id FROM item WHERE qr_code = 'QR-ITEM-004') AS item4_id,
    (SELECT id FROM item WHERE qr_code = 'QR-ITEM-005') AS item5_id,
    (SELECT id FROM item WHERE qr_code = 'QR-ITEM-006') AS item6_id,
    (SELECT id FROM item WHERE qr_code = 'QR-ITEM-007') AS item7_id
\gset

INSERT INTO warehouse (company_id, branch_id, name, address, active) VALUES
    (:'company_id', :'branch_toa_id', 'Entrepôt Toamasina', 'Tanambao V', TRUE),
    (:'company_id', :'branch_ant_id', 'Entrepôt Antsirabe', 'Ambohimena', TRUE);

SELECT
    (SELECT id FROM warehouse WHERE name = 'Entrepôt Toamasina') AS warehouse_toa_id,
    (SELECT id FROM warehouse WHERE name = 'Entrepôt Antsirabe') AS warehouse_ant_id
\gset

INSERT INTO stock_movement (warehouse_id, item_id, employee_id, movement_type, reference, movement_date, notes) VALUES
    (:'warehouse_id',     :'item2_id', :'employee_secondary_id', 'OUT', 'MVT-002', '2024-06-15 05:35:00+03', 'Sortie radio pour le portail'),
    (:'warehouse_id',     :'item4_id', :'emp8_id',               'OUT', 'MVT-003', '2024-06-15 05:40:00+03', 'Sortie lampe torche'),
    (:'warehouse_id',     :'item5_id', :'emp4_id',               'OUT', 'MVT-004', '2024-06-15 05:50:00+03', 'Sortie lampe torche banque'),
    (:'warehouse_id',     :'item7_id', :'emp3_id',               'OUT', 'MVT-005', '2024-06-15 06:00:00+03', 'Sortie téléphone superviseur'),
    (:'warehouse_id',     :'item3_id', :'employee_primary_id',   'IN',  'MVT-006', '2024-06-10 09:00:00+03', 'Réception de la commande de radios'),
    (:'warehouse_toa_id', :'item6_id', :'emp6_id',               'IN',  'MVT-007', '2024-06-12 10:30:00+03', 'Réception de gilets à Toamasina'),
    (:'warehouse_ant_id', :'item6_id', :'emp9_id',               'IN',  'MVT-008', '2024-06-12 11:00:00+03', 'Réception de gilets à Antsirabe');

INSERT INTO item_assignment (item_id, employee_id, post_id, assigned_at, condition_on_assignment, notes) VALUES
    (:'item2_id', :'employee_secondary_id',
     (SELECT id FROM security_post WHERE code = 'POST-001' LIMIT 1),
     '2024-06-15 05:50:00+03', 'Bon état', 'Radio du portail principal'),
    (:'item4_id', :'emp8_id',
     (SELECT id FROM security_post WHERE code = 'POST-001' LIMIT 1),
     '2024-06-15 05:55:00+03', 'Bon état', 'Lampe torche poste principal'),
    (:'item5_id', :'emp4_id',               :'post2_id', '2024-06-15 06:00:00+03', 'Neuf',         'Lampe torche accueil banque'),
    (:'item7_id', :'emp3_id',               :'post7_id', '2024-06-15 06:05:00+03', 'Bon état',     'Téléphone de supervision');

-- ---------------------------------------------------------------------
-- 2.11 Taxes, factures, lignes, paiements, allocations
-- ---------------------------------------------------------------------

INSERT INTO tax_rate (company_id, name, rate, active) VALUES
    (:'company_id', 'TVA réduite', 5, TRUE),
    (:'company_id', 'Exonéré',     0, TRUE);

INSERT INTO invoice (
    company_id, client_id, contract_id, invoice_number, issue_date, due_date, status, currency_code,
    subtotal, discount_amount, tax_amount, total_amount, amount_paid, notes
) VALUES
    (:'company_id', :'client_id',  :'contract_id',  'FAC-2024-002', '2024-07-01', '2024-07-31', 'ISSUED', 'MGA',
     2500000, 0, 500000, 3000000, 0,       'Facture de juillet 2024'),
    (:'company_id', :'client2_id', :'contract2_id', 'FAC-2024-003', '2024-06-01', '2024-06-16', 'PAID',   'MGA',
     3200000, 0, 640000, 3840000, 3840000, 'Facture de juin 2024 - Banque Océan'),
    (:'company_id', :'client3_id', :'contract3_id', 'FAC-2024-004', '2024-06-01', '2024-07-01', 'PAID',   'MGA',
     2500000, 0, 500000, 3000000, 3000000, 'Facture de juin 2024 - Hôtel Le Baobab'),
    (:'company_id', :'client4_id', :'contract4_id', 'FAC-2024-005', '2024-06-01', '2024-07-16', 'ISSUED', 'MGA',
     2150000, 0, 430000, 2580000, 0,       'Facture de juin 2024 - Mada Market'),
    (:'company_id', :'client_id',  :'contract5_id', 'FAC-2024-006', '2024-07-01', '2024-07-31', 'ISSUED', 'MGA',
     1400000, 0, 280000, 1680000, 0,       'Facture de juillet 2024 - Entrepôt');

SELECT
    (SELECT id FROM invoice WHERE invoice_number = 'FAC-2024-002') AS invoice2_id,
    (SELECT id FROM invoice WHERE invoice_number = 'FAC-2024-003') AS invoice3_id,
    (SELECT id FROM invoice WHERE invoice_number = 'FAC-2024-004') AS invoice4_id,
    (SELECT id FROM invoice WHERE invoice_number = 'FAC-2024-005') AS invoice5_id,
    (SELECT id FROM invoice WHERE invoice_number = 'FAC-2024-006') AS invoice6_id
\gset

INSERT INTO invoice_line (
    invoice_id, service_id, description, quantity, unit_price, discount_rate, tax_rate,
    line_subtotal, line_tax, line_total
) VALUES
    (:'invoice2_id', :'service_id',        'Sécurité 24 heures - juillet 2024',            1, 2500000, 0, 20, 2500000, 500000, 3000000),
    (:'invoice3_id', :'service_acces_id',  'Contrôle d''accès - juin 2024',                1, 1800000, 0, 20, 1800000, 360000, 2160000),
    (:'invoice3_id', :'service_12h_id',    'Sécurité 12 heures - juin 2024',               1, 1400000, 0, 20, 1400000, 280000, 1680000),
    (:'invoice4_id', :'service_id',        'Sécurité 24 heures - juin 2024',               1, 2500000, 0, 20, 2500000, 500000, 3000000),
    (:'invoice5_id', :'service_12h_id',    'Sécurité 12 heures - juin 2024',               1, 1400000, 0, 20, 1400000, 280000, 1680000),
    (:'invoice5_id', :'service_ronde_id',  'Rondes de surveillance - juin 2024 (30 rondes)',30,  25000, 0, 20,  750000, 150000,  900000),
    (:'invoice6_id', :'service_12h_id',    'Sécurité 12 heures - juillet 2024',            1, 1400000, 0, 20, 1400000, 280000, 1680000);

INSERT INTO payment (
    invoice_id, payment_reference, payment_date, amount, method, status, transaction_reference, notes
) VALUES
    (:'invoice3_id', 'PAY-2024-002', '2024-06-12', 3840000, 'BANK_TRANSFER', 'CONFIRMED', 'TRX-DEMO-002', 'Paiement intégral Banque Océan'),
    (:'invoice4_id', 'PAY-2024-003', '2024-06-20', 3000000, 'MOBILE_MONEY',  'CONFIRMED', 'TRX-DEMO-003', 'Paiement intégral Hôtel Le Baobab');

SELECT
    (SELECT id FROM payment WHERE payment_reference = 'PAY-2024-002') AS payment2_id,
    (SELECT id FROM payment WHERE payment_reference = 'PAY-2024-003') AS payment3_id
\gset

INSERT INTO payment_allocation (payment_id, invoice_id, amount) VALUES
    (:'payment2_id', :'invoice3_id', 3840000),
    (:'payment3_id', :'invoice4_id', 3000000);

-- ---------------------------------------------------------------------
-- 2.12 Documents
-- ---------------------------------------------------------------------

INSERT INTO document (
    company_id, client_id, employee_id, contract_id, invoice_id, document_type, title, file_name, file_url, mime_type
) VALUES
    (:'company_id', :'client2_id', NULL,       :'contract2_id', NULL,            'CONTRACT', 'Contrat Banque Océan',
     'contrat-2024-002.pdf', 'https://example.test/documents/contrat-2024-002.pdf', 'application/pdf'),
    (:'company_id', :'client3_id', NULL,       :'contract3_id', NULL,            'CONTRACT', 'Contrat Hôtel Le Baobab',
     'contrat-2024-003.pdf', 'https://example.test/documents/contrat-2024-003.pdf', 'application/pdf'),
    (:'company_id', :'client4_id', NULL,       :'contract4_id', NULL,            'CONTRACT', 'Contrat Mada Market',
     'contrat-2024-004.pdf', 'https://example.test/documents/contrat-2024-004.pdf', 'application/pdf'),
    (:'company_id', :'client2_id', NULL,       :'contract2_id', :'invoice3_id',  'INVOICE',  'Facture FAC-2024-003',
     'fac-2024-003.pdf',     'https://example.test/documents/fac-2024-003.pdf',     'application/pdf'),
    (:'company_id', :'client3_id', NULL,       :'contract3_id', :'invoice4_id',  'INVOICE',  'Facture FAC-2024-004',
     'fac-2024-004.pdf',     'https://example.test/documents/fac-2024-004.pdf',     'application/pdf'),
    (:'company_id', NULL,          :'emp4_id', NULL,            NULL,            'CONTRACT', 'Contrat de travail Tahina Razafindrabe',
     'cdi-emp-0004.pdf',     'https://example.test/documents/cdi-emp-0004.pdf',     'application/pdf');

-- ---------------------------------------------------------------------
-- 2.13 Notifications, e-mails, journal d'audit
-- ---------------------------------------------------------------------

INSERT INTO notification (
    user_id, employee_id, channel, status, subject, message, scheduled_at, sent_at
) VALUES
    (:'user_agent1_id', :'emp4_id',  'IN_APP', 'SENT', 'Planning du 17 juin',
     'Vous êtes affecté à l''accueil de la banque le 17 juin à 06h00.',
     '2024-06-16 18:00:00+03', '2024-06-16 18:00:03+03'),
    (:'user_agent2_id', :'emp8_id',  'IN_APP', 'SENT', 'Incident à traiter',
     'Un incident concernant la barrière du parking vous a été assigné.',
     '2024-06-17 09:25:00+03', '2024-06-17 09:25:02+03'),
    (:'user_sup_id',    :'emp3_id',  'IN_APP', 'SENT', 'Incident signalé',
     'Un client agressif a été signalé à l''agence Banque Océan.',
     '2024-06-17 10:36:00+03', '2024-06-17 10:36:01+03'),
    (:'user_compta_id', :'emp5_id',  'EMAIL',  'SENT', 'Factures de juillet',
     'Les factures de juillet 2024 sont prêtes à être validées.',
     '2024-07-01 08:00:00+03', '2024-07-01 08:00:10+03'),
    (:'user_id',        :'employee_primary_id', 'IN_APP', 'SENT', 'Rapport journalier',
     'Le rapport journalier du 16 juin est disponible.',
     '2024-06-16 20:00:00+03', '2024-06-16 20:00:04+03');

INSERT INTO email_log (recipient_email, subject, body, sent_at, status) VALUES
    ('lalao.randriamanana@sssm.test',  'Bienvenue dans SSSM',        'Votre compte superviseur est prêt.',                          '2023-09-01 08:00:00+03', 'SENT'),
    ('voahangy.rakotomalala@sssm.test','Bienvenue dans SSSM',        'Votre compte comptable est prêt.',                            '2023-06-01 08:00:00+03', 'SENT'),
    ('contact@banque-ocean.test',      'Facture FAC-2024-003',       'Veuillez trouver ci-joint votre facture de juin 2024.',       '2024-06-01 09:00:00+03', 'SENT'),
    ('direction@hotel-baobab.test',    'Facture FAC-2024-004',       'Veuillez trouver ci-joint votre facture de juin 2024.',       '2024-06-01 09:05:00+03', 'SENT'),
    ('achats@mada-market.test',        'Facture FAC-2024-005',       'Veuillez trouver ci-joint votre facture de juin 2024.',       '2024-06-01 09:10:00+03', 'SENT'),
    ('contact@client-demo.test',       'Rapport d''incident',        'Un incident a été constaté sur votre entrepôt le 17 juin.',   '2024-06-17 09:40:00+03', 'SENT');

INSERT INTO audit_log (user_id, action, entity_name, entity_id, old_data, new_data, ip_address, user_agent) VALUES
    (:'user_id',        'CREATE', 'client',   :'client2_id',   NULL,
     '{"code": "CLI-002", "name": "Banque Océan"}'::jsonb,                        '127.0.0.1', 'SSSM mock data script'),
    (:'user_id',        'CREATE', 'contract', :'contract2_id', NULL,
     '{"contract_number": "CTR-2024-002", "status": "ACTIVE"}'::jsonb,            '127.0.0.1', 'SSSM mock data script'),
    (:'user_compta_id', 'CREATE', 'invoice',  :'invoice3_id',  NULL,
     '{"invoice_number": "FAC-2024-003", "total_amount": 3840000}'::jsonb,        '127.0.0.1', 'SSSM mock data script'),
    (:'user_compta_id', 'UPDATE', 'invoice',  :'invoice3_id',
     '{"status": "ISSUED", "amount_paid": 0}'::jsonb,
     '{"status": "PAID", "amount_paid": 3840000}'::jsonb,                         '127.0.0.1', 'SSSM mock data script'),
    (:'user_sup_id',    'UPDATE', 'incident', :'inc4_id',
     '{"status": "OPEN"}'::jsonb,
     '{"status": "RESOLVED"}'::jsonb,                                             '127.0.0.1', 'SSSM mock data script');

COMMIT;
