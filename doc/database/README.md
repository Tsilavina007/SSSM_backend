# Base de données SSSM

Le schéma SQL est organisé par domaine fonctionnel dans le dossier `schema/`.

## Structure

```text
database/
├── sssm_schema2_v01.sql
├── mock_data/
│   └── sssm_mock_data.sql
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
    └── 18_lot1_security.sql
```

## Migration Lot 1

`18_lot1_security.sql` est une migration **additive** appliquée après
`17_seed_permissions.sql` :

- ajout de `company_id` nullable à `stock_movement`, `item_assignment` et
  `inventory_count_scan` ;
- backfill de `app_user.company_id` et des nouvelles colonnes à partir de
  l'entreprise unique de démonstration ;
- index de filtrage par entreprise ;
- tables `security.refresh_token` et `security.revoked_access_token` ;
- permissions `users.manage` et `roles.manage`, affectées au rôle
  d'administration de l'entreprise.

La migration ne contient ni `DROP`, ni `DELETE`, ni `TRUNCATE`, ni ajout de
`NOT NULL`, ni contrainte composite destructive. Elle est incluse dans
`sssm_schema2_v01.sql` pour les installations neuves et peut être appliquée
seule sur une base existante :

```bash
cd sssm_backend
psql -d sssm_db -f doc/database/schema/18_lot1_security.sql -U your_user
```

Après application, les comptes non-plateforme existants sont rattachés à
l'entreprise de démonstration. Un administrateur plateforme conserve
`company_id = NULL`.

## Exécution complète

Les scripts SQL sont exécutés depuis la racine du projet `sssm_backend` :

```bash
cd sssm_backend
psql -d sssm_db -f doc/database/sssm_schema2_v01.sql -U your_user
```

Les fichiers sont exécutés dans l'ordre numérique afin de respecter les
dépendances entre les types enum, les tables, les index, les triggers et les
données initiales.

## Exécution d'une section

Une section peut être exécutée séparément uniquement si ses dépendances ont
déjà été créées. Par exemple :

```bash
psql -d sssm_db -f doc/database/schema/03_authentication.sql
```

Pour une installation complète, utiliser le fichier principal plutôt qu'un
fichier individuel.

## Données de démonstration

Après avoir créé le schéma, charger les données de démonstration avec :

```bash
cd sssm_backend
psql -d sssm_db -f doc/database/mock_data/sssm_mock_data.sql -U your_user
```

Les identifiants sont générés automatiquement par PostgreSQL. Les références
entre les tables sont conservées avec les variables `psql` alimentées par les
clauses `RETURNING id`. Le script doit être exécuté après
`sssm_schema2_v01.sql` et est destiné à une base de démonstration vide.

Pour repartir d'une base de démonstration propre :

```bash
psql -d sssm_db -f doc/database/mock_data/reset_data.sql -U your_user
psql -d sssm_db -f doc/database/mock_data/sssm_mock_data.sql -U your_user
```
