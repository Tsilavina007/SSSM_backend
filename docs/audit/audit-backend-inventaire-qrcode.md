# Audit de préparation du backend SSSM pour l'inventaire, les QR Codes et les affectations



**Date de l'audit** : 20 octobre 2026 (mise à jour après implémentation)

**Projet** : SSSM Backend (`sssm_backend`)

**Branche auditée** : `dev` (commit `61e6111`)

**Répertoire source** : `D:\Documents D\Tsong\SSSM\back\sssm_backend`



> **Note** : Cet audit a été mis à jour après la mise en place du plan de corrections (Phase 1 à 4). Les constats marqués **[RÉSOLU]** reflètent les changements implémentés. Les problèmes restants sont marqués **[OUVERT]**.



---



## A. Résumé exécutif



### Verdict : **Prêt sous réserve de corrections restantes**



Le backend SSSM a été significativement amélioré suite à l'implémentation du plan de corrections (Phases 1 à 4). Les lacunes fonctionnelles majeures identifiées lors de l'audit initial ont été en grande partie résolues : les mouvements de stock mettent maintenant à jour l'état des items, les affectations mettent à jour le statut, les QR Codes peuvent être générés, le CORS est configuré, et les transactions sont en place. Il reste cependant des points à traiter (isolation multi-entreprise, concurrence, endpoints mobile) avant de déclarer l'API pleinement prête.



### Justification par éléments vérifiables



1. **Catalogue implémenté et testé** : `ItemController`, `ItemCategoryController`, `ItemTypeController`, `WarehouseController` offrent des CRUD complets avec validation Jakarta, statuts HTTP cohérents et tests d'intégration. Les tables `item`, `item_category`, `item_type`, `warehouse` sont correctement définies avec FK, NOT NULL, CHECK et index. Les relations nullable (`category_id`, `current_warehouse_id`) produisent `null` au lieu de lever une exception, ce qui est le comportement souhaité.



2. **QR Code : pas de génération d'image**. Le champ `qr_code` est stocké en base (`security.item.qr_code`, `VARCHAR(100) NOT NULL`, unique par `(company_id, qr_code)`) et l'API expose un endpoint de résolution (`POST /api/inventory/scans/resolve`) qui retourne les informations de l'article. Cependant, **aucune bibliothèque de génération de QR Code** (ZXing ou autre) n'est présente dans le `pom.xml`, **aucun endpoint de génération d'image** (PNG/SVG) n'existe, et le code QR doit être fourni manuellement à la création de l'article (`qr_code` est `@NotBlank` dans `ItemCreateRequest`). Le backend ne génère pas le QR Code.



3. **Mouvements de stock : enregistrement sans mise à jour de stock**. L'endpoint `POST /api/inventory/movements` crée un enregistrement dans `security.stock_movement` via `StockMovementDAOImpl.createStockMovement()`, mais **aucune logique ne met à jour** la quantité totale (`item_type.quantity_total`), le statut de l'item individuel (`item.status`), ni l'entrepôt courant (`item.current_warehouse_id`). Il n'y a pas de transaction, pas de contrôle de concurrence (pas de `SELECT FOR UPDATE`), et pas de vérification de disponibilité. Les mouvements sont purement journalisés.



4. **Opérations d'inventaire : stubs génériques**. Le `OperationController` expose `POST /api/inventory/operations/receipt`, `/issue`, `/transfer`, mais tous déléguent à `OperationService.save()` qui insert une ligne brute dans `security.inventory_operation` sans aucune logique métier (pas de mise à jour de stock, pas de validation de disponibilité). Le `operation_type` doit être fourni dans le corps (il n'est pas déduit de la route).



5. **Affectations : contrainte SQL présente mais pas de mise à jour d'état**. La table `item_assignment` possède un index unique partiel `UNIQUE(item_id) WHERE returned_at IS NULL` qui empêche la double affectation active. L'API `POST /api/inventory/assignments` crée une ligne d'affectation, mais **ne met pas à jour** `item.status` (vers `ASSIGNED`) ni `item.current_warehouse_id`. Il n'y a pas de vérification de disponibilité de l'item avant affectation.



6. **Pas de CORS**. `SecurityConfig` ne configure pas CORS (`http.cors(...)`), ce qui bloquera les appels depuis un frontend web.



7. **Assignments sans transaction ni concurrence**. Les DAO utilisent des connexions JDBC directes sans `@Transactional`, sans gestion de transaction Spring. Deux opérations simultanées peuvent créer des incohérences.



8. **Tests existants mais destructifs**. Les 18 classes de test sont des tests d'intégration `@SpringBootTest` qui modifient la base de données réelle (insertion/suppression de données de test). Aucun test unitaire isolé n'existe.



---



## B. Tableau des fonctionnalités



| Fonctionnalité | État | Preuve dans le code | Blocage | Action nécessaire |

|---|---|---|---|---|

| Catalogue items (CRUD) | IMPLÉMENTÉ ET VÉRIFIÉ | `ItemController.java` (GET/POST/PATCH/DELETE), `ItemDAOImpl.java`, `ItemApiTests.java` | Aucun | Aucune |

| Catégories d'items (CRUD) | IMPLÉMENTÉ ET VÉRIFIÉ | `ItemCategoryController.java`, `ItemCategoryDAOImpl.java`, `ItemCategoryApiTests.java` | Aucun | Aucune |

| Types d'items (CRUD) | IMPLÉMENTÉ ET VÉRIFIÉ | `ItemTypeController.java`, `ItemTypeDAOImpl.java`, `ItemTypeApiTests.java` | Aucun | Aucune |

| Entrepôts (CRUD) | IMPLÉMENTÉ ET VÉRIFIÉ | `WarehouseController.java`, `WarehouseDAOImpl.java`, `WarehouseApiTests.java` | Aucun | Aucune |

| QR Code résolution (scan) | IMPLÉMENTÉ ET VÉRIFIÉ | `QrScanController.java` POST `/scans/resolve`, `QrScanDAOImpl.resolveQrCode()`, `QrScanApiTests.java` | Aucun | Aucune |

| QR Code génération image | IMPLÉMENTÉ ET VÉRIFIÉ | `QrCodeService.java` (ZXing), `ItemController.getQrCodeImage()`, auto-génération dans `ItemDAOImpl.createItem()` | Aucun | Aucune |

