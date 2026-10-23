# 07 — Audit des mouvements de stock

**Note Lot 7 (23 octobre 2026) :** les routes canoniques sont désormais préfixées `/api/v1` ; l'alias `/api` reste accepté avec `Deprecation`/`Link`. Les chemins cités ci-dessous reflètent l'état de l'audit au moment de sa rédaction, sauf mise à jour explicite.

**Objectif :** décrire les règles réellement appliquées par les mouvements `IN`, `OUT`, `TRANSFER`, `RETURN`, `DESTRUCTION`, `ADJUSTMENT`, leurs effets sur l'article, et les lacunes du journal. Le stock global est traité dans le rapport 05 ; les affectations dans le rapport 08.

---

## 1. Contrat actuel

Base : `/api/inventory/movements`

| Méthode | Route | Succès | Statut |
|---|---|---|---|
| GET | `/` | 200 | **PARTIEL** |
| GET | `/{id}` | 200 / 404 | **IMPLÉMENTÉ** |
| POST | `/` | 201 | **PARTIEL** |
| PATCH | `/{id}` | 200 / 404 | **PARTIEL** |
| DELETE | `/{id}` | 204 | **PARTIEL** |

### 1.1. Champs de création

`StockMovementCreateRequest` :

- `warehouse_id` obligatoire ;
- `item_id` obligatoire ;
- `employee_id` facultatif ;
- `movement_type` facultatif ;
- `reference` facultatif ;
- `movement_date` facultatif ;
- `notes` facultatif.

### 1.2. Champs absents

Aucun DTO n'expose :

- `quantity` ;
- `source_warehouse_id` ;
- `destination_warehouse_id` ;
- `operation_id` ;
- `performed_by`.

Ces colonnes existent en base. Elles ne sont pas alimentées par l'API.

### 1.3. Réponse

`StockMovementRest` expose :

- `id` ;
- `warehouse_id` ;
- `item_id` ;
- `employee_id` ;
- `movement_type` ;
- `reference` ;
- `movement_date` ;
- `notes`.

Il n'expose ni quantité, ni source/destination, ni opération, ni acteur, ni `created_at`.

Le rapport `GET /api/inventory/reports/movements` expose davantage de champs, mais les valeurs sont souvent nulles.

---

## 2. Règles par type

Le service `StockMovementService.createStockMovement` :

1. charge l'article ; s'il n'existe pas, renvoie 404 ;
2. si le type est `OUT` et que l'article n'est pas `AVAILABLE`, renvoie 400 ;
3. insère le mouvement ;
4. modifie l'article selon le type.

| Type | Vérification | Statut après | Entrepôt après | Quantité |
|---|---|---|---|---|
| `IN` | aucune | `AVAILABLE` | `warehouse_id` | 1 |
| `OUT` | article `AVAILABLE` | `ASSIGNED` | `NULL` | 1 |
| `TRANSFER` | aucune | inchangé | `warehouse_id` | 1 |
| `RETURN` | aucune | `AVAILABLE` | `warehouse_id` | 1 |
| `DESTRUCTION` | aucune | `RETIRED` | `NULL` | 1 |
| `ADJUSTMENT` | aucune | inchangé | inchangé | 1 |

### 2.1. `IN`

Un `IN` rend l'article disponible et le place dans l'entrepôt fourni. Il ne vérifie pas :

- que l'article était réellement sorti ;
- qu'une affectation active existe ;
- que l'entrepôt appartient à la même entreprise ;
- que l'employé est actif.

Un `IN` sur un article `ASSIGNED` le repasse `AVAILABLE` sans clore l'affectation active. C'est une incohérence directe avec `uq_item_assignment_active`.

### 2.2. `OUT`

Un `OUT` :

- vérifie seulement que le statut est `AVAILABLE` ;
- passe l'article à `ASSIGNED` ;
- vide `current_warehouse_id`.

Il ne crée **aucune affectation**. Il ne renseigne ni employé obligatoire, ni poste, ni date de retour. L'article devient `ASSIGNED` sans détenteur identifiable.

