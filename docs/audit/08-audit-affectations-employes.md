# 08 — Audit des affectations et des employés

**Objectif :** vérifier la cohérence des affectations de matériel aux employés, la restitution, les acteurs et la suppression, ainsi que les liens avec les employés et les postes. Le stock et les mouvements sont traités dans les rapports 05 et 07.

**Mise à jour du 22 octobre 2026 (Lot 2) :** la création d'employé sans `status` applique désormais `DEFAULT 'ACTIVE'`, les FK/`CHECK` renvoient 422 et les doublons 409, sans SQLSTATE.

**Mise à jour du 22 octobre 2026 (Lot 4) :** les affectations dispose d'un **contrat canonique unique** (`/api/item-assignments`), d'une **machine à états** (création active, retour, void motivé, archivage, restauration), d'une **synchronisation avec le stock** et d'un **rattachement aux mouvements** `OUT`/`RETURN`. `/api/inventory/assignments` est devenu une **façade dépréciée** qui délègue au même service. La migration additive `20_lot4_assignments.sql` ajoute `source_warehouse_id`, `voided_at`, `voided_by` et `void_reason`. Suite complète : **150 tests, 0 échec, 0 erreur, 0 ignoré, 28 suites**.

Le § 9 conserve, à titre de trace, les constats de l'audit initial et leur statut courant. Les sections 1 à 8 décrivent **l'état du code après le Lot 4**.

---

## 1. Une seule implémentation, deux préfixes

| API | Base | Contrôleur | Service | DAO | Statut |
|---|---|---|---|---|---|
| Affectations (canonique) | `/api/item-assignments` | `ItemAssignmentController` | `ItemAssignmentService` | `ItemAssignmentDAOImpl` | Contrat de référence |
| Affectations (façade) | `/api/inventory/assignments` | `AssignmentController` | `ItemAssignmentService` | `ItemAssignmentDAOImpl` | Dépréciée |

La façade ne réimplémente rien : elle délègue aux mêmes méthodes du même service, avec les mêmes DTO, les mêmes validations et les mêmes codes HTTP. Ses réponses de succès portent `Deprecation: true` et `Link: </api/item-assignments>; rel="successor-version"`. Le JSON libre et les réponses 200 portant une enveloppe `errors` ont été supprimés.

Le code historique du module `assignment` (`AssignmentService`, `AssignmentDAO`/`AssignmentDAOImpl`, `AssignmentRest` et les DTO associés) **subsiste dans l'arborescence mais n'est plus référencé** par aucun composant vivant. Il est candidat à la suppression à la clôture de la fenêtre de dépréciation (décision D-07) ; il ne modifie plus le comportement.

### 1.1. Routes canoniques

| Méthode | Route | Effet |
|---|---|---|
| `GET` | `/api/item-assignments` | Liste paginée (`page`, `pageSize`, `search`, `active`) |
| `GET` | `/api/item-assignments/{id}` | Lecture, y compris d'une ligne voidée |
| `GET` | `/api/item-assignments/item/{itemId}` | Affectations d'un article (hors voidées) |
| `GET` | `/api/item-assignments/employee/{employeeId}` | Affectations d'un employé (hors voidées) |
| `GET` | `/api/item-assignments/post/{postId}` | Affectations d'un poste (hors voidées) |
| `POST` | `/api/item-assignments` | Création (201) |
| `PATCH` | `/api/item-assignments/{id}` | Métadonnées uniquement |
| `POST` | `/api/item-assignments/{id}/return` | Retour |
| `POST` | `/api/item-assignments/{id}/void?reason=…` | Annulation auditée |
| `POST` | `/api/item-assignments/{id}/restore` | Restauration d'un void |
| `DELETE` | `/api/item-assignments/{id}?reason=…` | Archivage d'une affectation inactive (204) |

La façade expose les mêmes opérations sous `/api/inventory/assignments`, plus les listes par article, employé et poste aux emplacements historiques `/api/inventory/items/{id}/assignments`, `/api/inventory/employees/{id}/assignments` et `/api/inventory/security-posts/{id}/assignments`.

---

## 2. Contrat courant

