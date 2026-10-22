# 03 — Audit du modèle de données PostgreSQL

**Objectif :** vérifier la structure réelle des données, les contraintes, les index et les triggers, puis confronter ce modèle au code Java et aux scripts SQL. Les anomalies métier qui en découlent sont analysées dans les rapports 04 à 09.

---

## 1. Organisation du schéma

- Base contrôlée : `sssm_db`.
- Schéma applicatif : `security`, créé par `00_init.sql` avec `pgcrypto`.
- Toutes les tables applicatives sont dans `security`.
- Les scripts sont exécutés dans l'ordre `00_init.sql` → `17_seed_permissions.sql`.
- Aucun outil de migration versionnée (Flyway, Liquibase) n'est présent.

Les tables d'inventaire sont définies dans `docs/database/schema/11_inventory.sql`, les index dans `15_indexes.sql`, les triggers dans `16_triggers.sql`.

---

## 2. Tables d'inventaire

### 2.1. `item_category`

| Colonne | Type | Contrainte |
|---|---|---|
| `id` | UUID | PK, `gen_random_uuid()` |
| `company_id` | UUID | NOT NULL, FK `company(id)` ON DELETE CASCADE |
| `name` | VARCHAR(150) | NOT NULL |
| `description` | TEXT | nullable |
| `created_at` | TIMESTAMPTZ | NOT NULL, défaut `now()` |
| `updated_at` | TIMESTAMPTZ | NOT NULL, défaut `now()` |
| `archived_at` | TIMESTAMPTZ | nullable |

Contraintes : `UNIQUE(company_id, name)`.

### 2.2. `item_type`

| Colonne | Type | Contrainte |
|---|---|---|
| `id` | UUID | PK |
| `company_id` | UUID | NOT NULL, FK `company(id)` CASCADE |
| `category_id` | UUID | FK `item_category(id)` ON DELETE SET NULL |
| `code` | VARCHAR(100) | NOT NULL |
| `name` | VARCHAR(180) | NOT NULL |
| `description` | TEXT | nullable |
| `unit` | VARCHAR(50) | NOT NULL, défaut `'piece'` |
| `quantity_total` | NUMERIC(12,2) | NOT NULL, défaut `0`, CHECK `>= 0` |
| `minimum_stock` | NUMERIC(12,2) | NOT NULL, défaut `0`, CHECK `>= 0` |
| `status` | `item_status` | NOT NULL, défaut `AVAILABLE` |
| `created_at`, `updated_at` | TIMESTAMPTZ | NOT NULL, défaut `now()` |
| `archived_at` | TIMESTAMPTZ | nullable |

Contraintes : `UNIQUE(company_id, code)`.

### 2.3. `warehouse`

| Colonne | Type | Contrainte |
|---|---|---|
| `id` | UUID | PK |
| `company_id` | UUID | NOT NULL, FK `company(id)` CASCADE |
| `branch_id` | UUID | FK `branch(id)` ON DELETE SET NULL |
| `name` | VARCHAR(150) | NOT NULL |
| `address` | TEXT | nullable |
| `active` | BOOLEAN | NOT NULL, défaut `TRUE` |
| `created_at`, `updated_at` | TIMESTAMPTZ | NOT NULL, défaut `now()` |
| `archived_at` | TIMESTAMPTZ | nullable |

Aucune contrainte d'unicité sur `(company_id, name)` : deux entrepôts de même nom sont possibles.

### 2.4. `item`

| Colonne | Type | Contrainte |
|---|---|---|
| `id` | UUID | PK |
| `company_id` | UUID | NOT NULL, FK `company(id)` CASCADE |
| `category_id` | UUID | FK `item_category(id)` SET NULL |
| `item_type_id` | UUID | NOT NULL, FK `item_type(id)` RESTRICT |
| `qr_code` | VARCHAR(100) | NOT NULL |
| `description` | TEXT | nullable |
| `status` | `item_status` | NOT NULL, défaut `AVAILABLE` |
| `current_warehouse_id` | UUID | FK `warehouse(id)` RESTRICT |
| `created_at`, `updated_at` | TIMESTAMPTZ | NOT NULL, défaut `now()` |
| `archived_at` | TIMESTAMPTZ | nullable |
| `archived_by` | UUID | nullable |
| `last_inventory_at` | TIMESTAMPTZ | nullable |