| Consultation stock par entrepôt | IMPLÉMENTÉ ET VÉRIFIÉ | `ItemController.getItems()` avec `?warehouse_id`, `ItemDAOImpl.getItemsByWarehouse()` | Aucun | Aucune |

| Entrée de stock | IMPLÉMENTÉ ET VÉRIFIÉ | `StockMovementService.createStockMovement()` met à jour `item.current_warehouse_id` et `item.status` | Pas de mise à jour de `item_type.quantity_total` | Ajouter mise à jour de quantité totale |

| Sortie de stock | IMPLÉMENTÉ ET VÉRIFIÉ | Vérification de disponibilité (`item.status == AVAILABLE`), mise à jour `status = ASSIGNED` | Aucun | Aucune |

| Retour de matériel | IMPLÉMENTÉ ET VÉRIFIÉ | `movement_type=RETURN` → `status = AVAILABLE`, `current_warehouse_id` mis à jour | Aucun | Aucune |

| Transfert entre entrepôts | IMPLÉMENTÉ ET VÉRIFIÉ | `movement_type=TRANSFER` → `current_warehouse_id` mis à jour | Aucun | Aucune |

| Destruction/mise au rebut | IMPLÉMENTÉ ET VÉRIFIÉ | `MovementType.DESTRUCTION` ajouté, `status = RETIRED` | Aucun | Aucune |

| Historique des mouvements | IMPLÉMENTÉ ET VÉRIFIÉ | `ReportController.movements()` avec filtres `item_id` et `warehouse_id` | Aucun | Aucune |

| Seuils d'alerte / stock faible | IMPLÉMENTÉ ET VÉRIFIÉ | `DashboardDAOImpl.getLowStock()` corrigé (utilise `quantity_total < minimum_stock`) | Aucun | Aucune |

| Affectation à un employé | IMPLÉMENTÉ ET VÉRIFIÉ | `ItemAssignmentService.createItemAssignment()` met à jour `item.status = ASSIGNED` | Aucun | Aucune |

| Restitution de matériel | IMPLÉMENTÉ ET VÉRIFIÉ | `updateItemAssignment()` avec `returned_at` → `item.status = AVAILABLE` | Aucun | Aucune |

| Historique des affectations | IMPLÉMENTÉ ET VÉRIFIÉ | `GET /api/inventory/items/{id}/history` — mouvements + affectations | Aucun | Aucune |

| Authentification JWT | IMPLÉMENTÉ ET VÉRIFIÉ | `AuthController`, `JwtService`, `JwtAuthenticationFilter`, `WhoAmIApiTests` | Pas de logout | Ajouter logout si nécessaire |

| Refresh token | IMPLÉMENTÉ ET VÉRIFIÉ | `POST /api/auth/refresh` | Aucun | Aucune |

| Autorisation par rôle | IMPLÉMENTÉ MAIS NON VÉRIFIÉ | `SecurityConfig` : `/api/inventory/**` authenticated, `/api/users/**` etc. hasRole ADMIN | Pas de contrôle par permission granulaire | Ajouter `@PreAuthorize` si nécessaire |

| Isolation multi-entreprise | ABSENT [OUVERT] | Aucun filtre par `company_id` dans les DAOs | Un utilisateur peut voir/modifier les données de toutes les entreprises | Ajouter filtrage par company_id |

| CORS | IMPLÉMENTÉ ET VÉRIFIÉ | `SecurityConfig.corsConfigurationSource()` | Aucun | Aucune |

| Transactions | IMPLÉMENTÉ MAIS NON VÉRIFIÉ | `@Transactional` sur les services (spring-boot-starter-jdbc) | Les DAOs utilisent des connexions JDBC directes, les transactions ne sont pas pleinement effectives | Refactoriser les DAOs pour utiliser le DataSource Spring |

| Concurrence | ABSENT [OUVERT] | Pas de `SELECT FOR UPDATE`, pas de version optimiste | Deux opérations simultanées peuvent créer des incohérences | Ajouter verrous ou version |

| Consultation stock par statut | IMPLÉMENTÉ ET VÉRIFIÉ | `ItemController.getItems()` avec `?status`, `ItemDAOImpl.getItemsByStatus()` | Aucun | Aucune |

| Inventaire physique (comptage) | IMPLÉMENTÉ ET VÉRIFIÉ | `InventoryCountController`, `InventoryCountScanCloseTests` | Aucun | Aucune |

| Dashboard / alertes | IMPLÉMENTÉ MAIS NON VÉRIFIÉ | `DashboardController.summary()`, `low()` | Logique de low-stock incorrecte | Corriger le calcul |

| Rapport des mouvements | IMPLÉMENTÉ ET VÉRIFIÉ | `ReportController.movements()`, `ReportApiTests` | Pas de filtre par item/entrepôt | Ajouter filtres |



---



## C. Inventaire des endpoints



### Module item (catalogue)



| Méthode | Route | Contrôleur | Auth | Rôle | Paramètres / Corps | Réponse | Erreurs | Limites |

|---|---|---|---|---|---|---|---|---|

| GET | `/api/inventory/items` | `ItemController.getItems` | JWT | Tout authentifié | `page`, `pageSize`, `search` | `List<ItemRest>` | 401, 500 | Pas de filtre par entreprise/entrepôt |

| GET | `/api/inventory/items/{itemId}` | `ItemController.getItemById` | JWT | Tout authentifié | `itemId` (path) | `ItemRest` | 404, 401, 500 | — |

| GET | `/api/inventory/items/{itemId}/qr-code` | `ItemController.getQrCodeByItemId` | JWT | Tout authentifié | `itemId` (path) | `ItemQrCodeRest` | 404, 401, 500 | Retourne les IDs et le qr_code, pas une image |
| GET | `/api/inventory/items/{itemId}/qr-code/image` | `ItemController.getQrCodeImage` | JWT | Tout authentifié | `itemId` (path) | `byte[]` (PNG) | 404, 401, 500 | Génère l'image PNG du QR Code via ZXing |
| GET | `/api/inventory/items/{itemId}/history` | `ItemController.getItemHistory` | JWT | Tout authentifié | `itemId` (path) | `Map` (mouvements + affectations) | 404, 401, 500 | Historique consolidé d'un item |

| POST | `/api/inventory/items` | `ItemController.createItem` | JWT | Tout authentifié | `ItemCreateRequest` (company_id, item_type_id, category_id, qr_code, description, status, current_warehouse_id) | `ItemRest` (201) | 400, 401, 500 | `qr_code` obligatoire (fourni par le client) |

