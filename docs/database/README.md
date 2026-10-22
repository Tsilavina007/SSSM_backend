# Base de données SSSM

Le schéma SQL est organisé par domaine fonctionnel dans le dossier `schema/`.
Les données de démonstration suivent la même numérotation dans `mock_data/`.

## Structure

```text
database/
├── sssm_schema2_v01.sql
├── mock_data/
│   ├── sssm_mock_data.sql              orchestrateur (\ir des fichiers)
│   ├── reset_data.sql                  réinitialisation de la démo
│   ├── 02_organisation.sql
│   ├── 03_authentication.sql
│   ├── 04_personnel.sql
│   ├── 05_clients.sql
│   ├── 06_sites.sql
│   ├── 07_contracts.sql
│   ├── 08_planning.sql
│   ├── 09_patrols.sql
│   ├── 10_incidents.sql
│   ├── 11_inventory.sql
│   ├── 12_billing.sql
│   ├── 13_documents_communications.sql
│   └── 14_audit.sql
├── README.md
└── schema/
    ├── 00_init.sql
    ├── 01_types.sql
    ├── 02_organisation.sql
    ├── 03_authentication.sql
    ├── 04_personnel.sql
    ├── 05_clients.sql
    ├── 06_sites.sql
    ├── 07_contracts.sql
    ├── 08_planning.sql
    ├── 09_patrols.sql
    ├── 10_incidents.sql
    ├── 11_inventory.sql
    ├── 12_billing.sql
    ├── 13_documents_communications.sql
    ├── 14_audit.sql
    ├── 15_indexes.sql
    ├── 16_triggers.sql
    ├── 17_seed_permissions.sql
    ├── 18_lot1_security.sql
    └── 19_lot3_stock.sql
```

## Données initiales (seed)

`17_seed_permissions.sql` charge le **référentiel** nécessaire dès la
création de la base :

- catalogue des permissions (`dashboard.view`, `users.manage`,
  `roles.manage`, `inventory.manage`, `employees.manage`, `sites.manage`,
  `posts.manage`, `planning.manage`, `patrols.manage`, `tracking.view`,
  `incidents.manage`, `clients.manage`, `contracts.manage`,
  `invoices.manage`, `payments.manage`, `reports.view`, …) ;
- rôles plateforme (aucune entreprise liée) : `ADMIN`, `USER`,
  `INVENTORY` ;
- droits par défaut : `ADMIN` reçoit toutes les permissions, `USER`
  la consultation (`dashboard.view`) et `INVENTORY` la gestion du stock
  (`inventory.manage`).

Le script est idempotent (`ON CONFLICT DO NOTHING`) : il peut être
rejoué sur une base existante sans effet de bord. Les données de
démonstration ne doivent **jamais** insérer de permissions : elles les
réutilisent via `SELECT ... FROM permission WHERE code = '...'`.
L'ancien fichier `mock_data/sssm_role_data.sql` est supprimé : le seed
des rôles plateforme est désormais centralisé dans `17`.

## Migration Lot 1

`18_lot1_security.sql` est une migration **additive** appliquée après
`17_seed_permissions.sql` :

- ajout de `company_id` nullable à `stock_movement`, `item_assignment` et
  `inventory_count_scan` ;
- backfill de `app_user.company_id` et des nouvelles colonnes à partir de
  l'entreprise unique de démonstration ;
- index de filtrage par entreprise ;
- tables `security.refresh_token` et `security.revoked_access_token` ;
- permissions `users.manage` et `roles.manage` désormais chargées par le
  seed `17`

La migration ne contient ni `DROP`, ni `DELETE`, ni `TRUNCATE`, ni ajout de
`NOT NULL`, ni contrainte composite destructive. Elle est incluse dans
`sssm_schema2_v01.sql` pour les installations neuves et peut être appliquée
seule sur une base existante :

```bash
cd sssm_backend
psql -d sssm_db -f docs/database/schema/18_lot1_security.sql -U your_user
```

