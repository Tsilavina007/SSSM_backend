\encoding UTF8
\set ON_ERROR_STOP on

-- =====================================================================
-- FIXTURES DÉDIÉES À LA DÉMONSTRATION POSTMAN
-- =====================================================================
-- Ce script prépare un jeu de données minimal, déterministe et isolé pour
-- les trois parcours de démonstration :
--
--   1. authentification : login + refresh (compte demo.admin existant) ;
--   2. affectation puis retour d'un article ;
--   3. sortie de stock OUT avec mise à jour du solde.
--
-- Il est IDEMPOTENT : chaque exécution supprime puis recrée uniquement les
-- lignes dont les codes sont préfixés DEMO-POSTMAN ou dont les UUID sont
-- réservés ci-dessous. Les autres données de démonstration ne sont pas
-- modifiées.
--
-- Prérequis :
--   - schéma chargé (docs/database/sssm_schema2_v01.sql) ;
--   - données de démonstration chargées (sssm_mock_data.sql), avec le compte
--     demo.admin et au moins une entreprise.
--
-- Exécution directe :
--   psql -d sssm_db -U postgres -f docs/database/mock_data/demo_postman.sql
--
-- Ou via le wrapper :
--   scripts/demo/reset-postman-demo.ps1
--
-- ATTENTION : ne pas exécuter sur une base de production. Ce script écrit
-- dans le schéma security et supprime les lignes DEMO-POSTMAN existantes.
-- =====================================================================

BEGIN;

SET search_path TO security, public;

-- ---------------------------------------------------------------------
-- 0. Contexte : entreprise et administrateur existants
-- ---------------------------------------------------------------------
CREATE TEMP TABLE _demo_postman_ctx ON COMMIT DROP AS
SELECT
    (SELECT id
       FROM security.company
      ORDER BY (name = 'SSSM Madagascar') DESC, created_at
      LIMIT 1) AS company_id,
    (SELECT id
       FROM security.app_user
      WHERE username = 'demo.admin'
      LIMIT 1) AS admin_id;

SELECT company_id AS demo_company_id,
       admin_id   AS demo_admin_id
  FROM _demo_postman_ctx
\gset

DO $$
BEGIN
    IF (SELECT company_id FROM _demo_postman_ctx) IS NULL THEN
        RAISE EXCEPTION
            'Aucune entreprise trouvée. Chargez d''abord docs/database/mock_data/sssm_mock_data.sql.';
    END IF;
    IF (SELECT admin_id FROM _demo_postman_ctx) IS NULL THEN
        RAISE EXCEPTION
            'Compte demo.admin introuvable. Chargez d''abord docs/database/mock_data/sssm_mock_data.sql.';
    END IF;
END $$;

-- ---------------------------------------------------------------------
-- 1. UUID réservés aux fixtures Postman
--    Ces valeurs sont reprises dans l'environnement Postman livré avec
--    la collection. Ne pas les réutiliser pour d'autres données.
-- ---------------------------------------------------------------------
\set demo_category_id   'd0e00000-0000-4000-8000-000000000001'
\set demo_item_type_id  'd0e00000-0000-4000-8000-000000000002'
\set demo_warehouse_id  'd0e00000-0000-4000-8000-000000000003'
\set demo_site_id       'd0e00000-0000-4000-8000-000000000004'
\set demo_post_id       'd0e00000-0000-4000-8000-000000000005'
\set demo_employee_id   'd0e00000-0000-4000-8000-000000000006'
\set demo_item_assign_id 'd0e00000-0000-4000-8000-000000000007'
\set demo_item_out_id    'd0e00000-0000-4000-8000-000000000008'
\set demo_item_spare_id  'd0e00000-0000-4000-8000-000000000009'
\set demo_balance_id     'd0e00000-0000-4000-8000-00000000000a'

-- ---------------------------------------------------------------------
-- 2. Nettoyage des fixtures précédentes
--    Ordre imposé par les clés étrangères : enfants avant parents.
-- ---------------------------------------------------------------------
DELETE FROM security.item_assignment
 WHERE item_id IN (SELECT id FROM security.item WHERE qr_code LIKE 'DEMO-POSTMAN-%')
    OR employee_id = :'demo_employee_id'::uuid
    OR post_id = :'demo_post_id'::uuid;

DELETE FROM security.inventory_operation_item
 WHERE item_id IN (SELECT id FROM security.item WHERE qr_code LIKE 'DEMO-POSTMAN-%')
    OR operation_id IN (
        SELECT id
          FROM security.inventory_operation
         WHERE source_warehouse_id = :'demo_warehouse_id'::uuid
            OR destination_warehouse_id = :'demo_warehouse_id'::uuid
            OR employee_id = :'demo_employee_id'::uuid
            OR post_id = :'demo_post_id'::uuid
    );