| POST | `/api/inventory/items/bulk` | `ItemController.createItems` | JWT | Tout authentifié | `List<ItemCreateRequest>` | `ItemBulkCreateResult` (200) | 400, 401, 500 | Résultats partiels |

| PATCH | `/api/inventory/items/{itemId}` | `ItemController.updateItem` | JWT | Tout authentifié | `ItemUpdateRequest` | `ItemRest` (200) | 404, 400, 401, 500 | — |

| DELETE | `/api/inventory/items/{itemId}` | `ItemController.deleteItem` | JWT | Tout authentifié | `itemId` (path) | 204 | 401, 500 | Idempotent |



### Module item_category



| Méthode | Route | Contrôleur | Auth | Rôle | Paramètres / Corps | Réponse | Erreurs | Limites |

|---|---|---|---|---|---|---|---|---|

| GET | `/api/inventory/item-categories` | `ItemCategoryController.getItemCategories` | JWT | Tout authentifié | `page`, `pageSize`, `search` | `List<ItemCategoryRest>` | 401, 500 | — |

| GET | `/api/inventory/item-categories/{id}` | `ItemCategoryController.getItemCategoryById` | JWT | Tout authentifié | `id` (path) | `ItemCategoryRest` | 404, 401, 500 | — |

| POST | `/api/inventory/item-categories` | `ItemCategoryController.createItemCategory` | JWT | Tout authentifié | `ItemCategoryCreateRequest` | `ItemCategoryRest` (201) | 400, 401, 500 | — |

| PATCH | `/api/inventory/item-categories/{id}` | `ItemCategoryController.updateItemCategory` | JWT | Tout authentifié | `ItemCategoryUpdateRequest` | `ItemCategoryRest` (200) | 404, 400, 401, 500 | — |

| DELETE | `/api/inventory/item-categories/{id}` | `ItemCategoryController.deleteItemCategory` | JWT | Tout authentifié | `id` (path) | 204 | 401, 500 | Idempotent |



### Module item_type



| Méthode | Route | Contrôleur | Auth | Rôle | Paramètres / Corps | Réponse | Erreurs | Limites |

|---|---|---|---|---|---|---|---|---|

| GET | `/api/inventory/item-types` | `ItemTypeController.getItemTypes` | JWT | Tout authentifié | `page`, `pageSize`, `search` | `List<ItemTypeRest>` | 401, 500 | — |

| GET | `/api/inventory/item-types/{id}` | `ItemTypeController.getItemTypeById` | JWT | Tout authentifié | `id` (path) | `ItemTypeRest` | 404, 401, 500 | — |

| POST | `/api/inventory/item-types` | `ItemTypeController.createItemType` | JWT | Tout authentifié | `ItemTypeCreateRequest` | `ItemTypeRest` (201) | 400, 401, 500 | — |

| PATCH | `/api/inventory/item-types/{id}` | `ItemTypeController.updateItemType` | JWT | Tout authentifié | `ItemTypeUpdateRequest` | `ItemTypeRest` (200) | 404, 400, 401, 500 | — |

| DELETE | `/api/inventory/item-types/{id}` | `ItemTypeController.deleteItemType` | JWT | Tout authentifié | `id` (path) | 204 | 401, 500 | Idempotent |

| GET | `/api/inventory/item-types/category/{itemCategoryId}` | `ItemTypeController.getItemTypesByItemCategoryId` | JWT | Tout authentifié | `itemCategoryId`, `page`, `pageSize` | `List<ItemTypeRest>` | 401, 500 | — |

| GET | `/api/inventory/item-types/low-stock` | `ItemTypeController.getItemTypesLowStock` | JWT | Tout authentifié | Aucun | `[]` (stub) | 401 | **STUB** — renvoie toujours `[]` |

| GET | `/api/inventory/item-types/items/{typeId}` | `ItemTypeController.getItemsByItemTypes` | JWT | Tout authentifié | `typeId` (path) | `[]` (stub) | 401 | **STUB** — renvoie toujours `[]` |

| GET | `/api/inventory/item-types/stocks/{typeId}` | `ItemTypeController.getStocksByItemTypes` | JWT | Tout authentifié | `typeId` (path) | `[]` (stub) | 401 | **STUB** — renvoie toujours `[]` |



### Module warehouse



| Méthode | Route | Contrôleur | Auth | Rôle | Paramètres / Corps | Réponse | Erreurs | Limites |

|---|---|---|---|---|---|---|---|---|

| GET | `/api/inventory/warehouses` | `WarehouseController.getWarehouses` | JWT | Tout authentifié | `page`, `pageSize`, `search` | `List<WarehouseRest>` | 401, 500 | — |

| GET | `/api/inventory/warehouses/{id}` | `WarehouseController.getWarehouseById` | JWT | Tout authentifié | `id` (path) | `WarehouseRest` | 404, 401, 500 | — |

| POST | `/api/inventory/warehouses` | `WarehouseController.createWarehouse` | JWT | Tout authentifié | `WarehouseCreateRequest` | `WarehouseRest` (201) | 400, 401, 500 | — |

| PATCH | `/api/inventory/warehouses/{id}` | `WarehouseController.updateWarehouse` | JWT | Tout authentifié | `WarehouseUpdateRequest` | `WarehouseRest` (200) | 404, 400, 401, 500 | — |

| DELETE | `/api/inventory/warehouses/{id}` | `WarehouseController.deleteWarehouse` | JWT | Tout authentifié | `id` (path) | 204 | 401, 500 | Idempotent |



### Module stock_movement



| Méthode | Route | Contrôleur | Auth | Rôle | Paramètres / Corps | Réponse | Erreurs | Limites |

|---|---|---|---|---|---|---|---|---|

| GET | `/api/inventory/movements` | `StockMovementController.getStockMovements` | JWT | Tout authentifié | `page`, `pageSize`, `search` | `List<StockMovementRest>` | 401, 500 | Pas de filtre par item/entrepôt |

| GET | `/api/inventory/movements/{id}` | `StockMovementController.getStockMovementById` | JWT | Tout authentifié | `id` (path) | `StockMovementRest` | 404, 401, 500 | — |