Après application, les comptes non-plateforme existants sont rattachés à
l'entreprise de démonstration. Un administrateur plateforme conserve
`company_id = NULL`.

## Migration Lot 3

`19_lot3_stock.sql` est une migration **additive** appliquée après
`18_lot1_security.sql` :

- création de `security.stock_balance`, avec clé unique
  `(company_id, warehouse_id, item_type_id)` et colonne `version` ;
- ajout de `expected_quantity`, `counted_quantity` et `difference` à
  `inventory_count_scan` ;
- backfill initial des soldes depuis les articles `AVAILABLE`, non
  archivés et rattachés à un entrepôt ;
- index de lecture pour le dashboard et les alertes.

Les colonnes de comptage restent nullables : les scans historiques ne
sont ni supprimés ni réécrits. Le backfill utilise `ON CONFLICT DO
NOTHING` et n'écrase donc pas un solde déjà alimenté par des mouvements
ou des comptages postérieurs à la bascule. Le script ne contient ni
`DROP`, ni `DELETE`, ni `TRUNCATE`, ni ajout de `NOT NULL` sur une
colonne historique.

Le fichier principal `sssm_schema2_v01.sql` valide le schéma de base,
puis exécute cette migration dans sa propre transaction. Sur une base
existante, l'application se fait après l'arrêt de l'application ou
pendant une fenêtre de maintenance :

```bash
cd sssm_backend
psql -d sssm_db -f docs/database/schema/19_lot3_stock.sql -U your_user
```

## Migration Lot 4

`20_lot4_assignments.sql` est une migration **additive** appliquée après
`19_lot3_stock.sql` :

- ajout de `source_warehouse_id` sur `item_assignment` (FK `warehouse`,
  `ON DELETE RESTRICT`) : entrepôt d'origine, restauré au retour ou au
  void ;
- ajout de `voided_at`, `voided_by` et `void_reason` sur `item_assignment` :
  trace d'archivage logique d'une affectation ;
- ajout de `archived_by` sur `item_category`, `item_type` et `warehouse` :
  acteur de l'archivage logique des données maîtres ;
- contraintes `item_assignment_voided_requires_return` (une ligne voidée est
  close) et `item_assignment_voided_requires_reason` (une ligne voidée est
  motivée) ;
- index `idx_item_assignment_source_warehouse`, `idx_item_assignment_voided_at`
  (partiel) et `idx_item_assignment_company_active` (partiel).

