-- 11. INVENTAIRE : DONNÉES DE DÉMONSTRATION
-- ============================================================
-- Catégories, types, matériels à QR code, entrepôts, opérations,
-- mouvements de stock, seuils, inventaires physiques, affectations.
--
-- Les colonnes company_id (ajoutées par 18_lot1_security.sql) sont
-- renseignées directement sur stock_movement, item_assignment et
-- inventory_count_scan.
--
-- Variables gset exposées aux fichiers suivants :
--   :item_category_id         Équipements de sécurité
--   :item_type_id             RADIO
--   :item_id                  QR-ITEM-001
--   :warehouse_id             Entrepôt principal
--   :warehouse_toa_id / :warehouse_ant_id
--   :inventory_operation_receipt_id  (réception)
--   :inventory_operation_issue_id    (sortie - nouveau)
--   :inventory_count_id       INV-2024-06-001 (complétée)
--   :inventory_count2_id      INV-2024-07-001 (en cours)
--   :item2_id..:item13_id, :it_torch_id..:it_phone_id, :it_cctv_id
-- ============================================================

SET search_path TO security, public;

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
    company_id,
    warehouse_id,
    item_id,
    employee_id,
    movement_type,
    reference,
    movement_date,
    notes
) VALUES (
    :'company_id',
    :'warehouse_id',
    :'item_id',
    :'employee_primary_id',
    'OUT',
    'MVT-001',
    '2024-06-15 05:30:00+03',
    'Sortie pour affectation'
);

INSERT INTO item_assignment (
    company_id,
    item_id,
    employee_id,
    post_id,
    assigned_at,
    condition_on_assignment,
    notes
) VALUES (
    :'company_id',
    :'item_id',
    :'employee_primary_id',
    (SELECT id FROM security_post WHERE code = 'POST-001' LIMIT 1),
    '2024-06-15 05:45:00+03',
    'Bon état',
    'Affectation permanente pendant le service'
);

INSERT INTO item_category (company_id, name, description) VALUES
    (:'company_id', 'Éclairage',          'Lampes et torches'),
    (:'company_id', 'Protection',         'Gilets et tenues de protection'),
    (:'company_id', 'Communication',      'Téléphones et accessoires'),
    (:'company_id', 'Surveillance vidéo', 'Caméras et équipements vidéo');

SELECT
    (SELECT id FROM item_category WHERE name = 'Éclairage'          AND company_id = :'company_id') AS cat_light_id,
    (SELECT id FROM item_category WHERE name = 'Protection'         AND company_id = :'company_id') AS cat_protect_id,
    (SELECT id FROM item_category WHERE name = 'Communication'      AND company_id = :'company_id') AS cat_comm_id,
    (SELECT id FROM item_category WHERE name = 'Surveillance vidéo' AND company_id = :'company_id') AS cat_cctv_id
\gset

INSERT INTO item_type (
    company_id, category_id, code, name, description, unit, quantity_total, minimum_stock, status
) VALUES
    (:'company_id', :'cat_light_id',   'TORCH', 'Lampe torche',        'Lampe torche rechargeable',        'piece', 25,  5, 'AVAILABLE'),
    (:'company_id', :'cat_protect_id', 'VEST',  'Gilet de protection', 'Gilet pare-coups',                  'piece', 15,  3, 'AVAILABLE'),
    (:'company_id', :'cat_protect_id', 'UNIF',  'Tenue complète',      'Uniforme complet agent',            'piece', 40, 10, 'AVAILABLE'),
    (:'company_id', :'cat_comm_id',    'PHONE', 'Téléphone pro',       'Téléphone professionnel',           'piece', 12,  2, 'AVAILABLE'),
    (:'company_id', :'cat_cctv_id',    'CCTV',  'Caméra de surveillance', 'Caméra intérieure/extérieure',   'piece',  6,  2, 'AVAILABLE');

SELECT
    (SELECT id FROM item_type WHERE code = 'TORCH') AS it_torch_id,
    (SELECT id FROM item_type WHERE code = 'VEST')  AS it_vest_id,
    (SELECT id FROM item_type WHERE code = 'UNIF')  AS it_unif_id,
    (SELECT id FROM item_type WHERE code = 'PHONE') AS it_phone_id,
    (SELECT id FROM item_type WHERE code = 'CCTV')  AS it_cctv_id
\gset