C'est un des conflits majeurs avec le module d'affectation : `OUT` et `POST /api/item-assignments` produisent le même statut, mais un seul crée une ligne d'affectation.

### 2.3. `TRANSFER`

Un `TRANSFER` change `current_warehouse_id` vers `warehouse_id`.

Le DTO ne distingue pas l'entrepôt source de l'entrepôt destination. Les colonnes `source_warehouse_id` et `destination_warehouse_id` restent nulles. On ne peut donc pas reconstituer le transfert.

Un transfert ne vérifie pas que l'article est dans l'entrepôt source, ni que les deux entrepôts appartiennent à la même entreprise.

### 2.4. `RETURN`

Un `RETURN` rend l'article `AVAILABLE` et le place dans `warehouse_id`.

Il ne clôt pas l'affectation active. Il ne renseigne pas `returned_at`, `returned_by` ni `condition_on_return`. L'article peut donc être `AVAILABLE` alors qu'une ligne `item_assignment` reste active.

### 2.5. `DESTRUCTION`

Un `DESTRUCTION` passe l'article à `RETIRED` et vide son entrepôt.

Il ne clôt pas l'affectation active. Il ne crée pas de ligne de destruction, ne renseigne pas `performed_by`, et ne conserve pas de quantité.

### 2.6. `ADJUSTMENT`

Un `ADJUSTMENT` insère une ligne, mais ne modifie ni le statut, ni l'entrepôt, ni la quantité. C'est un événement sans effet.

### 2.7. Absence de quantité

La quantité vaut toujours `1` :

- le DTO ne l'expose pas ;
- le DAO ne l'écrit pas ;
- le schéma applique `DEFAULT 1`.

Il est donc impossible d'enregistrer :

- une réception de 10 unités ;
- une sortie de 3 unités ;
- un ajustement de +5 ;
- une destruction partielle.

Chaque mouvement concerne un article individuel. Ce n'est pas nécessairement un défaut si le modèle est « un article = un actif unique », mais alors `quantity_total` et les rapports de quantité sont trompeurs.

---

## 3. Création : défauts et réponse incohérente

### 3.1. Type par défaut

Si `movement_type` est absent :

- le service calcule `MovementType.IN` ;
- le DAO insère `IN` ;
- la réponse est construite avec `value.getMovement_type()`, qui vaut `null`.

La réponse de création peut donc afficher :

```json
{
  "movement_type": null,
  "movement_date": null
}
```

alors que la ligne en base contient `IN` et `now()`.

### 3.2. Date par défaut

Même problème pour `movement_date` :

- la colonne a `DEFAULT now()` ;
- le DAO omet la colonne si la valeur est nulle ;
- la réponse renvoie la valeur d'entrée, donc `null`.

Le test `StockMovementTimestampTests.createWithoutMovementDateAppliesDatabaseDefault` vérifie que la base a bien une date, mais il ne corrige pas la réponse HTTP. Le frontend doit recharger la ligne pour obtenir la date réelle.

### 3.3. Erreurs de référence

- `warehouse_id` inconnu : FK `23503` → **500**.
- `item_id` inconnu : 404 avant insertion.
- `employee_id` inconnu : FK `23503` → **500**.
- `movement_type` invalide : erreur de désérialisation → **400** `INVALID_REQUEST_BODY`.

Aucune vérification d'appartenance à l'entreprise n'est effectuée.

---

## 4. Modification et suppression

### 4.1. `PATCH`

`StockMovementUpdateRequest` permet de changer :

- `warehouse_id` ;
- `item_id` ;
- `employee_id` ;
- `movement_type` ;
- `reference` ;
- `movement_date` ;
- `notes`.

Il ne permet pas de changer la quantité, les entrepôts source/destination, l'opération ou l'acteur.

Le service :

1. vérifie que le mouvement existe ;
2. met à jour la ligne ;
3. **ne réapplique aucun effet sur l'article**.

Conséquences :

- changer un `IN` en `OUT` ne change pas le statut de l'article ;
- changer `item_id` laisse l'ancien article dans son état ;
- changer `warehouse_id` laisse `current_warehouse_id` inchangé ;
- changer `movement_date` réécrit l'histoire.

