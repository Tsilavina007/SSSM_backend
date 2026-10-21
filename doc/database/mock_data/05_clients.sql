-- 5. CLIENTS : DONNÉES DE DÉMONSTRATION
-- ============================================================
-- Clients et contacts.
--
-- Variables gset exposées aux fichiers suivants :
--   :client_id            CLI-001 Client Démo
--   :client2_id..:client4_id
--   :client5_id           CLI-005 Garage Milasoa (nouveau)
-- ============================================================

SET search_path TO security, public;

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
     'Route d''Ambositra', 'Antsirabe', 'Madagascar', 45, TRUE),
    (:'company_id', :'agency_toa_id', 'CLI-005', 'Garage Milasoa',   'GARAGE MILASOA SARLU',       'Entreprise',
     'RC-CLI-005', 'NIF-CLI-005', 'contact@garage-milasoa.test', '+261 34 00 000 98',
     'Rue Nationale 5', 'Toamasina', 'Madagascar', 30, TRUE);

SELECT
    (SELECT id FROM client WHERE code = 'CLI-002') AS client2_id,
    (SELECT id FROM client WHERE code = 'CLI-003') AS client3_id,
    (SELECT id FROM client WHERE code = 'CLI-004') AS client4_id,
    (SELECT id FROM client WHERE code = 'CLI-005') AS client5_id
\gset

INSERT INTO client_contact (client_id, first_name, last_name, function, email, phone, is_primary) VALUES
    (:'client_id',  'Tojo',   'Rabary',        'Directeur des opérations', 'tojo@client-demo.test',      '+261 34 00 000 92', FALSE),
    (:'client2_id', 'Nirina', 'Rakotondrabe',  'Responsable sécurité',     'nirina@banque-ocean.test',   '+261 34 00 000 93', TRUE),
    (:'client2_id', 'Aina',   'Razanamahefa',  'Directrice d''agence',     'aina@banque-ocean.test',     '+261 34 00 000 94', FALSE),
    (:'client3_id', 'Sarobidy','Andrianjafy',  'Directeur général',        'sarobidy@hotel-baobab.test', '+261 34 00 000 95', TRUE),
    (:'client3_id', 'Lova',   'Ramanantsoa',   'Chef de réception',        'lova@hotel-baobab.test',     '+261 34 00 000 96', FALSE),
    (:'client4_id', 'Faly',   'Rasamoely',     'Responsable magasin',      'faly@mada-market.test',      '+261 34 00 000 97', TRUE),
    (:'client5_id', 'Rakoto', 'Milasoa',       'Gérant',                   'rakoto@garage-milasoa.test', '+261 34 00 000 99', TRUE);
