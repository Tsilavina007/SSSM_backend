# 04 — Audit du catalogue matériel

**Objectif :** évaluer le CRUD et l'intégrité des catégories, types de matériel, entrepôts et articles. Les mouvements, le stock, les QR Codes et les affectations sont traités dans les rapports 05 à 08.

---

## 1. Périmètre fonctionnel

| Objet | Table | Contrôleur | Service | DAO |
|---|---|---|---|---|
| Catégorie | `item_category` | `ItemCategoryController` | `ItemCategoryService` | `ItemCategoryDAOImpl` |
| Type de matériel | `item_type` | `ItemTypeController` | `ItemTypeService` | `ItemTypeDAOImpl` |
| Entrepôt | `warehouse` | `WarehouseController` | `WarehouseService` | `WarehouseDAOImpl` |
| Article | `item` | `ItemController` | `ItemService` | `ItemDAOImpl` |

Le catalogue est le module le plus complet du backend. Il permet de créer, lire, rechercher, paginer, modifier et supprimer les quatre objets. Les articles peuvent être filtrés par entrepôt et par statut, et un QR Code peut être généré automatiquement.

---

## 2. Ce qui fonctionne

- CRUD complet des catégories, types, entrepôts et articles.
- Validation Jakarta sur les champs obligatoires et les longueurs.
- Recherche `ILIKE` sur les champs textuels principaux.
- Pagination `page` / `pageSize` avec valeurs par défaut.
- Génération d'un QR Code `QR-XXXXXXXX` si le champ est absent.
- Filtre combiné des articles par entrepôt et statut.
- Image PNG du QR Code.
- Contraintes SQL d'unicité sur le code de type, le nom de catégorie et le QR Code par entreprise.
- Tests d'intégration nominaux pour les quatre objets.

---

## 3. Constats détaillés

### 3.1. Absence de filtrage par entreprise

**Constat :** toutes les requêtes de liste et de lecture sont globales.

Exemples de SQL :

- `SELECT * FROM security.item ORDER BY id LIMIT ? OFFSET ?`
- `SELECT * FROM security.item_type ORDER BY id LIMIT ? OFFSET ?`
- `SELECT * FROM security.item_category ORDER BY id LIMIT ? OFFSET ?`
- `SELECT * FROM security.warehouse ORDER BY id LIMIT ? OFFSET ?`

Aucune de ces requêtes ne reçoit ni ne filtre `company_id`. Un utilisateur authentifié voit donc les articles de toutes les entreprises présentes en base.

**Impact :** fuite de données et impossibilité de construire une vue client fiable.

**Référence :** `ItemDAOImpl.getAllItems`, `ItemTypeDAOImpl.getAllItemTypes`, `ItemCategoryDAOImpl.getAllItemCategories`, `WarehouseDAOImpl.getAllWarehouses`.

### 3.2. `company_id` contrôlé par le client

**Constat :** `company_id` est un champ du corps de création et de mise à jour. Il est converti en UUID puis écrit tel quel.

- `ItemCreateRequest.company_id` est obligatoire.
- `ItemTypeCreateRequest.company_id` est obligatoire.
- `ItemCategoryCreateRequest.company_id` est obligatoire.
- `WarehouseCreateRequest.company_id` est obligatoire.
- Les DTO de mise à jour acceptent aussi `company_id`.

Aucun contrôle ne vérifie que l'utilisateur appartient à cette entreprise. Un client peut donc créer un article dans n'importe quelle entreprise, ou déplacer un article d'une entreprise à une autre par `PATCH`.

**Impact :** contamination et déplacement de données entre tenants.

### 3.3. Références inter-entreprises acceptées

Les contrôles d'existence se limitent à l'UUID :

- un article peut référencer un `item_type` d'une autre entreprise ;
- un article peut référencer une `item_category` d'une autre entreprise ;
- un article peut être placé dans un `warehouse` d'une autre entreprise ;
- un entrepôt peut référencer une `branch` d'une autre entreprise ;
- un type peut référencer une catégorie d'une autre entreprise.

Aucune FK composite `(id, company_id)` ni vérification applicative n'empêche ces combinaisons. Le schéma ne garantit pas la cohérence de tenant.

### 3.4. Création de type de matériel fragile

**Constat :** `ItemTypeDAOImpl.createItemType` écrit explicitement les colonnes `unit`, `quantity_total` et `minimum_stock`, même si les valeurs sont nulles.

```sql
INSERT INTO security.item_type
  (company_id, category_id, code, name, description,
   unit, quantity_total, minimum_stock, status)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS security.item_status))
```

Or le schéma déclare :

- `unit VARCHAR(50) NOT NULL DEFAULT 'piece'`
- `quantity_total NUMERIC(12,2) NOT NULL DEFAULT 0`
- `minimum_stock NUMERIC(12,2) NOT NULL DEFAULT 0`

