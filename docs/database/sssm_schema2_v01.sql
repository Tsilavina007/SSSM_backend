\encoding UTF8
\set ON_ERROR_STOP on

\i docs/database/schema/00_init.sql
\i docs/database/schema/01_types.sql
\i docs/database/schema/02_organisation.sql
\i docs/database/schema/03_authentication.sql
\i docs/database/schema/04_personnel.sql
\i docs/database/schema/05_clients.sql
\i docs/database/schema/06_sites.sql
\i docs/database/schema/07_contracts.sql
\i docs/database/schema/08_planning.sql
\i docs/database/schema/09_patrols.sql
\i docs/database/schema/10_incidents.sql
\i docs/database/schema/11_inventory.sql
\i docs/database/schema/12_billing.sql
\i docs/database/schema/13_documents_communications.sql
\i docs/database/schema/14_audit.sql
\i docs/database/schema/15_indexes.sql
\i docs/database/schema/16_triggers.sql
\i docs/database/schema/17_seed_permissions.sql
\i docs/database/schema/18_lot1_security.sql

-- Le schéma de base est validé avant la migration de stock, qui gère
-- sa propre transaction et son backfill.
COMMIT;

\i docs/database/schema/19_lot3_stock.sql

\i docs/database/schema/20_lot4_assignments.sql
