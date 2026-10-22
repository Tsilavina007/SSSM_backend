-- 3. AUTHENTIFICATION / RBAC : DONNÉES DE DÉMONSTRATION
-- ============================================================
-- Comptes, rôles d'entreprise et habilitations.
--
-- Aucune permission n'est insérée ici : les données initiales
-- (catalogue des permissions, rôles plateforme ADMIN/USER/INVENTORY)
-- sont chargées par docs/database/schema/17_seed_permissions.sql et le
-- mock les référence directement (SELECT ... WHERE code = '...').
--
-- Variables gset exposées aux fichiers suivants :
--   :user_id           demo.admin
--   :role_id           rôle "Administrateur entreprise"
--   :user_sup_id, :user_compta_id, :user_agent1_id, :user_agent2_id,
--   :user_agent3_id (soan.agent), :user_locked_id (soa.locked)
--   :role_sup_id, :role_agent_id, :role_compta_id
-- ============================================================

SET search_path TO security, public;

INSERT INTO app_user (
    company_id,
    username,
    email,
    password_hash,
    first_name,
    last_name,
    phone,
    status
) VALUES (
    :'company_id',
    'demo.admin',
    'demo.admin@sssm.test',
    '$2a$10$demo-password-hash',
    'Demo',
    'Admin',
    '+261 34 00 000 51',
    'ACTIVE'
) RETURNING id \gset user_

-- Rôle d'entreprise « Administrateur entreprise » (données de démonstration).
INSERT INTO role (
    company_id,
    name,
    description
) VALUES (
    :'company_id',
    'Administrateur entreprise',
    'Administration de l''entreprise SSSM Madagascar'
) RETURNING id \gset role_

-- rôles métiers de l'entreprise (données de démonstration).
INSERT INTO role (company_id, name, description) VALUES
    (:'company_id', 'Superviseur',       'Supervision des sites, des agents et des incidents'),
    (:'company_id', 'Agent de sécurité', 'Agent de terrain'),
    (:'company_id', 'Comptable',         'Facturation et paiements');

SELECT
    (SELECT id FROM role WHERE name = 'Superviseur'       AND company_id = :'company_id') AS role_sup_id,
    (SELECT id FROM role WHERE name = 'Agent de sécurité' AND company_id = :'company_id') AS role_agent_id,
    (SELECT id FROM role WHERE name = 'Comptable'         AND company_id = :'company_id') AS role_compta_id
\gset

-- ------------------------------------------------------------
-- Habilitations : utilisation directe des données initiales.
-- ------------------------------------------------------------

-- L'administrateur d'entreprise reçoit toutes les permissions du
-- catalogue chargé par le seed (17).
INSERT INTO role_permission (role_id, permission_id)
SELECT :'role_id', id FROM permission;

-- Le superviseur pilote le terrain et supervise les incidents.
INSERT INTO role_permission (role_id, permission_id)
SELECT :'role_sup_id', id FROM permission
WHERE code IN ('dashboard.view', 'employees.view', 'employees.manage', 'sites.manage',
               'posts.manage', 'planning.manage', 'patrols.manage', 'tracking.view',
               'incidents.manage', 'reports.view');

-- L'agent consulte le suivi et déclare les incidents.
INSERT INTO role_permission (role_id, permission_id)
SELECT :'role_agent_id', id FROM permission
WHERE code IN ('dashboard.view', 'tracking.view', 'incidents.manage');

-- Le comptable gère la relation client et la facturation.
INSERT INTO role_permission (role_id, permission_id)
SELECT :'role_compta_id', id FROM permission
WHERE code IN ('dashboard.view', 'clients.manage', 'contracts.manage',
               'invoices.manage', 'payments.manage', 'reports.view');

-- ------------------------------------------------------------
-- Utilisateurs de démonstration
-- ------------------------------------------------------------

