-- ============================================================
-- 19. LOT 3 : STOCK CALCULE ET RECONCILIATION DES COMPTAGES
--
-- Migration strictement additive :
--   - creation de security.stock_balance (solde par entrepot et type) ;
--   - ajout des quantites attendue/comptee/ecart aux scans de comptage ;
--   - backfill initial des soldes depuis l'etat des articles existants ;
--   - index de lecture pour le dashboard et les alertes.
--
-- Aucune table, colonne ou ligne existante n'est supprimee.
-- Aucun NOT NULL n'est ajoute aux colonnes historiques de comptage :
-- les scans existants restent valides avec des quantites NULL tant
-- qu'ils n'ont pas ete rejoues.
--
-- Ordre d'application : apres 18_lot1_security.sql et apres l'arret
-- de l'application (ou pendant une fenetre de maintenance) pour que
-- le backfill soit coherent avec les articles au moment de la bascule.
-- Le backfill n'insere que les soldes absents : rejouer le script ne
-- remet pas a zero les soldes issus des mouvements ou des comptages.
-- ============================================================

BEGIN;

SET search_path TO security, public;

-- 1. Solde de stock calculé.
CREATE TABLE IF NOT EXISTS security.stock_balance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES security.company(id) ON DELETE CASCADE,
    warehouse_id UUID NOT NULL REFERENCES security.warehouse(id) ON DELETE RESTRICT,
    item_type_id UUID NOT NULL REFERENCES security.item_type(id) ON DELETE RESTRICT,
    quantity NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (company_id, warehouse_id, item_type_id)
);

CREATE INDEX IF NOT EXISTS idx_stock_balance_warehouse_type
    ON security.stock_balance (warehouse_id, item_type_id);

CREATE INDEX IF NOT EXISTS idx_stock_balance_company_type
    ON security.stock_balance (company_id, item_type_id);

-- 2. Quantités de comptage. Les colonnes sont nullables pour ne pas
-- invalider les lignes historiques ; les nouveaux scans les alimentent.
ALTER TABLE security.inventory_count_scan
    ADD COLUMN IF NOT EXISTS expected_quantity NUMERIC(12,2);

ALTER TABLE security.inventory_count_scan
    ADD COLUMN IF NOT EXISTS counted_quantity NUMERIC(12,2);

ALTER TABLE security.inventory_count_scan
    ADD COLUMN IF NOT EXISTS difference NUMERIC(12,2);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'inventory_count_scan_expected_quantity_check'
    ) THEN
        ALTER TABLE security.inventory_count_scan
            ADD CONSTRAINT inventory_count_scan_expected_quantity_check
            CHECK (expected_quantity IS NULL OR expected_quantity >= 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'inventory_count_scan_counted_quantity_check'
    ) THEN
        ALTER TABLE security.inventory_count_scan
            ADD CONSTRAINT inventory_count_scan_counted_quantity_check
            CHECK (counted_quantity IS NULL OR counted_quantity >= 0);
    END IF;
END $$;

COMMENT ON TABLE security.stock_balance IS
    'Solde de stock disponible par entreprise, entrepot et type d''article.';
COMMENT ON COLUMN security.stock_balance.quantity IS
    'Quantite disponible calculee ; mise a jour dans la meme transaction que les mouvements.';
COMMENT ON COLUMN security.inventory_count_scan.expected_quantity IS
    'Quantite theorique figee au moment du scan (1 si l''article est attendu disponible dans l''entrepot du comptage).';
COMMENT ON COLUMN security.inventory_count_scan.counted_quantity IS
    'Quantite physique constatee lors du scan.';
COMMENT ON COLUMN security.inventory_count_scan.difference IS
    'Ecart calcule par le serveur : counted_quantity - expected_quantity.';

-- 3. Backfill initial depuis l'etat physique des articles.
-- Seuls les articles disponibles, non archives et rattaches a un
-- entrepot contribuent au stock disponible. Les articles affectes,
-- perdus, retires, endommages ou en reparation ne sont pas comptes
-- dans le solde disponible ; leur etat reste visible dans item.
--
-- ON CONFLICT DO NOTHING rend la migration rejouable sans ecraser un
-- solde deja alimente par des mouvements ou des comptages posterieurs
-- a la bascule. Les soldes existants ne sont pas reinitialises.
INSERT INTO security.stock_balance
    (company_id, warehouse_id, item_type_id, quantity)
SELECT
    i.company_id,
    i.current_warehouse_id,
    i.item_type_id,
    count(*)::NUMERIC(12,2)
FROM security.item i
WHERE i.current_warehouse_id IS NOT NULL
  AND i.status = 'AVAILABLE'
  AND i.archived_at IS NULL
GROUP BY i.company_id, i.current_warehouse_id, i.item_type_id
ON CONFLICT (company_id, warehouse_id, item_type_id)
DO NOTHING;

-- 4. Aide de lecture pour le dashboard : seuil effectif par entrepot.
CREATE INDEX IF NOT EXISTS idx_warehouse_stock_config_lookup
    ON security.warehouse_stock_config (warehouse_id, item_type_id);

COMMIT;
