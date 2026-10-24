# 15 — Critères de validation de l'API

**Objectif :** fournir une checklist testable pour décider si l'API peut passer d'un prototype à un pilote, puis à la production. Chaque critère décrit un comportement attendu, pas une opinion.

**Portée :** API backend uniquement. Le frontend n'est pas évalué ici, sauf pour les contrats dont il dépend.

**Décision du 24 octobre 2026 — déploiement mono-entreprise :** le produit est exploité pour **une seule entreprise**. Les critères d'isolation entre entreprises (portes `G0-1`/`G0-2`, section 3 « Tenancy », `CAT-05`, `ASG-04`/`ASG-05`, `TST-05`, `FE-11`/`FE-12`) sont marqués **N/A** et ne bloquent plus la mise en production. Les colonnes `company_id`, le claim `company_id` du JWT et le scope entreprise des utilisateurs et rôles restent en place, mais ne constituent pas une garantie d'isolation : `P0-01` devient un **écart accepté et documenté**, pas un correctif planifié. **Condition de réversibilité :** admettre une deuxième entreprise impose de corriger `P0-01` (filtres `company_id`, FK composites ou RLS) et de repasser ces critères en `PASS`/`FAIL` bloquants avant toute donnée réelle.

---

## 1. Comment utiliser ce document

### 1.1. Statuts

| Statut | Signification |
|---|---|
| **PASS** | Comportement vérifié par le code et/ou le dernier rapport de tests disponible, sans contradiction connue |
| **PARTIEL** | Implémentation partielle, ou vérifiée seulement statiquement, ou insuffisamment testée |
| **FAIL** | Comportement absent, faux, ou contraire au critère |
| **NON VÉRIFIÉ** | Nécessite une exécution, un environnement ou une donnée non disponible pendant l'audit |
| **N/A** | Hors périmètre par décision produit : le critère ne s'applique pas au déploiement mono-entreprise |

**Important :** la suite complète exécutée après le Lot 4 comptait **150 tests dans 28 suites**. Elle a été réduite le 22 octobre 2026 à **20 tests dans 4 suites**, puis complétée par les contrats QR (Lot 5), comptage (Lot 6) et HTTP (Lot 7). Au 23 octobre 2026, elle comptait **58 tests dans 8 suites** : conformité du schéma SQL à l'application, contrat QR, contrat des comptages et contrat HTTP (versionnement, erreurs, pagination, `Location`, dates). **Après le Lot 8 (24 octobre 2026), elle compte 74 tests dans 13 classes** : s'y ajoutent le cycle de vie des affectations, la cohérence mouvement/stock/affectation, la concurrence, l'échec de login et une mesure de latence sur 1 000 articles. Les fixtures des 16 nouveaux tests sont autonomes (entreprise, utilisateur, entrepôt, type, employé et poste créés puis supprimés par chaque test). Un `PASS` signifie « cohérent avec le code, vérifié par la suite courante ou par lecture statique » ; la tenancy A/B est hors périmètre (mono-entreprise par conception, décision du 24 octobre 2026) et l'idempotence HTTP reste absente. Le détail historique reste disponible dans les rapports 11 et 16.

### 1.2. Règle de décision

- Un seul **FAIL** sur un critère P0 bloque la production.
- Un **PARTIEL** sur un critère P0 bloque aussi, sauf dérogation écrite et compensée.
- Les critères P1 peuvent être partiels pour un pilote mono-entreprise encadré, avec liste d'écarts acceptés.
- Les critères P2 sont planifiables après stabilisation.
- Les critères **N/A** ne comptent ni comme `PASS` ni comme échec : ils documentent un choix de périmètre, réversible aux conditions indiquées en tête de document.

### 1.3. Preuves attendues

Chaque critère doit être prouvé par :

- un test automatisé sur base isolée ;
- ou une vérification manuelle reproductible ;
- ou une contrainte de base démontrée ;
- avec la trace dans le rapport de recette.

Une capture d'écran ne remplace pas un test de contrat.

---

## 2. Portes Go/No-Go

| Porte | Critère | Statut actuel | Preuve / écart |
|---|---|---|---|
| **G0-1** | Un utilisateur de l'entreprise A ne peut lire aucune donnée de B | **N/A** | Mono-entreprise par conception : une seule entreprise est déployée. La lecture reste globale (`ItemDAOImpl` sans filtre `company_id`), ce qui interdit d'admettre une deuxième entreprise avant correction de `P0-01`. |
| **G0-2** | Un utilisateur de A ne peut écrire aucune donnée de B | **N/A** | Idem : `company_id` vient du client et aucune FK composite ne l'empêche, sans conséquence tant qu'une seule entreprise existe. Condition de réversibilité en tête de document. |
| **G0-3** | Un compte sans entreprise ou sans rôle ne peut pas accéder à l'inventaire | **PARTIEL** | Un compte sans rôle est bloqué ; un admin plateforme sans entreprise reste autorisé par conception. |
| **G0-4** | Les écritures multi-lignes sont atomiques | **PASS** | Opérations `receipt`/`issue`/`transfer` avec lignes et mouvements atomiques ; rollback testé après échec sur la deuxième ligne (`P1-06`). |
| **G0-5** | Le stock a une source de vérité unique et testée | **PASS** | `stock_balance` alimenté par les mouvements, opérations, articles et clôtures de comptage ; `GET /stock-balances` et dashboard low-stock testés (`P0-05`). Les ajustements de clôture sont journalisés en mouvements `ADJUSTMENT` depuis le Lot 6. |
| **G0-6** | `ASSIGNED` implique exactement une affectation active | **PASS** | Création, `OUT`, retour, void et suppression passent par le même service transactionnel ; index unique actif et tests dédiés (`P0-06`, Lot 4). Réserve : une affectation active préexistante de la base de démonstration pointe vers un article `AVAILABLE` ; elle n'est pas créée par le code corrigé. |
| **G0-7** | Aucune erreur de saisie ne produit 500 avec SQLSTATE | **PASS** | Doublons 409, FK/CHECK 422, NOT NULL/format 400 ; aucun SQLSTATE exposé. |
| **G0-8** | Un rôle révoqué ne peut pas être rafraîchi | **PASS** | `/refresh` recharge les rôles et permissions en base ; rotation testée. |

**Verdict :** 5 portes sur 8 en `PASS` (`G0-4`, `G0-5`, `G0-6`, `G0-7`, `G0-8`), 1 `PARTIEL` (`G0-3`), 2 `N/A` (`G0-1`, `G0-2`, mono-entreprise par conception) et **aucun `FAIL`**. La tenancy ne bloque plus la production ; les portes encore ouvertes sont `G0-3` (compte plateforme sans entreprise) et les critères `FAIL` hors tenancy du reste du document. Une donnée de démonstration antérieure reste à réconcilier sur les affectations.