Contraintes : `UNIQUE(company_id, qr_code)`.

### 2.5. `inventory_operation`

| Colonne | Type | Contrainte |
|---|---|---|
| `id` | UUID | PK |
| `company_id` | UUID | NOT NULL, FK `company(id)` RESTRICT |
| `operation_type` | VARCHAR(30) | NOT NULL, CHECK dans `RECEIPT`, `ISSUE`, `TRANSFER`, `ASSIGNMENT`, `RETURN`, `ADJUSTMENT` |
| `status` | VARCHAR(20) | NOT NULL, défaut `PENDING`, CHECK dans `PENDING`, `COMPLETED`, `CANCELLED`, `FAILED` |
| `reference` | VARCHAR(150) | nullable |
| `idempotency_key` | VARCHAR(150) | nullable |
| `source_warehouse_id` | UUID | FK `warehouse(id)` RESTRICT |
| `destination_warehouse_id` | UUID | FK `warehouse(id)` RESTRICT |
| `employee_id` | UUID | FK `employee(id)` RESTRICT |
| `post_id` | UUID | FK `security_post(id)` RESTRICT |
| `performed_by` | UUID | nullable |
| `notes` | TEXT | nullable |
| `created_at` | TIMESTAMPTZ | NOT NULL, défaut `now()` |
| `completed_at` | TIMESTAMPTZ | nullable |

Contrainte : `source_warehouse_id` et `destination_warehouse_id` doivent être différents s'ils sont tous deux renseignés.

### 2.6. `stock_movement`

| Colonne | Type | Contrainte |
|---|---|---|
| `id` | UUID | PK |
| `warehouse_id` | UUID | NOT NULL, FK `warehouse(id)` RESTRICT |
| `item_id` | UUID | NOT NULL, FK `item(id)` RESTRICT |
| `employee_id` | UUID | FK `employee(id)` SET NULL |
| `movement_type` | `movement_type` | NOT NULL |
| `reference` | VARCHAR(150) | nullable |
| `movement_date` | TIMESTAMPTZ | NOT NULL, défaut `now()` |
| `notes` | TEXT | nullable |
| `source_warehouse_id` | UUID | FK `warehouse(id)` RESTRICT |
| `destination_warehouse_id` | UUID | FK `warehouse(id)` RESTRICT |
| `operation_id` | UUID | FK `inventory_operation(id)` RESTRICT |
| `quantity` | NUMERIC(12,2) | NOT NULL, défaut `1`, CHECK `> 0` |
| `performed_by` | UUID | nullable |
| `created_at` | TIMESTAMPTZ | NOT NULL, défaut `now()` |

Contrainte : source et destination différentes si toutes deux renseignées.

### 2.7. `inventory_operation_item`

| Colonne | Type | Contrainte |
|---|---|---|
| `id` | UUID | PK |
| `operation_id` | UUID | NOT NULL, FK `inventory_operation(id)` RESTRICT |
| `item_id` | UUID | NOT NULL, FK `item(id)` RESTRICT |
| `movement_id` | UUID | FK `stock_movement(id)` RESTRICT |
| `quantity` | NUMERIC(12,2) | NOT NULL, défaut `1`, CHECK `> 0` |
| `condition_before`, `condition_after` | TEXT | nullable |
| `scanned_at` | TIMESTAMPTZ | NOT NULL, défaut `now()` |
| `notes` | TEXT | nullable |

Contrainte : `UNIQUE(operation_id, item_id)`.

### 2.8. `item_assignment`

