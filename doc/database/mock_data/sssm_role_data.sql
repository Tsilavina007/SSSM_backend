\encoding UTF8
\set ON_ERROR_STOP on

BEGIN;

SET search_path TO security, public;
INSERT INTO security.role (company_id, name, description)
VALUES
(NULL, 'ADMIN', 'Administrateur système'),
(NULL, 'USER', 'Utilisateur standard'),
(NULL, 'INVENTORY', 'Utilisateur chargé de l inventaire')
ON CONFLICT DO NOTHING;

-- INSERT INTO security.user_role (user_id, role_id)
-- SELECT u.id, r.id
-- FROM security.app_user u
-- CROSS JOIN security.role r
-- WHERE u.username = 'admin'
--   AND r.name = 'ADMIN'
-- ON CONFLICT DO NOTHING;


COMMIT;
