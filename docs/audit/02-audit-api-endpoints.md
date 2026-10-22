# 02 — Audit des endpoints API

**Objectif :** dresser le contrat HTTP réel du backend, route par route, avec les statuts, les paramètres, les règles de validation et les écarts visibles. Les propositions de nouvelles routes sont dans le rapport 13 ; ce rapport ne décrit que l'existant.

**Périmètre :** 26 contrôleurs REST, **131 routes applicatives après le Lot 1** (130 lors de l'audit initial, plus `POST /api/auth/logout`), dont 57 routes d'inventaire. Les deux contrôleurs de test `HelloController` et `PingController` sont exclus du décompte métier.

---

## 1. Conventions générales observées

| Sujet | Comportement réel |
|---|---|
| Format | JSON, `Content-Type: application/json`, sauf l'image PNG |
| Authentification | `Authorization: Bearer <JWT>` sur toutes les routes, sauf `/api/auth/login`, `/api/auth/refresh`, `/api/auth/logout`, `/ping`, `/error`. `/api/auth/register` exige un admin. |
| Autorisation | `/api/inventory/**` : `ROLE_ADMIN`, `ROLE_INVENTORY` ou `inventory.manage` ; `/api/users/**` et `/api/auth/register` : `ROLE_ADMIN` ou `users.manage` ; `/api/roles/**` : `ROLE_ADMIN` ou `roles.manage` ; `/api/permissions/**` : `ROLE_ADMIN` ; reste : authentification |
| Enveloppe d'erreur | `{"errors":{"message":"...","detail":"...","code":"..."}}` |
| Erreurs de validation | **400**, code `VALIDATION_ERROR`, détail par champ |
| Erreurs SQL | **500**, code = SQLSTATE (`23505`, `23503`, `23502`, `23514`, etc.), détail = message PostgreSQL |
| Erreurs douces | Certains modules renvoient **200** avec `errors` au lieu de 404/400 |
| Pagination | `page` (1-based) et `pageSize`, sans métadonnées de total |
| Tri | Souvent `ORDER BY id`, rarement par date |
| Champs | `snake_case` dans les DTO et les réponses |

---

## 2. Routes d'inventaire (57)

### 2.1. Articles — `ItemController`

Base : `/api/inventory/items`

| Méthode | Chemin | Succès | Statut | Remarques |
|---|---|---|---|---|
| GET | `/` | 200 | **IMPLÉMENTÉ** | Paramètres `page`, `pageSize`, `search`, `warehouseId`, `status`. Filtre `search` sur `qr_code` et `description`. |
| GET | `/{itemId}` | 200 / 404 | **IMPLÉMENTÉ** | 404 si UUID inconnu ; 400 si UUID invalide. |
| GET | `/{itemId}/qr-code` | 200 / 404 | **IMPLÉMENTÉ** | Retourne `id`, `company_id`, `category_id`, `item_type_id`, `current_warehouse_id`, `qr_code`. |
| GET | `/{itemId}/history` | 200 | **PARTIEL** | Retourne `movements` et `assignments`, limités aux 100 premières lignes **globales** de chaque table puis filtrés en mémoire. Aucun contrôle d'existence : un UUID inconnu renvoie 200 avec des listes vides. L'ancien test `historyUnknownReturnsNotFound` attendait 404 ; il n'est plus présent dans l'arbre (anomalie `P1-12` toujours ouverte, sans test négatif). |
| GET | `/{itemId}/qr-code/image` | 200 PNG / 400 / 404 / 500 | **IMPLÉMENTÉ** | PNG 300 × 300 généré à la demande. UUID invalide → 400 `BAD_REQUEST` ; article inconnu → 404 `NOT_FOUND` ; contenu QR absent → 500 `QR_CODE_MISSING` ; échec d'encodage/flux → 500 `QR_CODE_GENERATION_FAILED`. Les erreurs restent en JSON même avec `Accept: image/png` strict (correctif `7e3f196`). |
| POST | `/` | 201 | **IMPLÉMENTÉ** | `company_id` et `item_type_id` obligatoires. QR auto si absent. |
| POST | `/bulk` | 200 | **PARTIEL** | Liste d'articles ; succès partiel dans `created` / `failed`. Pas de `@Valid` sur les éléments. |
| PATCH | `/{itemId}` | 200 / 404 | **PARTIEL** | Champs non nuls uniquement ; impossible d'effacer une relation. |
| DELETE | `/{itemId}` | 204 | **PARTIEL** | Idempotent sur UUID inconnu ; **500** si l'article est référencé (RESTRICT). |

**Constats spécifiques**

- `POST /bulk` n'applique pas la validation Jakarta aux éléments de la liste. Un UUID invalide lève `IllegalArgumentException` et interrompt toute la requête en 400, au lieu d'être isolé dans `failed`.
- `PATCH` peut changer `company_id`, `item_type_id`, `category_id` ou `current_warehouse_id` sans vérifier l'appartenance à la même entreprise.
- `PATCH` peut passer un article à `ASSIGNED` sans créer d'affectation.
- `DELETE` d'un article référencé par un mouvement ou une affectation renvoie 500 `23503`, pas 409.
- `GET /items?status=INCONNU` produit une erreur SQL `22P02` convertie en **500**.
- `page=0` ou `pageSize` négatif produit un `OFFSET`/`LIMIT` invalide, généralement **500**.

### 2.2. Types de matériel — `ItemTypeController`

Base : `/api/inventory/item-types`

| Méthode | Chemin | Succès | Statut | Remarques |
|---|---|---|---|---|
| GET | `/` | 200 | **IMPLÉMENTÉ** | `page`, `pageSize`, `search` ; inclut les types archivés. |
| GET | `/{itemTypeId}` | 200 / 404 | **IMPLÉMENTÉ** | |
| POST | `/` | 201 | **PARTIEL** | Si `unit`, `quantity_total` ou `minimum_stock` sont absents, le DAO insère `NULL` explicitement : **500 `23502`** attendu. |
| PATCH | `/{itemTypeId}` | 200 / 404 | **PARTIEL** | Mise à jour partielle ; doublon de code → 500 `23505`. |
| DELETE | `/{itemTypeId}` | 204 | **PARTIEL** | 500 si des articles référencent le type (RESTRICT). |
| GET | `/category/{itemCategoryId}` | 200 | **IMPLÉMENTÉ** | Liste les types d'une catégorie. |
| GET | `/low-stock` | 200 `[]` | **STUB** | Retourne toujours une liste vide. |
| GET | `/items/{typeId}` | 200 `[]` | **STUB** | La méthode n'a pas de `@PathVariable` : `typeId` est ignoré. |
| GET | `/stocks/{typeId}` | 200 `[]` | **STUB** | Retourne toujours une liste vide. |

**Constats spécifiques**

- `ItemTypeDAOImpl.createItemType` écrit explicitement les colonnes `unit`, `quantity_total` et `minimum_stock`, même lorsqu'elles sont nulles. En PostgreSQL, un `NULL` explicite ne déclenche pas le `DEFAULT` et viole les contraintes `NOT NULL`.
- Les routes `low-stock`, `items/{typeId}` et `stocks/{typeId}` ne sont pas des fonctionnalités : ce sont des coquilles.
- Aucun filtre `archived_at IS NULL` n'est appliqué à la liste.

### 2.3. Catégories — `ItemCategoryController`

Base : `/api/inventory/item-categories`

| Méthode | Chemin | Succès | Statut | Remarques |
|---|---|---|---|---|
| GET | `/` | 200 | **IMPLÉMENTÉ** | `page`, `pageSize`, `search`. |
| GET | `/{itemCategoryId}` | 200 / 404 | **IMPLÉMENTÉ** | |
| POST | `/` | 201 | **IMPLÉMENTÉ** | `company_id`, `name` obligatoires ; doublon → 500 `23505`. |
| PATCH | `/{itemCategoryId}` | 200 / 404 | **PARTIEL** | Peut changer `company_id`. |
| DELETE | `/{itemCategoryId}` | 204 | **IMPLÉMENTÉ** | Les références `item.category_id` et `item_type.category_id` passent à `NULL` (SET NULL). |

### 2.4. Entrepôts — `WarehouseController`

Base : `/api/inventory/warehouses`

| Méthode | Chemin | Succès | Statut | Remarques |
|---|---|---|---|---|
| GET | `/` | 200 | **IMPLÉMENTÉ** | `page`, `pageSize`, `search` ; inclut les entrepôts archivés. |
| GET | `/{warehouseId}` | 200 / 404 | **IMPLÉMENTÉ** | |
| POST | `/` | 201 | **IMPLÉMENTÉ** | `company_id`, `name` obligatoires ; aucun contrôle d'unicité du nom. |
| PATCH | `/{warehouseId}` | 200 / 404 | **PARTIEL** | Peut changer `company_id` et `branch_id` sans contrôle d'entreprise. |
| DELETE | `/{warehouseId}` | 204 | **PARTIEL** | 500 si l'entrepôt est référencé par un article, un mouvement ou un comptage. |

### 2.5. Mouvements de stock — `StockMovementController`

Base : `/api/inventory/movements`

| Méthode | Chemin | Succès | Statut | Remarques |
|---|---|---|---|---|
| GET | `/` | 200 | **PARTIEL** | `page`, `pageSize`, `search` ; recherche sur `reference` et `notes` ; pas de filtre date/type/entrepôt. |
| GET | `/{stockMovementId}` | 200 / 404 | **IMPLÉMENTÉ** | |
| POST | `/` | 201 | **PARTIEL** | `warehouse_id`, `item_id` obligatoires ; `movement_type` optionnel (défaut `IN`). |
| PATCH | `/{stockMovementId}` | 200 / 404 | **PARTIEL** | Modifie le journal sans resynchroniser l'état de l'article. |
| DELETE | `/{stockMovementId}` | 204 | **PARTIEL** | Supprime une ligne d'audit sans compensation. |

**Constats spécifiques**

- Aucun champ `quantity` dans `StockMovementCreateRequest` : la colonne vaut toujours `1`.
- Aucun champ `source_warehouse_id`, `destination_warehouse_id`, `operation_id` ou `performed_by` dans le DTO de création.
- `movement_type` absent → la base applique `IN`, mais la réponse de création peut renvoyer `movement_type: null`, car le DAO construit la réponse avec la valeur d'entrée.
- `movement_date` absent → la base applique `now()`, mais la réponse de création renvoie `null` (comportement attesté par `StockMovementTimestampTests.createWithoutMovementDateAppliesDatabaseDefault`).
- `PATCH` et `DELETE` ne sont pas protégés : un mouvement peut être réécrit ou supprimé, ce qui détruit la valeur d'audit.
- Un `PATCH` qui change `item_id` ou `movement_type` ne met pas à jour l'article concerné.

### 2.6. Affectations typées — `ItemAssignmentController`

Base : `/api/item-assignments`

| Méthode | Chemin | Succès | Statut | Remarques |
|---|---|---|---|---|
| GET | `/` | 200 | **IMPLÉMENTÉ** | `page`, `pageSize`, `search` ; pas de plafond. |
| GET | `/{itemAssignmentId}` | 200 / 404 | **IMPLÉMENTÉ** | |
| POST | `/` | 201 | **PARTIEL** | `item_id`, `employee_id`, `post_id` obligatoires ; vérifie seulement que l'article est `AVAILABLE`. |
| PATCH | `/{itemAssignmentId}` | 200 / 404 | **PARTIEL** | `returned_at` remet l'article `AVAILABLE`, sans restaurer l'entrepôt. |
| DELETE | `/{itemAssignmentId}` | 204 | **PARTIEL** | Supprime l'historique sans remettre l'article `AVAILABLE`. |

**Constats spécifiques**

- `POST` avec un `returned_at` déjà renseigné crée une affectation historique, mais le service passe quand même l'article à `ASSIGNED`.
- Aucun contrôle d'employé actif, de cohérence d'entreprise, de poste compatible ou de date `returned_at >= assigned_at` au niveau métier (la contrainte SQL le fait, mais l'erreur devient 500).
- Le DTO de réponse ne contient ni `assigned_by`, ni `returned_by`, ni `created_at`/`updated_at`, ni `expected_return_at`.
- Une seconde affectation active sur le même article est refusée par l'index unique `uq_item_assignment_active`, mais l'erreur SQL devient 500.

### 2.7. Affectations génériques — `AssignmentController`

Base : `/api/inventory`

| Méthode | Chemin | Succès | Statut | Remarques |
|---|---|---|---|---|
| GET | `/assignments` | 200 | **PARTIEL** | Liste les affectations actives (`returned_at IS NULL`), `page`/`pageSize`, plafond 500. |
| POST | `/assignments` | 200 | **PARTIEL** | Accepte un JSON libre ; aucune validation DTO ; peut omettre les colonnes obligatoires → 500. |
| GET | `/assignments/{id}` | 200 | **PARTIEL** | 200 avec `errors` si inconnu (pas de 404). |
| PATCH | `/assignments/{id}` | 200 | **PARTIEL** | 200 avec `errors` si inconnu ; accepte des colonnes arbitraires. |
| POST | `/assignments/{id}/return` | 200 | **PARTIEL** | Ne clôt que si `returned_at` est fourni ; sinon no-op. |
| GET | `/employees/{id}/assignments` | 200 | **PARTIEL** | Limité à 500, sans pagination. |
| GET | `/security-posts/{id}/assignments` | 200 | **PARTIEL** | Limité à 500, sans pagination. |

**Constats spécifiques**

- Aucune route `DELETE` n'est exposée, alors que `AssignmentService.delete` existe.
- Le corps de création peut contenir n'importe quelle colonne valide de `item_assignment` ; le service ne vérifie pas la disponibilité de l'article.
- Le retour avec seulement `condition_on_return` renvoie 200 et ne clôt rien. Le test `AssignmentApiTests.returnRecordsCondition` ne vérifie ni `returned_at` ni le statut de l'article.
- En cas de doublon d'affectation active, l'erreur SQL devient 500.

### 2.8. Comptages d'inventaire — `InventoryCountController`

Base : `/api/inventory/inventory-counts`

| Méthode | Chemin | Succès | Statut | Remarques |
|---|---|---|---|---|
| GET | `/` | 200 | **PARTIEL** | `page`, `pageSize` ; retourne tous les comptages, sans filtre entreprise/entrepôt. |
| POST | `/` | 200 | **PARTIEL** | Corps libre ; aucune validation ; peut omettre `company_id`, `warehouse_id`, `reference` → 500. |
| POST | `/{id}/scans` | 200 | **PARTIEL** | Vérifie l'existence du comptage ; enregistre une ligne de scan ; pas de calcul d'écart. |
| GET | `/{id}/results` | 200 | **PARTIEL** | Retourne les scans ; **200 `[]` si le comptage n'existe pas**. |
| POST | `/{id}/close` | 200 | **PARTIEL** | Sans corps : passe à `COMPLETED`, sans `completed_at`, sans réconciliation. |

**Constats spécifiques**

- `POST` et `close` renvoient 200, pas 201/204.
- `POST /{id}/scans` accepte `expected_warehouse_id`, `actual_warehouse_id`, `discrepancy_type` fournis par le client ; aucun calcul automatique à partir du stock ou de l'emplacement réel.
- `close` avec un corps invalide peut produire 500 `23514` (statut non autorisé).
- `last_inventory_at` n'est jamais mis à jour par la clôture.
- Aucun endpoint de lecture d'un comptage unique, de lignes attendues, de validation ou de réconciliation.

### 2.9. Opérations d'inventaire — `OperationController`

Base : `/api/inventory/operations`

| Méthode | Chemin | Succès | Statut | Remarques |
|---|---|---|---|---|
| POST | `/receipt` | 200 | **PARTIEL** | Crée uniquement un en-tête `inventory_operation`. |
| POST | `/issue` | 200 | **PARTIEL** | Même comportement que `receipt`. |
| POST | `/transfer` | 200 | **PARTIEL** | Même comportement que `receipt`. |

**Constats spécifiques**

- Les trois routes appellent la même méthode privée `save(request)`.
- `operation_type` n'est pas déduit du chemin : il doit être fourni dans le corps. Absent → 500 `23502`.
- Le corps peut fournir `operation_type: "ISSUE"` sur `/receipt` : aucune contrainte de route.
- Aucune ligne `inventory_operation_item`, aucun `stock_movement`, aucune mise à jour d'article.
- Aucune route de lecture, de complétion ou d'annulation.

### 2.10. QR Scan — `QrScanController`

Base : `/api/inventory/scans`

| Méthode | Chemin | Succès | Statut | Remarques |
|---|---|---|---|---|
| POST | `/resolve` | 200 | **PARTIEL** | Corps `{"qrCode":"..."}` ; retourne une liste d'articles, vide si inconnu. |

**Constats spécifiques**

- Requête globale : `SELECT * FROM security.item WHERE qr_code = ?`, sans `company_id`.
- Retourne la ligne brute de l'article, sans type lisible, entrepôt lisible, affectation active ou détenteur.
- Aucun journal de scan, aucune idempotence, aucune action associée.
- Un code inconnu renvoie 200 `[]`, pas 404.
- Un code qui ressemble à un UUID est converti en `UUID` avant le binding, ce qui peut produire une erreur de type PostgreSQL `42883` en 500 (non vérifié en exécution).

### 2.11. Tableau de bord — `DashboardController`

Base : `/api/inventory`

| Méthode | Chemin | Succès | Statut | Remarques |
|---|---|---|---|---|
| GET | `/dashboard/summary` | 200 | **PARTIEL** | Tableau d'un seul objet : nombre global d'articles, d'entrepôts, d'opérations. |
| GET | `/alerts/low-stock` | 200 | **PARTIEL** | Types dont `quantity_total < minimum_stock` ; globale, sans entreprise. |

**Constats spécifiques**

- `summary` ne filtre pas par entreprise et ne distingue pas les statuts.
- `low-stock` compare `quantity_total`, champ manuel, à `minimum_stock`. Ce n'est pas le stock réel.
- `stock_quantity` est un `Long` dérivé de `quantity_total` (`BigDecimal`), ce qui peut tronquer les décimales.

### 2.12. Rapports — `ReportController`

Base : `/api/inventory/reports`

| Méthode | Chemin | Succès | Statut | Remarques |
|---|---|---|---|---|
| GET | `/movements` | 200 | **PARTIEL** | Filtres `itemId`, `warehouseId`, `page`, `pageSize` ; pas de filtre date/type/entreprise. |

**Constats spécifiques**

- Retourne les champs complets du mouvement (`quantity`, `source_warehouse_id`, `destination_warehouse_id`, `operation_id`, `performed_by`), mais ces champs sont presque toujours nuls dans les données créées par l'API.
- Tri par `id`, pas par `movement_date`.
- Plafond 500, page par défaut 1.
- Aucun export CSV/PDF, aucun rapport agrégé.

---

## 3. Routes d'authentification et d'administration (24)

### 3.1. Authentification — `AuthController`

Base : `/api/auth`

| Méthode | Chemin | Succès | Statut | Remarques |
|---|---|---|---|---|
| POST | `/register` | 201 | **IMPLÉMENTÉ** | Réservé à `ROLE_ADMIN` ou `users.manage` ; entreprise déduite du jeton. Ne renvoie pas de jeton (`token: null`). |
| POST | `/login` | 200 | **IMPLÉMENTÉ** | Login/email + mot de passe. Identifiants invalides → **400**, pas 401. |
| GET | `/whoami` | 200 | **IMPLÉMENTÉ** | JWT requis ; 404 si l'utilisateur du jeton n'existe plus. |
| POST | `/refresh` | 200 | **IMPLÉMENTÉ** | Public ; `refreshToken` dans le corps ; rotation, rechargement des rôles/permissions, révocation de l'ancien token. |
| POST | `/logout` | 204 | **IMPLÉMENTÉ** | Révoque le refresh token fourni et le `jti` de l'access token courant. |

### 3.2. Utilisateurs — `AppUserController`

Base : `/api/users`, réservé à `ROLE_ADMIN` ou `users.manage` ; scope entreprise pour un admin d'entreprise.

| Méthode | Chemin | Succès | Statut |
|---|---|---|---|
| GET | `/` | 200 | **IMPLÉMENTÉ** |
| GET | `/{id}` | 200 / 404 | **IMPLÉMENTÉ** |
| POST | `/` | 201 | **IMPLÉMENTÉ** |
| PATCH | `/{id}` | 200 / 404 | **IMPLÉMENTÉ** |
| DELETE | `/{id}` | 204 | **IMPLÉMENTÉ** |
| POST | `/{userId}/roles/{roleId}` | 204 | **IMPLÉMENTÉ** |
| DELETE | `/{userId}/roles/{roleId}` | 204 | **IMPLÉMENTÉ** |

### 3.3. Rôles — `RoleController`

Base : `/api/roles`, réservé à `ROLE_ADMIN` ou `roles.manage` ; scope entreprise pour un admin d'entreprise.

| Méthode | Chemin | Succès | Statut |
|---|---|---|---|
| GET | `/` | 200 | **IMPLÉMENTÉ** |
| GET | `/{id}` | 200 / 404 | **IMPLÉMENTÉ** |
| POST | `/` | 201 | **IMPLÉMENTÉ** |
| PATCH | `/{id}` | 200 / 404 | **IMPLÉMENTÉ** |
| DELETE | `/{id}` | 204 | **IMPLÉMENTÉ** |
| POST | `/{roleId}/permissions/{permissionId}` | 204 | **IMPLÉMENTÉ** |
| DELETE | `/{roleId}/permissions/{permissionId}` | 204 | **IMPLÉMENTÉ** |

### 3.4. Permissions — `PermissionController`

Base : `/api/permissions`, réservé à `ROLE_ADMIN`.

| Méthode | Chemin | Succès | Statut |
|---|---|---|---|
| GET | `/` | 200 | **IMPLÉMENTÉ** |
| GET | `/{id}` | 200 / 404 | **IMPLÉMENTÉ** |
| POST | `/` | 201 | **IMPLÉMENTÉ** |
| PATCH | `/{id}` | 200 / 404 | **IMPLÉMENTÉ** |
| DELETE | `/{id}` | 204 | **IMPLÉMENTÉ** |

**Constat transversal après le Lot 1 :** les permissions `inventory.manage`, `users.manage` et `roles.manage` sont consommées par `SecurityConfig`. `/api/permissions/**` reste réservé à `ROLE_ADMIN`. Les modules organisation, personnel, clients et sites restent seulement `authenticated` ; le RBAC n'y est pas encore appliqué.

---

## 4. Routes des autres modules (50)

Ces routes ne sont pas détaillées une par une dans ce rapport, car elles sortent du cœur inventaire. Elles partagent le même modèle CRUD.

| Module | Contrôleurs | Routes | Base |
|---|---|---|---|
| Organisation | `CompanyController`, `AgencyController`, `BranchController` | 15 | `/api/companies`, `/api/agencies`, `/api/branches` |
| Personnel | `EmployeeController`, `EmployeeDocumentController` | 10 | `/api/employees`, `/api/employee-documents` |
| Clients | `ClientController`, `ClientContactController` | 10 | `/api/clients`, `/api/client-contacts` |
| Sites | `SiteController`, `SecurityPostController`, `PostRequirementController` | 15 | `/api/sites`, `/api/security-posts`, `/api/post-requirements` |

Toutes ces routes sont accessibles à **tout utilisateur authentifié**, car `SecurityConfig` ne leur applique pas de rôle. Elles ne sont pas filtrées par entreprise dans les DAO.

---

## 5. Synthèse des problèmes de contrat

| Problème | Routes concernées | Impact frontend |
|---|---|---|
| Erreurs douces en 200 | `assignment`, `inventory_count`, `operation` | Le frontend doit inspecter le corps pour savoir si l'action a réussi. |
| Conflits métier en 500 | doublons QR/code, doublons d'affectation, FK, `operation_type` manquant | Le frontend ne peut pas distinguer une erreur utilisateur d'une panne serveur. |
| Créations en 200 | `assignment`, `inventory_count`, `operation` | Contrat incohérent avec les autres modules en 201. |
| Pas de métadonnées de pagination | toutes les listes | Impossible d'afficher un total ou un nombre de pages. |
| Pas de plafond | `items`, `item-types`, `item-assignments`, etc. | Risque de réponse énorme. |
| Champs d'audit absents | affectations typées, mouvements | Impossible d'afficher « qui a fait quoi ». |
| Dates nulles à la création | mouvements et affectations | Le frontend doit recharger la ligne pour obtenir la date réelle. |
| Deux contrats d'affectation | `/api/item-assignments` et `/api/inventory/assignments` | Le frontend doit choisir un module, sans garantie d'équivalence. |
| Stubs silencieux | `item-types/low-stock`, `/items/{typeId}`, `/stocks/{typeId}` | Le frontend reçoit `[]` et croit qu'il n'y a rien. |

---

## 6. Cohérence avec OpenAPI

Les 26 fichiers OpenAPI couvrent les 26 contrôleurs métier, dont 12 fichiers pour les 12 contrôleurs d'inventaire. Ces 12 fichiers décrivent les mêmes routes que les contrôleurs, avec plusieurs dettes explicitement documentées :

- le double module d'affectation ;
- l'absence de déduction de `operation_type` depuis la route ;
- les erreurs douces en 200 ;
- les limites de pagination ;
- les champs non exposés.

Aucune route fantôme majeure n'a été détectée dans les spécifications. En revanche, la documentation ne compense pas les incohérences de comportement : elle les décrit parfois, ce qui confirme qu'elles sont connues.

La vérification OpenAPI de l'audit initial a relevé **183 avertissements Redocly non bloquants**, principalement des réponses d'erreur incomplètes, des exemples manquants et des `operationId` absents. Le décompte a été **rejoué au Lot 4** avec la version utilisée par la CI (`@redocly/cli@2.65.1`) : **179 avertissements, 0 erreur** sur les 26 fichiers. Les opérations écrites ou modifiées au Lot 4 portent désormais un `operationId` ; les avertissements restants concernent les opérations antérieures (`license`, `localhost`, exemples) et ne sont pas bloquants.

---

## 7. Conclusion

Le contrat HTTP est **vaste et lisible dans son ensemble**, mais il n'est pas **normalisé** : les statuts, les erreurs, les dates et les paginations varient d'un module à l'autre. Pour un frontend, les routes de lecture du catalogue sont utilisables ; les routes d'écriture de stock, d'affectation et d'opération ne le sont pas sans une couche d'adaptation qui masquerait des comportements faux.

Les endpoints manquants et les contrats proposés sont détaillés dans [`13-endpoints-manquants-proposes.md`](13-endpoints-manquants-proposes.md).