| Colonne | Type | Contrainte |
|---|---|---|
| `id` | UUID | PK |
| `item_id` | UUID | NOT NULL, FK `item(id)` RESTRICT |
| `employee_id` | UUID | NOT NULL, FK `employee(id)` RESTRICT |
| `post_id` | UUID | NOT NULL, FK `security_post(id)` RESTRICT |
| `assigned_at` | TIMESTAMPTZ | NOT NULL, défaut `now()` |
| `returned_at` | TIMESTAMPTZ | nullable |
| `condition_on_assignment` | TEXT | nullable |
| `condition_on_return` | TEXT | nullable |
| `notes` | TEXT | nullable |
| `created_at`, `updated_at` | TIMESTAMPTZ | NOT NULL, défaut `now()` |
| `assigned_by` | UUID | nullable |
| `returned_by` | UUID | nullable |
| `expected_return_at` | TIMESTAMPTZ | nullable |

Contrainte : `returned_at IS NULL OR returned_at >= assigned_at`.

Index partiel unique : `uq_item_assignment_active ON item_assignment(item_id) WHERE returned_at IS NULL`. Il garantit qu'un article n'a qu'une affectation active.

### 2.9. `warehouse_stock_config`

| Colonne | Type | Contrainte |
|---|---|---|
| `id` | UUID | PK |
| `warehouse_id` | UUID | NOT NULL, FK `warehouse(id)` RESTRICT |
| `item_type_id` | UUID | NOT NULL, FK `item_type(id)` RESTRICT |
| `minimum_stock` | NUMERIC(12,2) | NOT NULL, défaut `0`, CHECK `>= 0` |
| `created_at`, `updated_at` | TIMESTAMPTZ | NOT NULL, défaut `now()` |

Contrainte : `UNIQUE(warehouse_id, item_type_id)`.

Cette table n'est utilisée par aucun contrôleur, service ou DAO.

### 2.10. `inventory_count`

| Colonne | Type | Contrainte |
|---|---|---|
| `id` | UUID | PK |
| `company_id` | UUID | NOT NULL, FK `company(id)` RESTRICT |
| `warehouse_id` | UUID | NOT NULL, FK `warehouse(id)` RESTRICT |
| `reference` | VARCHAR(150) | NOT NULL |
| `status` | VARCHAR(20) | NOT NULL, défaut `IN_PROGRESS`, CHECK `IN_PROGRESS`, `COMPLETED`, `CANCELLED` |
| `started_by` | UUID | nullable |
| `completed_by` | UUID | nullable |
| `started_at` | TIMESTAMPTZ | NOT NULL, défaut `now()` |
| `completed_at` | TIMESTAMPTZ | nullable |
| `notes` | TEXT | nullable |

Contraintes : `UNIQUE(company_id, reference)`, `completed_at >= started_at` si non nul.

### 2.11. `inventory_count_scan`

| Colonne | Type | Contrainte |
|---|---|---|
| `id` | UUID | PK |
| `inventory_count_id` | UUID | NOT NULL, FK `inventory_count(id)` RESTRICT |
| `item_id` | UUID | NOT NULL, FK `item(id)` RESTRICT |
| `scanned_by` | UUID | nullable |
| `scanned_at` | TIMESTAMPTZ | NOT NULL, défaut `now()` |
| `expected_warehouse_id` | UUID | FK `warehouse(id)` RESTRICT |
| `actual_warehouse_id` | UUID | FK `warehouse(id)` RESTRICT |
| `discrepancy_type` | VARCHAR(30) | CHECK `NONE`, `MISSING`, `UNEXPECTED`, `WRONG_LOCATION`, `DAMAGED` |
| `notes` | TEXT | nullable |

Contrainte : `UNIQUE(inventory_count_id, item_id)`.

---

## 3. Énumérations

Vérifiées en base et dans `01_types.sql` :

| Enum | Valeurs |
|---|---|
| `item_status` | `AVAILABLE`, `ASSIGNED`, `DAMAGED`, `LOST`, `REPAIR`, `RETIRED` |
| `movement_type` | `IN`, `OUT`, `TRANSFER`, `RETURN`, `ADJUSTMENT`, `DESTRUCTION` |
| `employee_status` | `ACTIVE`, `INACTIVE`, `SUSPENDED`, `LEFT` |
| `user_status` | `ACTIVE`, `INACTIVE`, `LOCKED` |

Les valeurs d'énumération sont conformes entre les scripts et la base contrôlée.

---

## 4. Index et intégrité

### 4.1. Index vérifiés

