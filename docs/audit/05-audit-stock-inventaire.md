# 05 — Audit du stock et des inventaires

**Objectif :** déterminer si le backend permet de connaître, de mouvementer et de réconcilier un stock réel. Ce rapport traite des quantités, des soldes, des opérations, des comptages, du tableau de bord et des rapports. Les règles détaillées des mouvements sont dans le rapport 07 ; les affectations dans le rapport 08.

---

## 1. Réponse courte

**Non, le backend ne gère pas un stock fiable.**

Il sait déplacer un article d'un état à un autre et d'un entrepôt à un autre. Il ne sait pas :

- connaître la quantité disponible d'un type de matériel ;
- connaître la quantité par entrepôt ;
- déduire ou ajouter une quantité à partir d'un mouvement ;
- réconcilier un comptage physique avec un stock théorique ;
- garantir qu'un mouvement et l'état de l'article sont enregistrés ensemble.

Le stock repose sur un champ déclaratif, `item_type.quantity_total`, saisi manuellement et jamais recalculé.

---

## 2. Les trois représentations du stock

| Représentation | Emplacement | Alimentée par | Utilisée par | Fiabilité |
|---|---|---|---|---|
| Quantité agrégée | `item_type.quantity_total` | `PATCH /item-types/{id}` | dashboard low-stock | **Faible** : manuelle |
| Journal de mouvements | `stock_movement` | `POST /movements` | rapport mouvements | **Partielle** : quantité fixe, champs incomplets |
| Seuil par entrepôt | `warehouse_stock_config` | aucun code | aucun code | **Inutilisée** |

Il manque une quatrième représentation : un **solde par entrepôt et par type**, ou par article, calculé à partir des mouvements et protégé par transaction.

---

## 3. Quantité agrégée `item_type.quantity_total`

### 3.1. Comportement

- Colonne `NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (quantity_total >= 0)`.
- Modifiable par `PATCH /api/inventory/item-types/{itemTypeId}`.
- Affichée dans les réponses de type et dans le dashboard low-stock.
- Jamais mise à jour par un mouvement, une affectation, une opération ou un comptage.

### 3.2. Conséquences

- Un mouvement `IN` ne l'incrémente pas.
- Un mouvement `OUT` ne la décrémente pas.
- Une affectation ne la décrémente pas.
- Une destruction ne la décrémente pas.
- Un comptage ne la corrige pas.
- Un client peut la modifier directement, sans lien avec les articles réellement présents.

### 3.3. Confusion de granularité

`quantity_total` est porté par le **type** de matériel. Les articles, eux, sont des lignes individuelles dans `item`, avec un statut et un entrepôt. Il n'existe aucune règle qui maintient la cohérence entre :

- le nombre d'articles `item` de ce type ;
- le nombre d'articles disponibles ;
- le nombre d'articles affectés ;
- `quantity_total`.

Le champ peut donc afficher 100 alors qu'il existe 3 articles, ou 0 alors qu'il en existe 10.

---

## 4. Journal de mouvements

### 4.1. Ce qui est enregistré

`stock_movement` contient :

- l'entrepôt ;
- l'article ;
- l'employé éventuel ;
- le type de mouvement ;
- une référence ;
- une date ;
- des notes ;
- une quantité ;
- des entrepôts source/destination ;
- une opération ;
- l'acteur `performed_by`.

### 4.2. Ce que l'API expose réellement

`StockMovementCreateRequest` ne contient que :

- `warehouse_id` ;
- `item_id` ;
- `employee_id` ;
- `movement_type` ;
- `reference` ;
- `movement_date` ;
- `notes`.

Il ne contient pas :

- `quantity` ;
- `source_warehouse_id` ;
- `destination_warehouse_id` ;
- `operation_id` ;
- `performed_by`.

La colonne `quantity` prend donc sa valeur par défaut `1` pour tous les mouvements créés par l'API. Les quatre autres colonnes restent nulles, sauf insertion manuelle en base.

### 4.3. Vérification en base

Sur la base de démonstration :

- 8 mouvements ;
- 0 mouvement avec `quantity <> 1` ;
- 0 mouvement avec `source_warehouse_id` renseigné ;
- 0 mouvement avec `destination_warehouse_id` renseigné ;
- 0 mouvement avec `performed_by` renseigné.

Ces chiffres confirment que les colonnes existent mais ne sont pas alimentées par le flux applicatif.

### 4.4. Effets sur les articles

Le service `StockMovementService.createStockMovement` modifie l'article après l'insertion du mouvement :

| Type | Statut de l'article | Entrepôt |
|---|---|---|
| `IN` | `AVAILABLE` | `warehouse_id` |
| `OUT` | `ASSIGNED` | `NULL` |
| `TRANSFER` | inchangé | `warehouse_id` |
| `RETURN` | `AVAILABLE` | `warehouse_id` |
| `DESTRUCTION` | `RETIRED` | `NULL` |
| `ADJUSTMENT` | inchangé | inchangé |

Ces effets sont des changements d'état, pas des changements de quantité. Ils sont détaillés dans le rapport 07.

---

## 5. Opérations d'inventaire