| Sujet | Comportement après le Lot 4 |
|---|---|
| Création | DTO typé `ItemAssignmentCreateRequest`, `@NotBlank` sur `item_id`, `employee_id`, `post_id` |
| Code de création | `201` sur les deux préfixes |
| Erreur « inconnu » | `404 NOT_FOUND` sur les deux préfixes (plus de 200 + `errors`) |
| Erreur « article non disponible » | `409 ITEM_NOT_AVAILABLE` |
| Doublon d'affectation active | `409 ASSIGNMENT_ALREADY_ACTIVE` (index unique) |
| Création avec `returned_at` | Ligne **historique** : `assigned_at` obligatoire, état de l'article inchangé |
| Retour | `POST /{id}/return` ; date et acteur posés par le serveur |
| Retour déjà effectué | `409 ASSIGNMENT_ALREADY_RETURNED` |
| `PATCH` avec `returned_at` | Délègue au retour (compatibilité) |
| `PATCH` avec `condition_on_return` seul | `400 RETURN_ENDPOINT_REQUIRED` |
| `PATCH` des liens | `400 IMMUTABLE_ASSIGNMENT_FIELDS` (`item_id`, `employee_id`, `post_id`, `assigned_at`, `condition_on_assignment`) |
| `PATCH` de `notes` seul | Aucun changement d'état |
| Suppression d'une affectation active | `409 ACTIVE_ASSIGNMENT` |
| Suppression d'une affectation déjà voidée | `409 ASSIGNMENT_ALREADY_VOIDED` |
| Suppression d'un UUID inconnu | `404 NOT_FOUND` |
| Void sans motif | `400 CLIENT_ERROR` |
| Void déjà effectué | `409 ASSIGNMENT_ALREADY_VOIDED` |
| Restauration d'une ligne non voidée | `409 ASSIGNMENT_NOT_VOIDED` |
| Listes | Les lignes voidées sont exclues ; `active=true` exclut aussi les retours |

---

## 3. Modèle de données

`security.item_assignment` compte **19 colonnes** après la migration `20_lot4_assignments.sql` :

`id`, `item_id`, `employee_id`, `post_id`, `assigned_at`, `returned_at`, `condition_on_assignment`, `condition_on_return`, `notes`, `created_at`, `updated_at`, `assigned_by`, `returned_by`, `expected_return_at`, `company_id`, `source_warehouse_id`, `voided_at`, `voided_by`, `void_reason`.

### 3.1. Contraintes

- FK `item_id` → `item(id)` RESTRICT ;
- FK `employee_id` → `employee(id)` RESTRICT ;
- FK `post_id` → `security_post(id)` RESTRICT ;
- FK `source_warehouse_id` → `warehouse(id)` RESTRICT (ajoutée au Lot 4) ;
- `returned_at IS NULL OR returned_at >= assigned_at` ;
- `voided_at IS NULL OR returned_at IS NOT NULL` (ajoutée au Lot 4) ;
- `voided_at IS NULL OR void_reason IS NOT NULL` (ajoutée au Lot 4) ;
- index partiel unique `uq_item_assignment_active(item_id) WHERE returned_at IS NULL` ;
- index `idx_item_assignment_source_warehouse`, `idx_item_assignment_voided_at` (partiel) et `idx_item_assignment_company_active` (partiel) ajoutés au Lot 4.

### 3.2. Ce que le schéma garantit

Un article ne peut avoir qu'une **affectation active** (`returned_at IS NULL` et `voided_at IS NULL`) à la fois. Une ligne voidée est nécessairement close et motivée, ce qui empêche de conserver une affectation active « annulée ».

### 3.3. Ce que le schéma ne garantit toujours pas

- que l'article est `ASSIGNED` ;
- que l'employé est actif ;
- que l'article, l'employé et le poste appartiennent à la même entreprise ;
- que le poste correspond à l'employé ;
- que `assigned_by` / `returned_by` / `voided_by` sont renseignés ;
- que `returned_at` est cohérent avec le statut de l'article pour les lignes antérieures au Lot 4.

La cohérence de tenant reste vérifiable uniquement en joignant `item`, `employee` et `security_post` : c'est le reliquat de `P0-01`.

---

## 4. Machine à états

### 4.1. Création active

`POST /api/item-assignments` sans `returned_at` :