---

## 3. Tenancy — hors périmètre (mono-entreprise par conception)

Les critères ci-dessous décrivent l'isolation entre entreprises. Les critères d'isolation sont **N/A** depuis la décision du 24 octobre 2026 : le déploiement ne comporte qu'une entreprise. Ils restent documentés comme **spécification de reprise** si une deuxième entreprise devait être admise, et redeviennent alors bloquants. Les critères qui portent sur un comportement encore vérifiable en mono-entreprise restent évalués : `T-09`, `T-10` et `T-14` en `PASS`, `T-15` en `PARTIEL`.

| ID | Critère testable | Statut | Preuve / référence |
|---|---|---|---|
| T-01 | `GET /items` ne retourne que les articles de l'entreprise du jeton | **N/A** | `ItemDAOImpl.getAllItems` reste sans filtre `company_id` ; sans objet en mono-entreprise, bloquant pour une deuxième entreprise. |
| T-02 | `GET /items/{id}` d'une autre entreprise renvoie 404, pas 200 | **N/A** | Lecture par ID global, aucune entreprise tierce déployée. |
| T-03 | `PATCH /items/{id}` d'une autre entreprise renvoie 404 | **N/A** | Aucun contrôle de tenant ; sans objet en mono-entreprise. |
| T-04 | `DELETE /items/{id}` d'une autre entreprise renvoie 404 | **N/A** | Aucun contrôle de tenant ; sans objet en mono-entreprise. |
| T-05 | `POST /items` ignore un `company_id` différent du jeton et utilise celui du jeton | **N/A** | `company_id` vient du corps ; une seule entreprise existe, donc la valeur est toujours la bonne. |
| T-06 | Un article ne peut pas référencer un type/catégorie/entrepôt d'une autre entreprise | **N/A** | Aucune FK composite ; sans objet tant qu'une seule entreprise est déployée. |
| T-07 | Une affectation ne peut pas lier un article, un employé et un poste de trois entreprises différentes | **N/A** | Aucun contrôle ; sans objet en mono-entreprise. |
| T-08 | Un mouvement ne peut pas référencer un entrepôt ou un employé d'une autre entreprise | **N/A** | FK simples ; sans objet en mono-entreprise. |
| T-09 | Un scan QR ne renvoie jamais un article d'une autre entreprise | **PASS** | `QrScanDAOImpl` filtre `i.company_id = ?` depuis le Lot 5 ; un code d'une autre entreprise renvoie 404 (`QrScanApiTests`). Correctif de lecture par rapport à l'audit initial. |
| T-10 | Un comptage ne peut scanner que des articles de son entreprise et de son périmètre | **PASS** | Le scan lit l'instantané figé à l'ouverture et refuse en `409 ITEM_OUT_OF_SCOPE` un article d'une autre entreprise, archivé ou créé après l'ouverture ; un article attendu ailleurs produit un écart `WRONG_LOCATION`. Test `InventoryCountApiTests`. |
| T-11 | Les listes de types, catégories, entrepôts, mouvements, affectations, comptages et rapports sont tenant-scoped | **N/A** | Les comptages sont tenant-scoped depuis le Lot 6 ; les autres listes restent globales, sans conséquence en mono-entreprise. |
| T-12 | Un administrateur d'entreprise ne peut pas administrer une autre entreprise | **N/A** | Aucune autre entreprise à administrer ; le scope `company_id` sur `/api/users` et `/api/roles` reste en place. |
| T-13 | La base possède des contraintes composites ou une RLS empêchant le cross-tenant | **N/A** | Schéma à FK simples ; contraintes requises uniquement en cas d'ouverture à une deuxième entreprise. |
| T-14 | Un utilisateur anonyme ne peut pas appeler l'inventaire | **PASS** | `SecurityConfig` : `authenticated()` |
| T-15 | Un utilisateur sans entreprise ne peut pas appeler l'inventaire | **PARTIEL** | Un admin plateforme sans entreprise y accède par conception ; les autres comptes sans entreprise n'ont pas de rôle métier. |

### Scénario d'acceptation T-A — à réactiver si une deuxième entreprise est admise

```text
Étant donné l'entreprise A et l'entreprise B, chacune avec un article
Et un utilisateur de A authentifié
Quand il appelle GET /api/v1/inventory/items
Alors la réponse ne contient que les articles de A
Et GET /api/v1/inventory/items/{id_de_B} renvoie 404
Et PATCH /api/v1/inventory/items/{id_de_B} renvoie 404
Et POST /api/v1/inventory/items avec company_id=B crée l'article dans A ou renvoie 422
```

---

## 4. Authentification et autorisation

| ID | Critère testable | Statut | Preuve / référence |
|---|---|---|---|
| A-01 | Un login valide renvoie un access token et le profil | **PASS** | Login réel utilisé par `RefreshTokenApiTests` et `AuthHardeningApiTests`. |
| A-02 | Un login invalide renvoie 401, pas 400 | **FAIL** | `AuthService` lève toujours `ClientException` → 400. |
| A-03 | Un compte inactif renvoie 403 | **FAIL** | 400. |
| A-04 | Un compte verrouillé ne peut pas se connecter | **FAIL** | `LOCKED` non appliqué automatiquement. |
| A-05 | L'inscription ne crée pas de compte sans entreprise | **PASS** | Entreprise déduite du jeton ; admin plateforme doit passer par `/api/users`. |
| A-06 | L'inscription exige une invitation ou une validation | **PASS** | Route réservée à `ROLE_ADMIN` ou `users.manage`. |
| A-07 | Un utilisateur sans rôle ne peut pas accéder à l'inventaire | **PASS** | `ROLE_ADMIN`, `ROLE_INVENTORY` ou `inventory.manage` requis. |
| A-08 | Un `USER` ne peut pas appeler `/api/users`, `/roles`, `/permissions` | **PASS** | `users.manage`/`roles.manage` requis ; `/permissions` réservé à `ROLE_ADMIN` ; test admin d'entreprise. |
| A-09 | Un `INVENTORY` peut lire et écrire l'inventaire selon ses permissions | **PARTIEL** | `ROLE_INVENTORY` ou `inventory.manage` autorise l'ensemble de l'inventaire ; pas de permission par action. |
| A-10 | Un `ADMIN` d'entreprise ne peut pas gérer une autre entreprise | **PASS** | Scope `company_id` sur les utilisateurs et rôles. |
| A-11 | Le refresh recharge les rôles depuis la base | **PASS** | `AuthService.refresh` recharge rôles et permissions. |
| A-12 | Un rôle révoqué ne peut pas être conservé par refresh | **PASS** | Le nouveau jeton reprend les rôles rechargés ; pas de test dédié de révocation de rôle. |
| A-13 | Le refresh token est distinct, tourne et est révocable | **PASS** | Table `refresh_token`, consommation atomique de l'ancien token, tests. |
| A-14 | Le logout révoque l'access et le refresh | **PASS** | Test `logoutRevokesRefreshAndAccessTokens`. |
| A-15 | Un jeton expiré renvoie 401 | **PARTIEL** | Filtre présent ; pas de test d'expiration réelle. |
| A-16 | Un jeton signé avec une mauvaise clé renvoie 401 | **PARTIEL** | Filtre présent ; pas de test de mauvaise clé. |
| A-17 | Le JWT contient l'entreprise et un `jti` | **PASS** | Claims `company_id` et `jti` émis et testés. |
| A-18 | Le login et le refresh sont limités en débit | **FAIL** | Absent. |
| A-19 | Le mot de passe est hashé et jamais renvoyé | **PASS** | BCrypt ; DTO sans hash. |
| A-20 | Le CORS n'accepte que les origines nécessaires | **PASS** | `CORS_ALLOWED_ORIGINS` explicite ; en-têtes/méthodes encore larges. |
| A-21 | Les erreurs SQL ne sont pas exposées en production | **PASS** | Mapping stable 400/409/422/503 ; aucun SQLSTATE ni message PostgreSQL. |
| A-22 | Un compte supprimé ne peut plus rafraîchir | **PASS** | 401 sur refresh d'un compte supprimé/inconnu. |

