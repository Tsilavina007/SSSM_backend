-- ============================================================
-- 18. LOT 1 : IDENTITE, TENANCY MONO-ENTREPRISE ET JETONS
--
-- Migration non destructive :
--   - ajout de company_id nullable aux tables d'inventaire qui en
--     étaient dépourvues ;
--   - backfill depuis item / inventory_count ;
--   - index de lecture ;
--   - création des tables de refresh token et de révocation des
--     access tokens ;
--   - ajout des permissions d'administration utilisateurs/rôles.
--
-- Aucun NOT NULL, aucune suppression et aucune remise à zéro.
-- ============================================================

SET search_path TO security, public;

ALTER TABLE security.stock_movement
    ADD COLUMN IF NOT EXISTS company_id UUID REFERENCES security.company(id) ON DELETE RESTRICT;

ALTER TABLE security.item_assignment
    ADD COLUMN IF NOT EXISTS company_id UUID REFERENCES security.company(id) ON DELETE RESTRICT;

ALTER TABLE security.inventory_count_scan
    ADD COLUMN IF NOT EXISTS company_id UUID REFERENCES security.company(id) ON DELETE RESTRICT;

-- Backfill des données existantes. Les lignes sans item/session
-- restent à NULL : le passage en NOT NULL est volontairement laissé
-- à une migration ultérieure après contrôle des orphelins.
UPDATE security.stock_movement sm
SET company_id = i.company_id
FROM security.item i
WHERE sm.item_id = i.id
  AND sm.company_id IS NULL;

UPDATE security.item_assignment ia
SET company_id = i.company_id
FROM security.item i
WHERE ia.item_id = i.id
  AND ia.company_id IS NULL;

UPDATE security.inventory_count_scan s
SET company_id = c.company_id
FROM security.inventory_count c
WHERE s.inventory_count_id = c.id
  AND s.company_id IS NULL;

-- Rattache les comptes non-plateforme à l'unique entreprise lorsque
-- le déploiement est mono-entreprise. Les administrateurs plateforme
-- (rôle global ADMIN) restent volontairement sans company_id.
UPDATE security.app_user u
SET company_id = (SELECT id FROM security.company ORDER BY created_at LIMIT 1)
WHERE u.company_id IS NULL
  AND (SELECT count(*) FROM security.company) = 1
  AND NOT EXISTS (
    SELECT 1
    FROM security.user_role ur
    JOIN security.role r ON r.id = ur.role_id
    WHERE ur.user_id = u.id
      AND r.company_id IS NULL
      AND upper(r.name) = 'ADMIN'
  );

CREATE INDEX IF NOT EXISTS idx_stock_movement_company
    ON security.stock_movement (company_id);

CREATE INDEX IF NOT EXISTS idx_item_assignment_company
    ON security.item_assignment (company_id);

CREATE INDEX IF NOT EXISTS idx_inventory_count_scan_company
    ON security.inventory_count_scan (company_id);

CREATE TABLE IF NOT EXISTS security.refresh_token (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES security.app_user(id) ON DELETE CASCADE,
    token_hash TEXT NOT NULL UNIQUE,
    issued_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    replaced_by_id UUID REFERENCES security.refresh_token(id) ON DELETE SET NULL,
    user_agent VARCHAR(500),
    ip_address VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_refresh_token_user
    ON security.refresh_token (user_id);

CREATE INDEX IF NOT EXISTS idx_refresh_token_expires
    ON security.refresh_token (expires_at);

CREATE TABLE IF NOT EXISTS security.revoked_access_token (
    jti UUID PRIMARY KEY,
    user_id UUID REFERENCES security.app_user(id) ON DELETE CASCADE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_revoked_access_token_expires
    ON security.revoked_access_token (expires_at);

INSERT INTO security.permission (code, name, description)
VALUES
    ('users.manage', 'Gérer les utilisateurs', 'Créer, modifier et affecter les comptes de son entreprise'),
    ('roles.manage', 'Gérer les rôles', 'Créer et modifier les rôles de son entreprise')
ON CONFLICT (code) DO NOTHING;

-- Attribution initiale aux rôles d'administration d'entreprise déjà
-- présents. Les rôles sont créés après ce script dans un schéma neuf ;
-- le script de données de démonstration refait cette attribution.
INSERT INTO security.role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM security.role r
CROSS JOIN security.permission p
WHERE r.company_id IS NOT NULL
  AND lower(r.name) LIKE '%admin%'
  AND p.code IN ('users.manage', 'roles.manage')
ON CONFLICT DO NOTHING;