### 5.1. Routes

- `POST /api/inventory/operations/receipt`
- `POST /api/inventory/operations/issue`
- `POST /api/inventory/operations/transfer`

### 5.2. Comportement réel

Les trois routes appellent la même méthode privée `save(request)`.

Le DAO insère une ligne dans `inventory_operation` avec les colonnes fournies dans le corps JSON, sans liste blanche. Il n'insère :

- aucune ligne dans `inventory_operation_item` ;
- aucun mouvement dans `stock_movement` ;
- aucune modification d'article.

### 5.3. `operation_type`

La colonne `inventory_operation.operation_type` est `NOT NULL` et n'a pas de valeur par défaut.

La route ne déduit pas le type :

- `/receipt` n'impose pas `RECEIPT` ;
- `/issue` n'impose pas `ISSUE` ;
- `/transfer` n'impose pas `TRANSFER`.

Le client doit fournir `operation_type` dans le corps. S'il l'omet, PostgreSQL renvoie `23502`, converti en **500**.

Un client peut fournir `operation_type: "ISSUE"` sur `/receipt` : aucune contrainte de route ne l'en empêche.

### 5.4. Conséquences

Une opération est un en-tête vide. Elle ne prouve pas qu'un article a été reçu, sorti ou transféré. Elle ne peut pas être auditée au niveau ligne. Le tableau de bord compte ces en-têtes comme des « opérations », ce qui donne une fausse impression d'activité.

---

## 6. Comptages d'inventaire

### 6.1. Modèle

- `inventory_count` : en-tête (entreprise, entrepôt, référence, statut, dates).
- `inventory_count_scan` : une ligne par article scanné, avec entrepôt attendu, entrepôt réel, type d'écart, notes.

### 6.2. Ce qui fonctionne

- création d'un en-tête ;
- scan d'un article dans un comptage ;
- rejet des doublons de scan ;
- rejet d'un `item_id` manquant ;
- rejet d'un champ inconnu dans le scan ;
- rejet d'un `discrepancy_type` invalide ;
- clôture de statut.

### 6.3. Ce qui manque

- **Aucune quantité attendue** : `inventory_count_scan` ne contient pas de colonne `expected_quantity` ni `counted_quantity`. Le client fournit seulement un entrepôt attendu/réel et un type d'écart.
- **Aucun calcul d'écart** : le backend ne compare pas l'emplacement attendu avec `item.current_warehouse_id`.
- **Aucune réconciliation** : la clôture ne crée pas de mouvement d'ajustement, ne met pas à jour `item.current_warehouse_id`, ne met pas à jour `item.status` et ne met pas à jour `item.last_inventory_at`.
- **Aucune date de clôture automatique** : `close` sans corps met `status = COMPLETED`, mais laisse `completed_at` à `NULL`.
- **Aucun contrôle de cohérence** : un scan peut référencer un article d'une autre entreprise ou d'un autre entrepôt.
- **Aucune lecture unitaire** : il n'existe pas de `GET /inventory-counts/{id}` alors que le service `one` existe.
- **Résultats d'un comptage inconnu** : `GET /{id}/results` renvoie **200 `[]`**, pas 404.
- **Corps libre à la clôture** : `close` passe par le `save` générique, qui accepte des colonnes arbitraires (`company_id`, `warehouse_id`, etc.).

### 6.4. `last_inventory_at`

La colonne `item.last_inventory_at` n'est écrite par aucun code. Même après un comptage clôturé, l'article n'a aucune trace de sa dernière vérification physique. La valeur est nulle pour les 10 articles de la base de démonstration.

---

## 7. Tableau de bord

### 7.1. `GET /api/inventory/dashboard/summary`

Requête :

```sql
SELECT
  (SELECT count(*) FROM security.item) AS items,
  (SELECT count(*) FROM security.warehouse) AS warehouses,
  (SELECT count(*) FROM security.inventory_operation) AS operations
```

- pas de filtre entreprise ;
- pas de filtre statut ;
- pas de filtre date ;
- retourne une liste d'un seul objet ;
- compte les en-têtes d'opération, même vides.

### 7.2. `GET /api/inventory/alerts/low-stock`

Requête :

```sql
SELECT t.*, t.quantity_total AS stock_quantity
FROM security.item_type t
WHERE t.quantity_total < t.minimum_stock
  AND t.archived_at IS NULL
```

- pas de filtre entreprise ;
- compare un champ manuel à un seuil manuel ;
- ne consulte ni les articles, ni les mouvements, ni les entrepôts ;
- `stock_quantity` est un `Long` dérivé d'un `NUMERIC(12,2)` : les décimales peuvent être tronquées ;
- ne signale pas les types sans seuil ni les articles manquants.

### 7.3. Ce que le dashboard ne peut pas afficher

- stock réel par entrepôt ;
- stock réel par type ;
- articles disponibles / affectés / endommagés ;
- entrées et sorties de la période ;
- écarts de comptage ;
- valeur du stock ;
- rotation ou consommation.

---

## 8. Rapports

### 8.1. `GET /api/inventory/reports/movements`

