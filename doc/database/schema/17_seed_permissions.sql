-- 17. DONNÉES INITIALES : RÉFÉRENTIEL PERMISSIONS / RÔLES PLATEFORME
-- ============================================================
-- Données de référence nécessaires dès la création de la base :
--   - catalogues des permissions ;
--   - rôles plateforme (company_id NULL) : ADMIN, USER, INVENTORY ;
--   - droits par défaut des rôles plateforme.
--
-- Ces données n'appartiennent à aucune entreprise et ne doivent être
-- ni modifiées ni supprimées par les données de démonstration.
--
-- Le script est idempotent (ON CONFLICT DO NOTHING / garde NOT EXISTS) :
-- il peut être rejoué sur une base existante sans effet de bord.
--
-- Les données de démonstration (mock_data/) réutilisent directement ce
-- référentiel via SELECT ... FROM permission WHERE code = '...' et ne
-- doivent jamais insérer de nouvelles permissions.
-- ============================================================

SET search_path TO security, public;

-- ------------------------------------------------------------
-- 17.1. Catalogue des permissions
-- ------------------------------------------------------------

INSERT INTO permission (code, name, description) VALUES
    ('dashboard.view',   'Voir le tableau de bord', 'Accès au dashboard'),
    ('users.manage',     'Gérer les utilisateurs',  'Création/modification des comptes'),
    ('roles.manage',     'Gérer les rôles',         'Gestion RBAC'),
    ('employees.view',   'Voir les agents',         'Consultation du personnel'),
    ('employees.manage', 'Gérer les agents',        'Gestion du personnel'),
    ('sites.manage',     'Gérer les sites',         'Gestion des sites clients'),
    ('posts.manage',     'Gérer les postes',        'Gestion des postes de sécurité'),
    ('planning.manage',  'Gérer le planning',       'Création et modification du planning'),
    ('patrols.manage',   'Gérer les patrouilles',   'Rondes et checkpoints'),
    ('tracking.view',    'Voir le suivi GPS',       'Géolocalisation'),
    ('incidents.manage', 'Gérer les incidents',     'Déclaration et traitement'),
    ('inventory.manage', 'Gérer le stock',          'Matériel et mouvements'),
    ('clients.manage',   'Gérer les clients',       'Gestion des clients et contacts'),
    ('contracts.manage', 'Gérer les contrats',      'Gestion des contrats et prestations'),
    ('invoices.manage',  'Gérer les factures',      'Facturation et paiements'),
    ('payments.manage',  'Gérer les paiements',     'Encaissements et allocations'),
    ('reports.view',     'Voir les rapports',       'Rapports et statistiques')
ON CONFLICT (code) DO NOTHING;

-- ------------------------------------------------------------
-- 17.2. Rôles plateforme (company_id NULL)
-- ------------------------------------------------------------

-- Garde NOT EXISTS : la clé unique de role est (company_id, name) et
-- les NULL ne déclenchent pas de conflit, donc un simple
-- ON CONFLICT DO NOTHING ne suffirait pas lors d'un rejeu.
INSERT INTO role (company_id, name, description)
SELECT NULL, 'ADMIN', 'Administrateur plateforme - tous les droits'
WHERE NOT EXISTS (SELECT 1 FROM role WHERE company_id IS NULL AND name = 'ADMIN');

INSERT INTO role (company_id, name, description)
SELECT NULL, 'USER', 'Utilisateur standard - consultation'
WHERE NOT EXISTS (SELECT 1 FROM role WHERE company_id IS NULL AND name = 'USER');

INSERT INTO role (company_id, name, description)
SELECT NULL, 'INVENTORY', 'Utilisateur chargé de l''inventaire'
WHERE NOT EXISTS (SELECT 1 FROM role WHERE company_id IS NULL AND name = 'INVENTORY');

-- ------------------------------------------------------------
-- 17.3. Droits par défaut des rôles plateforme
-- ------------------------------------------------------------

-- ADMIN : toutes les permissions.
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE r.company_id IS NULL
  AND r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

-- USER : consultation seule.
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.code = 'dashboard.view'
WHERE r.company_id IS NULL
  AND r.name = 'USER'
ON CONFLICT DO NOTHING;

-- INVENTORY : gestion du matériel.
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.code = 'inventory.manage'
WHERE r.company_id IS NULL
  AND r.name = 'INVENTORY'
ON CONFLICT DO NOTHING;