### Scénario d'acceptation A-A

```text
Étant donné un utilisateur ADMIN actif
Quand un administrateur lui retire le rôle ADMIN
Et qu'il appelle POST /api/v1/auth/refresh avec son refresh token
Alors la réponse ne contient plus ADMIN
Et l'accès à /api/v1/users renvoie 403
```

---

## 5. Catalogue

| ID | Critère testable | Statut | Preuve / référence |
|---|---|---|---|
| CAT-01 | CRUD nominal article, type, catégorie, entrepôt | **PARTIEL** | Code + dernier rapport, non rejoué |
| CAT-02 | Créer un type sans `unit`, `quantity_total`, `minimum_stock` applique les défauts | **PASS** | Test `ItemTypeApiTests.createWithoutOptionalValuesAppliesDatabaseDefaults`. |
| CAT-03 | Créer un employé sans `status` applique `ACTIVE` | **PASS** | Test `EmployeeDefaultsTests.createWithoutStatusAppliesDatabaseDefault`. |
| CAT-04 | Un doublon de QR/code/nom renvoie 409 | **PASS** | Mapping `23505` → 409 ; test doublon de catégorie. |
| CAT-05 | Un article ne peut pas référencer une relation d'une autre entreprise | **N/A** | Aucun contrôle, sans objet en mono-entreprise ; requis si une deuxième entreprise est admise. |
| CAT-06 | `GET /items` retourne `total`, `page`, `pageSize`, `totalPages` | **PASS** | `PageResponse<ItemRest>` ; test `HttpContractApiTests`. |
| CAT-07 | `page=0` renvoie 400, pas 500 | **PARTIEL** | `PageQuery` normalise `page=0` à 1 et renvoie 200 ; aucune erreur 500, mais le critère attend 400. |
| CAT-08 | `pageSize` est plafonné | **PASS** | Plafond `PageQuery.MAX_PAGE_SIZE = 500`, testé avec `pageSize=500`. |
| CAT-09 | Les transitions de statut d'article sont contrôlées | **FAIL** | `PATCH` libre |
| CAT-10 | On peut retirer une relation nullable par `PATCH` | **FAIL** | Convention « non nul seulement » |
| CAT-11 | Une suppression référencée renvoie 409, pas 500 | **PARTIEL** | `23503` → 422 (plus de 500), mais le critère attend 409. |
| CAT-12 | Une suppression inconnue renvoie 404, pas 204 | **PASS** | `DELETE` d'un article, type, catégorie ou entrepôt inconnu renvoie 404 ; tests dédiés Lot 4. |
| CAT-13 | Les éléments archivés ne figurent pas dans les listes | **PASS** | Filtres `archived_at IS NULL` sur articles, catégories, types et entrepôts ; tests de suppression/restauration. |
| CAT-14 | `GET /items/{id}/history` renvoie 404 pour un article inconnu | **PASS** | Lecture préalable de l'article ; test `HttpContractApiTests`. |
| CAT-15 | L'historique est filtré et paginé côté SQL | **PASS** | `getStockMovementsByItem` et `getItemAssignmentsByItem` avec `page`/`pageSize`, counts SQL, filtre `movementType` et `active`. |
| CAT-16 | Le bulk est atomique ou retourne des échecs indexés | **FAIL** | Partiel, sans index, sans `@Valid` |
| CAT-17 | Les filtres article couvrent type, catégorie, entrepôt, statut, dates | **FAIL** | Filtres limités |
| CAT-18 | Un article supprimé conserve son historique | **PASS** | Archivage logique ; la ligne, les affectations et les mouvements sont conservés. |

---

## 6. Stock et mouvements

| ID | Critère testable | Statut | Preuve / référence |
|---|---|---|---|
| STK-01 | Un mouvement porte une quantité > 0 | **PASS** | `quantity` exposée, valeur par défaut 1, quantité nulle ou négative refusée. |
| STK-02 | Un mouvement porte source, destination, opération et acteur | **PARTIEL** | Champs exposés et alimentés ; l'acteur peut rester nul si le JWT et la requête n'apportent pas d'UUID. Les lignes historiques restent incomplètes. |
| STK-03 | Le stock disponible est calculé par entrepôt et type | **PASS** | `stock_balance` et `GET /stock-balances`. |
| STK-04 | Le stock ne peut pas devenir négatif | **PASS** | `409 INSUFFICIENT_STOCK`, état inchangé, test dédié. |
| STK-05 | Un mouvement validé est immuable | **PASS** | `PATCH`/`DELETE` renvoient `409 IMMUTABLE_RESOURCE`. |
| STK-06 | Une correction crée un mouvement inverse | **PARTIEL** | Un mouvement inverse manuel est possible ; aucune compensation automatique. |
| STK-07 | `IN` incrémente le stock | **PASS** | Solde incrémenté et article remis `AVAILABLE`. |
| STK-08 | `OUT` décrémente le stock et exige une affectation | **PASS** | `employee_id` et `post_id` obligatoires, affectation créée dans la transaction, solde décrémenté ; test dédié (`P0-06`). |
| STK-09 | `TRANSFER` déplace une quantité d'un entrepôt à l'autre | **PASS** | Solde source/destination et emplacement de l'article mis à jour. |
| STK-10 | `RETURN` clôt l'affectation active | **PASS** | Le retour clôt l'affectation active et restaure l'article ; test dédié (`P0-06`). |
| STK-11 | `DESTRUCTION` interdit un article affecté | **PASS** | `IN`, `TRANSFER` et `DESTRUCTION` refusent une affectation active (`409 ITEM_ASSIGNED`) ; test dédié. |
| STK-12 | `ADJUSTMENT` modifie une quantité | **PASS** | Solde ajusté selon la source/destination, sans changer l'état physique. |
| STK-13 | La réponse de création renvoie le type et la date réellement stockés | **PASS** | Création relue depuis la base (Lot 2). |
| STK-14 | Un mouvement inconnu renvoie 404 | **PASS** | Code + test. |
| STK-15 | Un mouvement référencé ne peut pas être supprimé sans compensation | **PASS** | `DELETE` interdit par le journal append-only. |
| STK-16 | Le rapport mouvements filtre par date, type, acteur et entreprise | **FAIL** | Filtres item/entrepôt seulement ; le filtre entreprise est sans objet en mono-entreprise, les filtres date/type/acteur manquent toujours. |
| STK-17 | Le dashboard utilise le stock réel | **PARTIEL** | Le low-stock lit `stock_balance` ; les compteurs de synthèse restent globaux. |
| STK-18 | Les alertes low-stock utilisent un seuil par entrepôt | **PASS** | Seuil effectif `warehouse_stock_config.minimum_stock`, sinon `item_type.minimum_stock`. |

