# Base de données SSSM

Le schéma SQL est organisé par domaine fonctionnel dans le dossier `schema/`.

## Structure

```text
database/
├── sssm_schema2_v01.sql
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
    └── 17_seed_permissions.sql
```

## Exécution complète

Le fichier principal utilise les commandes `\i` de `psql` et doit être exécuté
depuis le dossier `doc/database` :

```bash
psql -d sssm_db -f sssm_schema2_v01.sql
```

Les fichiers sont exécutés dans l'ordre numérique afin de respecter les
dépendances entre les types enum, les tables, les index, les triggers et les
données initiales.

## Exécution d'une section

Une section peut être exécutée séparément uniquement si ses dépendances ont
déjà été créées. Par exemple :

```bash
psql -d sssm_db -f schema/03_authentication.sql
```

Pour une installation complète, utiliser le fichier principal plutôt qu'un
fichier individuel.
