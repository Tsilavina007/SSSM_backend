-- ATTENTION : vide TOUTES les tables du schéma "security" (base de démo uniquement).
-- À exécuter avant sssm_mock_data.sql pour repartir d'une base propre.
\encoding UTF8
\set ON_ERROR_STOP on

DO $$
DECLARE r record;
BEGIN
    FOR r IN SELECT tablename FROM pg_tables WHERE schemaname = 'security' LOOP
        EXECUTE format('TRUNCATE TABLE security.%I RESTART IDENTITY CASCADE', r.tablename);
    END LOOP;
END $$;
