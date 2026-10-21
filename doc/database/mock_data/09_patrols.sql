-- 9. PATROUILLES : DONNÉES DE DÉMONSTRATION
-- ============================================================
-- Routes de ronde, checkpoints, patrouilles et suivi GPS.
--
-- Variables gset exposées aux fichiers suivants :
--   :patrol_route_id        Ronde périmétrique (SITE-001)
--   :checkpoint_id          CP-001
--   :patrol_id              patrouille du 15 juin (COMPLETED)
--   :patrol_route_hotel_id / :patrol_route_bank_id / :patrol_route_market_id
--   :patrol_route_depot_id  Ronde entrepôt (SITE-005, nouveau)
--   :cp2_id..:cp13_id
--   :patrol_2_id..:patrol_8_id
-- ============================================================

SET search_path TO security, public;

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

INSERT INTO patrol_route (site_id, name, description, estimated_duration_minutes, active) VALUES (
    :'site3_id', 'Ronde hôtel', 'Entrée, piscine et plage de l''hôtel', 40, TRUE
) RETURNING id \gset patrol_route_hotel_

INSERT INTO patrol_route (site_id, name, description, estimated_duration_minutes, active) VALUES (
    :'site2_id', 'Ronde banque', 'Hall d''accueil et salle des coffres', 30, TRUE
) RETURNING id \gset patrol_route_bank_

INSERT INTO patrol_route (site_id, name, description, estimated_duration_minutes, active) VALUES (
    :'site4_id', 'Ronde magasin', 'Entrée clients, réserve et parking', 35, TRUE
) RETURNING id \gset patrol_route_market_

INSERT INTO patrol_route (site_id, name, description, estimated_duration_minutes, active) VALUES (
    :'site5_id', 'Ronde entrepôt', 'Portail, quai de déchargement et clôture', 25, TRUE
) RETURNING id \gset patrol_route_depot_

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
    (:'patrol_route_market_id', 'Parking',            'CP-011', 3, -19.8656000, 47.0338000, 40, 'QR-CP-011'),
    (:'patrol_route_depot_id',  'Portail entrepôt',   'CP-012', 1, -18.8811000, 47.5102000, 30, 'QR-CP-012'),
    (:'patrol_route_depot_id',  'Quai de déchargement','CP-013',2, -18.8814000, 47.5105000, 40, 'QR-CP-013');

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
    (SELECT id FROM checkpoint WHERE code = 'CP-011') AS cp11_id,
    (SELECT id FROM checkpoint WHERE code = 'CP-012') AS cp12_id,
    (SELECT id FROM checkpoint WHERE code = 'CP-013') AS cp13_id
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

-- Patrouille en cours : pas de fin, un seul checkpoint scanné.
INSERT INTO patrol (patrol_route_id, employee_id, site_id, scheduled_at, started_at, ended_at, status, notes) VALUES (
    :'patrol_route_depot_id', :'emp8_id', :'site5_id',
    '2024-06-17 20:00:00+03', '2024-06-17 20:02:00+03', NULL,
    'IN_PROGRESS', 'Ronde de nuit entrepôt en cours'
) RETURNING id \gset patrol_6_

-- Patrouille échouée : orage, un seul checkpoint atteint.
INSERT INTO patrol (patrol_route_id, employee_id, site_id, scheduled_at, started_at, ended_at, status, notes) VALUES (
    :'patrol_route_bank_id', :'emp4_id', :'site2_id',
    '2024-06-17 21:00:00+03', '2024-06-17 21:00:00+03', '2024-06-17 21:25:00+03',
    'FAILED', 'Ronde interrompue par un orage violent'
) RETURNING id \gset patrol_7_

-- Patrouille planifiée : pas encore commencée.
INSERT INTO patrol (patrol_route_id, employee_id, site_id, scheduled_at, started_at, ended_at, status, notes) VALUES (
    :'patrol_route_market_id', :'emp10_id', :'site4_id',
    '2024-06-18 15:00:00+03', NULL, NULL,
    'PLANNED', 'Ronde d''après-midi planifiée'
) RETURNING id \gset patrol_8_

INSERT INTO patrol_checkpoint (patrol_id, checkpoint_id, scanned_at, latitude, longitude, distance_meters, status, notes) VALUES
    (:'patrol_2_id', :'checkpoint_id', '2024-06-16 10:08:00+03', -18.8789100, 47.5076100,  4.1, 'SCANNED', NULL),
    (:'patrol_2_id', :'cp2_id',        '2024-06-16 10:22:00+03', -18.8795100, 47.5082200,  3.0, 'SCANNED', NULL),
    (:'patrol_2_id', :'cp3_id',        '2024-06-16 10:36:00+03', -18.8799200, 47.5071100,  5.6, 'SCANNED', NULL),
    (:'patrol_3_id', :'cp4_id',        '2024-06-16 09:08:00+03', -18.1499100, 49.4001100,  2.8, 'SCANNED', NULL),
    (:'patrol_3_id', :'cp5_id',        '2024-06-16 09:21:00+03', -18.1503100, 49.4006200,  3.9, 'SCANNED', NULL),
    (:'patrol_3_id', :'cp6_id',        '2024-06-16 09:36:00+03', -18.1508200, 49.4012100,  6.2, 'SCANNED', NULL),
    (:'patrol_4_id', :'cp7_id',        '2024-06-16 11:08:00+03', -18.9100100, 47.5255100,  1.9, 'SCANNED', NULL),
    (:'patrol_4_id', :'cp8_id',        '2024-06-16 11:22:00+03', -18.9102100, 47.5257100,  2.4, 'SCANNED', NULL),
    (:'patrol_5_id', :'cp9_id',        '2024-06-16 15:08:00+03', -19.8650100, 47.0340100,  3.5, 'SCANNED', NULL),
    (:'patrol_5_id', :'cp10_id',       '2024-06-16 15:20:00+03', -19.8653100, 47.0344200,  4.8, 'SCANNED', NULL),
    (:'patrol_5_id', :'cp11_id',       '2024-06-16 15:32:00+03', -19.8656100, 47.0338100,  5.0, 'SCANNED', NULL),
    (:'patrol_6_id', :'cp12_id',       '2024-06-17 20:10:00+03', -18.8811200, 47.5102400,  2.6, 'SCANNED', 'Portail sécurisé au moment du passage'),
    (:'patrol_7_id', :'cp7_id',        '2024-06-17 21:08:00+03', -18.9100100, 47.5255100,  1.7, 'SCANNED', NULL);

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
    (:'emp8_id',  :'patrol_2_id', '2024-06-16 10:38:00+03', -18.8798000, 47.5072000, 4.4, 3.9, 270, 88),
    (:'emp8_id',  :'patrol_6_id', '2024-06-17 20:12:00+03', -18.8811500, 47.5103000, 4.1, 2.8,  60, 70),
    (:'emp8_id',  :'patrol_6_id', '2024-06-17 20:20:00+03', -18.8813200, 47.5104800, 4.6, 3.1, 150, 68);
