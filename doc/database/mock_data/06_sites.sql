-- 6. SITES / POSTES : DONNÉES DE DÉMONSTRATION
-- ============================================================
-- Sites clients, postes de sécurité et exigences matérielles.
--
-- Variables gset exposées aux fichiers suivants :
--   :site_id    SITE-001   :post_id   POST-001
--   :site2_id..:site5_id, :post2_id..:post7_id
--   :site6_id   SITE-006 (inactif), :post8_id POST-008 (inactif)
-- ============================================================

SET search_path TO security, public;

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

-- Site inactif (gardiennage arrêté) pour éclairer l'historique.
INSERT INTO site (
    company_id, client_id, branch_id, name, code, address, city,
    latitude, longitude, status, access_instructions, risk_level
) VALUES (
    :'company_id', :'client4_id', :'branch_ant_id', 'Dépôt Andraharo', 'SITE-006',
    'Andraharo', 'Antsirabe', -19.8640000, 47.0320000, 'INACTIVE',
    'Site fermé depuis mars 2024 - gardiennage arrêté', 'LOW'
) RETURNING id \gset site6_

INSERT INTO security_post (site_id, name, code, description, required_agents, start_time, end_time, status) VALUES
    (:'site2_id', 'Accueil banque',      'POST-002', 'Contrôle des entrées et du hall',         2, '06:00', '18:00', 'ACTIVE'),
    (:'site2_id', 'Distributeurs',       'POST-003', 'Surveillance des distributeurs',          1, '06:00', '18:00', 'ACTIVE'),
    (:'site3_id', 'Entrée hôtel',        'POST-004', 'Accueil et contrôle de l''entrée',        1, '06:00', '14:00', 'ACTIVE'),
    (:'site3_id', 'Jardin et piscine',   'POST-005', 'Surveillance du jardin et de la piscine', 1, '14:00', '22:00', 'ACTIVE'),
    (:'site4_id', 'Caisses et entrée',   'POST-006', 'Contrôle clientèle et caisses',           2, '06:00', '18:00', 'ACTIVE'),
    (:'site5_id', 'Portail entrepôt',    'POST-007', 'Contrôle des camions et visiteurs',       1, '06:00', '18:00', 'ACTIVE'),
    (:'site6_id', 'Poste dormant dépôt', 'POST-008', 'Ancien poste dormant du dépôt Andraharo', 1, '06:00', '18:00', 'INACTIVE');

SELECT
    (SELECT id FROM security_post WHERE code = 'POST-002') AS post2_id,
    (SELECT id FROM security_post WHERE code = 'POST-003') AS post3_id,
    (SELECT id FROM security_post WHERE code = 'POST-004') AS post4_id,
    (SELECT id FROM security_post WHERE code = 'POST-005') AS post5_id,
    (SELECT id FROM security_post WHERE code = 'POST-006') AS post6_id,
    (SELECT id FROM security_post WHERE code = 'POST-007') AS post7_id,
    (SELECT id FROM security_post WHERE code = 'POST-008') AS post8_id
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
    (:'post7_id', 'Registre des visiteurs'),
    (:'post8_id', 'Registre des visiteurs');