Les colonnes restent nullables : les affectations historiques ne sont pas
devinées et les lignes existantes restent valides. Le script ne contient ni
`DROP`, ni `DELETE`, ni `TRUNCATE`, ni ajout de `NOT NULL` sur une colonne
historique. L'index unique `uq_item_assignment_active(item_id) WHERE
returned_at IS NULL` reste valide : un void renseigne `returned_at` et ne
crée donc pas de faux conflit.

```bash
cd sssm_backend
psql -d sssm_db -f docs/database/schema/20_lot4_assignments.sql -U your_user
```

## Exécution complète

Les scripts SQL sont exécutés depuis la racine du projet `sssm_backend` :

```bash
cd sssm_backend
psql -d sssm_db -f docs/database/sssm_schema2_v01.sql -U your_user
```

Les fichiers sont exécutés dans l'ordre numérique afin de respecter les
dépendances entre les types enum, les tables, les index, les triggers et
les données initiales. Le fichier principal inclut les migrations
`18_lot1_security.sql`, `19_lot3_stock.sql` et `20_lot4_assignments.sql` ;
une installation neuve dispose donc directement de `stock_balance` et des
colonnes de cycle de vie des affectations.

## Exécution d'une section

Une section peut être exécutée séparément uniquement si ses
dépendances ont déjà été créées. Par exemple :

```bash
psql -d sssm_db -f docs/database/schema/03_authentication.sql
```

Pour une installation complète, utiliser le fichier principal plutôt qu'un
fichier individuel.

## Données de démonstration

Après avoir créé le schéma, charger les données de démonstration avec :

```bash
cd sssm_backend
psql -d sssm_db -f docs/database/mock_data/sssm_mock_data.sql -U your_user
```

`sssm_mock_data.sql` est un orchestrateur : il ouvre une transaction,
positionne le chemin de recherche puis chaîne les fichiers
`02_organisation.sql` … `14_audit.sql` via `\ir`. Chaque fichier couvre le
même domaine que son homologue numéroté dans `schema/` :

| Fichier mock                        | Contenu |
|-------------------------------------|---------|
| `02_organisation.sql`               | Entreprise SSSM Madagascar, agences (Antananarivo, Toamasina, Antsirabe) et branches |
| `03_authentication.sql`             | Comptes utilisateurs, rôles d'entreprise, habilitations réutilisant le seed de `17`, jetons de rafraîchissement et accès révoqués |
| `04_personnel.sql`                  | 14 employés (actifs, suspendu, congé, parti) et documents administratifs |
| `05_clients.sql`                    | 5 clients et leurs contacts |
| `06_sites.sql`                      | 6 sites dont un inactif, 8 postes et exigences matérielles |
| `07_contracts.sql`                  | 6 prestations dont une inactive, 7 contrats (actifs, DRAFT, EXPIRED) |
| `08_planning.sql`                   | 4 équipes, plannings des 15–18 juin (dont un annulé et un planifié), affectations, présences dérivées, absent et retard |
| `09_patrols.sql`                    | 5 routes, 13 checkpoints, 8 patrouilles (COMPLETED, IN_PROGRESS, FAILED, PLANNED), traces GPS |
| `10_incidents.sql`                  | 5 types, 7 incidents couvrant sévérité et statuts (dont un CRITICAL/OPEN assigné, un CLOSED résolu avec date) |
| `11_inventory.sql`                  | 4 catégories, 6 types, 13 matériels à QR code (statuts variés), entrepôts, opérations, mouvements (IN/OUT/TRANSFER/RETURN/ADJUSTMENT/DESTRUCTION), seuils, 2 comptages dont un en cours avec écarts, affectations (dont un retour) |
| `12_billing.sql`                    | 3 taux de TVA, 9 factures (ISSUED, PAID, OVERDUE, DRAFT, PARTIALLY_PAID), 6 paiements (CONFIRMED, PENDING, FAILED) et allocations |
| `13_documents_communications.sql`   | Documents (CONTRACT, INVOICE, RECEIPT, REPORT, CERTIFICATE), notifications (IN_APP, EMAIL, SMS, PUSH ; SENT, PENDING, READ, FAILED), journal e-mails (SENT, FAILED, PENDING) |
| `14_audit.sql`                       | Journal d'audit de démonstration (CREATE, UPDATE, DELETE) |

Les identifiants sont générés automatiquement par PostgreSQL. Les références
entre les tables — et même entre les fichiers — sont conservées avec les
variables `psql` alimentées par les clauses `RETURNING id \gset` : exécuter
la démo via `sssm_mock_data.sql` garantit que les références existent.

Le script doit être exécuté après `sssm_schema2_v01.sql` et est destiné à
une base de démonstration vide.

Pour repartir d'une base de démonstration propre :

```bash
psql -d sssm_db -f docs/database/mock_data/reset_data.sql -U your_user
psql -d sssm_db -f docs/database/mock_data/sssm_mock_data.sql -U your_user
```

`reset_data.sql` tronque toutes les tables du schéma `security`
**sauf** `role` et `permission` puis supprime les rôles liés à une
entreprise (`company_id IS NOT NULL`) : les données initiales chargées
par `17_seed_permissions.sql` (catalogue des permissions et rôles
plateforme) sont ainsi conservées d'un cycle de réinitialisation à
l'autre.