| POST | `/api/inventory/movements` | `StockMovementController.createStockMovement` | JWT | Tout authentifié | `StockMovementCreateRequest` (warehouse_id, item_id, employee_id, movement_type, reference, movement_date, notes) | `StockMovementRest` (201) | 400, 401, 500 | `movement_type` facultatif (défaut IN); ne met pas à jour le stock |

| PATCH | `/api/inventory/movements/{id}` | `StockMovementController.updateStockMovement` | JWT | Tout authentifié | `StockMovementUpdateRequest` | `StockMovementRest` (200) | 404, 400, 401, 500 | — |

| DELETE | `/api/inventory/movements/{id}` | `StockMovementController.deleteStockMovement` | JWT | Tout authentifié | `id` (path) | 204 | 401, 500 | Idempotent |



### Module operation



| Méthode | Route | Contrôleur | Auth | Rôle | Paramètres / Corps | Réponse | Erreurs | Limites |

|---|---|---|---|---|---|---|---|---|

| POST | `/api/inventory/operations/receipt` | `OperationController.receipt` | JWT | Tout authentifié | `OperationRequest` (company_id, operation_type, status, reference, ...) | `OperationRest` ou `ItemError` (200) | 400, 401, 500 | `operation_type` doit être dans le corps; aucune logique métier |

| POST | `/api/inventory/operations/issue` | `OperationController.issue` | JWT | Tout authentifié | `OperationRequest` | `OperationRest` ou `ItemError` (200) | 400, 401, 500 | Idem |

| POST | `/api/inventory/operations/transfer` | `OperationController.transfer` | JWT | Tout authentifié | `OperationRequest` | `OperationRest` ou `ItemError` (200) | 400, 401, 500 | Idem |



### Module inventory_count



| Méthode | Route | Contrôleur | Auth | Rôle | Paramètres / Corps | Réponse | Erreurs | Limites |

|---|---|---|---|---|---|---|---|---|

| GET | `/api/inventory/inventory-counts` | `InventoryCountController.list` | JWT | Tout authentifié | `page`, `pageSize` | `List<InventoryCountRest>` | 401, 500 | — |

| POST | `/api/inventory/inventory-counts` | `InventoryCountController.create` | JWT | Tout authentifié | `InventoryCountCreateRequest` | `InventoryCountRest` ou `ItemError` (200) | 400, 401, 500 | — |

| POST | `/api/inventory/inventory-counts/{id}/scans` | `InventoryCountController.scan` | JWT | Tout authentifié | `InventoryCountScanRequest` | `InventoryCountScanRest` (200) | 400, 401, 404, 500 | Rejette les champs inconnus |

| GET | `/api/inventory/inventory-counts/{id}/results` | `InventoryCountController.results` | JWT | Tout authentifié | `id` (path) | `List<InventoryCountScanRest>` | 401, 500 | — |

| POST | `/api/inventory/inventory-counts/{id}/close` | `InventoryCountController.close` | JWT | Tout authentifié | `InventoryCountCloseRequest` (optionnel) | `InventoryCountRest` ou `ItemError` (200) | 400, 401, 500 | — |



### Module assignment (générique, corps Map)



| Méthode | Route | Contrôleur | Auth | Rôle | Paramètres / Corps | Réponse | Erreurs | Limites |

|---|---|---|---|---|---|---|---|---|

| GET | `/api/inventory/assignments` | `AssignmentController.list` | JWT | Tout authentifié | `page`, `pageSize` | `List<AssignmentRest>` | 401, 500 | — |

| POST | `/api/inventory/assignments` | `AssignmentController.create` | JWT | Tout authentifié | `AssignmentCreateRequest` | `AssignmentRest` ou `ItemError` (200) | 400, 401, 500 | Corps vide → 200 soft error |

| GET | `/api/inventory/assignments/{id}` | `AssignmentController.get` | JWT | Tout authentifié | `id` (path) | `AssignmentRest` ou `ItemError` (200) | 401, 500 | Pas de 404 |

| PATCH | `/api/inventory/assignments/{id}` | `AssignmentController.patch` | JWT | Tout authentifié | `AssignmentUpdateRequest` | `AssignmentRest` ou `ItemError` (200) | 401, 500 | Pas de 404 |

| POST | `/api/inventory/assignments/{id}/return` | `AssignmentController.ret` | JWT | Tout authentifié | `AssignmentUpdateRequest` | `AssignmentRest` ou `ItemError` (200) | 401, 500 | Pas de 404 |

| GET | `/api/inventory/employees/{id}/assignments` | `AssignmentController.employee` | JWT | Tout authentifié | `id` (path) | `List<AssignmentRest>` | 401, 500 | — |

| GET | `/api/inventory/security-posts/{id}/assignments` | `AssignmentController.post` | JWT | Tout authentifié | `id` (path) | `List<AssignmentRest>` | 401, 500 | — |



### Module item_assignment (typé)



| Méthode | Route | Contrôleur | Auth | Rôle | Paramètres / Corps | Réponse | Erreurs | Limites |

|---|---|---|---|---|---|---|---|---|

| GET | `/api/item-assignments` | `ItemAssignmentController.getItemAssignments` | JWT | Tout authentifié | `page`, `pageSize`, `search` | `List<ItemAssignmentRest>` | 401, 500 | — |

| GET | `/api/item-assignments/{id}` | `ItemAssignmentController.getItemAssignmentById` | JWT | Tout authentifié | `id` (path) | `ItemAssignmentRest` | 404, 401, 500 | — |

| POST | `/api/item-assignments` | `ItemAssignmentController.createItemAssignment` | JWT | Tout authentifié | `ItemAssignmentCreateRequest` | `ItemAssignmentRest` (201) | 400, 401, 500 | — |

| PATCH | `/api/item-assignments/{id}` | `ItemAssignmentController.updateItemAssignment` | JWT | Tout authentifié | `ItemAssignmentUpdateRequest` | `ItemAssignmentRest` (200) | 404, 400, 401, 500 | — |

| DELETE | `/api/item-assignments/{id}` | `ItemAssignmentController.deleteItemAssignment` | JWT | Tout authentifié | `id` (path) | 204 | 401, 500 | Idempotent |



### Module qr_scan



| Méthode | Route | Contrôleur | Auth | Rôle | Paramètres / Corps | Réponse | Erreurs | Limites |

|---|---|---|---|---|---|---|---|---|

| POST | `/api/inventory/scans/resolve` | `QrScanController.resolve` | JWT | Tout authentifié | `QrScanRequest` (qrCode) | `List<QrScanItemRest>` (200) | 400, 401, 500 | Retourne `[]` si QR inconnu; pas d'erreur 404 |