### Scénario d'acceptation STK-A

```text
Étant donné un article disponible dans l'entrepôt A
Et un stock initial de 5
Quand un mouvement OUT de quantité 2 est validé
Alors le stock de l'entrepôt A devient 3
Et l'article est ASSIGNED avec une affectation active
Et le mouvement contient performed_by, quantity=2, source_warehouse_id=A
Et un second OUT de 4 renvoie 409 sans modifier le stock
```

---

## 7. Affectations

| ID | Critère testable | Statut | Preuve / référence |
|---|---|---|---|
| ASG-01 | Un seul contrat d'affectation est exposé | **PARTIEL** | `/api/item-assignments` est canonique ; `/api/inventory/assignments` reste une façade dépréciée déléguant au même service. Deux préfixes subsistent jusqu'à D-07. |
| ASG-02 | Affecter un article non disponible renvoie 409 | **PASS** | `409 ITEM_NOT_AVAILABLE` ; test dédié. |
| ASG-03 | Affecter à un employé inactif renvoie 422 | **FAIL** | Aucun contrôle |
| ASG-04 | Affecter à un employé d'une autre entreprise renvoie 404/422 | **N/A** | Aucun contrôle de cohérence entreprise ; sans objet en mono-entreprise. |
| ASG-05 | Affecter à un poste d'une autre entreprise renvoie 404/422 | **N/A** | Aucun contrôle de cohérence entreprise ; sans objet en mono-entreprise. |
| ASG-06 | L'affectation et le passage à `ASSIGNED` sont atomiques | **PASS** | Service `@Transactional`, DAO sur connexion Spring, solde synchronisé ; tests de création et rollback. |
| ASG-07 | Un retour exige une date de retour | **PASS** | `POST /{id}/return` pose `returned_at` côté serveur ; date bornée à `assigned_at` si nécessaire. |
| ASG-08 | Un retour restaure l'entrepôt | **PASS** | `source_warehouse_id` ajouté et restauré ; test dédié. |
| ASG-09 | Un retour renseigne `returned_by` | **PASS** | Acteur JWT UUID ; test `returned_by`. Un acteur non-UUID reste nul. |
| ASG-10 | Un retour est idempotent | **PARTIEL** | Un second retour renvoie 409 `ASSIGNMENT_ALREADY_RETURNED` de façon déterministe, mais aucune clé d'idempotence (`P1-14`). |
| ASG-11 | Un retour avec seulement une condition ne renvoie pas 200 | **PASS** | `PATCH` avec `condition_on_return` seul renvoie 400 `RETURN_ENDPOINT_REQUIRED`. |
| ASG-12 | Une affectation active ne peut pas être supprimée | **PASS** | `DELETE` renvoie 409 `ACTIVE_ASSIGNMENT` ; test dédié. |
| ASG-13 | Une annulation est auditée et remet l'article disponible | **PASS** | `void` motivé, `voided_at`/`voided_by`/`void_reason`, article et stock restaurés. |
| ASG-14 | `ASSIGNED` implique exactement une affectation active | **PASS** | Tous les chemins passent par le service ; index unique actif et tests. |
| ASG-15 | `AVAILABLE` implique aucune affectation active | **PARTIEL** | Le code corrigé garantit la règle ; une affectation active préexistante de la base de démonstration pointe encore vers un article `AVAILABLE`. |
| ASG-16 | Deux affectations concurrentes produisent un 409 | **PASS** | Index unique + contrôle d'état transactionnel ; `ConcurrentInventoryApiTests` lance deux créations simultanées et n'observe qu'un `201` et un `409`. |
| ASG-17 | Les affectations en retard sont identifiables | **FAIL** | `expected_return_at` est modifiable mais aucune route ni requête de retard n'existe. |
| ASG-18 | L'historique des acteurs est conservé | **PASS** | `assigned_by`, `returned_by` et `voided_by` alimentés depuis le JWT ; les lignes historiques restent nulles. |

### Scénario d'acceptation ASG-A

```text
Étant donné un article ASSIGNED avec une affectation active
Quand un opérateur appelle POST /api/v1/item-assignments/{id}/return
Alors returned_at et returned_by sont renseignés
Et l'affectation est clôturée
Et l'article redevient AVAILABLE dans son entrepôt de retour
Et un second appel renvoie 409 ou la même réponse idempotente
Et il n'existe plus aucune affectation active pour cet article
```

---

## 8. QR Codes et mobile