1. charge l'article (`404` s'il n'existe pas) ;
2. refuse un article archivé (`409 ITEM_ARCHIVED`) ;
3. refuse un article non `AVAILABLE` (`409 ITEM_NOT_AVAILABLE`) ;
4. refuse un article déjà affecté (`409 ASSIGNMENT_ALREADY_ACTIVE`) ;
5. insère l'affectation avec `assigned_by` = UUID du JWT, `source_warehouse_id` = entrepôt courant de l'article ;
6. passe l'article à `ASSIGNED` et vide `current_warehouse_id` ;
7. synchronise `stock_balance` via `StockBalanceService.syncItem`.

L'ensemble est couvert par une seule transaction Spring : affectation, état de l'article et solde sont cohérents ou rien n'est écrit.

### 4.2. Retour

`POST /{id}/return` :

1. charge l'affectation (`404`) et refuse une ligne voidée (`409 ASSIGNMENT_VOIDED`) ;
2. refuse un retour déjà effectué (`409 ASSIGNMENT_ALREADY_RETURNED`) ;
3. refuse un article dans un état incompatible (`409 ITEM_STATE_INCOMPATIBLE`) ;
4. pose `returned_at` (borné à `assigned_at` si une date future avait été fournie) et `returned_by` (UUID du JWT) ;
5. remet l'article `AVAILABLE` et restaure `source_warehouse_id`, ou l'entrepôt courant à défaut ;
6. synchronise le solde.

Le corps est facultatif ; seul `condition_on_return` est lu.

### 4.3. Void

`POST /{id}/void?reason=…` exige un motif.

- **Affectation active** : la ligne est close (`returned_at` renseigné), `voided_at`, `voided_by` et `void_reason` sont posés, l'article est remis `AVAILABLE`, l'entrepôt d'origine est restauré et le solde est synchronisé. Le void est donc un retour avec justification tracée.
- **Affectation déjà retournée** : la ligne est seulement archivée ; l'état de l'article n'est pas modifié.

### 4.4. Archivage et restauration

- `DELETE /{id}` n'archive qu'une affectation **déjà retournée** ; une affectation active renvoie `409 ACTIVE_ASSIGNMENT`. Un motif est facultatif (libellé par défaut sinon).
- `POST /{id}/restore` efface `voided_at`, `voided_by` et `void_reason`. Une affectation voidée alors qu'elle était active conserve son `returned_at` : après restauration, elle reste historique et ne redevient pas active.

### 4.5. Invariant d'état

| État de l'article | Affectation active attendue |
|---|---|
| `AVAILABLE` | 0 |
| `ASSIGNED` | exactement 1 |

L'invariant est garanti **par le code** sur les écritures de l'API. Il ne l'est pas sur les données antérieures : une affectation active préexistante de la base de démonstration pointe vers un article `AVAILABLE` (voir § 9.3).

---

## 5. Rattachement aux mouvements

`StockMovementService` s'appuie désormais sur `ItemAssignmentService` :

| Type de mouvement | Effet sur l'affectation |
|---|---|
| `OUT` | Exige `employee_id` **et** `post_id` (`400 CLIENT_ERROR` sinon) ; refuse un article non `AVAILABLE` (`400 CLIENT_ERROR`) ou déjà affecté (`409 ASSIGNMENT_ALREADY_ACTIVE`) ; crée l'affectation active de façon atomique ; l'article passe `ASSIGNED` et le stock est décrémenté |
| `RETURN` | Clôt l'affectation active (`returned_at`, `returned_by`) ; l'article redevient `AVAILABLE` et le stock est réintégré ; refusé si l'article n'est ni affecté ni `ASSIGNED` (`409 ITEM_NOT_ASSIGNED`) |
| `IN` | Refusé si l'article a une affectation active (`409 ITEM_ASSIGNED`) |
| `TRANSFER` | Refusé si l'article a une affectation active (`409 ITEM_ASSIGNED`) |
| `DESTRUCTION` | Refusé si l'article a une affectation active (`409 ITEM_ASSIGNED`) |

Il n'existe donc plus deux sources de vérité pour `ASSIGNED` : la table `item_assignment` et le journal de mouvements décrivent le même état.

---

## 6. Archivage des référentiels

Le Lot 4 a généralisé l'archivage logique aux données maîtres :