DELETE FROM security.inventory_count_scan
 WHERE item_id IN (SELECT id FROM security.item WHERE qr_code LIKE 'DEMO-POSTMAN-%')
    OR inventory_count_id IN (
        SELECT id
          FROM security.inventory_count
         WHERE warehouse_id = :'demo_warehouse_id'::uuid
    );

DELETE FROM security.stock_movement
 WHERE item_id IN (SELECT id FROM security.item WHERE qr_code LIKE 'DEMO-POSTMAN-%')
    OR warehouse_id = :'demo_warehouse_id'::uuid
    OR source_warehouse_id = :'demo_warehouse_id'::uuid
    OR destination_warehouse_id = :'demo_warehouse_id'::uuid
    OR employee_id = :'demo_employee_id'::uuid;

DELETE FROM security.inventory_operation
 WHERE source_warehouse_id = :'demo_warehouse_id'::uuid
    OR destination_warehouse_id = :'demo_warehouse_id'::uuid
    OR employee_id = :'demo_employee_id'::uuid
    OR post_id = :'demo_post_id'::uuid;

DELETE FROM security.inventory_count
 WHERE warehouse_id = :'demo_warehouse_id'::uuid;

DELETE FROM security.stock_balance
 WHERE warehouse_id = :'demo_warehouse_id'::uuid
    OR item_type_id = :'demo_item_type_id'::uuid;

DELETE FROM security.warehouse_stock_config
 WHERE warehouse_id = :'demo_warehouse_id'::uuid
    OR item_type_id = :'demo_item_type_id'::uuid;

DELETE FROM security.item
 WHERE qr_code LIKE 'DEMO-POSTMAN-%'
    OR id IN (:'demo_item_assign_id'::uuid,
              :'demo_item_out_id'::uuid,
              :'demo_item_spare_id'::uuid);

DELETE FROM security.item_type
 WHERE code = 'DEMO-RADIO'
    OR id = :'demo_item_type_id'::uuid;

DELETE FROM security.item_category
 WHERE name = 'Démonstration Postman'
    OR id = :'demo_category_id'::uuid;

DELETE FROM security.employee
 WHERE employee_number = 'DEMO-POSTMAN-EMP'
    OR id = :'demo_employee_id'::uuid;

DELETE FROM security.security_post
 WHERE code = 'DEMO-POSTMAN-POST'
    OR id = :'demo_post_id'::uuid;

DELETE FROM security.site
 WHERE code = 'DEMO-POSTMAN-SITE'
    OR id = :'demo_site_id'::uuid;

DELETE FROM security.warehouse
 WHERE name = 'Entrepôt démo Postman'
    OR id = :'demo_warehouse_id'::uuid;

-- ---------------------------------------------------------------------
-- 3. Fixtures : catégorie, type, entrepôt, site, poste, employé
-- ---------------------------------------------------------------------
INSERT INTO security.item_category (id, company_id, name, description)
VALUES (
    :'demo_category_id'::uuid,
    :'demo_company_id'::uuid,
    'Démonstration Postman',
    'Catégorie dédiée aux parcours de démonstration Postman'
);

INSERT INTO security.item_type (
    id, company_id, category_id, code, name, description,
    unit, quantity_total, minimum_stock, status
) VALUES (
    :'demo_item_type_id'::uuid,
    :'demo_company_id'::uuid,
    :'demo_category_id'::uuid,
    'DEMO-RADIO',
    'Radio démo Postman',
    'Type dédié au suivi du solde de démonstration',
    'piece',
    3,
    1,
    'AVAILABLE'
);

INSERT INTO security.warehouse (id, company_id, name, address, active)
VALUES (
    :'demo_warehouse_id'::uuid,
    :'demo_company_id'::uuid,
    'Entrepôt démo Postman',
    'Entrepôt isolé pour la démonstration technique',
    TRUE
);

INSERT INTO security.site (
    id, company_id, name, code, address, city, status
) VALUES (
    :'demo_site_id'::uuid,
    :'demo_company_id'::uuid,
    'Site démo Postman',
    'DEMO-POSTMAN-SITE',
    'Site dédié à la démonstration technique',
    'Antananarivo',
    'ACTIVE'
);

INSERT INTO security.security_post (
    id, site_id, name, code, description,
    required_agents, start_time, end_time, status
) VALUES (
    :'demo_post_id'::uuid,
    :'demo_site_id'::uuid,
    'Poste démo Postman',
    'DEMO-POSTMAN-POST',
    'Poste utilisé pour l''affectation et la sortie de démonstration',
    1,
    '06:00',
    '18:00',
    'ACTIVE'
);