INSERT INTO item (company_id, category_id, item_type_id, qr_code, description, status) VALUES
    (:'company_id', :'item_category_id', :'item_type_id', 'QR-ITEM-002', 'Radio affectée à l''agent du portail', 'ASSIGNED'),
    (:'company_id', :'item_category_id', :'item_type_id', 'QR-ITEM-003', 'Radio de réserve',                     'AVAILABLE'),
    (:'company_id', :'cat_light_id',     :'it_torch_id',  'QR-ITEM-004', 'Lampe torche poste principal',         'ASSIGNED'),
    (:'company_id', :'cat_light_id',     :'it_torch_id',  'QR-ITEM-005', 'Lampe torche accueil banque',          'ASSIGNED'),
    (:'company_id', :'cat_protect_id',   :'it_vest_id',   'QR-ITEM-006', 'Gilet de protection en réserve',       'AVAILABLE'),
    (:'company_id', :'cat_comm_id',      :'it_phone_id',  'QR-ITEM-007', 'Téléphone du superviseur',             'ASSIGNED'),
    (:'company_id', :'cat_protect_id',   :'it_vest_id',   'QR-ITEM-008', 'Gilet de protection à Antsirabe',      'AVAILABLE');

-- Matériels supplémentaires couvrant tous les statuts.
INSERT INTO item (company_id, category_id, item_type_id, qr_code, description, status) VALUES
    (:'company_id', :'cat_cctv_id',      :'it_cctv_id',   'QR-ITEM-009', 'Caméra installée à l''entrée principale',     'AVAILABLE'),
    (:'company_id', :'item_category_id', :'item_type_id', 'QR-ITEM-010', 'Radio à réparer après une chute',             'DAMAGED'),
    (:'company_id', :'cat_comm_id',      :'it_phone_id',  'QR-ITEM-011', 'Téléphone déclaré perdu lors d''une ronde',   'LOST'),
    (:'company_id', :'cat_protect_id',   :'it_vest_id',   'QR-ITEM-012', 'Gilet en couture après une déchirure',        'REPAIR'),
    (:'company_id', :'cat_light_id',     :'it_torch_id',  'QR-ITEM-013', 'Torche retirée du service pour usure',        'RETIRED');

SELECT
    (SELECT id FROM item WHERE qr_code = 'QR-ITEM-002') AS item2_id,
    (SELECT id FROM item WHERE qr_code = 'QR-ITEM-003') AS item3_id,
    (SELECT id FROM item WHERE qr_code = 'QR-ITEM-004') AS item4_id,
    (SELECT id FROM item WHERE qr_code = 'QR-ITEM-005') AS item5_id,
    (SELECT id FROM item WHERE qr_code = 'QR-ITEM-006') AS item6_id,
    (SELECT id FROM item WHERE qr_code = 'QR-ITEM-007') AS item7_id,
    (SELECT id FROM item WHERE qr_code = 'QR-ITEM-008') AS item8_id,
    (SELECT id FROM item WHERE qr_code = 'QR-ITEM-009') AS item9_id,
    (SELECT id FROM item WHERE qr_code = 'QR-ITEM-010') AS item10_id,
    (SELECT id FROM item WHERE qr_code = 'QR-ITEM-011') AS item11_id,
    (SELECT id FROM item WHERE qr_code = 'QR-ITEM-012') AS item12_id,
    (SELECT id FROM item WHERE qr_code = 'QR-ITEM-013') AS item13_id
\gset

INSERT INTO warehouse (company_id, branch_id, name, address, active) VALUES
    (:'company_id', :'branch_toa_id', 'Entrepôt Toamasina', 'Tanambao V', TRUE),
    (:'company_id', :'branch_ant_id', 'Entrepôt Antsirabe', 'Ambohimena', TRUE);

SELECT
    (SELECT id FROM warehouse WHERE name = 'Entrepôt Toamasina') AS warehouse_toa_id,
    (SELECT id FROM warehouse WHERE name = 'Entrepôt Antsirabe') AS warehouse_ant_id
\gset

UPDATE item
SET current_warehouse_id = CASE qr_code
    WHEN 'QR-ITEM-001' THEN :'warehouse_id'
    WHEN 'QR-ITEM-002' THEN :'warehouse_id'
    WHEN 'QR-ITEM-003' THEN :'warehouse_id'
    WHEN 'QR-ITEM-004' THEN :'warehouse_id'
    WHEN 'QR-ITEM-005' THEN :'warehouse_id'
    WHEN 'QR-ITEM-006' THEN :'warehouse_toa_id'
    WHEN 'QR-ITEM-007' THEN :'warehouse_id'
    WHEN 'QR-ITEM-008' THEN :'warehouse_ant_id'
    WHEN 'QR-ITEM-009' THEN :'warehouse_toa_id'
    WHEN 'QR-ITEM-010' THEN :'warehouse_id'
    WHEN 'QR-ITEM-011' THEN NULL
    WHEN 'QR-ITEM-012' THEN :'warehouse_id'
    WHEN 'QR-ITEM-013' THEN :'warehouse_id'
