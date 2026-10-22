-- 7. CONTRATS ET PRESTATIONS : DONNÉES DE DÉMONSTRATION
-- ============================================================
--
-- Variables gset exposées aux fichiers suivants :
--   :service_id            SEC-24H
--   :service_12h_id, :service_acces_id, :service_ronde_id
--   :service_escort_id     ESCORT (inactif, nouveau)
--   :contract_id           CTR-2024-001
--   :contract2_id..:contract5_id
--   :contract6_id (DRAFT), :contract7_id (EXPIRED)
-- ============================================================

SET search_path TO security, public;

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

INSERT INTO service (company_id, code, name, description, unit, unit_price, vat_rate, active) VALUES
    (:'company_id', 'SEC-12H',     'Sécurité 12 heures',   'Surveillance de jour ou de nuit',             'month', 1400000, 20, TRUE),
    (:'company_id', 'CTRL-ACCES',  'Contrôle d''accès',    'Gestion des entrées, badges et registres',    'month', 1800000, 20, TRUE),
    (:'company_id', 'RONDE',       'Ronde de surveillance','Ronde avec pointage de checkpoints',          'visit',   25000, 20, TRUE),
    (:'company_id', 'INTERVENTION','Intervention urgente', 'Intervention d''une équipe mobile',            'hour',    60000, 20, TRUE),
    (:'company_id', 'ESCORT',      'Escort de convoi',     'Escorte motorisée de convois sensibles',       'trip',   220000, 20, FALSE);

SELECT
    (SELECT id FROM service WHERE code = 'SEC-12H')     AS service_12h_id,
    (SELECT id FROM service WHERE code = 'CTRL-ACCES')  AS service_acces_id,
    (SELECT id FROM service WHERE code = 'RONDE')       AS service_ronde_id,
    (SELECT id FROM service WHERE code = 'ESCORT')      AS service_escort_id
\gset

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

-- Contrats aux statuts représentatifs.
INSERT INTO contract (
    company_id, client_id, contract_number, title, start_date, end_date, status,
    billing_frequency, currency_code, payment_terms_days, total_amount, notes
) VALUES
    (:'company_id', :'client5_id', 'CTR-2024-006', 'Escort de convois Garage Milasoa',
     '2024-08-01', NULL, 'DRAFT', 'WEEKLY', 'MGA', 30, 2640000,
     'Contrat en préparation - prestataire Escort inactif à activer'),
    (:'company_id', :'client_id',  'CTR-2024-007', 'Surveillance ponctuelle du site démo 2023',
     '2023-06-01', '2023-12-31', 'EXPIRED', 'MONTHLY', 'MGA', 30, 4200000,
     'Contrat historique arrivé à échéance non renouvelé');

SELECT
    (SELECT id FROM contract WHERE contract_number = 'CTR-2024-006') AS contract6_id,
    (SELECT id FROM contract WHERE contract_number = 'CTR-2024-007') AS contract7_id
\gset

INSERT INTO contract_service (
    contract_id, service_id, site_id, quantity, unit_price, vat_rate, start_date, end_date, notes
) VALUES
    (:'contract6_id', :'service_escort_id', NULL,       12, 220000, 20, '2024-08-01', NULL,          'Escortes hebdomadaires - à contractualiser'),
    (:'contract7_id', :'service_12h_id',    :'site_id',  3, 1400000, 20, '2023-06-01', '2023-12-31', 'Prestation ponctuelle de jour');