| ID | Critère testable | Statut | Preuve / référence |
|---|---|---|---|
| QR-01 | Le code est stable après impression | **PASS** | `PATCH qr_code` refusé en 400 `IMMUTABLE_QR_CODE` (décision D-05) ; test dédié. |
| QR-02 | L'unicité est garantie par entreprise | **PASS** | `UNIQUE(company_id, qr_code)` |
| QR-03 | Un doublon de code renvoie 409 | **PASS** | Mapping `23505` → 409. |
| QR-04 | Un scan est strictement tenant-scoped | **PASS** | Filtrage sur le `company_id` du JWT ; un code d'une autre entreprise renvoie 404 ; test dédié. |
| QR-05 | Un scan inconnu renvoie 404 | **PASS** | 404 `NOT_FOUND` ; le code est aussi journalisé en `NOT_FOUND`. |
| QR-06 | Un scan renvoie type, catégorie, entrepôt, affectation et détenteur | **PASS** | Réponse enrichie (type, catégorie, entrepôt, affectation active, employé, poste) ; test dédié. |
| QR-07 | Un scan ne renvoie pas de champs internes inutiles | **PASS** | Plus aucun `archived_by`, `created_at` ni `company_id` brut dans la réponse ; test dédié. |
| QR-08 | Chaque scan est journalisé | **PASS** | Table `qr_scan` (migration `21_lot5_qr.sql`) : acteur, horodatage, terminal, résultat ; y compris les codes inconnus ; test dédié. |
| QR-09 | Le scan est limité en débit | **PARTIEL** | Limiteur à fenêtre glissante en mémoire par utilisateur et par entreprise (429 `RATE_LIMITED`) ; non partagé entre instances et sans politique de purge. |
| QR-10 | L'image PNG est générée correctement | **PASS** | Code + test existant |
| QR-11 | L'image est mise en cache | **PASS** | `Cache-Control: public, max-age=300` et `ETag` ; 304 sur `If-None-Match` ; taille configurable. |
| QR-12 | Une étiquette PDF est disponible | **PARTIEL** | Étiquette et planche HTML imprimables (100 articles en un appel), texte lisible et QR intégré ; pas de PDF généré par le serveur (impression via le navigateur). |
| QR-13 | Une planche d'étiquettes est disponible | **PASS** | Planche HTML de 100 articles en un appel (`POST /api/v1/inventory/labels/batch`), Lot 5. |
| QR-14 | Un code peut être révoqué avec grâce | **FAIL** | Rotation absente (décision D-05 : code immuable, sans historique ni rotation). |
| QR-15 | Deux entreprises peuvent avoir le même code sans ambiguïté | **PASS** | `UNIQUE(company_id, qr_code)` et résolution filtrée par l'entreprise du jeton ; un code d'une autre entreprise renvoie 404 (Lot 5). |

### Scénario d'acceptation QR-A

```text
Étant donné deux entreprises A et B avec le même code QR
Et un utilisateur de A
Quand il scanne le code
Alors seul l'article de A est retourné
Et la réponse contient le type, l'entrepôt et l'affectation active
Et le scan est journalisé avec l'utilisateur et l'appareil
```

---

## 9. Comptages et opérations