### 4.2. `DELETE`

La suppression :

- supprime la ligne ;
- ne compense pas l'effet sur l'article ;
- ne vérifie pas si le mouvement est référencé par `inventory_operation_item` ;
- renvoie 204 même si l'identifiant est inconnu.

Un article peut donc rester `ASSIGNED`, `RETIRED` ou dans un entrepôt alors que le mouvement qui l'y a mis a disparu.

### 4.3. Immuabilité

Un journal de stock doit être append-only ou compensé par un mouvement inverse. Ici, il est librement modifiable et supprimable. La valeur d'audit est nulle.

---

## 5. Historique d'un article

`GET /api/inventory/items/{itemId}/history` :

- appelle `getStockMovements(page=1, pageSize=100)` ;
- appelle `getItemAssignments(page=1, pageSize=100)` ;
- filtre en mémoire sur `item_id` ;
- renvoie `movements` et `assignments`.

Conséquences :

- l'historique ne contient que les 100 premiers mouvements **globaux** et les 100 premières affectations **globales** ;
- un article dont les mouvements sont plus anciens ou plus nombreux n'apparaît pas ;
- aucun tri par date ;
- aucune pagination ;
- aucun contrôle d'existence de l'article : un UUID inconnu renvoie **200** avec des listes vides.

**Divergence de test (historique) :** `ItemApiTests.historyUnknownReturnsNotFound` attendait un **404**, alors que le contrôleur ne contient aucun appel à `getItemById` avant de construire l'historique. Ce test a depuis été retiré de l'arbre ; l'absence de 404 reste une anomalie fonctionnelle (`P1-12`) non couverte par un test négatif.

---

## 6. Filtres et rapport

`GET /api/inventory/movements` :

- `search` sur `reference` et `notes` ;
- pas de filtre par article, entrepôt, type, date ou entreprise ;
- tri par `id`.

`GET /api/inventory/reports/movements` :

- filtres `itemId` et `warehouseId` ;
- pas de filtre date/type/entreprise/opération/acteur ;
- tri par `id` ;
- plafond 500 ;
- champs complets, mais souvent nuls.

Aucun des deux ne permet de reconstituer un solde à une date donnée.

---

## 7. Tests existants

`StockMovementBusinessRulesTests` (7 tests) vérifie :

- `IN` → `AVAILABLE` + entrepôt ;
- `OUT` → `ASSIGNED` + entrepôt nul ;
- `OUT` refusé si l'article n'est pas disponible ;
- `TRANSFER` → changement d'entrepôt ;
- `RETURN` → `AVAILABLE` + entrepôt ;
- `DESTRUCTION` → `RETIRED` + entrepôt nul ;
- `ADJUSTMENT` → aucun changement.

`StockMovementTimestampTests` (3 tests) vérifie :

- date par défaut en base ;
- date explicite ;
- date invalide → 400.

Aucun test ne couvre :

- la quantité ;
- les champs source/destination/opération/acteur ;
- `PATCH` et ses effets ;
- `DELETE` et ses effets ;
- la cohérence avec une affectation active ;
- l'historique au-delà de 100 lignes ;
- le 404 de l'historique ;
- le filtrage par entreprise.

---

## 8. Verdict

Les mouvements sont **des transitions d'état unitaires**, pas **un journal de stock**.

Ils sont utilisables pour un prototype qui suit un article individuel. Ils ne permettent pas :

- de connaître une quantité ;
- de justifier un stock ;
- de reconstituer un transfert ;
- de savoir qui a agi ;
- de corriger une erreur sans réécrire l'histoire ;
- de garantir qu'un mouvement et l'état de l'article sont cohérents.

Les priorités sont :

1. rendre le journal immuable ou compensatoire ;
2. exposer la quantité et les champs source/destination/opération/acteur ;
3. séparer `OUT` de l'affectation ;
4. faire de `RETURN` une clôture d'affectation ;
5. rendre l'historique filtré et paginé côté SQL ;
6. corriger la réponse de création pour renvoyer les valeurs réellement stockées.

Les endpoints proposés sont dans le rapport 13 ; le plan de correction dans le rapport 14.
