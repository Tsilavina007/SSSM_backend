-- 10. INCIDENTS : DONNÉES DE DÉMONSTRATION
-- ============================================================
-- Types d'incidents, incidents, pièces jointes et actions.
--
-- Variables gset exposées aux fichiers suivants :
--   :incident_type_id      Accès non autorisé
--   :incident_id           incident du 15 juin (RESOLVED)
--   :it_theft_id / :it_fire_id / :it_disorder_id / :it_damage_id
--   :inc2_id..:inc5_id
--   :inc6_id (CRITICAL / OPEN), :inc7_id (CLOSED)
-- ============================================================

SET search_path TO security, public;

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
    company_id, site_id, post_id, incident_type_id, reported_by_employee_id, assigned_to_employee_id, title, description,
    severity, status, occurred_at, reported_at, latitude, longitude
) VALUES
    (:'company_id', :'site4_id', :'post6_id', :'it_theft_id', :'emp10_id', NULL,
     'Vol à l''étalage',
     'Un client a été surpris en train de dissimuler des produits. Remise à la direction du magasin.',
     'LOW', 'RESOLVED', '2024-06-16 16:20:00+03', '2024-06-16 16:25:00+03', -19.8650000, 47.0340000),
    (:'company_id', :'site3_id', :'post5_id', :'it_fire_id', :'emp7_id', NULL,
     'Alarme incendie en cuisine',
     'Déclenchement de l''alarme dans la cuisine. Évacuation partielle, fausse alerte due à la fumée.',
     'HIGH', 'RESOLVED', '2024-06-16 19:10:00+03', '2024-06-16 19:12:00+03', -18.1501000, 49.4003000),
    (:'company_id', :'site2_id', :'post2_id', :'it_disorder_id', :'emp4_id', NULL,
     'Client agressif au guichet',
     'Un client a haussé le ton et refusé de quitter le hall. Calmé puis raccompagné à l''extérieur.',
     'MEDIUM', 'RESOLVED', '2024-06-17 10:30:00+03', '2024-06-17 10:35:00+03', -18.9100000, 47.5255000),
    (:'company_id', :'site_id',  :'post_id',  :'it_damage_id', :'emp8_id', :'emp8_id',
     'Barrière du parking endommagée',
     'La barrière du parking a été percutée par un camion de livraison. Constat établi.',
     'MEDIUM', 'IN_PROGRESS', '2024-06-17 09:15:00+03', '2024-06-17 09:20:00+03', -18.8795000, 47.5082000);

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

-- Incident critique en cours, assigné au superviseur.
INSERT INTO incident (
    company_id, site_id, post_id, incident_type_id, reported_by_employee_id, assigned_to_employee_id,
    title, description, severity, status, occurred_at, reported_at, latitude, longitude
) VALUES (
    :'company_id', :'site3_id', :'post5_id', :'it_theft_id', :'emp7_id', :'emp3_id',
    'Tentative d''effraction au parking',
    'Détection de mouvements et de forçage de porte dans la zone de parking en pleine nuit.',
    'CRITICAL', 'OPEN', '2024-06-17 23:40:00+03', '2024-06-17 23:45:00+03', -18.1502000, 49.4004000
) RETURNING id \gset inc6_

-- Incident clôturé avec résolution complète.
INSERT INTO incident (
    company_id, site_id, post_id, incident_type_id, reported_by_employee_id,
    title, description, severity, status, occurred_at, reported_at, resolved_at, latitude, longitude
) VALUES (
    :'company_id', :'site5_id', :'post7_id', :'it_disorder_id', :'employee_secondary_id',
    'Véhicule suspect au portail',
    'Un véhicule inconnu stationnait devant le portail de l''entrepôt pendant plus d''une heure. Départ constaté après contrôle.',
    'MEDIUM', 'CLOSED', '2024-06-14 08:10:00+03', '2024-06-14 08:12:00+03', '2024-06-14 10:00:00+03', -18.8810000, 47.5100000
) RETURNING id \gset inc7_

INSERT INTO incident_attachment (incident_id, file_name, file_url, mime_type) VALUES
    (:'inc6_id', 'photo-parking.jpg',      'https://example.test/incidents/photo-parking.jpg',       'image/jpeg'),
    (:'inc7_id', 'photo-vehicule.jpg',     'https://example.test/incidents/photo-vehicule.jpg',      'image/jpeg');

INSERT INTO incident_action (incident_id, employee_id, action, action_at) VALUES
    (:'inc6_id', :'emp7_id', 'Balisage de la zone et information du superviseur',            '2024-06-17 23:50:00+03'),
    (:'inc6_id', :'emp3_id', 'Coordination avec la police nationale et renforcement de nuit','2024-06-18 00:30:00+03'),
    (:'inc7_id', :'employee_secondary_id', 'Vérification véhicule et information du gardien','2024-06-14 08:30:00+03'),
    (:'inc7_id', :'employee_primary_id',   'Clôture de l''incident après retour à la normale','2024-06-14 10:00:00+03');