| Table | Index | Définition |
|---|---|---|
| `item` | `item_company_id_qr_code_key` | UNIQUE `(company_id, qr_code)` |
| `item` | `idx_item_company_status` | `(company_id, status) WHERE archived_at IS NULL` |
| `item` | `idx_item_current_warehouse` | `(current_warehouse_id)` |
| `item` | `idx_item_qr_code` | `(qr_code)` |
| `item` | `idx_item_type` | `(item_type_id)` |
| `item_type` | `item_type_company_id_code_key` | UNIQUE `(company_id, code)` |
| `item_type` | `idx_item_type_company_status` | `(company_id, status) WHERE archived_at IS NULL` |
| `item_assignment` | `uq_item_assignment_active` | UNIQUE `(item_id) WHERE returned_at IS NULL` |
| `item_assignment` | `idx_item_assignment_employee_active` | `(employee_id, assigned_at DESC) WHERE returned_at IS NULL` |
| `stock_movement` | `idx_stock_movement_item_date` | `(item_id, movement_date DESC)` |
| `stock_movement` | `idx_stock_movement_warehouse_date` | `(warehouse_id, movement_date DESC)` |
| `inventory_count` | `inventory_count_company_id_reference_key` | UNIQUE `(company_id, reference)` |
| `inventory_count_scan` | `inventory_count_scan_inventory_count_id_item_id_key` | UNIQUE `(inventory_count_id, item_id)` |

### 4.2. Ce que les contraintes garantissent

- Un QR Code est unique **par entreprise**, pas globalement.
- Un code de type est unique par entreprise.
- Une catégorie a un nom unique par entreprise.
- Un article n'a qu'une affectation active à la fois.
- Un article ne peut pas être scanné deux fois dans le même comptage.
- Les quantités de mouvement et de ligne d'opération sont strictement positives.
- Les dates de retour et de clôture ne peuvent pas précéder leur début.

### 4.3. Ce que les contraintes ne garantissent pas

- **Cohérence d'entreprise** : `item.company_id` peut différer de `item_type.company_id`, `warehouse.company_id`, `employee.company_id` ou `security_post` de l'employé. Aucune FK composite ni contrainte ne l'interdit.
- **Cohérence entre `item.status` et l'affectation active** : la base peut avoir un article `AVAILABLE` avec une affectation active, ou `ASSIGNED` sans affectation.
- **Cohérence entre `stock_movement` et l'état de l'article** : rien ne lie le dernier mouvement à `item.status` ou `item.current_warehouse_id`.
- **Cohérence entre `inventory_count` et son entrepôt** : `company_id` et `warehouse_id` peuvent désigner deux entreprises différentes.
- **Unicité du nom d'entrepôt** : aucune contrainte.
- **Absence de doublons d'opération** : `idempotency_key` n'est pas unique.
- **Immuabilité du journal** : aucune contrainte n'empêche `UPDATE` ou `DELETE` sur `stock_movement`.

---

## 5. Triggers `updated_at`

Vérification en base :

| Table | Trigger `updated_at` | Constat |
|---|---|---|
| `item` | `trg_item_updated_at` | Présent |
| `item_type` | `trg_item_type_updated_at` | Présent |
| `item_category` | — | **Absent** |
| `warehouse` | — | **Absent** |
| `item_assignment` | — | **Absent** |
| `company`, `agency`, `branch`, `client`, `contract`, `employee`, `incident`, `invoice`, `site`, `app_user` | triggers présents | Présents |

Conséquence : `updated_at` ne se met pas à jour automatiquement pour les catégories, les entrepôts et les affectations. Les mises à jour via `PATCH` laissent une date obsolète, ce qui fausse l'audit et les synchronisations frontend.

---

## 6. Vérifications en lecture seule sur la base

Les contrôles suivants ont été exécutés sans modification :