| ID | Critère testable | Statut | Preuve / référence |
|---|---|---|---|
| CNT-01 | Un comptage fige la quantité attendue à l'ouverture | **PASS** | Instantané `inventory_count_expected` créé à l'ouverture pour tous les articles non archivés de l'entreprise ; le scan lit cet attendu figé (`InventoryCountApiTests.scanUsesTheFrozenExpectedQuantity`). |
| CNT-02 | Un scan de comptage calcule l'écart côté serveur | **PASS** | `difference` et `discrepancy_type` calculés par le serveur ; le client ne peut pas les imposer. |
| CNT-03 | Un article hors périmètre est refusé | **PASS** | `409 ITEM_OUT_OF_SCOPE` pour un article absent de l'instantané (autre entreprise, archivé, créé après l'ouverture) ; un article attendu dans un autre entrepôt reste un écart `WRONG_LOCATION`. |
| CNT-04 | Un doublon de scan est traité explicitement | **PASS** | Contrainte unique et erreur métier `Article déjà scanné`. |
| CNT-05 | Un comptage vide ne peut pas être clôturé comme complet | **PASS** | `409 COUNT_EMPTY` et statut conservé `IN_PROGRESS` (`InventoryCountApiTests.emptyCountCannotBeClosed`). |
| CNT-06 | La clôture renseigne `completed_at` | **PASS** | Horodatage serveur écrit dans la clôture transactionnelle. |
| CNT-07 | La clôture réconcilie le stock | **PASS** | Statut, emplacement, `stock_balance` et un mouvement `ADJUSTMENT` par écart non nul (quantité égale à la valeur absolue de l'écart, source/destination) ; test de clôture. |
| CNT-08 | La clôture met à jour `last_inventory_at` | **PASS** | Écrit pour chaque ligne scannée dans la transaction de clôture (`InventoryCountApiTests.closeReconcilesStockAndJournalsAdjustments`). |
| CNT-09 | `GET /inventory-counts/{id}` existe et renvoie 404 si inconnu | **PASS** | Route ajoutée et testée. |
| CNT-10 | Les lignes sont paginées et filtrables | **PARTIEL** | `GET /{id}/results?page=&pageSize=` (maximum 500) avec `X-Total-Count` ; les filtres par statut ou type d'écart restent absents. |
| CNT-11 | Un comptage peut être annulé | **PASS** | `POST /{id}/cancel` idempotent, statut `CANCELLED`, sans ajustement ni mouvement ; `409 COUNT_COMPLETED` après clôture. |
| CNT-12 | Un comptage est tenant-scoped | **PASS** | Création, liste, détail, lignes, scan, clôture et annulation filtrent par l'entreprise du jeton ; un comptage étranger renvoie `404` (`P0-01` reste ouvert pour les autres modules). |
| OPS-01 | `POST /operations/receipt` impose `RECEIPT` | **PASS** | Type imposé par la route et testé. |
| OPS-02 | `POST /operations/issue` impose `ISSUE` | **PASS** | Type imposé par la route et testé. |
| OPS-03 | Une opération contient des lignes d'articles | **PASS** | `inventory_operation_item` créé pour chaque ligne. |
| OPS-04 | Une opération crée des mouvements atomiques | **PASS** | Mouvement, ligne et en-tête dans une transaction ; rollback testé. |
| OPS-05 | Un type manquant renvoie 400/422 | **PASS** | `23502` → 400 ; test `OperationApiTests.missingOperationTypeReturns400Envelope`. |
| OPS-06 | Une opération inconnue renvoie 404 | **FAIL** | Soft 200 |
| OPS-07 | Une opération est idempotente | **FAIL** | Absent |

### Scénario d'acceptation CNT-A

```text
Étant donné un comptage ouvert avec un instantané d'attendus figé
Quand un opérateur scanne 2 lignes avec des quantités
Et clôture le comptage
Alors chaque ligne a expected_quantity, counted_quantity et difference
Et un mouvement ADJUSTMENT est écrit pour chaque écart non nul
Et les soldes, completed_at et item.last_inventory_at sont mis à jour atomiquement
Et une seconde clôture renvoie 200 sans nouvel ajustement
Et un article hors de l'instantané est refusé en 409
```

---

## 10. Contrat HTTP

| ID | Critère testable | Statut | Preuve / référence |
|---|---|---|---|
| API-01 | Les routes métier sont versionnées `/api/v1` | **PASS** | Les 28 contrôleurs sont préfixés `/api/v1` ; l'alias `/api` est réécrit avec `Deprecation` et `Link` (Lot 7). |
| API-02 | Les listes renvoient un `PageResponse` avec total | **PARTIEL** | `PageResponse` appliqué à l'inventaire (articles, mouvements, affectations, comptages et lignes, soldes, rapports) ; les modules organisation/personnel/clients/sites et les référentiels types/catégories/entrepôts restent des tableaux bruts. |
| API-03 | Aucun endpoint ne renvoie 200 avec `errors` | **PASS** | Plus aucun endpoint atteignable ne renvoie ce format ; les occurrences restantes sont du code mort non exposé (Lot 7). |
| API-04 | Une erreur de saisie ne renvoie pas 500 | **PASS** | Doublons 409, FK/CHECK 422, NOT NULL/format/UUID 400, base indisponible 503. |
| API-05 | Aucun SQLSTATE ni message PostgreSQL en production | **PASS** | `ErrorMapper` et `SqlErrorMappingTests`. |
| API-06 | Une création renvoie 201 et `Location` | **PASS** | 201 + `Location` centralisés par `CreatedLocationAdvice` (`id()`/`getId()`), y compris `POST /inventory-counts` ; `POST /auth/register` fournit `Location: /api/v1/users/{id}` ; les créations en lot et les actions restent 200 (pas d'identifiant unique ou pas de ressource adressable). |
| API-07 | Une suppression réussie renvoie 204 | **PASS** | Tous les `DELETE` des contrôleurs renvoient 204, y compris les façades d'affectation. |
| API-08 | Une réponse de création ne contient pas de date nulle due au défaut | **PASS** | Relecture persistée ; tests timestamp mouvement/affectation. |
| API-09 | Les dates sont ISO-8601 UTC | **PASS** | `spring.jackson.time-zone=UTC` + sérialisation ISO-8601 de Jackson 3 ; test de contrat sur `created_at` et `timestamp`. |
| API-10 | Les créations sensibles acceptent `Idempotency-Key` | **FAIL** | Toujours absent ; décision de stockage requise (mémoire vs table) avant implémentation. |
| API-11 | Les `PATCH` sensibles utilisent `If-Match`/ETag | **FAIL** | Toujours absent ; décision requise sur une colonne `version`/`updated_at` ou un ETag faible calculé. |
| API-12 | Les erreurs ont un `code` stable et documenté | **PASS** | `Problem.code` stable (`BAD_REQUEST`, `VALIDATION_ERROR`, `NOT_FOUND`, `CLIENT_ERROR`, `SERVER_ERROR`, `UNAUTHORIZED`, `FORBIDDEN`, etc.) et documenté. |
| API-13 | L'OpenAPI décrit tous les endpoints, erreurs et exemples | **PARTIEL** | Depuis le Lot 9, **28 fichiers** couvrent les 28 contrôleurs, dont `stock_balance-openapi.yaml` pour `/inventory/stock-balances` ; schéma `Problem` unique conforme RFC 9457, `ItemError` supprimé des erreurs globales et `Location` documenté sur tous les 201. Validation structurelle des 28 fichiers : 0 erreur de parsing, 0 `$ref` orphelin ; `validate_specs.py` : 0 problème sur 14 fichiers / 76 opérations. Lint Redocly rejoué au Lot 9 : **0 erreur, 175 avertissements**. Manquent encore des exemples contractuels et les `license`. |
| API-14 | Le CORS est restreint par environnement | **PASS** | `CORS_ALLOWED_ORIGINS` avec défauts locaux (`localhost:3000/5173/4200`) ; plus de `*`. |
| API-15 | Les filtres couvrent les besoins des écrans | **FAIL** | L'historique article est filtrable (type de mouvement, actif) ; les filtres des listes articles et des autres modules restent partiels. |
| API-16 | Chaque réponse d'erreur contient un `traceId` | **PASS** | `Problem.traceId` renseigné par le handler global, les erreurs de sécurité et le filtre JWT ; vérifié par `HttpContractApiTests`. |
| API-17 | Les enums sont documentés avec leurs libellés | **FAIL** | Les valeurs sont listées dans certaines specs, sans libellés métier complets. |
| API-18 | Un UUID invalide renvoie 400, pas 500 | **PASS** | Test `HttpContractApiTests.invalidUuidReturnsBadRequest` : 400 `BAD_REQUEST` en problem+json avec `traceId`. |

---

## 11. Performance, exploitation et sécurité

| ID | Critère testable | Statut | Preuve / référence |
|---|---|---|---|
| NFR-01 | Les DAO utilisent un `DataSource` et un pool | **PASS** | `DatabaseConnection` + HikariCP ; test de pool. |
| NFR-02 | Les DAO utilisent le `DataSource` et la configuration de production (pool, timeouts) | **PASS** | `spring.datasource.hikari.*` surchargé par l'environnement. |
| NFR-03 | Une liste de 1 000 articles reste sous un seuil défini | **PASS** | `ItemListPerformanceTests` : budget de 2 000 ms sur la médiane de dix appels ; mesures locales 149 à 222 ms de médiane (max 318 ms) pour 1 000 articles. Seuil volontairement large, adapté à la CI et non à la production. |
| NFR-04 | Le N+1 est éliminé ou borné | **FAIL** | Mapper relationnel |
| NFR-05 | Les listes sont plafonnées | **PARTIEL** | `pageSize` plafonné à 500 et pagination appliquée à l'inventaire ; les listes hors inventaire restent non plafonnées. |
| NFR-06 | Les images QR sont mises en cache | **PASS** | `Cache-Control: public, max-age=300`, `ETag` et 304 (Lot 5, QR-11). |
| NFR-07 | Aucun secret n'apparaît dans une réponse ou un log | **PARTIEL** | Hash absent ; SQLSTATE retiré des réponses, mais logs et secrets non audités |
| NFR-08 | HTTPS est obligatoire en production | **FAIL** | Aucune règle |
| NFR-09 | Les migrations sont sauvegardées et réversibles | **NON VÉRIFIÉ** | Hors audit |
| NFR-10 | Les erreurs 500 sont corrélées par `traceId` | **PASS** | `traceId` présent dans toutes les réponses Problem Details ; vérifié par tests. |
| NFR-11 | Les routes sensibles sont limitées en débit | **PARTIEL** | Le scan QR et le résolveur sont limités (429) ; login/refresh ne le sont pas encore. |
| NFR-12 | Les conflits concurrents renvoient 409 | **FAIL** | Conflits SQL traduits en 409, mais aucune détection d'écrasement par version |
| NFR-13 | Les journaux d'audit couvrent les actions sensibles | **FAIL** | Absent |

---

## 12. Tests et CI

| ID | Critère testable | Statut | Preuve / référence |
|---|---|---|---|
| TST-01 | La suite tourne sur une base isolée et reproductible | **PARTIEL** | CI PostgreSQL 16 éphémère ; base locale réelle et pas d'isolation par test |
| TST-02 | Aucun test n'est ignoré silencieusement sur base vide | **PASS** | Aucun `Assumptions` ni `@Disabled` dans `src/test` ; le CI échoue désormais si un rapport Surefire contient `Skipped: [1-9]` (étape « Fail on skipped tests »). |
| TST-03 | Le login et l'inscription sont testés | **PASS** | Inscription, refresh et logout sont vérifiés par le code (les classes historiques correspondantes ne font plus partie de la suite) ; depuis le Lot 8, `AuthLoginFailureApiTests` couvre le mot de passe incorrect et le compte inactif (aucun jeton émis). |
| TST-04 | Le RBAC est testé pour `USER`, `INVENTORY`, `ADMIN`, sans rôle | **PARTIEL** | Tests sans permission, admin d'entreprise et inventaire ; matrice complète des rôles non couverte. |
| TST-05 | La tenancy A/B est testée en lecture et en écriture | **N/A** | Mono-entreprise par conception : aucun test A/B n'est requis. Un tel test redeviendrait nécessaire après correction de `P0-01`. |
| TST-06 | Le rollback transactionnel est testé | **PASS** | `DatabaseTransactionRollbackTests`, `StockMovementTransactionTests`. |
| TST-07 | La concurrence est testée | **PASS** | `ConcurrentInventoryApiTests` : deux affectations, deux sorties et deux retours simultanés ; une seule écriture aboutit, le solde n'est jamais négatif. Limite : ni la mise à jour d'article (`If-Match`) ni l'idempotence ne sont couvertes. |
| TST-08 | Le stock et la réconciliation sont testés | **PASS** | `InventoryCountApiTests` vérifie le solde, les ajustements, `last_inventory_at` et l'idempotence de la clôture. |
| TST-09 | Les contrats d'erreur et de pagination sont testés | **PASS** | `HttpContractApiTests` couvre Problem Details, `errors[]`, `PageResponse`, `page=0`, `Location`, dates UTC, alias `/api` et 204. |
| TST-10 | Le code et les tests sont synchronisés | **PASS** | L'historique 404, la pagination et les filtres sont testés ; le test obsolète a été remplacé. |
| TST-11 | Un pipeline CI bloque les régressions | **PARTIEL** | Workflow GitHub Actions présent : schéma et données initialisés, `mvnw test` bloquant, garde-fou sur les tests ignorés, lint OpenAPI et packaging. Restent la tenancy A/B, les seuils de couverture et la protection de branche. |
| TST-12 | Les performances critiques sont mesurées | **PASS** | `ItemListPerformanceTests` mesure la médiane de dix appels sur 1 000 articles (page de 50) et échoue au-delà de 2 000 ms ; mesures locales : **médiane 149 à 222 ms, max 318 ms**. Limite : mesure de non-régression, pas une preuve de performance de production. |
| TST-13 | La suite actuelle est verte sur l'état courant | **PASS** | 74 tests exécutés ; 0 échec, 0 erreur, 0 ignoré, 13 suites (24 octobre 2026, base dédiée `sssm_lot8_it` initialisée depuis les scripts) ; 3 passes consécutives des suites de concurrence et d'affectation. |
| TST-14 | Les tests n'écrivent pas dans la base de démonstration partagée | **PARTIEL** | Les 16 tests du Lot 8 créent leurs propres fixtures et les suppriment ; la suite a été rejouée sur une base dédiée (`sssm_lot8_it`) et n'y laisse aucune ligne `LOT8-*`. Les suites antérieures restent dépendantes des données de démonstration, et aucun garde-fou automatique n'empêche de pointer la base de démonstration. |

---

## 13. Adéquation frontend

| ID | Critère testable | Statut | Preuve / référence |
|---|---|---|---|
| FE-01 | Le catalogue peut être listé, filtré et paginé sans total manquant | **PARTIEL** | Articles paginés avec `total` ; les filtres restent limités et les listes hors inventaire ne sont pas paginées. |
| FE-02 | Le détail d'un article peut afficher type, catégorie, entrepôt et statut | **PARTIEL** | Réponses imbriquées, relations nulles |
| FE-03 | La création d'un type ne casse pas si les champs facultatifs sont omis | **PASS** | Défauts PostgreSQL appliqués et relus. |
| FE-04 | Les erreurs peuvent être mappées sur des messages utilisateur | **PASS** | Problem Details homogènes avec `code` stable, `detail` et `errors[]` ; plus de soft-200 atteignable. |
| FE-05 | L'historique d'un article est complet et paginé | **PASS** | `/api/v1/inventory/items/{id}/history` paginé et filtré, avec 404 sur article inconnu. |
| FE-06 | Un mouvement peut être créé avec quantité et acteur | **PARTIEL** | `quantity`, source/destination, opération et acteur sont exposés (Lot 3) ; l'acteur peut rester nul si le jeton et le corps n'apportent pas d'UUID. |
| FE-07 | Une affectation peut être créée, retournée et auditée | **PASS** | Contrat unique `/item-assignments`, retour serveur daté, void et archivage audités (Lot 4). |
| FE-08 | Un scan QR fournit assez d'informations pour agir | **PASS** | Réponse enrichie, tenant-scoped, 404 et journal de scan (Lot 5). |
| FE-09 | Une étiquette PDF peut être imprimée en lot | **PARTIEL** | Planche HTML de 100 articles et impression navigateur (Lot 5) ; pas de PDF généré par le serveur. |
| FE-10 | Le dashboard affiche un stock réel | **PARTIEL** | Le low-stock utilise `stock_balance` ; les compteurs de synthèse restent globaux. |
| FE-11 | Le dashboard est tenant-scoped | **N/A** | `DashboardDAOImpl` reste global, ce qui correspond au déploiement mono-entreprise ; aucun filtre à ajouter. |
| FE-12 | Les écrans peuvent gérer le multi-entreprise | **N/A** | Hors périmètre : l'interface n'expose qu'une entreprise (décision du 24 octobre 2026). |
| FE-13 | Les dates sont affichables sans rechargement après création | **PASS** | Réponses de création relues depuis la base. |
| FE-14 | Les actions sensibles sont idempotentes | **FAIL** | `Idempotency-Key` et `If-Match`/ETag absents (`P1-14`). |

---

## 14. Bilan actuel

| Domaine | PASS | PARTIEL | FAIL | NON VÉRIFIÉ | N/A |
|---|---:|---:|---:|---:|---:|
| Portes Go/No-Go | 5 | 1 | 0 | 0 | 2 |
| Tenancy | 3 | 1 | 0 | 0 | 11 |
| Authentification | 15 | 3 | 4 | 0 | 0 |
| Catalogue | 10 | 3 | 4 | 0 | 1 |
| Stock | 14 | 3 | 1 | 0 | 0 |
| Affectations | 11 | 3 | 2 | 0 | 2 |
| QR | 12 | 2 | 1 | 0 | 0 |
| Comptages / opérations | 16 | 1 | 2 | 0 | 0 |
| Contrat HTTP | 12 | 2 | 4 | 0 | 0 |
| Performance / sécurité | 5 | 3 | 4 | 1 | 0 |
| Tests / CI | 9 | 4 | 0 | 0 | 1 |
| Frontend | 6 | 5 | 1 | 0 | 2 |
| **Total** | **118** | **31** | **23** | **1** | **19** |

**Total : 192 critères** (118 `PASS`, 31 `PARTIEL`, 23 `FAIL`, 1 `NON VÉRIFIÉ`, 19 `N/A`) après le Lot 8 et la décision mono-entreprise du 24 octobre 2026. La ligne « Comptages / opérations » contient 19 critères : `CNT-01` à `CNT-12` (12) et `OPS-01` à `OPS-07` (7).

**Lecture :** le Lot 7 fait passer le contrat HTTP de 4 à 12 `PASS` : versionnement `/api/v1`, Problem Details, `traceId`, dates UTC, `Location`, 204, UUID invalide, CORS restreint. Les `PASS` supplémentaires portent aussi sur l'historique article (404, pagination, filtres), la pagination des listes d'inventaire et la mise en cache QR. Le Lot 9 ajoute deux `PASS` QR (planche, unicité inter-entreprise) et deux `PASS` frontend (affectation auditée, scan enrichi) ; l'étiquette en lot passe de `FAIL` à `PARTIEL` (HTML sans PDF serveur). Le Lot 8 ajoute trois `PASS` « Tests / CI » (aucun test ignoré, concurrence testée, performances mesurées), un `PASS` de concurrence sur les affectations (`ASG-16`) et un `PASS` de latence (`NFR-03`) ; et la tenancy A/B (`TST-05`) est passée en `N/A`. Le choix mono-entreprise du 24 octobre 2026 retire 19 critères de tenancy du décompte des échecs (`G0-1`, `G0-2`, `T-01` à `T-08`, `T-11` à `T-13`, `CAT-05`, `ASG-04`, `ASG-05`, `TST-05`, `FE-11`, `FE-12`). Restent bloquants hors tenancy : la concurrence sans ETag/idempotence (`API-10`, `API-11`), le statut HTTP du login (`A-02`, `A-03`), l'absence de rate limit sur login/refresh (`A-18`), les filtres d'écran (`API-15`) et le RBAC des modules hors inventaire.

---

## 15. Scénarios de recette minimaux

Ces scénarios doivent être exécutés sur une base isolée avant tout Go.

### S-01 — Isolation A/B — non applicable (mono-entreprise)

Scénario conservé comme **spécification de reprise** : il ne s'exécute qu'après correction de `P0-01`, si une deuxième entreprise est admise.

```text
Étant donné A et B avec chacune un article, un entrepôt et un utilisateur
Quand l'utilisateur de A liste, lit, modifie et supprime les ressources de B
Alors chaque lecture renvoie 404 et chaque écriture renvoie 404/403
Et aucune ligne de B n'est modifiée
```

### S-02 — Révocation

```text
Étant donné un ADMIN avec un refresh token valide
Quand son rôle ADMIN est retiré
Et qu'il rafraîchit
Alors le nouveau jeton ne contient plus ADMIN
Et /api/v1/users renvoie 403
```

### S-03 — Transaction

```text
Étant donné un mouvement valide
Quand la mise à jour de l'article échoue après l'insertion
Alors aucun mouvement n'est conservé
Et l'article garde son état initial
```

### S-04 — Stock

```text
Étant donné 5 unités dans l'entrepôt A
Quand 2 unités sont sorties
Alors le solde est 3
Et un mouvement de 4 unités est refusé sans modifier le solde
Et le mouvement contient quantité, acteur et entrepôt source
```

### S-05 — Affectation

```text
Étant donné un article disponible
Quand il est affecté à un employé actif de la même entreprise
Alors l'article est ASSIGNED et une affectation active existe
Quand il est retourné
Alors l'article est AVAILABLE, l'affectation est clôturée et l'entrepôt est restauré
```

### S-06 — Comptage

```text
Étant donné un comptage avec 3 lignes attendues
Quand 2 lignes sont scannées et le comptage clôturé avec ajustements
Alors les écarts sont calculés par le serveur
Et les soldes sont ajustés atomiquement
Et last_inventory_at et completed_at sont renseignés
```

### S-07 — QR

```text
Étant donné un article avec un code QR connu
Quand un utilisateur authentifié scanne ce code
Alors l'article est retourné avec son affectation active
Et le scan est journalisé
Quand le code est inconnu
Alors la réponse est 404 et la tentative est journalisée
```

### S-08 — Erreurs

```text
Étant donné une création avec un doublon
Quand l'appel est effectué
Alors la réponse est 409 avec un code stable
Et aucun SQLSTATE n'est exposé
Et aucun 200 avec errors n'est retourné
```

---

## 16. Critères de sortie par phase

| Phase | Critères minimum |
|---|---|
| Pilote interne mono-entreprise | T-14, A-19, CAT-01, STK-14, QR-10, API-07 |
| Pilote multi-utilisateurs | T-10, T-14, T-15, A-01 à A-22, ASG-01 à ASG-18 |
| Production mono-entreprise | G0-3 à G0-8, STK-01 à STK-18, API-03 à API-05, A-02, A-03, A-18 |
| Ouverture à une deuxième entreprise (hors périmètre) | Correction de `P0-01`, puis `G0-1`, `G0-2`, `T-01` à `T-13`, `CAT-05`, `ASG-04`, `ASG-05`, `TST-05`, `FE-11`, `FE-12` et scénario S-01 |
| Ouverture frontend | FE-01 à FE-14, API-01 à API-18 |

---

## 17. Conclusion

Le déploiement est **mono-entreprise par conception**. La tenancy n'entre donc plus dans les portes Go/No-Go : `G0-1` et `G0-2` sont `N/A`, et aucun `FAIL` ne subsiste sur les portes.

Le backend peut servir un pilote mono-entreprise avec un utilisateur de confiance et un stock désormais prouvé par les tests. Il ne peut pas encore être déclaré prêt pour la production : le login renvoie 400 au lieu de 401/403 (`A-02`, `A-03`), login et refresh ne sont pas limités en débit (`A-18`), l'idempotence HTTP reste absente (`API-10`, `API-11`), le N+1 sur l'historique n'est pas mesuré (`P1-08`) et aucun seuil de couverture n'est appliqué.

Si une deuxième entreprise devait être admise, la condition préalable est la correction de `P0-01`, puis le retour en `PASS`/`FAIL` bloquant de tous les critères aujourd'hui `N/A` (voir section 3 et scénario S-01). Le plan de correction est dans le rapport 14, les anomalies et priorités dans le rapport 12.
