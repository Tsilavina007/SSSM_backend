-- ATTENTION : vide les tables de démonstration du schéma "security"
-- (base de démo uniquement). À exécuter avant sssm_mock_data.sql
-- pour repartir d'une base propre.
--
-- Le référentiel des données initiales (données nécessaires à
-- l'application) est préservé et reste chargé par
-- doc/database/schema/17_seed_permissions.sql (idempotent) :
--   - security.permission : catalogue des permissions (non tronqué) ;
--   - security.role       : les rôles plateforme (company_id NULL :
--                           ADMIN, USER, INVENTORY) sont conservés ;
--                           seuls les rôles rattachés à une entreprise
--                           (company_id NOT NULL) sont supprimés.
--
-- Détail important : role.company_id référence company(id) ON DELETE
-- CASCADE, donc un simple TRUNCATE company CASCADE viderait aussi la
-- table role et détruirait les rôles plateforme. C'est pourquoi
-- company est exclu de la boucle TRUNCATE et vidé par DELETE, qui ne
-- supprime que les rôles rattachés à l'entreprise.
\encoding UTF8
\set ON_ERROR_STOP on

DO $$
DECLARE r record;
BEGIN
    FOR r IN SELECT tablename
             FROM pg_tables
             WHERE schemaname = 'security'
               AND tablename NOT IN ('role', 'permission', 'company')
    LOOP
        EXECUTE format('TRUNCATE TABLE security.%I RESTART IDENTITY CASCADE', r.tablename);
    END LOOP;
END $$;

-- Suppression de l'entreprise de démonstration : en cascade, elle
-- emporte les rôles d'entreprise (company_id NOT NULL) mais laisse
-- intacts les rôles plateforme (company_id NULL) et le catalogues des
-- permissions déjà présent.
DELETE FROM security.company;
