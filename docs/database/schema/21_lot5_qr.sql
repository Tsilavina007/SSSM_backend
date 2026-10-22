-- ============================================================
-- 21. LOT 5 : SCAN QR JOURNALISE, CODE IMMUABLE, ETIQUETTES
--
-- Migration strictement additive :
--   - type enum security.qr_scan_result (resultat d'une resolution) ;
--   - table security.qr_scan (journal des scans) ;
--   - index de lecture associes.
--
-- Aucune table, colonne ou ligne existante n'est supprimee ou modifiee.
-- Aucun NOT NULL n'est ajoute aux colonnes historiques.
-- La colonne item.qr_code reste en place : le code devient immuable
-- cote application (Lot 5), sans changement de schema.
--
-- Ordre d'application : apres 20_lot4_assignments.sql, idealement pendant
-- une fenetre de maintenance courte. Le script est rejouable.
-- ============================================================

BEGIN;

SET search_path TO security, public;

-- 1. Resultat possible d'une resolution de QR code.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_type t
        JOIN pg_namespace n ON n.oid = t.typnamespace
        WHERE t.typname = 'qr_scan_result'
          AND n.nspname = 'security'
    ) THEN
        CREATE TYPE security.qr_scan_result AS ENUM ('RESOLVED', 'NOT_FOUND');
    END IF;
END $$;

-- 2. Journal des scans : qui, quand, sur quel appareil, avec quel resultat.
--    company_id est portee par l'appelant (JWT) ; item_id est null quand le
--    code est inconnu ou appartient a une autre entreprise.
CREATE TABLE IF NOT EXISTS security.qr_scan (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID REFERENCES security.company(id) ON DELETE RESTRICT,
    item_id UUID REFERENCES security.item(id) ON DELETE SET NULL,
    qr_code TEXT NOT NULL,
    scanned_by UUID REFERENCES security.app_user(id) ON DELETE SET NULL,
    scanned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    device_id TEXT,
    result security.qr_scan_result NOT NULL
);

-- 3. Index de lecture : journal par entreprise et par recence, par article,
--    par code (recherche d'historique d'un code).
CREATE INDEX IF NOT EXISTS idx_qr_scan_company_scanned_at
    ON security.qr_scan (company_id, scanned_at DESC);

CREATE INDEX IF NOT EXISTS idx_qr_scan_item
    ON security.qr_scan (item_id)
    WHERE item_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_qr_scan_code
    ON security.qr_scan (qr_code);

COMMENT ON TABLE security.qr_scan IS
    'Journal des resolutions de QR code : qui, quand, sur quel appareil, avec quel resultat.';

COMMENT ON COLUMN security.qr_scan.result IS
    'RESOLVED : code resolu dans l''entreprise de l''appelant ; NOT_FOUND : code inconnu ou hors entreprise.';

COMMENT ON COLUMN security.qr_scan.item_id IS
    'Article resolu ; NULL lorsque le code est inconnu ou appartient a une autre entreprise.';

COMMIT;