INSERT INTO app_user (company_id, username, email, password_hash, first_name, last_name, phone, status) VALUES
    (:'company_id', 'lalao.superviseur', 'lalao.randriamanana@sssm.test', '$2a$10$demo-password-hash',
     'Lalao', 'Randriamanana', '+261 34 00 000 52', 'ACTIVE'),
    (:'company_id', 'voahangy.compta',    'voahangy.rakotomalala@sssm.test', '$2a$10$demo-password-hash',
     'Voahangy', 'Rakotomalala', '+261 34 00 000 53', 'ACTIVE'),
    (:'company_id', 'tahina.agent',       'tahina.razafindrabe@sssm.test', '$2a$10$demo-password-hash',
     'Tahina', 'Razafindrabe', '+261 34 00 000 54', 'ACTIVE'),
    (:'company_id', 'tsiky.agent',        'tsiky.ravelomanana@sssm.test', '$2a$10$demo-password-hash',
     'Tsiky', 'Ravelomanana', '+261 34 00 000 55', 'ACTIVE'),
    (:'company_id', 'soan.agent',         'soan.rajaonarivelo@sssm.test', '$2a$10$demo-password-hash',
     'Soan', 'Rajaonarivelo', '+261 34 00 000 56', 'ACTIVE'),
    (:'company_id', 'soa.locked',         'soa.andriamalala@sssm.test', '$2a$10$demo-password-hash',
     'Soa', 'Andriamalala', '+261 34 00 000 57', 'LOCKED');

SELECT
    (SELECT id FROM app_user WHERE username = 'lalao.superviseur') AS user_sup_id,
    (SELECT id FROM app_user WHERE username = 'voahangy.compta')   AS user_compta_id,
    (SELECT id FROM app_user WHERE username = 'tahina.agent')      AS user_agent1_id,
    (SELECT id FROM app_user WHERE username = 'tsiky.agent')       AS user_agent2_id,
    (SELECT id FROM app_user WHERE username = 'soan.agent')        AS user_agent3_id,
    (SELECT id FROM app_user WHERE username = 'soa.locked')        AS user_locked_id
\gset

-- ------------------------------------------------------------
-- Affectations des rôles
-- ------------------------------------------------------------

INSERT INTO user_role (user_id, role_id) VALUES
    (:'user_sup_id',    :'role_sup_id'),
    (:'user_compta_id', :'role_compta_id'),
    (:'user_agent1_id', :'role_agent_id'),
    (:'user_agent2_id', :'role_agent_id'),
    (:'user_agent3_id', :'role_sup_id'),
    (:'user_locked_id', :'role_agent_id');

-- L'administrateur reçoit son rôle d'entreprise...
INSERT INTO user_role (user_id, role_id) VALUES
    (:'user_id', :'role_id');

-- ... et directement le rôle plateforme ADMIN chargé par le seed (17),
-- qui porte l'autorité ROLE_ADMIN attendue par Spring Security.
INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id
FROM app_user u
CROSS JOIN role r
WHERE u.username = 'demo.admin'
  AND r.company_id IS NULL
  AND r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

-- Les jetons (tables créées par 18_lot1_security.sql).
INSERT INTO refresh_token (user_id, token_hash, issued_at, expires_at, revoked_at, user_agent, ip_address) VALUES
    (:'user_id',        'demo-refresh-token-hash-admin-1', '2024-06-15 07:00:00+03', '2024-06-22 07:00:00+03', NULL,
     'SSSM demo (web)',     '127.0.0.1'),
    (:'user_id',        'demo-refresh-token-hash-admin-2', '2024-06-10 09:30:00+03', '2024-06-17 09:30:00+03', '2024-06-10 11:00:00+03',
     'SSSM demo (mobile)',  '127.0.0.1'),
    (:'user_sup_id',    'demo-refresh-token-hash-sup-1',   '2024-06-16 06:30:00+03', '2024-06-23 06:30:00+03', NULL,
     'SSSM demo (web)',     '127.0.0.1'),
    (:'user_agent1_id', 'demo-refresh-token-hash-agent-1', '2024-06-16 06:45:00+03', '2024-06-23 06:45:00+03', NULL,
     'SSSM demo (mobile)',  '127.0.0.1');

INSERT INTO revoked_access_token (jti, user_id, expires_at, revoked_at) VALUES
    ('d6a2b1c0-0000-4000-8000-000000000001', :'user_id', '2024-06-15 10:30:00+03', '2024-06-15 08:15:00+03');
