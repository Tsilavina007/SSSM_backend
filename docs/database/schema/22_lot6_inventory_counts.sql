-- ============================================================
-- 22. LOT 6 : COMPTAGES ET RECONCILIATION
--
-- Migration strictement additive :
--   - table security.inventory_count_expected : instantane des quantites
--     attendues, fige a l'ouverture du comptage ;
--   - index de lecture associes.
--
-- Aucune table, colonne ou ligne existante n'est supprimee ou modifiee.
-- Les comptages anterieurs restent lisibles, mais n'ont pas d'instantane
-- retroactif : un scan sur un comptage encore ouvert est refuse
-- (ITEM_OUT_OF_SCOPE). Il faut le cloturer ou l'annuler, puis en ouvrir un
-- nouveau pour figer un instantane.
--
-- Ordre d'application : apres 21_lot5_qr.sql, avant la mise en service.
-- Le script est rejouable.
-- ============================================================

BEGIN;

SET search_path TO security, public;

-- 1. Instantane des attendus, fige a l'ouverture du comptage.
--    expected_quantity = 1 lorsque l'article etait disponible dans
--    l'entrepot compte a l'ouverture, 0 sinon. expected_warehouse_id
--    conserve l'emplacement theorique pour qualifier un ecart
--    WRONG_LOCATION.
CREATE TABLE IF NOT EXISTS security.inventory_count_expected (
    inventory_count_id UUID NOT NULL
        REFERENCES security.inventory_count(id) ON DELETE RESTRICT,
    item_id UUID NOT NULL
        REFERENCES security.item(id) ON DELETE RESTRICT,
    company_id UUID NOT NULL
        REFERENCES security.company(id) ON DELETE RESTRICT,
    expected_warehouse_id UUID
        REFERENCES security.warehouse(id) ON DELETE RESTRICT,
    expected_quantity NUMERIC(12, 2) NOT NULL DEFAULT 0
        CHECK (expected_quantity >= 0),
    item_status_at_open security.item_status,
    snapshot_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (inventory_count_id, item_id)
);

-- 2. Index : lecture par article (historique des comptages d'un article)
--    et par entreprise (controle de perimetre).
CREATE INDEX IF NOT EXISTS idx_inventory_count_expected_item
    ON security.inventory_count_expected (item_id);

CREATE INDEX IF NOT EXISTS idx_inventory_count_expected_company
    ON security.inventory_count_expected (company_id);

COMMENT ON TABLE security.inventory_count_expected IS
    'Instantane des quantites attendues d''un comptage, fige a l''ouverture.';

COMMENT ON COLUMN security.inventory_count_expected.expected_quantity IS
    'Quantite attendue a l''ouverture : 1 si l''article etait disponible dans l''entrepot compte, 0 sinon.';

COMMENT ON COLUMN security.inventory_count_expected.expected_warehouse_id IS
    'Emplacement theorique de l''article a l''ouverture, utilise pour qualifier un ecart WRONG_LOCATION.';

COMMIT;