| Contrôle | Résultat |
|---|---|
| Entreprises | 1 |
| Articles | 10 |
| Mouvements | 8 |
| Affectations actives (`returned_at IS NULL`) | 5 |
| Mouvements avec `quantity <> 1` | 0 |
| Mouvements avec `source_warehouse_id` renseigné | 0 |
| Mouvements avec `destination_warehouse_id` renseigné | 0 |
| Mouvements avec `performed_by` renseigné | 0 |
| Affectations avec `assigned_by` renseigné | 0 |
| Affectations avec `returned_by` renseigné | 0 |
| Articles avec `last_inventory_at` renseigné | 0 |
| Tables/colonnes/contraintes inspectées | Conformes aux scripts `11_inventory.sql` et `15_indexes.sql` |
| Enums `item_status`, `movement_type` | Conformes |
| Index `uq_item_assignment_active` | Présent |
| Triggers `updated_at` de `item_category`, `warehouse`, `item_assignment` | Absents |

La base de démonstration est mono-entreprise, ce qui ne permet pas de vérifier l'isolation multi-entreprise en exécution. Les conclusions de tenancy sont donc statiques.

---

## 7. Écarts entre le schéma et le code

| Écart | Preuve | Conséquence |
|---|---|---|
| `item_type.unit`, `quantity_total`, `minimum_stock` ont des défauts SQL, mais le DAO insère `NULL` explicite | `ItemTypeDAOImpl.createItemType` | 500 `23502` si les champs sont omis |
| `employee.status` a un défaut SQL, mais le DAO insère `NULL` explicite | `EmployeeDAOImpl.create` | 500 `23502` si le statut est omis |
| `inventory_operation.operation_type` est NOT NULL sans défaut, mais la route ne l'injecte pas | `OperationController.save` | 500 `23502` si le corps l'omet |
| `stock_movement.quantity` a un défaut 1, mais aucun DTO ne l'expose | `StockMovementCreateRequest` | Tous les mouvements valent 1 |
| `stock_movement.source_warehouse_id`, `destination_warehouse_id`, `operation_id`, `performed_by` existent mais ne sont jamais écrits | `StockMovementDAOImpl.createStockMovement` | Journal incomplet |
| `item_assignment.assigned_by`, `returned_by`, `expected_return_at` existent mais ne sont pas exposés par le module typé | `ItemAssignmentCreateRequest` | Acteurs et échéance absents |
| `item.last_inventory_at` n'est jamais mis à jour | `InventoryCountService.close` | Aucun historique de comptage par article |
| `archived_at`/`archived_by` ne sont jamais écrits par l'API | Aucun endpoint d'archive | Suppression physique ou inutilisée |
| `warehouse_stock_config` n'est jamais lue | Aucun DAO | Seuils par entrepôt inopérants |
| `inventory_count.completed_at` n'est pas renseigné par `close` sans corps | `InventoryCountService.close` | Clôture sans date de fin |
| `inventory_operation_item` n'est jamais alimentée | `OperationService` | Opérations sans détail |

---

## 8. Modélisation du stock : le problème central

Le schéma contient trois représentations potentielles du stock :

1. `item_type.quantity_total` : un agrégat **manuel** par type.
2. `stock_movement.quantity` : un journal de mouvements, mais toujours à `1` via l'API.
3. `warehouse_stock_config.minimum_stock` : un seuil par entrepôt et type, **inutilisé**.

Il manque une quatrième représentation, celle qui serait réellement exploitable : un **solde de stock par entrepôt et par type**, calculé à partir des mouvements, ou une table de soldes maintenue transactionnellement.

En l'état :

- on ne peut pas répondre à « combien d'articles de type X sont dans l'entrepôt Y ? » ;
- on ne peut pas répondre à « quel est le stock réel disponible ? » ;
- on ne peut pas détecter un stock négatif ;
- on ne peut pas réconcilier un comptage physique avec un stock théorique.

Le détail fonctionnel est dans le rapport 05.

---

## 9. Conclusion

Le schéma est **globalement propre** : UUID, timestamps, clés étrangères, index, contraintes de base. Il est **incomplet pour le métier** : pas de tenancy contraignante, pas de solde de stock, pas d'immabilité du journal, pas de cohérence d'entreprise, triggers manquants.

Les scripts SQL et la base locale concordent pour les tables inspectées. Les problèmes ne viennent donc pas d'un décalage de déploiement, mais de choix de modèle et de code qui ne les exploite pas.