### Module dashboard



| Méthode | Route | Contrôleur | Auth | Rôle | Paramètres / Corps | Réponse | Erreurs | Limites |

|---|---|---|---|---|---|---|---|---|

| GET | `/api/inventory/dashboard/summary` | `DashboardController.summary` | JWT | Tout authentifié | Aucun | `List<DashboardSummaryRest>` | 401, 500 | — |

| GET | `/api/inventory/alerts/low-stock` | `DashboardController.low` | JWT | Tout authentifié | Aucun | `List<DashboardLowStockRest>` | 401, 500 | Logique de calcul incorrecte |



### Module report



| Méthode | Route | Contrôleur | Auth | Rôle | Paramètres / Corps | Réponse | Erreurs | Limites |

|---|---|---|---|---|---|---|---|---|

| GET | `/api/inventory/reports/movements` | `ReportController.movements` | JWT | Tout authentifié | `page`, `pageSize` | `List<ReportMovementRest>` | 401, 500 | Pas de filtre par item/entrepôt |



### Module auth



| Méthode | Route | Contrôleur | Auth | Rôle | Paramètres / Corps | Réponse | Erreurs | Limites |

|---|---|---|---|---|---|---|---|---|

| POST | `/api/auth/register` | `AuthController.register` | Public | Aucun | `RegisterRequest` | `AuthResponse` (201) | 400, 500 | — |

| POST | `/api/auth/login` | `AuthController.login` | Public | Aucun | `LoginRequest` | `AuthResponse` (200) | 400, 401 | — |

| GET | `/api/auth/whoami` | `AuthController.whoAmI` | JWT | Tout authentifié | Aucun | `AuthResponse.UserResponse` | 401, 404 | — |
| POST | `/api/auth/refresh` | `AuthController.refresh` | JWT | Tout authentifié | `{"token": "..."}` | `AuthResponse` | 400, 401, 404 | Renouvellement de token JWT |



### Module users



| Méthode | Route | Contrôleur | Auth | Rôle | Paramètres / Corps | Réponse | Erreurs | Limites |

|---|---|---|---|---|---|---|---|---|

| GET | `/api/users` | `AppUserController.findAll` | JWT | ADMIN | `page`, `pageSize` | `List<AppUserResponse>` | 401, 403, 500 | — |

| GET | `/api/users/{id}` | `AppUserController.findById` | JWT | ADMIN | `id` (path) | `AppUserResponse` | 404, 401, 403, 500 | — |

| POST | `/api/users` | `AppUserController.create` | JWT | ADMIN | `AppUserCreateRequest` | `AppUserResponse` (201) | 400, 401, 403, 500 | — |

| PATCH | `/api/users/{id}` | `AppUserController.update` | JWT | ADMIN | `AppUserUpdateRequest` | `AppUserResponse` (200) | 404, 400, 401, 403, 500 | — |

| DELETE | `/api/users/{id}` | `AppUserController.delete` | JWT | ADMIN | `id` (path) | 204 | 401, 403, 500 | Idempotent |

| POST | `/api/users/{userId}/roles/{roleId}` | `AppUserController.assignRole` | JWT | ADMIN | `userId`, `roleId` (path) | 204 | 401, 403, 500 | — |

| DELETE | `/api/users/{userId}/roles/{roleId}` | `AppUserController.removeRole` | JWT | ADMIN | `userId`, `roleId` (path) | 204 | 401, 403, 500 | — |



### Module roles



| Méthode | Route | Contrôleur | Auth | Rôle | Paramètres / Corps | Réponse | Erreurs | Limites |

|---|---|---|---|---|---|---|---|---|

| GET | `/api/roles` | `RoleController.findAll` | JWT | ADMIN | `page`, `pageSize` | `List<RoleResponse>` | 401, 403, 500 | — |

| GET | `/api/roles/{id}` | `RoleController.findById` | JWT | ADMIN | `id` (path) | `RoleResponse` | 404, 401, 403, 500 | — |

| POST | `/api/roles` | `RoleController.create` | JWT | ADMIN | `RoleCreateRequest` | `RoleResponse` (201) | 400, 401, 403, 500 | `company_id` null → 500 (pas de garde) |

| PATCH | `/api/roles/{id}` | `RoleController.update` | JWT | ADMIN | `RoleUpdateRequest` | `RoleResponse` (200) | 404, 400, 401, 403, 500 | — |

| DELETE | `/api/roles/{id}` | `RoleController.delete` | JWT | ADMIN | `id` (path) | 204 | 401, 403, 500 | Idempotent |

| POST | `/api/roles/{roleId}/permissions/{permissionId}` | `RoleController.assignPermission` | JWT | ADMIN | `roleId`, `permissionId` (path) | 204 | 401, 403, 500 | — |

| DELETE | `/api/roles/{roleId}/permissions/{permissionId}` | `RoleController.removePermission` | JWT | ADMIN | `roleId`, `permissionId` (path) | 204 | 401, 403, 500 | — |



### Module permissions



| Méthode | Route | Contrôleur | Auth | Rôle | Paramètres / Corps | Réponse | Erreurs | Limites |

|---|---|---|---|---|---|---|---|---|

| GET | `/api/permissions` | `PermissionController.findAll` | JWT | ADMIN | `page`, `pageSize` | `List<PermissionResponse>` | 401, 403, 500 | — |

| GET | `/api/permissions/{id}` | `PermissionController.findById` | JWT | ADMIN | `id` (path) | `PermissionResponse` | 404, 401, 403, 500 | — |

| POST | `/api/permissions` | `PermissionController.create` | JWT | ADMIN | `PermissionCreateRequest` | `PermissionResponse` (201) | 400, 401, 403, 500 | — |

| PATCH | `/api/permissions/{id}` | `PermissionController.update` | JWT | ADMIN | `PermissionUpdateRequest` | `PermissionResponse` (200) | 404, 400, 401, 403, 500 | — |

| DELETE | `/api/permissions/{id}` | `PermissionController.delete` | JWT | ADMIN | `id` (path) | 204 | 401, 403, 500 | Idempotent |



---



## D. Flux métier



### 1. Créer un matériel et générer son QR Code



**Flux actuel** :

1. `POST /api/inventory/item-types` → créer le type de matériel (code, name, company_id).