Le rapport retourne les champs complets de `stock_movement` :

- `quantity` ;
- `source_warehouse_id` ;
- `destination_warehouse_id` ;
- `operation_id` ;
- `performed_by` ;
- `created_at`.

Mais ces champs sont presque toujours nuls ou égaux à 1 dans les données créées par l'API.

### 8.2. Limites

- filtres uniquement `itemId` et `warehouseId` ;
- pas de filtre par date, type, entreprise, opération ou acteur ;
- tri par `id`, pas par `movement_date` ;
- pagination plafonnée à 500, page par défaut 1 ;
- aucune agrégation ;
- aucun export CSV/PDF ;
- aucune vue « stock à date ».

---

## 9. Transactions et cohérence

Les opérations de stock sont annotées `@Transactional` et, depuis le Lot 2, les DAO utilisent la connexion transactionnelle issue du `DataSource` Spring/HikariCP. Un échec après l'insertion du mouvement annule désormais le mouvement et la mise à jour de l'article ; ce rollback est couvert par `StockMovementTransactionTests`.

**Mise à jour du 22 octobre 2026 (Lot 3) :** `stock_balance` est la source du stock disponible ; les mouvements, opérations, créations/modifications/suppressions d'articles et clôtures de comptage mettent à jour le solde. Les opérations multi-lignes sont implémentées et atomiques, et la clôture de comptage réconcilie statut, emplacement et solde avec `last_inventory_at`.

**Mise à jour du 22 octobre 2026 (Lot 4) :** les affectations sont raccordées au solde. La création d'une affectation, le retour (`POST /{id}/return`) et le void d'une affectation active synchronisent `stock_balance` dans la même transaction que le changement d'état de l'article. L'archivage d'un article retire son stock disponible et sa restauration le réintègre. Les limites restantes sont :

- les ajustements de clôture de comptage ne créent pas de mouvement compensatoire ;
- un comptage vide peut encore être clôturé comme complet ;
- la concurrence entre deux écritures n'est pas contrôlée par version (`P1-14`) ;
- les mouvements historiques n'ont pas de source/destination/acteur rétroactifs ;
- une affectation active antérieure au Lot 4 pointe vers un article `AVAILABLE` dans la base de démonstration et n'a pas été corrigée.

---

## 10. Vérifications et tests

### 10.1. Vérifications en base (audit initial)

| Contrôle | Résultat |
|---|---|
| Mouvements | 8 |
| Mouvements avec quantité différente de 1 | 0 |
| Mouvements avec source/destination | 0 |
| Mouvements avec `performed_by` | 0 |
| Articles avec `last_inventory_at` | 0 |
| Lignes `inventory_operation_item` | non alimentées par l'API |
| Configuration `warehouse_stock_config` | non utilisée par l'API |

### 10.2. Tests existants (état avant la réduction de la suite, 22 octobre 2026)

- `StockMovementBusinessRulesTests` : 9 tests sur les effets d'état et le raccordement aux affectations.
- `StockMovementTimestampTests` : 4 tests sur les dates.
- `InventoryCountScanCloseTests` : 6 tests sur le scan et la clôture.
- `DashboardApiTests` : 3 tests sur les compteurs et le low-stock.
- `ReportApiTests` : 5 tests sur les filtres du rapport.
- `OperationApiTests` : 4 tests sur les en-têtes et les erreurs.
- `Lot3StockModelTests` : 8 tests sur les soldes, l'API, l'acteur, le stock négatif, l'immuabilité, le rollback multi-lignes, le comptage/écart/clôture et les erreurs 404.
- `AssignmentApiTests` : 14 tests sur le cycle de vie des affectations et la synchronisation du solde.
- Suite complète après le Lot 4 : 150 tests, 0 échec, 28 suites (suite depuis réduite, voir le rapport 11).

### 10.3. Ce que les tests ne couvrent pas encore

- concurrence, `If-Match` et idempotence (`P1-14`) ;
- scénario réel à deux entreprises (`P0-01`) ;
- journalisation des ajustements de clôture de comptage ;
- réconciliation complète des mouvements historiques sans source/destination/acteur ;
- la course réelle sur `uq_item_assignment_active` (le conflit est traduit en 409, mais n'est pas testé en concurrence).

---

## 11. Verdict

Le stock est **déclaratif et partiellement tracé**, pas **calculé et réconcilié**. Le backend peut servir à enregistrer des événements unitaires et à changer l'état d'un article. Il ne peut pas servir à piloter un stock, à décider d'une rupture ou à justifier une valeur d'inventaire.

Les priorités sont :

1. choisir le modèle de stock (journal + solde calculé, ou stock déclaratif assumé) ;
2. exposer `quantity`, `source_warehouse_id`, `destination_warehouse_id`, `operation_id`, `performed_by` ;
3. rendre les mouvements immuables ou compensatoires ;
4. alimenter les opérations avec des lignes et des mouvements ;
5. réconcilier les comptages et mettre à jour `last_inventory_at` ;
6. rendre le dashboard et les alertes dépendants du stock réel.

Le plan de correction est dans le rapport 14 ; les critères de validation dans le rapport 15.