En PostgreSQL, un `NULL` explicite ne déclenche pas le `DEFAULT`. Si le client omet `unit`, `quantity_total` ou `minimum_stock`, la requête échoue avec `23502` et l'API renvoie **500**.

**Preuve :** `ItemTypeDAOImpl.createItemType` ; `ItemTypeCreateRequest` rend ces trois champs facultatifs ; `ItemTypeApiTests.createUpdateDeleteLifecycle` fournit probablement les trois valeurs, ce qui masque le cas d'omission.

**Impact :** un formulaire minimal de création de type échoue.

### 3.5. Mise à jour incapable d'effacer une relation

Les DTO de mise à jour utilisent la convention « champ non nul = à modifier ». Il n'existe aucun moyen de passer `category_id`, `current_warehouse_id` ou `description` à `NULL` par `PATCH`.

Conséquences :

- on ne peut pas retirer un article d'un entrepôt par l'API (`current_warehouse_id` reste renseigné) ;
- on ne peut pas retirer une catégorie d'un article ;
- on ne peut pas vider une description.

La seule façon de retirer un article d'un entrepôt est de passer par un mouvement `OUT`, qui met la colonne à `NULL` en base. C'est un effet de bord métier, pas une opération de catalogue.

### 3.6. Suppression physique et erreurs 500

- `DELETE /api/inventory/items/{id}` supprime la ligne `item`.
- Les FK `stock_movement.item_id` et `item_assignment.item_id` sont en `ON DELETE RESTRICT`.
- Si l'article a un historique, PostgreSQL refuse la suppression avec `23503`, converti en **500**.
- Si l'article n'a pas d'historique, la suppression réussit et l'API renvoie 204.
- `DELETE` sur un UUID inconnu renvoie aussi 204 : l'opération est idempotente, mais le client ne sait pas si quelque chose a été supprimé.
- Les entrepôts et types référencés produisent le même comportement 500.
- Les catégories, elles, sont supprimées et les références passent à `NULL` via `ON DELETE SET NULL`, sans avertissement.

**Impact :** le frontend ne peut pas distinguer « supprimé », « inexistant » et « impossible à cause de l'historique ». La suppression d'une catégorie peut vider silencieusement des relations.

### 3.7. Archivage inopérant

Les tables `item`, `item_type`, `item_category` et `warehouse` possèdent `archived_at` (et `archived_by` pour `item`), mais :

- aucun endpoint ne les renseigne ;
- aucune liste ne filtre `archived_at IS NULL` ;
- la suppression est physique, pas un archivage ;
- les index partiels `idx_item_company_status` et `idx_item_type_company_status` filtrent les archivés, mais les requêtes DAO ne les utilisent pas.

**Impact :** le champ d'archivage est une dette de modèle. Les éléments archivés manuellement en base continueraient d'apparaître dans les listes.

### 3.8. Unicité et erreurs de conflit

Les contraintes existent :

- `UNIQUE(company_id, qr_code)` sur `item` ;
- `UNIQUE(company_id, code)` sur `item_type` ;
- `UNIQUE(company_id, name)` sur `item_category`.

Mais aucun service ne vérifie l'unicité avant l'insertion. Un doublon produit `23505`, converti en **500** avec le message PostgreSQL. Le frontend ne reçoit pas un **409 Conflict** exploitable.

Le nom d'entrepôt, lui, n'a aucune contrainte d'unicité : deux entrepôts de même nom sont autorisés dans la même entreprise.

### 3.9. Statut d'article modifiable sans règle

`PATCH /api/inventory/items/{id}` accepte n'importe quelle valeur de `ItemStatus` :

- `AVAILABLE`, `ASSIGNED`, `DAMAGED`, `LOST`, `REPAIR`, `RETIRED`.

Aucune transition n'est vérifiée. Un article peut donc passer :

- de `RETIRED` à `AVAILABLE` sans contrôle ;
- à `ASSIGNED` sans affectation ;
- à `AVAILABLE` alors qu'une affectation active existe.

Le statut est une donnée libre, pas une machine à états.

### 3.10. Filtres et recherche limités

**Articles :**

- `search` cherche uniquement dans `qr_code` et `description` ;
- pas de recherche par nom de type, catégorie, code, numéro de série ou entreprise ;
- `warehouseId` et `status` sont combinables ;
- pas de filtre par type, catégorie, entreprise ou plage de dates ;
- tri fixe `ORDER BY id`.

**Types :**

- `search` cherche dans `code`, `name`, `description` ;
- pas de filtre par entreprise, catégorie ou statut.

**Catégories :**

- `search` cherche dans `name`, `description` ;
- pas de filtre par entreprise.

**Entrepôts :**

- `search` cherche dans `name`, `address` ;
- pas de filtre par entreprise, agence ou actif.

**Impact :** les écrans de catalogue devront filtrer côté client ou multiplier les appels.