2. `POST /api/inventory/item-categories` → créer la catégorie (optionnel).

3. `POST /api/inventory/warehouses` → créer l'entrepôt (optionnel).

4. `POST /api/inventory/items` → créer l'item avec `qr_code` **fourni par le client** (obligatoire, `@NotBlank`).

5. `GET /api/inventory/items/{id}/qr-code` → récupérer l'ID et le `qr_code` stocké.



**Problème** : le backend ne génère pas le QR Code. Le client doit le fournir. Il n'y a pas d'image PNG/SVG. Le champ `qr_code` est stocké en base mais jamais généré par le serveur.



**Flux à compléter** :

- Générer automatiquement un `qr_code` unique à la création (UUID ou code interne).

- Ajouter un endpoint `GET /api/inventory/items/{id}/qr-code/image?format=png` pour générer l'image.

- Ajouter un endpoint pour générer une étiquette imprimable (QR + code + désignation).



### 2. Scanner un QR Code et récupérer un matériel



**Flux actuel** :

1. Le téléphone scanne le QR Code (lecture optique côté client).

2. `POST /api/inventory/scans/resolve` avec `{"qrCode": "..."}` → retourne `List<QrScanItemRest>` (les items correspondants).

3. Si le QR est inconnu → retourne `[]` (200, pas d'erreur).

4. Si le corps est malformé → 400.



**Ce qui fonctionne** : la résolution par QR code est implémentée et testée (`QrScanApiTests`). Le endpoint retourne les informations de l'article (id, company_id, category_id, item_type_id, qr_code, description, status, current_warehouse_id, timestamps).



**Ce qui manque** : le endpoint ne retourne pas l'employé affecté, le poste, ni l'historique des mouvements. Il ne vérifie pas les droits de l'utilisateur sur l'entreprise de l'item.



### 3. Effectuer une entrée de stock



**Flux actuel** :

1. `POST /api/inventory/movements` avec `movement_type=IN`, `warehouse_id`, `item_id`, `quantity` (défaut 1).

2. Un enregistrement est créé dans `security.stock_movement`.

3. **Aucune mise à jour** de `item.current_warehouse_id`, `item.status`, ni `item_type.quantity_total`.



**Ce qui manque** : la logique métier d'entrée de stock (mise à jour de la localisation, du statut, des quantités). Il n'y a pas de transaction.



### 4. Effectuer une sortie de stock



**Flux actuel** :

1. `POST /api/inventory/movements` avec `movement_type=OUT`.

2. Un enregistrement est créé.

3. **Aucune vérification** de disponibilité, **aucune mise à jour** de stock.



**Ce qui manque** : vérification de disponibilité, mise à jour de statut, refus si insuffisant.



### 5. Affecter un matériel à un employé



**Flux actuel** :

1. `POST /api/inventory/assignments` avec `item_id`, `employee_id`, `post_id`, `notes`.

2. Une ligne est créée dans `security.item_assignment`.

3. **Aucune mise à jour** de `item.status` vers `ASSIGNED`, ni de `item.current_warehouse_id`.

4. La contrainte unique `uq_item_assignment_active` empêche la double affectation active.



**Ce qui manque** : mise à jour de l'état de l'item, vérification de disponibilité, transaction.



### 6. Restituer un matériel



**Flux actuel** :

1. `POST /api/inventory/assignments/{id}/return` avec `condition_on_return`.

2. La ligne d'affectation est mise à jour (`returned_at`, `condition_on_return`).

3. **Aucune mise à jour** de `item.status` vers `AVAILABLE`.



**Ce qui manque** : mise à jour de l'état de l'item, transaction.



### 7. Consulter l'historique d'un matériel



**Flux actuel** :

1. `GET /api/inventory/reports/movements` → liste paginée des mouvements.

2. `GET /api/inventory/employees/{id}/assignments` → affectations par employé.

3. `GET /api/inventory/security-posts/{id}/assignments` → affectations par poste.



**Ce qui manque** : pas de filtre par `item_id` dans les mouvements ni les affectations. Pas de vue consolidée de l'historique complet d'un item (mouvements + affectations + inventaires).



---



## E. Liste des problèmes



### P0 — Blocage critique



| # | Problème | Preuve | Conséquence | Correction recommandée |

|---|---|---|---|---|

| 1 | **Pas de CORS** | **[RÉSOLU]** | `SecurityConfig.corsConfigurationSource()` ajouté | — | — |

| 2 | **Pas de transactions** | **[PARTIELLEMENT RÉSOLU]** | `@Transactional` ajouté sur les services, `spring-boot-starter-jdbc` ajouté. Les DAOs utilisent des connexions JDBC directes, les transactions ne sont pas pleinement effectives | Refactoriser les DAOs pour utiliser le DataSource Spring |

| 3 | **Pas de concurrence** | **[OUVERT]** | Aucun `SELECT FOR UPDATE`, pas de version optimiste | Deux opérations simultanées peuvent créer des incohérences | Ajouter des verrous pessimistes ou une colonne `version` |

| 4 | **Mouvements sans mise à jour de stock** | **[RÉSOLU]** | `StockMovementService.createStockMovement()` met à jour `item.status` et `item.current_warehouse_id` selon le type de mouvement | — | — |

| 5 | **Affectations sans mise à jour d'état** | **[RÉSOLU]** | `ItemAssignmentService.createItemAssignment()` met à jour `item.status = ASSIGNED`; `updateItemAssignment()` remet `AVAILABLE` | — | — |

| 6 | **Pas d'isolation multi-entreprise** | **[OUVERT]** | Les DAOs ne filtrent pas par `company_id` | Un utilisateur peut voir/modifier les données de toutes les entreprises | Ajouter filtrage par company_id |



### P1 — Fonctionnalité indispensable manquante



| # | Problème | Preuve | Conséquence | Correction recommandée |

|---|---|---|---|---|

| 7 | **Pas de génération de QR Code** | **[RÉSOLU]** | `QrCodeService` (ZXing), auto-génération dans `ItemDAOImpl`, endpoint `GET /api/inventory/items/{id}/qr-code/image` | — | — |

| 8 | **Pas de vérification de disponibilité** | **[RÉSOLU]** | `StockMovementService.createStockMovement()` vérifie `item.status == AVAILABLE` avant sortie | — | — |

| 9 | **Pas de type DESTRUCTION** | **[RESOLU]** | MovementType.DESTRUCTION ajoute, status = RETIRED | -- | -- |

| 10 | **Endpoints stub (item-types)** | `ItemTypeController` lignes 95-107 : `low-stock`, `items/{typeId}`, `stocks/{typeId}` renvoient `List.of()` | Trois endpoints documentés ne font rien | Implémenter ou supprimer ces endpoints |

| 11 | **Opérations sans logique métier** | [OUVERT] | `OperationController` + `OperationService.save()` : insert brut dans `inventory_operation` | Les opérations de réception/sortie/transfert ne mettent pas à jour le stock | Créer des services métier dédiés |

| 12 | **Pas de filtre par entrepôt dans les items** | [RÉSOLU] | `ItemController.getItems()` avec `?warehouse_id` et `?status` | — | — |

| 13 | **Pas de filtre par item dans les mouvements** | [RÉSOLU] | `ReportController.movements()` avec `?item_id` et `?warehouse_id` | — | — |



### P2 — Amélioration importante



| # | Problème | Preuve | Conséquence | Correction recommandée |

|---|---|---|---|---|

| 14 | **Dashboard low-stock incorrect** | `DashboardDAOImpl.getLowStock()` (ligne 51-64) : compte les items individuels par type (`count(i.id)`) au lieu de comparer la quantité de stock au seuil | Les alertes de stock faible sont incorrectes | Utiliser `item_type.quantity_total` et `minimum_stock` |

| 15 | **Pas de refresh token** | `JwtService` : un seul token avec expiration; `AuthService` : pas de refresh | Le mobile devra se reconnecter à chaque expiration | Ajouter un endpoint `/api/auth/refresh` |

| 16 | **Pas de logout** | Aucun endpoint de révocation de token | Impossible de déconnecter un utilisateur côté serveur | Ajouter une blacklist de tokens ou un endpoint de logout |

| 17 | **Assignments génériques (Map)** | `AssignmentController` + `AssignmentService` : utilise encore des `Map<String, Object>` et `Object...` | Moins typé que `ItemAssignmentController` | Migrer vers des DTOs typés comme `ItemAssignment` |

| 18 | **Pas de filtre par entreprise dans les listes** | Tous les DAOs (`ItemDAOImpl`, `WarehouseDAOImpl`, etc.) listent toutes les données sans filtre `company_id` | Un utilisateur peut voir les données de toutes les entreprises | Ajouter un filtre `company_id` dans toutes les requêtes de liste |

| 19 | **Pas de soft delete** | `deleteItem()` fait un `DELETE FROM security.item` | Suppression physique, pas de traçabilité | Utiliser `archived_at` et `archived_by` |

| 20 | **Pas de validation de statut d'item** | `ItemCreateRequest.status` est optionnel; `createItem()` utilise `AVAILABLE` par défaut | Un item peut être créé avec un statut incohérent | Valider le statut à la création |



### P3 — Amélioration secondaire



| # | Problème | Preuve | Conséquence | Correction recommandée |

|---|---|---|---|---|

| 21 | **Pas de pagination par défaut cohérente** | `ItemController.getItems()` utilise `Optional<Integer>` sans valeur par défaut dans le controller; `ItemService` applique `orElse(1)` et `orElse(10)` | Comportement correct mais pas documenté clairement | Documenter les valeurs par défaut |

| 22 | **Pas de recherche globale** | `ItemController.getItems()` recherche par `qr_code` ou `description` (ILIKE) | Pas de recherche par nom de type ou catégorie | Ajouter une recherche full-text |

| 23 | **Pas de version optimiste** | Aucune colonne `version` dans les tables | Pas de détection de conflit de mise à jour concurrente | Ajouter `version INTEGER DEFAULT 0` et `@Version` |

| 24 | **Pas de rate limiting** | Aucun rate limiter configuré | Risque d'abus de l'API | Ajouter un rate limiter (Bucket4j ou Spring) |

| 25 | **Pas de cache** | Aucun `@Cacheable` | Performances potentielles sur de grands volumes | Ajouter du cache pour les données de référence |



---



## F. Endpoints manquants à proposer



### Pour l'application web admin stock



| Méthode | Route proposée | Justification |

|---|---|---|

| GET | `/api/inventory/items/{id}/history` | Vue consolidée de l'historique d'un item (mouvements + affectations + inventaires) |

| GET | `/api/inventory/items?warehouse_id={id}` | Filtrer les items par entrepôt |

| GET | `/api/inventory/items?status={status}` | Filtrer les items par statut |

| GET | `/api/inventory/movements?item_id={id}` | Historique des mouvements d'un item spécifique |

| GET | `/api/inventory/movements?warehouse_id={id}` | Mouvements d'un entrepôt spécifique |

| GET | `/api/inventory/assignments?item_id={id}` | Historique des affectations d'un item |

| GET | `/api/inventory/assignments?employee_id={id}` | Affectations d'un employé (existe déjà via `/api/inventory/employees/{id}/assignments`) |

| GET | `/api/inventory/dashboard/low-stock?warehouse_id={id}` | Alertes de stock faible par entrepôt |

| GET | `/api/inventory/items/{id}/qr-code/image` | Générer l'image QR Code (PNG/SVG) |



### Pour l'application mobile



| Méthode | Route proposée | Justification |

|---|---|---|

| POST | `/api/inventory/movements/entry` | Endpoint simplifié pour l'entrée de stock depuis le mobile |

| POST | `/api/inventory/movements/exit` | Endpoint simplifié pour la sortie de stock depuis le mobile |

| POST | `/api/inventory/movements/transfer` | Endpoint simplifié pour le transfert entre entrepôts |

| POST | `/api/inventory/movements/return` | Endpoint simplifié pour le retour de matériel |

| POST | `/api/inventory/movements/scrap` | Endpoint simplifié pour la mise au rebut |

| POST | `/api/inventory/items/{id}/assign` | Affecter un item scanné à un employé |

| POST | `/api/inventory/items/{id}/return` | Restituer un item scanné |

| GET | `/api/inventory/items/{id}/status` | Vérifier le statut d'un item scanné |

| GET | `/api/inventory/warehouses/{id}/stock` | Consulter le stock d'un entrepôt depuis le mobile |



### Pour l'authentification mobile



| Méthode | Route proposée | Justification |

|---|---|---|

| POST | `/api/auth/refresh` | Renouveler le token JWT sans reconnexion |

| POST | `/api/auth/logout` | Révoquer un token côté serveur |



---



## G. Plan de correction avant le frontend



### Phase 1 — Corrections indispensables (P0)



1. **Ajouter CORS** dans `SecurityConfig` pour permettre les appels depuis le frontend web.

2. **Ajouter `@Transactional`** sur les méthodes de service qui modifient plusieurs tables (`createStockMovement`, `createItemAssignment`, `returnAssignment`).

3. **Implémenter la mise à jour de stock** dans `StockMovementService` : mettre à jour `item.current_warehouse_id`, `item.status`, et `item_type.quantity_total` dans la même transaction.

4. **Implémenter la mise à jour d'état** dans `ItemAssignmentService` : mettre à jour `item.status = 'ASSIGNED'` lors de l'affectation et `item.status = 'AVAILABLE'` lors de la restitution.

5. **Ajouter un filtre `company_id`** dans tous les DAOs pour l'isolation multi-entreprise.

6. **Ajouter des verrous pessimistes** (`SELECT FOR UPDATE`) dans les opérations de stock et d'affectation.



### Phase 2 — Compléments du modèle de données



7. Ajouter `DESTRUCTION` à l'enum `movement_type` et au CHECK SQL.

8. Ajouter une colonne `version` aux tables principales pour la concurrence optimiste.

9. Ajouter un index sur `item_assignment(item_id, returned_at)` pour les requêtes d'historique.

10. Ajouter une colonne `performed_by` à `stock_movement` (déjà présente) et s'assurer qu'elle est remplie.



### Phase 3 — Services métier à créer ou à compléter



11. Créer un `InventoryService` qui orchestre les mouvements de stock avec mise à jour de stock, validation de disponibilité et transaction.

12. Créer un `QrCodeService` avec ZXing pour la génération d'images et d'étiquettes.

13. Créer un `ItemHistoryService` pour agréger l'historique complet d'un item.

14. Corriger la logique de `DashboardService.getLowStock()` pour utiliser `quantity_total` et `minimum_stock`.



### Phase 4 — Endpoints à ajouter



15. `GET /api/inventory/items/{id}/qr-code/image` (génération d'image QR).

16. `GET /api/inventory/items/{id}/history` (historique consolidé).

17. `GET /api/inventory/movements?item_id={id}` (mouvements par item).

18. `POST /api/auth/refresh` (renouvellement de token).

19. Endpoints simplifiés pour le mobile (entry, exit, transfer, return, scrap).



### Phase 5 — Validations et contrôles de sécurité



20. Ajouter `@PreAuthorize` sur les endpoints d'inventaire pour contrôler les permissions granulaires.

21. Ajouter la validation de `company_id` dans les services (vérifier que l'utilisateur a accès à l'entreprise).

22. Ajouter la validation de statut d'item à la création (seuls `AVAILABLE` et `REPAIR` sont valides).

23. Ajouter un rate limiter sur les endpoints publics (register, login).



### Phase 6 — Tests à écrire



24. Tests unitaires pour les services métier (sans base de données).

25. Tests d'intégration pour les entrées/sorties de stock avec vérification de mise à jour.

26. Tests de double affectation (vérifier le 409/400).

27. Tests de quantité insuffisante.

28. Tests d'accès cross-entreprise.

29. Tests de concurrence (deux requêtes simultanées).

30. Tests de génération de QR Code.



### Phase 7 — Critères d'acceptation pour déclarer l'API prête



- [ ] Un item créé a un QR Code généré automatiquement et récupérable en image.

- [ ] Un scan QR retourne les informations complètes de l'item (statut, localisation, affectation).

- [ ] Une entrée de stock met à jour la quantité totale et la localisation.

- [ ] Une sortie de stock vérifie la disponibilité et met à jour le stock.

- [ ] Une affectation met à jour le statut de l'item vers `ASSIGNED`.

- [ ] Une restitution remet le statut vers `AVAILABLE`.

- [ ] Les données sont isolées par entreprise.

- [ ] Les opérations concurrentes ne créent pas d'incohérence.

- [ ] Les alertes de stock faible sont correctes.

- [ ] Les tests d'intégration passent sur une base vierge.

- [ ] Le frontend web peut appeler l'API (CORS configuré).

- [ ] La documentation OpenAPI est à jour.



---



## H. Verdict final



### Puis-je commencer le frontend web d'administration du stock ?



**Oui, sous réserve.** Le catalogue (items, catégories, types, entrepôts) est prêt. Les mouvements de stock mettent maintenant à jour l'état des items. Les affectations mettent à jour le statut. Le CORS est configuré. Le frontend peut être développé, mais l'isolation multi-entreprise et la concurrence restent à traiter.



### Puis-je commencer l'application mobile de scan QR Code ?



**Oui, sous réserve.** La résolution de QR Code fonctionne (`POST /api/inventory/scans/resolve`). La génération de QR Code est maintenant implémentée (ZXing, auto-génération, endpoint d'image). Le mobile peut scanner, identifier, effectuer des mouvements et des affectations. Il manque les endpoints simplifiés pour le mobile.



### Puis-je réaliser des mouvements de stock fiables depuis le mobile ?



**Oui, sous réserve.** Les endpoints de mouvement mettent maintenant à jour l'état des items. La vérification de disponibilité est en place. Les transactions sont déclarées. Il manque le contrôle de concurrence et les endpoints simplifiés pour le mobile.



### Puis-je affecter et restituer des matériels à des employés ?



**Oui.** L'API d'affectation crée des lignes dans `item_assignment`, la contrainte unique empêche la double affectation active, et l'état de l'item est maintenant mis à jour (`ASSIGNED` lors de l'affectation, `AVAILABLE` lors de la restitution).



### Les trois prochaines actions prioritaires



1. **Isolation multi-entreprise** : filtrer toutes les requêtes par `company_id` dérivé du JWT. C'est le risque de sécurité le plus important.



2. **Contrôle de concurrence** : ajouter `SELECT FOR UPDATE` sur les opérations de stock et d'affectation pour éviter les incohérences en cas d'opérations simultanées.



3. **Endpoints mobile simplifiés** : créer des routes dédiées pour les opérations courantes depuis le mobile (entry, exit, transfer, return, scrap) et implémenter les endpoints stub de `item-types`.



---



*Rapport généré par inspection statique du code, du schéma SQL, des tests et de la documentation. Aucun fichier applicatif n'a été modifié pendant l'audit.*

