-- 2. ORGANISATION : DONNÉES DE DÉMONSTRATION
-- ============================================================
-- Entreprise unique (mono-entreprise), agences et branches.
--
-- Variables gset exposées aux fichiers suivants :
--   :company_id           SSSM Madagascar
--   :agency_id            Agence Centrale (Antananarivo)
--   :branch_id            Branche TANA
--   :agency_toa_id        Agence Toamasina
--   :branch_toa_id        Branche TOAM
--   :agency_ant_id        Agence Antsirabe
--   :branch_ant_id        Branche ANTS
-- ============================================================

SET search_path TO security, public;

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