END::uuid
WHERE company_id = :'company_id';

INSERT INTO inventory_operation (
    company_id,
    operation_type,
    status,
    reference,
    idempotency_key,
    destination_warehouse_id,
    performed_by,
    notes,
    created_at,
    completed_at
) VALUES (
    :'company_id',
    'RECEIPT',
    'COMPLETED',
    'OP-RECEPTION-001',
    'demo-reception-001',
    :'warehouse_id',
    :'employee_primary_id',
    'Réception de la commande de radios',
    '2024-06-10 09:00:00+03',
    '2024-06-10 09:05:00+03'
) RETURNING id \gset inventory_operation_receipt_

-- Opération de sortie regroupant les dotations du 15 juin.
INSERT INTO inventory_operation (
    company_id,
    operation_type,
    status,
    reference,
    idempotency_key,
    source_warehouse_id,
    performed_by,
    notes,
    created_at,
    completed_at
) VALUES (
    :'company_id',
    'ISSUE',
    'COMPLETED',
    'OP-SORTIE-002',
    'demo-issue-002',
    :'warehouse_id',
    :'employee_primary_id',
    'Dotation matinale des agents du 15 juin',
    '2024-06-15 05:30:00+03',
    '2024-06-15 06:05:00+03'
) RETURNING id \gset inventory_operation_issue_

-- Ajustement annulé : stock cohérent après contrôle.
INSERT INTO inventory_operation (
    company_id,
    operation_type,
    status,
    reference,
    idempotency_key,
    destination_warehouse_id,
    performed_by,
    notes,
    created_at
) VALUES (
    :'company_id',
    'ADJUSTMENT',
    'CANCELLED',
    'OP-REGUL-003',
    'demo-regul-003',
    :'warehouse_id',
    :'emp3_id',
    'Ajustement annulé : stock cohérent après contrôle du superviseur',
    '2024-06-13 15:00:00+03'
) RETURNING id \gset inventory_operation_adjust_

INSERT INTO stock_movement (
    company_id,
    warehouse_id,
    item_id,
    employee_id,
    movement_type,
    reference,
    movement_date,
    notes,
    operation_id
) VALUES
    (:'company_id', :'warehouse_id',     :'item2_id', :'employee_secondary_id', 'OUT', 'MVT-002', '2024-06-15 05:35:00+03', 'Sortie radio pour le portail', NULL),
    (:'company_id', :'warehouse_id',     :'item4_id', :'emp8_id',               'OUT', 'MVT-003', '2024-06-15 05:40:00+03', 'Sortie lampe torche', NULL),
    (:'company_id', :'warehouse_id',     :'item5_id', :'emp4_id',               'OUT', 'MVT-004', '2024-06-15 05:50:00+03', 'Sortie lampe torche banque', NULL),
    (:'company_id', :'warehouse_id',     :'item7_id', :'emp3_id',               'OUT', 'MVT-005', '2024-06-15 06:00:00+03', 'Sortie téléphone superviseur', NULL),
    (:'company_id', :'warehouse_id',     :'item3_id', :'employee_primary_id',   'IN',  'MVT-006', '2024-06-10 09:00:00+03', 'Réception de la commande de radios', :'inventory_operation_receipt_id'),
    (:'company_id', :'warehouse_toa_id', :'item6_id', :'emp6_id',               'IN',  'MVT-007', '2024-06-12 10:30:00+03', 'Réception de gilets à Toamasina', NULL),
    (:'company_id', :'warehouse_ant_id', :'item8_id', :'emp9_id',               'IN',  'MVT-008', '2024-06-12 11:00:00+03', 'Réception de gilets à Antsirabe', NULL);

-- Transferts (deux jambes), retour, ajustement et destruction.
INSERT INTO stock_movement (
    company_id,
    warehouse_id,
    item_id,
    employee_id,
    movement_type,
    reference,
    movement_date,
    notes,
    destination_warehouse_id
) VALUES (
    :'company_id', :'warehouse_id',     :'item9_id', :'emp3_id', 'TRANSFER', 'MVT-009',
    '2024-06-14 16:30:00+03', 'Envoi de la caméra vers Toamasina', :'warehouse_toa_id'
);