INSERT INTO security.employee (
    id, company_id, employee_number, first_name, last_name,
    position, status, notes
) VALUES (
    :'demo_employee_id'::uuid,
    :'demo_company_id'::uuid,
    'DEMO-POSTMAN-EMP',
    'Démo',
    'Postman',
    'Agent de démonstration',
    'ACTIVE',
    'Employé dédié à la démonstration Postman'
);

-- ---------------------------------------------------------------------
-- 4. Fixtures : trois articles disponibles dans l'entrepôt démo
--    - ASSIGN : affectation puis retour ;
--    - OUT    : sortie de stock ;
--    - SPARE  : témoin, jamais modifié par la démo.
--    Le solde initial du type DEMO-RADIO dans l'entrepôt est donc 3.
-- ---------------------------------------------------------------------
INSERT INTO security.item (
    id, company_id, category_id, item_type_id,
    qr_code, description, status, current_warehouse_id
) VALUES
    (
        :'demo_item_assign_id'::uuid,
        :'demo_company_id'::uuid,
        :'demo_category_id'::uuid,
        :'demo_item_type_id'::uuid,
        'DEMO-POSTMAN-ASSIGN',
        'Article pour l''affectation puis le retour',
        'AVAILABLE',
        :'demo_warehouse_id'::uuid
    ),
    (
        :'demo_item_out_id'::uuid,
        :'demo_company_id'::uuid,
        :'demo_category_id'::uuid,
        :'demo_item_type_id'::uuid,
        'DEMO-POSTMAN-OUT',
        'Article pour la sortie de stock',
        'AVAILABLE',
        :'demo_warehouse_id'::uuid
    ),
    (
        :'demo_item_spare_id'::uuid,
        :'demo_company_id'::uuid,
        :'demo_category_id'::uuid,
        :'demo_item_type_id'::uuid,
        'DEMO-POSTMAN-SPARE',
        'Article témoin du solde initial',
        'AVAILABLE',
        :'demo_warehouse_id'::uuid
    );

-- ---------------------------------------------------------------------
-- 5. Seuil et solde initial
-- ---------------------------------------------------------------------
INSERT INTO security.warehouse_stock_config (
    warehouse_id, item_type_id, minimum_stock
) VALUES (
    :'demo_warehouse_id'::uuid,
    :'demo_item_type_id'::uuid,
    1
);

INSERT INTO security.stock_balance (
    id, company_id, warehouse_id, item_type_id, quantity, version
) VALUES (
    :'demo_balance_id'::uuid,
    :'demo_company_id'::uuid,
    :'demo_warehouse_id'::uuid,
    :'demo_item_type_id'::uuid,
    3,
    0
)
ON CONFLICT (company_id, warehouse_id, item_type_id)
DO UPDATE
   SET quantity   = EXCLUDED.quantity,
       version    = security.stock_balance.version + 1,
       updated_at = now();

COMMIT;

-- ---------------------------------------------------------------------
-- 6. Récapitulatif pour la personne qui anime la démo
-- ---------------------------------------------------------------------
\echo ''
\echo '=== Fixtures Postman prêtes ==='
SELECT element, id, detail
  FROM (
        SELECT 1 AS ord, 'Entreprise'        AS element, :'demo_company_id'::text    AS id, 'SSSM Madagascar (existante)' AS detail
  UNION ALL SELECT 2, 'Administrateur',  :'demo_admin_id'::text,       'demo.admin / Demo@2024'
  UNION ALL SELECT 3, 'Entrepôt',        :'demo_warehouse_id'::text,   'Entrepôt démo Postman'
  UNION ALL SELECT 4, 'Type d''article', :'demo_item_type_id'::text,   'DEMO-RADIO'
  UNION ALL SELECT 5, 'Article affectation', :'demo_item_assign_id'::text, 'DEMO-POSTMAN-ASSIGN'
  UNION ALL SELECT 6, 'Article sortie',  :'demo_item_out_id'::text,    'DEMO-POSTMAN-OUT'
  UNION ALL SELECT 7, 'Article témoin',  :'demo_item_spare_id'::text,  'DEMO-POSTMAN-SPARE'
  UNION ALL SELECT 8, 'Employé',         :'demo_employee_id'::text,    'DEMO-POSTMAN-EMP'
  UNION ALL SELECT 9, 'Poste',           :'demo_post_id'::text,        'DEMO-POSTMAN-POST'
  UNION ALL SELECT 10, 'Solde initial',  '3',                          'DEMO-RADIO dans l''entrepôt démo'
       ) t
 ORDER BY ord;
