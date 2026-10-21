\encoding UTF8
\set ON_ERROR_STOP on

-- =====================================================================
-- ORCHESTRATEUR DES DONNÉES DE DÉMONSTRATION SSSM
-- =====================================================================
-- À exécuter sur une base fraîchement créée par
-- doc/database/sssm_schema2_v01.sql (le référentiel des permissions et
-- des rôles plateforme est chargé par schema/17_seed_permissions.sql).
--
-- Chaque fichier-cl duplique la numérotation du dossier schema/ :
--
--   02_organisation.sql................entreprise, agences, branches
--   03_authentication.sql.............comptes, rôles, habilitations, jetons
--   04_personnel.sql..................employés et documents
--   05_clients.sql....................clients et contacts
--   06_sites.sql......................sites, postes et exigences
--   07_contracts.sql..................prestations, contrats
--   08_planning.sql...................équipes, plannings, affectations
--   09_patrols.sql....................rondes, checkpoints, GPS
--   10_incidents.sql..................types, incidents, actions
--   11_inventory.sql..................matériel, stocks, inventaires
--   12_billing.sql....................TVA, factures, paiements
--   13_documents_communications.sql...documents, notifications, e-mails
--   14_audit.sql......................journal d'audit
--
-- Les variables psql (gset) permettent de suivre les identifiants
-- d'un fichier à l'autre : exécuter tout le script via ce fichier
-- garant que les références existent.
-- =====================================================================
BEGIN;

SET search_path TO security, public;

\ir 02_organisation.sql
\ir 03_authentication.sql
\ir 04_personnel.sql
\ir 05_clients.sql
\ir 06_sites.sql
\ir 07_contracts.sql
\ir 08_planning.sql
\ir 09_patrols.sql
\ir 10_incidents.sql
\ir 11_inventory.sql
\ir 12_billing.sql
\ir 13_documents_communications.sql
\ir 14_audit.sql

COMMIT;