INSERT INTO stock_movement (
    company_id,
    warehouse_id,
    item_id,
    employee_id,
    movement_type,
    reference,
    movement_date,
    notes,
    source_warehouse_id
) VALUES (
    :'company_id', :'warehouse_toa_id', :'item9_id', :'emp6_id', 'TRANSFER', 'MVT-010',
    '2024-06-14 19:00:00+03', 'Réception du transfert depuis Antananarivo', :'warehouse_id'
);

INSERT INTO stock_movement (
    company_id,
    warehouse_id,
    item_id,
    employee_id,
    movement_type,
    reference,
    movement_date,
    notes
) VALUES
    (:'company_id', :'warehouse_id', :'item5_id',  :'emp4_id', 'RETURN',      'MVT-011', '2024-06-14 17:30:00+03', 'Retour du prêt après l''inventaire de la banque'),
    (:'company_id', :'warehouse_id', :'item10_id', :'emp10_id','ADJUSTMENT',  'MVT-012', '2024-06-18 16:00:00+03', 'Reclassification du stock après contrôle'),
    (:'company_id', :'warehouse_id', :'item13_id', :'emp3_id', 'DESTRUCTION', 'MVT-013', '2024-06-19 10:00:00+03', 'Destruction d''une torche usée - bon REF-DES-001');

INSERT INTO inventory_operation_item (
    operation_id,
    item_id,
    movement_id,
    quantity,
    condition_before,
    condition_after,
    scanned_at,
    notes
) VALUES
    (:'inventory_operation_receipt_id', :'item3_id',
     (SELECT id FROM stock_movement WHERE reference = 'MVT-006' LIMIT 1),
     1, NULL, 'Neuf', '2024-06-10 09:02:00+03', 'Équipement ajouté au stock'),
    (:'inventory_operation_issue_id', :'item2_id',
     (SELECT id FROM stock_movement WHERE reference = 'MVT-002' LIMIT 1),
     1, 'Bon état', 'Bon état', '2024-06-15 05:35:00+03', NULL),
    (:'inventory_operation_issue_id', :'item4_id',
     (SELECT id FROM stock_movement WHERE reference = 'MVT-003' LIMIT 1),
     1, 'Bon état', 'Bon état', '2024-06-15 05:40:00+03', NULL),
    (:'inventory_operation_issue_id', :'item5_id',
     (SELECT id FROM stock_movement WHERE reference = 'MVT-004' LIMIT 1),
     1, 'Neuf', 'Neuf', '2024-06-15 05:50:00+03', NULL),
    (:'inventory_operation_issue_id', :'item7_id',
     (SELECT id FROM stock_movement WHERE reference = 'MVT-005' LIMIT 1),
     1, 'Bon état', 'Bon état', '2024-06-15 06:00:00+03', NULL);

INSERT INTO warehouse_stock_config (warehouse_id, item_type_id, minimum_stock) VALUES
    (:'warehouse_id',     :'item_type_id', 2),
    (:'warehouse_id',     :'it_torch_id',  5),
    (:'warehouse_id',     :'it_vest_id',   3),
    (:'warehouse_id',     :'it_phone_id',  2),
    (:'warehouse_toa_id', :'it_vest_id',   2),
    (:'warehouse_ant_id', :'it_vest_id',   2),
    (:'warehouse_id',     :'it_cctv_id',   1),
    (:'warehouse_toa_id', :'it_phone_id',  1);

INSERT INTO inventory_count (
    company_id,
    warehouse_id,
    reference,
    status,
    started_by,
    completed_by,
    started_at,
    completed_at,
    notes
) VALUES (
    :'company_id',
    :'warehouse_id',
    'INV-2024-06-001',
    'COMPLETED',
    :'employee_primary_id',
    :'employee_primary_id',
    '2024-06-30 08:00:00+03',
    '2024-06-30 10:30:00+03',
    'Comptage mensuel de l''entrepôt principal'
) RETURNING id \gset inventory_count_