| Référentiel | Archivage | Restauration | Particularité |
|---|---|---|---|
| Article | `archived_at`, `archived_by` | `POST /api/inventory/items/{id}/restore` | Refus si l'article a une affectation active (`409 ITEM_ASSIGNED`) ; le stock disponible est retiré puis réintégré |
| Catégorie | `archived_at`, `archived_by` | `POST /api/inventory/item-categories/{id}/restore` | Exclue des listes |
| Type | `archived_at`, `archived_by` | `POST /api/inventory/item-types/{id}/restore` | Exclu des listes |
| Entrepôt | `archived_at`, `archived_by` | `POST /api/inventory/warehouses/{id}/restore` | Exclu des listes |
| Affectation | `voided_at`, `voided_by`, `void_reason` | `POST /api/item-assignments/{id}/restore` | Motif obligatoire ; exclue des listes |

Un UUID inconnu renvoie `404` ; une ressource déjà archivée renvoie `404` (elle n'est plus active) et sa restauration `409` (`ITEM_NOT_ARCHIVED`, `ITEM_CATEGORY_NOT_ARCHIVED`, `ITEM_TYPE_NOT_ARCHIVED`, `WAREHOUSE_NOT_ARCHIVED`).

**Limite :** la suppression physique définitive n'est pas offerte, et les références vers un référentiel archivé (un article typé par un type archivé, par exemple) ne sont pas systématiquement refusées.

---

## 7. Liens avec les employés et les postes

### 7.1. Employé inactif

Aucun contrôle de `employee.status` : un employé `INACTIVE`, `SUSPENDED` ou `LEFT` peut encore recevoir du matériel. **Ouvert.**

### 7.2. Cohérence d'entreprise

Aucun contrôle croisé entre `item.company_id`, `employee.company_id` et l'entreprise du poste. La colonne `company_id` est renseignée sur l'affectation, mais elle est déduite de l'article et non vérifiée contre les autres référents. **Ouvert (`P0-01`).**

### 7.3. Poste incompatible

Aucun contrôle que l'employé occupe le poste, ni que le poste est actif. **Ouvert.**

### 7.4. Acteurs

`assigned_by`, `returned_by` et `voided_by` sont déduits du JWT lorsqu'il porte un UUID ; un sujet JWT non UUID (identifiant technique) est converti en `NULL` au lieu de faire échouer l'écriture. Les lignes historiques restent sans acteur.

### 7.5. Création d'employé

`EmployeeDAOImpl.create` n'insère plus `status` lorsqu'il est absent : la base applique `DEFAULT 'ACTIVE'` (Lot 2). Un formulaire minimal ne produit plus de `500 23502`.

---

## 8. Tests

| Classe | Tests | Couverture |
|---|---|---|
| `AssignmentApiTests` | 14 | Contrat canonique et façade, création, retour, void, restauration, archivage, listes excluant les voidées, acteur, entrepôt d'origine, codes 404/409 |
| `ItemAssignmentStateTests` | 5 | Machine à états : `AVAILABLE` → `ASSIGNED` → `AVAILABLE`, seconde affectation refusée, void actif, suppression active refusée |
| `ItemAssignmentTimestampTests` | 3 | Dates par défaut, dates explicites, dates invalides, bornage de `returned_at` |
| `StockMovementBusinessRulesTests` | 9 | `OUT` avec/sans `employee_id`/`post_id`, `RETURN`, refus `IN`/`TRANSFER`/`DESTRUCTION` sur article affecté |
| `ItemApiTests` | 16 | Archivage et restauration d'un article, 404 sur UUID inconnu, historique par article |
| `ItemCategoryApiTests`, `ItemTypeApiTests`, `WarehouseApiTests` | 21 | Archivage, restauration, exclusion des listes, 404 sur UUID inconnu |
| `OperationApiTests` | 4 | Propagation de l'employé et du poste vers les mouvements |
| `Lot3StockModelTests` | 8 | Non-régression du solde et du journal |

### 8.1. Ce que les tests ne couvrent pas encore

- la course réelle sur `uq_item_assignment_active` (le `23505` est traduit en 409, mais aucun test de concurrence) ;
- le refus d'un employé inactif ;
- le cross-entreprise ;
- l'immutabilité des liens en cross-article (le cas `item_id` changeant est couvert par le refus global) ;
- la réconciliation de la donnée de démonstration incohérente.

---

## 9. Constats initiaux et statut courant

### 9.1. Tableau de correspondance

| Constat de l'audit initial | Statut après le Lot 4 |
|---|---|
| Deux API divergentes sur la même table | **Corrigé** : un service, un contrat ; `/api/inventory/assignments` est une façade dépréciée |
| Création générique en JSON libre | **Corrigé** : DTO typés et `@Valid` sur les deux préfixes |
| Erreur « inconnu » en 200 + `errors` | **Corrigé** : `404` sur les deux préfixes |
| Retour sans date = no-op présenté en succès | **Corrigé** : `400 RETURN_ENDPOINT_REQUIRED` ; le retour passe par `/return` |
| `DELETE` supprimant la ligne et laissant l'article `ASSIGNED` | **Corrigé** : `409 ACTIVE_ASSIGNMENT`, archivage logique, article restauré |
| Création avec `returned_at` laissant l'article `ASSIGNED` | **Corrigé** : ligne historique, état de l'article inchangé |
| `assigned_by` / `returned_by` absents | **Corrigé** : acteurs alimentés depuis le JWT |
| Retour ne restaurant pas l'entrepôt | **Corrigé** : `source_warehouse_id` et restauration |
| `OUT` sans affectation, `RETURN` sans clôture | **Corrigé** : `OUT` crée, `RETURN` clôt |
| Employé inactif accepté | **Ouvert** |
| Cross-entreprise accepté | **Ouvert** (`P0-01`) |
| Poste incompatible accepté | **Ouvert** |
| Course sur l'index unique renvoyant 500 | **Partiellement corrigé** : traduit en 409, sans verrou ni test de concurrence (`P1-14`) |
| `assigned_at` nul dans la réponse de création | **Corrigé** : ligne relue après insertion |
| Absence d'archivage et de restauration | **Corrigé** pour les référentiels et les affectations |

### 9.2. Dates

`assigned_at` est désormais borné : une date future fournie explicitement est conservée, mais le retour et le void sont bornés à `assigned_at` pour ne jamais produire une chronologie inversée. Une affectation historique sans `assigned_at` est refusée (`400`).

### 9.3. Donnée de démonstration

Une affectation active antérieure au Lot 4 (`f7223d51-39cc-4e3e-8736-24d32d3289ad`, article `54488506-9b2d-4430-b126-7b33f7b784c5`) pointe vers un article `AVAILABLE`. **Aucune correction automatique n'a été appliquée** : la réconciliation d'une donnée existante relève d'une décision de gestion, pas d'une correction de code. Le contrôle SQL de l'invariant retourne `ASSIGNED sans affectation = 0` et `affectation active sur article non ASSIGNED = 1`.

---

## 10. Verdict

Les affectations sont **cohérentes dans le périmètre du Lot 4** :

- un contrat unique et une façade explicitement dépréciée ;
- une machine à états testée : création, retour, void, archivage, restauration ;
- l'article, son emplacement et le solde évoluent de façon atomique avec l'affectation ;
- les mouvements `OUT`/`RETURN` sont raccordés, et les autres types sont refusés sur un article affecté ;
- la suppression est impossible sur une affectation active ; l'historique est conservé ;
- les acteurs et les motifs sont tracés.

Les risques résiduels sont :

1. **Tenancy** (`P0-01`) : aucune vérification d'entreprise entre l'article, l'employé et le poste, et aucun contrôle d'employé actif.
2. **Concurrence et idempotence** (`P1-14`) : pas de `version`, `If-Match` ni `Idempotency-Key` ; un second retour reçoit 409 au lieu d'être idempotent.
3. **Donnée existante** : une affectation active de démonstration reste incohérente avec le statut de son article.
4. **Code mort** : l'ancien `AssignmentService` et son DAO ne sont plus référencés et attendent la décision D-07.
5. **Périmètre d'archivage** : la purge physique définitive n'existe pas et les références vers un référentiel archivé ne sont pas toutes refusées.

Le plan de correction est dans le rapport 14 ; le registre des risques dans le rapport 12 ; les critères de validation dans le rapport 15.