### 3.11. Pagination non bornée

`page` et `pageSize` sont des `Optional<Integer>` avec défaut 10. Aucun plafond n'est appliqué. Un client peut demander `pageSize=100000`, ce qui charge toute la table et déclenche ensuite le N+1 de mapping.

À l'inverse, `page=0` ou une valeur négative produit un `OFFSET` négatif et une erreur SQL, généralement **500**.

Aucune réponse ne contient le total, le nombre de pages ou un curseur.

### 3.12. N+1 sur la liste des articles

`ItemRestMapper.toRest` charge, pour chaque article :

- l'entreprise ;
- le type ;
- l'entreprise du type ;
- la catégorie du type ;
- l'entreprise de la catégorie ;
- la catégorie de l'article ;
- l'entrepôt ;
- l'entreprise de l'entrepôt ;
- l'agence de l'entrepôt ;
- l'agence de la branche.

Selon les relations renseignées, cela représente plusieurs requêtes supplémentaires par article, alors que la liste est déjà paginée. Avec 10 articles et des relations complètes, le nombre de requêtes peut dépasser la dizaine.

**Impact :** latence et charge base importantes, alors que la jointure SQL serait naturelle.

### 3.13. Création en masse partiellement transactionnelle

`POST /api/inventory/items/bulk` :

- boucle sur les articles ;
- attrape uniquement `ServerException` ;
- ajoute les succès à `created` et les échecs SQL à `failed` ;
- ne passe pas par `@Transactional` au niveau de la méthode ;
- n'applique pas `@Valid` aux éléments de la liste.

Conséquences :

- une erreur `IllegalArgumentException` (UUID invalide) interrompt toute la requête en 400 et les articles déjà créés restent en base ;
- un champ obligatoire manquant produit une erreur SQL capturée dans `failed`, mais les autres articles sont créés ;
- le client reçoit un mélange de succès et d'échecs sans identifiant de corrélation.

### 3.14. Tests du catalogue

Les tests couvrent les parcours nominaux :

- `ItemCategoryApiTests` : 4 tests ;
- `ItemTypeApiTests` : 7 tests, dont les trois stubs ;
- `WarehouseApiTests` : 5 tests ;
- `ItemApiTests` : 17 tests.

Ils ne couvrent pas :

- l'omission de `unit`/`quantity_total`/`minimum_stock` à la création d'un type ;
- la création d'un article avec un type ou un entrepôt d'une autre entreprise ;
- la modification de `company_id` ;
- la suppression d'un article référencé ;
- la suppression d'une catégorie utilisée ;
- les doublons de QR/code/nom ;
- les filtres avec `page=0` ;
- le N+1 ou les performances.

---

## 4. Matrice d'intégrité du catalogue

| Règle attendue | Code | Base | Statut |
|---|---|---|---|
| Un article appartient à une entreprise | `company_id` fourni par le client | FK simple | **NON GARANTI** |
| Un type appartient à la même entreprise que l'article | aucun contrôle | aucune FK composite | **ABSENT** |
| Une catégorie appartient à la même entreprise | aucun contrôle | aucune FK composite | **ABSENT** |
| Un entrepôt appartient à la même entreprise | aucun contrôle | aucune FK composite | **ABSENT** |
| Un QR Code est unique par entreprise | pas de pré-contrôle | UNIQUE | **GARANTI EN BASE** |
| Un code de type est unique par entreprise | pas de pré-contrôle | UNIQUE | **GARANTI EN BASE** |
| Un nom de catégorie est unique par entreprise | pas de pré-contrôle | UNIQUE | **GARANTI EN BASE** |
| Un nom d'entrepôt est unique | aucun contrôle | aucune contrainte | **ABSENT** |
| Un article archivé n'apparaît pas | aucun filtre | index partiel seulement | **NON GARANTI** |
| Un article supprimé conserve son historique | FK RESTRICT | OK | **GARANTI** (mais 500) |
| Le statut suit une machine à états | aucun contrôle | enum seulement | **ABSENT** |
| Les relations nulles peuvent être retirées par PATCH | impossible | nullable | **ABSENT** |

---

## 5. Verdict

Le catalogue est **utilisable en lecture et en CRUD nominal mono-entreprise**. Il n'est pas **fiable en multi-entreprise**, ni **cohérent sur les transitions de statut**, ni **robuste sur les erreurs de conflit**.

Avant un frontend de production, il faut au minimum :

1. filtrer toutes les requêtes par entreprise, de façon non contournable ;
2. valider les références inter-entreprises ;
3. corriger la création de type (ne pas insérer `NULL` explicite) ;
4. remplacer les 500 de conflit par des 409 ;
5. décider si la suppression est physique ou un archivage.

Les corrections sont intégrées au plan du rapport 14. Les critères d'acceptation correspondants sont dans le rapport 15.