INSERT INTO inventory_count_scan (
    company_id,
    inventory_count_id,
    item_id,
    scanned_by,
    scanned_at,
    expected_warehouse_id,
    actual_warehouse_id,
    discrepancy_type,
    notes
) VALUES
    (:'company_id', :'inventory_count_id', :'item_id',  :'employee_primary_id', '2024-06-30 08:15:00+03', :'warehouse_id', :'warehouse_id', 'NONE', NULL),
    (:'company_id', :'inventory_count_id', :'item2_id', :'employee_primary_id', '2024-06-30 08:20:00+03', :'warehouse_id', :'warehouse_id', 'NONE', NULL),
    (:'company_id', :'inventory_count_id', :'item3_id', :'employee_primary_id', '2024-06-30 08:25:00+03', :'warehouse_id', :'warehouse_id', 'NONE', NULL),
    (:'company_id', :'inventory_count_id', :'item4_id', :'employee_primary_id', '2024-06-30 08:30:00+03', :'warehouse_id', :'warehouse_id', 'NONE', NULL),
    (:'company_id', :'inventory_count_id', :'item5_id', :'employee_primary_id', '2024-06-30 08:35:00+03', :'warehouse_id', :'warehouse_id', 'NONE', NULL),
    (:'company_id', :'inventory_count_id', :'item7_id', :'employee_primary_id', '2024-06-30 08:40:00+03', :'warehouse_id', :'warehouse_id', 'NONE', NULL);

-- Comptage en cours avec écarts (équipements égarés et mal placés).
INSERT INTO inventory_count (
    company_id,
    warehouse_id,
    reference,
    status,
    started_by,
    started_at,
    notes
) VALUES (
    :'company_id',
    :'warehouse_toa_id',
    'INV-2024-07-001',
    'IN_PROGRESS',
    :'emp6_id',
    '2024-07-15 08:00:00+03',
    'Comptage en cours - gilets et nouveau matériel de communication'
) RETURNING id \gset inventory_count2_

INSERT INTO inventory_count_scan (
    company_id,
    inventory_count_id,
    item_id,
    scanned_by,
    scanned_at,
    expected_warehouse_id,
    actual_warehouse_id,
    discrepancy_type,
    notes
) VALUES
    (:'company_id', :'inventory_count2_id', :'item6_id',  :'emp6_id', '2024-07-15 08:10:00+03', :'warehouse_toa_id', :'warehouse_toa_id', 'NONE',          NULL),
    (:'company_id', :'inventory_count2_id', :'item8_id',  :'emp6_id', '2024-07-15 08:20:00+03', :'warehouse_ant_id', :'warehouse_toa_id', 'WRONG_LOCATION','Gilet destiné à Antsirabe trouvé à Toamasina'),
    (:'company_id', :'inventory_count2_id', :'item9_id',  :'emp6_id', '2024-07-15 08:30:00+03', :'warehouse_toa_id', :'warehouse_toa_id', 'NONE',          'Reçu par transfert le 14 juin'),
    (:'company_id', :'inventory_count2_id', :'item11_id', :'emp6_id', '2024-07-15 08:40:00+03', :'warehouse_id',     NULL,                'MISSING',       'Téléphone déjà signalé perdu : confirmé manquant');

INSERT INTO item_assignment (company_id, item_id, employee_id, post_id, assigned_at, condition_on_assignment, notes) VALUES
    (:'company_id', :'item2_id', :'employee_secondary_id',
     (SELECT id FROM security_post WHERE code = 'POST-001' LIMIT 1),
     '2024-06-15 05:50:00+03', 'Bon état', 'Radio du portail principal'),
    (:'company_id', :'item4_id', :'emp8_id',
     (SELECT id FROM security_post WHERE code = 'POST-001' LIMIT 1),
     '2024-06-15 05:55:00+03', 'Bon état', 'Lampe torche poste principal'),
    (:'company_id', :'item5_id', :'emp4_id',               :'post2_id', '2024-06-15 06:00:00+03', 'Neuf',         'Lampe torche accueil banque'),
    (:'company_id', :'item7_id', :'emp3_id',               :'post7_id', '2024-06-15 06:05:00+03', 'Bon état',     'Téléphone de supervision');

-- Affectations supplémentaires dont une avec retour.
INSERT INTO item_assignment (company_id, item_id, employee_id, post_id, assigned_at, condition_on_assignment, notes) VALUES
    (:'company_id', :'item6_id', :'emp6_id', :'post4_id', '2024-06-15 07:00:00+03', 'Neuf', 'Gilet de protection pour la saison hôtelière');

INSERT INTO item_assignment (
    company_id,
    item_id,
    employee_id,
    post_id,
    assigned_at,
    returned_at,
    expected_return_at,
    condition_on_assignment,
    condition_on_return,
    notes
) VALUES (
    :'company_id',
    :'item5_id',
    :'emp4_id',
    :'post2_id',
    '2024-06-10 09:00:00+03',
    '2024-06-14 17:30:00+03',
    '2024-06-14 18:00:00+03',
    'Neuf',
    'Bon état',
    'Prêt temporaire pour l''inventaire de la banque'
);
