\encoding UTF8
\set ON_ERROR_STOP on

\i doc/database/schema/00_init.sql
\i doc/database/schema/01_types.sql
\i doc/database/schema/02_organisation.sql
\i doc/database/schema/03_authentication.sql
\i doc/database/schema/04_personnel.sql
\i doc/database/schema/05_clients.sql
\i doc/database/schema/06_sites.sql
\i doc/database/schema/07_contracts.sql
\i doc/database/schema/08_planning.sql
\i doc/database/schema/09_patrols.sql
\i doc/database/schema/10_incidents.sql
\i doc/database/schema/11_inventory.sql
\i doc/database/schema/12_billing.sql
\i doc/database/schema/13_documents_communications.sql
\i doc/database/schema/14_audit.sql
\i doc/database/schema/15_indexes.sql
\i doc/database/schema/16_triggers.sql
\i doc/database/schema/17_seed_permissions.sql

COMMIT;
