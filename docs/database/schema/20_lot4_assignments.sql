-- ============================================================
-- 20. LOT 4 : CYCLE DE VIE DES AFFECTATIONS ET ARCHIVAGE
--
-- Migration strictement additive :
--   - entrepot d'origine et trace de void sur item_assignment ;
--   - acteur d'archivage sur les donnees maitres qui possedent
--     deja archived_at ;
--   - index de lecture associes ;
--   - contraintes de coherence sur les nouvelles colonnes.
--
-- Aucune table, colonne ou ligne existante n'est supprimee.
-- aucun NOT NULL n'est ajoute aux colonnes historiques : les lignes
-- existantes restent valides avec source_warehouse_id, voided_at,
-- voided_by, void_reason et archived_by a NULL.
--
-- Ordre d'application : apres 19_lot3_stock.sql, idealement pendant
-- une fenetre de maintenance courte. Le script est rejouable.
-- ============================================================

BEGIN;

SET search_path TO security, public;

-- 1. Entrepot d'origine : permet de restaurer l'emplacement lors
-- d'un retour ou d'un void. Les affectations historiques ne sont pas
-- devinees : la colonne reste NULL si l'information n'existe pas.
ALTER TABLE security.item_assignment
    ADD COLUMN IF NOT EXISTS source_warehouse_id UUID
        REFERENCES security.warehouse(id) ON DELETE RESTRICT;

-- 2. Trace de void/archivage d'une affectation. Une affectation
-- voided conserve toute sa ligne et son historique. Un void actif
-- renseigne aussi returned_at dans la meme transaction applicative.
ALTER TABLE security.item_assignment
    ADD COLUMN IF NOT EXISTS voided_at TIMESTAMPTZ;

ALTER TABLE security.item_assignment
    ADD COLUMN IF NOT EXISTS voided_by UUID;

ALTER TABLE security.item_assignment
    ADD COLUMN IF NOT EXISTS void_reason TEXT;

-- 3. Acteur d'archivage sur les tables qui possedent deja archived_at.
ALTER TABLE security.item_category
    ADD COLUMN IF NOT EXISTS archived_by UUID;

ALTER TABLE security.item_type
    ADD COLUMN IF NOT EXISTS archived_by UUID;

ALTER TABLE security.warehouse
    ADD COLUMN IF NOT EXISTS archived_by UUID;

-- 4. Contraintes : une ligne voided doit etre close et motivee.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'item_assignment_voided_requires_return'
          AND conrelid = 'security.item_assignment'::regclass
    ) THEN
        ALTER TABLE security.item_assignment
            ADD CONSTRAINT item_assignment_voided_requires_return
            CHECK (voided_at IS NULL OR returned_at IS NOT NULL);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'item_assignment_voided_requires_reason'
          AND conrelid = 'security.item_assignment'::regclass
    ) THEN
        ALTER TABLE security.item_assignment
            ADD CONSTRAINT item_assignment_voided_requires_reason
            CHECK (voided_at IS NULL OR void_reason IS NOT NULL);
    END IF;
END $$;

-- 5. Index de lecture. L'index unique actif existant continue de
-- proteger un seul retour NULL par article ; un void renseigne
-- returned_at, il ne cree donc pas de faux conflit.
CREATE INDEX IF NOT EXISTS idx_item_assignment_source_warehouse
    ON security.item_assignment (source_warehouse_id);

CREATE INDEX IF NOT EXISTS idx_item_assignment_voided_at
    ON security.item_assignment (voided_at)
    WHERE voided_at IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_item_assignment_company_active
    ON security.item_assignment (company_id, assigned_at DESC)
    WHERE returned_at IS NULL AND voided_at IS NULL;

COMMENT ON COLUMN security.item_assignment.source_warehouse_id IS
    'Entrepot d''ou l''article a ete affecte ; restaure au retour ou au void.';
COMMENT ON COLUMN security.item_assignment.voided_at IS
    'Date du void/archivage logique. La ligne et son historique sont conserves.';
COMMENT ON COLUMN security.item_assignment.voided_by IS
    'Acteur du void/archivage, issu du jeton lorsqu''il est disponible.';
COMMENT ON COLUMN security.item_assignment.void_reason IS
    'Motif obligatoire du void/archivage.';
COMMENT ON COLUMN security.item_category.archived_by IS
    'Acteur de l''archivage logique de la categorie.';
COMMENT ON COLUMN security.item_type.archived_by IS
    'Acteur de l''archivage logique du type d''article.';
COMMENT ON COLUMN security.warehouse.archived_by IS
    'Acteur de l''archivage logique de l''entrepot.';

COMMIT;
