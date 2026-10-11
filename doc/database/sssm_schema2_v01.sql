-- SSSM database schema entry point.
-- Execute with psql from the doc/database directory:
--   psql -d sssm_db -f sssm_schema2_v01.sql

\i schema/00_init.sql
\i schema/01_types.sql
\i schema/02_organisation.sql
\i schema/03_authentication.sql
\i schema/04_personnel.sql
\i schema/05_clients.sql
\i schema/06_sites.sql
\i schema/07_contracts.sql
\i schema/08_planning.sql
\i schema/09_patrols.sql
\i schema/10_incidents.sql
\i schema/11_inventory.sql
\i schema/12_billing.sql
\i schema/13_documents_communications.sql
\i schema/14_audit.sql
\i schema/15_indexes.sql
\i schema/16_triggers.sql
\i schema/17_seed_permissions.sql

COMMIT;
