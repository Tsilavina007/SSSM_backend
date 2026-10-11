-- 17. DONNÉES INITIALES : permissions de base
-- ============================================================

INSERT INTO permission(code, name, description) VALUES
('dashboard.view', 'Voir le tableau de bord', 'Accès au dashboard'),
('users.manage', 'Gérer les utilisateurs', 'Création/modification des comptes'),
('roles.manage', 'Gérer les rôles', 'Gestion RBAC'),
('employees.view', 'Voir les agents', 'Consultation du personnel'),
('employees.manage', 'Gérer les agents', 'Gestion du personnel'),
('sites.manage', 'Gérer les sites', 'Gestion des sites clients'),
('posts.manage', 'Gérer les postes', 'Gestion des postes de sécurité'),
('planning.manage', 'Gérer le planning', 'Création et modification du planning'),
('patrols.manage', 'Gérer les patrouilles', 'Rondes et checkpoints'),
('tracking.view', 'Voir le suivi GPS', 'Géolocalisation'),
('incidents.manage', 'Gérer les incidents', 'Déclaration et traitement'),
('inventory.manage', 'Gérer le stock', 'Matériel et mouvements'),
('clients.manage', 'Gérer les clients', 'CRM clients'),
('contracts.manage', 'Gérer les contrats', 'Contrats et prestations'),
('invoices.manage', 'Gérer les factures', 'Facturation'),
('payments.manage', 'Gérer les paiements', 'Encaissements'),
('reports.view', 'Voir les rapports', 'Rapports et statistiques')
ON CONFLICT (code) DO NOTHING;

