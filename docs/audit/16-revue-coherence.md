# 16 — Revue de cohérence et vérification des preuves

**Date :** 21 octobre 2026  
**Objet :** deuxième passe de cohérence sur les rapports 00 à 15, vérification des affirmations contre le code, le schéma PostgreSQL, les scripts SQL, les tests et OpenAPI.  
**HEAD examiné :** `7e3f196` (`fix(inventory): guarantee ItemError envelope on QR code image endpoint`).  
**Audit initial :** commit `a3f7c56` (`docs(audit): add backend readiness and security audit`).  
**Nature :** revue documentaire et statique, sans exécution de la suite complète et sans modification de données lors de la revue initiale.

**Mise à jour du 21 octobre 2026 (Lot 1) :** le code d'authentification, de tenancy et de RBAC a été corrigé, la migration `18_lot1_security.sql` a été appliquée et la suite complète a été exécutée : **120 tests, 119 succès, 1 échec préexistant**. Les sections ci-dessous distinguent l'état de la revue initiale et l'état après Lot 1.

**Mise à jour du 22 octobre 2026 (Lot 2) :** l'accès JDBC passe désormais par le `DataSource` Spring/HikariCP et les transactions de service couvrent réellement les DAO. Les erreurs SQL sont traduites en 400/409/422/503 sans exposer SQLSTATE ni message PostgreSQL ; les créations de type, d'employé, de mouvement et d'affectation relisent la ligne persistée. **P1-09, P0-04, P1-01, P1-02 et P1-10 sont corrigés** dans le périmètre documenté. Suite complète : **132 tests, 0 échec, 0 erreur, 0 ignoré, 27 suites**. `P0-01` (filtres `company_id` des DAO d'inventaire) et la concurrence (`P1-14`) restent hors de ce lot ; aucune migration SQL n'a été exécutée.

**Mise à jour du 22 octobre 2026 (Lot 3) :** le stock disponible est désormais calculé dans `security.stock_balance` et exposé par `GET /api/inventory/stock-balances`. Le journal est append-only : `PATCH` et `DELETE` renvoient `409 IMMUTABLE_RESOURCE`, et les mouvements portent quantité, entrepôts source/destination, opération et acteur. Les routes d'opération créent l'en-tête, les lignes et les mouvements dans une seule transaction, avec le type imposé par la route. Les scans de comptage figent `expected_quantity`, `counted_quantity` et `difference` calculés serveur ; la clôture réconcilie l'article et le solde, écrit `last_inventory_at` et `completed_at` dans la même transaction. **P0-05, P1-03, P1-06 et P1-07 sont corrigés dans le périmètre du Lot 3.** Suite complète : **140 tests, 0 échec, 0 erreur, 0 ignoré, 28 suites**. La migration additive `19_lot3_stock.sql` a été appliquée et rejouée sans écraser les soldes existants. `P0-01`, `P0-06` et `P1-14` restent ouverts : les affectations peuvent encore modifier l'état d'un article sans mettre à jour le solde, et la clôture d'un comptage n'écrit pas de mouvement de compensation dans le journal.

**Mise à jour du 22 octobre 2026 (Lot 4) :** le cycle de vie des affectations est unifié derrière un contrat canonique `/api/item-assignments` ; `/api/inventory/assignments` devient une façade dépréciée qui délègue au même service, avec les mêmes DTO, les mêmes codes HTTP et des en-têtes `Deprecation`/`Link`. Le JSON libre et les réponses 200 portant une erreur douce ont été supprimés. Un mouvement `OUT` exige `employee_id` et `post_id` et crée une affectation active ; un `RETURN` clôt l'affectation ; `IN`, `TRANSFER` et `DESTRUCTION` refusent un article affecté. Le retour (`POST /{id}/return`) pose la date et l'acteur côté serveur, restaure l'entrepôt d'origine et l'article, et synchronise le solde. Les articles, catégories, types et entrepôts sont archivés logiquement, restaurables, et absents des listes ; les affectations sont archivées par un void motivé et audité, et une affectation active ne peut pas être supprimée. **P0-06, P1-05 et P1-11 sont corrigés dans le périmètre du Lot 4.** Suite complète à cette date : **150 tests, 0 échec, 0 erreur, 0 ignoré, 28 suites** (suite depuis réduite, voir § 7.6). La migration additive `20_lot4_assignments.sql` a été appliquée, rejouée et vérifiée comme idempotente. `P0-01` (tenancy) et `P1-14` (concurrence/idempotence) restent ouverts, ainsi qu'une affectation active préexistante de la base de démonstration qui pointe vers un article `AVAILABLE` et n'a pas été corrigée automatiquement.

**Mise à jour du 22 octobre 2026 (Lot 5) :** le scan QR est désormais **strictement limité à l'entreprise de l'appelant**, renvoie **404** sur un code inconnu et une réponse **enrichie** (type, catégorie, entrepôt, affectation active, détenteur, poste) sans champ technique interne. Chaque tentative alimente la nouvelle table `qr_scan` (acteur, horodatage, terminal, résultat). Les résolutions sont **limitées en débit** (429 `RATE_LIMITED`). Le code QR est **immuable** (décision D-05) : `PATCH qr_code` est refusé en 400 `IMMUTABLE_QR_CODE`. L'image PNG est **mise en cache** (`ETag`, `Cache-Control`, 304) et accepte une taille configurable. Une **planche HTML** d'étiquettes (une ou cent) est produite sans dépendance PDF. La migration additive `21_lot5_qr.sql` a été appliquée, rejouée et vérifiée idempotente. **P1-04 est corrigé dans le périmètre du Lot 5 ; P2-05 est partiellement corrigé** (HTML imprimable, mais pas de PDF serveur). Suite complète : **38 tests, 0 échec, 0 erreur, 0 ignoré, 6 suites**. `P0-01`, `P1-14`, la rétention du journal et le partage du limiteur entre instances restent ouverts.

**Mise à jour du 23 octobre 2026 (Lot 6) :** les comptages figent leurs attendus à l'ouverture dans `inventory_count_expected` et refusent un article hors périmètre (`409 ITEM_OUT_OF_SCOPE`). Ils sont cloisonnés par entreprise, leur clôture est idempotente et refuse un comptage vide, l'annulation est disponible, les lignes sont paginées et chaque écart non nul produit un mouvement `ADJUSTMENT`. La migration additive `22_lot6_inventory_counts.sql` a été appliquée, rejouée et vérifiée idempotente. **P1-07 est complété dans son périmètre documenté.** Suite complète : **49 tests, 0 échec, 0 erreur, 0 ignoré, 7 suites**. `P0-01` (tenancy générale), `P1-14` (concurrence), le marquage des attendus non scannés et les filtres de lignes restent ouverts.

**Mise à jour du 23 octobre 2026 (Lot 7) :** les 28 contrôleurs exposent `/api/v1` ; l'alias `/api` reste accepté et porte `Deprecation`/`Link`. Les erreurs sont des Problem Details RFC 9457 (`application/problem+json`) avec `code`, `traceId` et `timestamp` ; les validations ajoutent `errors[]`. Les listes d'inventaire renvoient `PageResponse` avec `total` et `pageSize` plafonné à 500. Les créations renvoient 201 + `Location` (y compris `auth/register`), toutes les suppressions 204, les dates sont ISO-8601 UTC. L'historique article renvoie 404 s'il est inconnu, est paginé et filtré (`movementType`, `active`) : **P1-12 est corrigé**. **P2-02 est corrigé pour l'inventaire** et **P2-03 est corrigé** (versionnement, dates, documentation des enums partielle). Les 27 specs OpenAPI utilisent désormais un schéma `Problem` unique conforme RFC 9457 et documentent `Location` sur les 201. Suite complète : **58 tests, 0 échec, 0 erreur, 0 ignoré, 8 suites**. Aucune migration SQL. `P1-08` reste partiel (N+1) et `P1-14` reste ouvert : `API-10` (`Idempotency-Key`) et `API-11` (`If-Match`/ETag) exigent une décision de stockage. `API-02` reste partiel hors inventaire, `API-15` (filtres d'écran) et `API-17` (libellés d'enums) restent ouverts.

**Mise à jour du 23 octobre 2026 (Lot 9) :** la préparation frontend est documentée dans `docs/frontend/` (environnements, stockage/refresh des jetons, catalogue d'erreurs, pagination et filtres, maquettes scan/étiquettes, critères d'acceptation, données de démonstration). Le contrat OpenAPI gagne `stock_balance-openapi.yaml` : le corpus passe à **28 fichiers / 149 opérations**, et la structure invalide de ce fichier (`servers`/`security`/`tags` imbriqués sous `info`) et deux descriptions YAML non citées ont été corrigées. Lint Redocly rejoué : **0 erreur, 175 avertissements**. Le jeu de démonstration reste **mono-entreprise** : le multi-entreprise est reporté tant que `P0-01` n'est pas corrigé. Verdict : **No-Go production, Go démonstration mono-entreprise**. Aucun changement de code, de schéma, de donnée ou de dépendance ; aucune migration.

**Mise à jour du 24 octobre 2026 (Lot 8) :** la suite passe à **74 tests dans 13 classes**, tous verts sur une base dédiée (`sssm_lot8_it`) initialisée depuis les scripts. Seize tests autonomes couvrent le cycle de vie des affectations, la cohérence mouvement/stock (rollback complet sur stock insuffisant), trois courses concurrentes, l'échec de login et la latence d'une page de 50 sur 1 000 articles (médiane 149 à 222 ms selon l'exécution, max 318 ms). Les fixtures créent et suppriment leur entreprise, utilisateur, entrepôt, type, employé et poste ; le CI échoue si un test est ignoré. **`P1-13` passe à partiellement corrigé** (score 15) et **`P2-06` aussi**. Toujours ouverts : l'idempotence (`P1-14`), la matrice RBAC complète et tout seuil de couverture. Aucun code applicatif, schéma, donnée ou dépendance modifié ; aucune migration.

**Décision du 24 octobre 2026 — mono-entreprise :** le produit est exploité pour une seule entreprise ; `P0-01` devient un **écart accepté** (score résiduel 5) et la tenancy A/B sort du périmètre. Les colonnes `company_id`, le claim JWT et le scope entreprise des utilisateurs/rôles restent en place. Toute deuxième entreprise exigerait la correction préalable de `P0-01`. Documentaire uniquement : aucun code, schéma ou donnée modifié.

**Verdict :** inchangé — **NON PRÊT POUR LA PRODUCTION**. Le backend reste **PARTIELLEMENT PRÊT** pour un prototype mono-entreprise, en lecture et CRUD encadré, avec un utilisateur de confiance. Le stock est fiable pour les flux mouvement/opération/comptage du Lot 3, les flux d'affectation et de scan QR sont encadrés depuis les Lots 4 et 5, et le contrat HTTP est stabilisé depuis le Lot 7. La concurrence (ETag/idempotence) reste bloquante ; la tenancy est hors périmètre par la décision mono-entreprise du 24 octobre 2026.

---

## 1. Résultat de la revue

Les conclusions structurantes de l'audit initial sont confirmées :

- aucune isolation multi-entreprise effective (hors périmètre depuis la décision mono-entreprise du 24 octobre 2026) ;
- aucun contrôle d'accès métier sur l'inventaire ;
- transactions Spring inopérantes sur les DAO `DriverManager` ;
- stock non calculé et non réconcilié ;
- cycle de vie des affectations incohérent ;
- erreurs de saisie souvent transformées en 500 avec SQLSTATE ;
- QR Code fonctionnel pour un prototype, mais scan global et brut ;
- tests insuffisants pour la tenancy, le RBAC, les transactions, la concurrence et la réconciliation.

**Après le Lot 1 :** l'inscription publique est fermée, le JWT porte l'entreprise et les permissions, le refresh est distinct/rotatif/révocable, le logout est implémenté, le RBAC couvre l'inventaire et l'administration, et les utilisateurs/rôles sont filtrés par entreprise. Les points encore ouverts sont l'isolation complète des DAO d'inventaire, le RBAC des modules hors inventaire, les transactions, le stock, les affectations et la concurrence.

**Après le Lot 3 :** les mouvements, opérations et clôtures de comptage alimentent un solde calculé et testé ; le journal est append-only et les opérations multi-lignes sont atomiques. La tenancy (`P0-01`), les machines à états d'affectation (`P0-06`) et la concurrence (`P1-14`) restent les limites structurantes.

**Après le Lot 4 :** les affectations ont un contrat unique, une machine à états testée (création, retour, void, archivage, restauration) et sont synchronisées avec le stock ; les référentiels sont archivés logiquement et restaurables. La tenancy (`P0-01`), la concurrence (`P1-14`), la cohérence entreprise/article/employé/poste et le rejet des références vers des référentiels archivés restent les limites structurantes, ainsi que la réconciliation de la donnée de démonstration incohérente.

La revue a toutefois trouvé **onze incohérences ou imprécisions documentaires**, dont plusieurs erreurs factuelles. Elles sont corrigées dans les fichiers concernés et listées au § 4. Aucune anomalie de code nouvelle n'a été ajoutée au registre du rapport 12.

Le registre reste composé de **26 anomalies uniques** :

| Priorité | Nombre | Identifiants |
|---|---:|---|
| P0 | 6 | `P0-01` à `P0-06` |
| P1 | 14 | `P1-01` à `P1-14` |
| P2 | 6 | `P2-01` à `P2-06` |

Aucun identifiant n'est dupliqué. La matrice du rapport 12 contient désormais les 26 lignes, y compris `P1-02`, qui manquait.

---

## 2. Matrice de preuves

La matrice ci-dessous est la photographie de l'**audit initial**. Les lignes relatives à l'inscription, au RBAC, au refresh et au JWT ont été corrigées par le Lot 1 ; voir le § 2.1 pour l'état courant.

| Conclusion vérifiée | Preuve principale | Niveau |
|---|---|---|
| 26 contrôleurs métier, 130 routes applicatives | 29 fichiers `*Controller.java` : 26 contrôleurs métier, `HelloController`, `PingController`, `ItemErrorController` sans route métier. Les mappings donnent 57 routes d'inventaire, 23 d'authentification/administration et 50 pour les autres modules. | Statique, vérifié |
| 12 contrôleurs d'inventaire, 57 routes | 12 fichiers dans `a11_inventory/**/controller` ; somme des mappings : `Assignment` 7, `Dashboard` 2, `InventoryCount` 5, `Item` 9, `ItemAssignment` 5, `ItemCategory` 5, `ItemType` 9, `Operation` 3, `QrScan` 1, `Report` 1, `StockMovement` 5, `Warehouse` 5. | Statique, vérifié |
| OpenAPI : 26 fichiers, dont 12 d'inventaire | `docs/api/**/*.yaml` contient 26 fichiers ; `docs/api/11_inventory` en contient 12. | Fichiers, vérifié |
| Les 33 endpoints du rapport 13 sont des propositions | Tous les chemins proposés commencent par `/api/v1` ; aucune occurrence de `/api/v1` dans `src/main/java`. | Statique, vérifié |
| Aucune isolation entre entreprises | `ItemDAOImpl.getAllItems` exécute `SELECT * FROM security.item ORDER BY id LIMIT ? OFFSET ?` sans `company_id` ; mêmes absences dans les listes de mouvements, affectations, comptages, opérations, dashboard, rapports et scan. `company_id` est fourni dans les DTO de création. | Code, vérifié |
| Aucun RBAC métier | `SecurityConfig` protège `/api/inventory/**` par `authenticated()` seulement ; `hasRole("ADMIN")` ne couvre que `/api/users/**`, `/api/roles/**` et `/api/permissions/**`. Aucun `@PreAuthorize`, `@Secured` ou `@RolesAllowed` dans l'inventaire. Les tables `permission` et `role_permission` ne sont pas consommées. | Code, vérifié |
| Inscription publique non encadrée | `AuthService.register` met le compte à `ACTIVE`, ne renseigne pas d'entreprise et affecte au mieux le rôle global par défaut. Un compte sans entreprise peut appeler l'inventaire. | Code, vérifié |
| Le refresh conserve les rôles du jeton | `AuthService.refresh` lit `claims.get("roles", List.class)` et recrée un jeton avec cette liste ; il recharge l'utilisateur et vérifie son statut `ACTIVE`, mais ne recharge pas ses rôles en base. | Code, vérifié |
| JWT sans révocation ni contexte tenant | `JwtService.createToken` pose `sub`, `uid`, `roles` et `exp` ; il n'émet ni `jti`, ni `iss`, ni `aud`, ni `company_id`. Aucun denylist ni session serveur. | Code, vérifié |
| Les DAO contournent Spring et le pool (constat initial, corrigé au Lot 2) | 25 classes de type DAO/répository utilisent `DatabaseConnection.getConnection()` ; aucune ne référence `DataSource` ou `JdbcTemplate`. `DatabaseConnection` ouvre une connexion `DriverManager` avec des valeurs de repli locales. | Code, vérifié ; voir section 5 pour l'état après Lot 2 |
| Les transactions ne couvrent pas les écritures multi-tables (constat initial, corrigé au Lot 2) | Les services portent des `@Transactional`, mais les connexions sont ouvertes hors du `DataSource` Spring. Les écritures article + mouvement ou article + affectation ne partagent pas la transaction Spring. | Code, vérifié ; voir section 5 pour l'état après Lot 2 |
| Stock non fiable | `item_type.quantity_total` est un champ manuel (`NUMERIC(12,2) NOT NULL DEFAULT 0`) ; `stock_movement.quantity` a un défaut à 1 et n'est pas exposé par le DTO ; `warehouse_stock_config` existe mais n'est pas utilisé ; `last_inventory_at` est lu mais jamais écrit. | Schéma + code, vérifié |
| Cycle d'affectation incohérent | `StockMovementService` : `OUT` passe l'article à `ASSIGNED` sans créer d'affectation ; `RETURN` ne clôt aucune affectation. `AssignmentService.returnItem` sans `returned_at` est un no-op ; `ItemAssignmentService.delete` supprime l'affectation sans remettre l'article à `AVAILABLE` ; une création avec `returned_at` déjà renseigné passe quand même l'article à `ASSIGNED`. L'index `uq_item_assignment_active` protège seulement l'unicité active. | Code + schéma, vérifié |
| Historique global et tronqué | `ItemController.getItemHistory` appelle `getStockMovements(1, 100)` et `getItemAssignments(1, 100)` sans filtre SQL, puis filtre en mémoire. Aucun contrôle d'existence : UUID inconnu → **200** avec listes vides. | Code, vérifié |
| Erreurs QR de l'image corrigées | `ItemController.getQrCodeImage` mappe UUID invalide → 400 `BAD_REQUEST`, article inconnu → 404 `NOT_FOUND`, contenu absent → 500 `QR_CODE_MISSING`, échec d'encodage/flux → 500 `QR_CODE_GENERATION_FAILED`, et sérialise les erreurs en JSON même avec `Accept: image/png` strict. | Code + test, vérifié |
| Suite de tests non probante sur HEAD | 22 classes et 112 tests dans l'arbre ; le seul rapport Surefire courant couvre 7 tests QR, tous verts. Aucun rapport complet pour `7e3f196`. L'ancien test `ItemApiTests.historyUnknownReturnsNotFound` attendait 404 alors que le contrôleur renvoyait 200 ; il a depuis été retiré. | Fichiers + lecture statique |
| CI présente mais incomplète | `.github/workflows/ci.yml` initialise PostgreSQL 16, applique le schéma et les données de démonstration, exécute `./mvnw -B -q test`, un lint Java/OpenAPI et un packaging. Aucun test de tenancy, RBAC, transaction, concurrence ou réconciliation. | CI, vérifié |
| Schéma et triggers | 18 scripts `00_init.sql` à `17_seed_permissions.sql` lors de la revue ; 19 après ajout de `18_lot1_security.sql`, 20 après `19_lot3_stock.sql`. Les triggers `updated_at` couvrent notamment `company`, `branch`, `agency`, `app_user`, `employee`, `client`, `site`, `contract`, `incident`, `invoice` ; `item_category`, `warehouse` et `item_assignment` n'en ont pas. | SQL, vérifié |
| Base de démonstration mono-entreprise | 1 entreprise, 10 articles, 8 mouvements, 5 affectations actives, 0 mouvement avec quantité différente de 1, 0 source/destination/acteur renseigné, 0 affectation avec `assigned_by`/`returned_by`, 0 article avec `last_inventory_at`. | Lecture seule |

### 2.1. État après le Lot 1

| Point | État vérifié |
|---|---|
| Routes | 131 routes applicatives après ajout de `POST /api/auth/logout` (130 à l'audit initial). |
| Inscription | `POST /api/auth/register` exige `ROLE_ADMIN` ou `users.manage` ; entreprise déduite du jeton. |
| JWT | `jti`, `iss`, `aud`, `company_id`, `roles`, `permissions`, `platform_admin`. |
| Refresh | Token distinct, haché, consommation atomique de l'ancien token, rechargement des rôles/permissions. |
| Révocation | `POST /api/auth/logout`, denylist `security.revoked_access_token`, contrôle du `jti` dans le filtre. |
| RBAC | Inventaire, utilisateurs, rôles et catalogue des permissions ; modules organisation/personnel/clients/sites encore `authenticated`. |
| Scope entreprise | Utilisateurs et rôles filtrés par `company_id` ; admin plateforme non filtré. |
| CORS | Origines explicites via `CORS_ALLOWED_ORIGINS`. |
| Tests | 120 exécutés, 119 succès, 1 échec préexistant ; 29 tests ciblés auth/RBAC verts. |
| Reste ouvert | DAO d'inventaire non filtrés, `NOT NULL`/FK composites non appliqués, transactions, stock, affectations, rate limiting, purge des jetons, erreurs SQL. |

**Depuis le Lot 2 (22 octobre 2026) :** les transactions et les erreurs SQL ne figurent plus dans les points ouverts. Depuis le 24 octobre 2026, `P0-01` est un écart accepté (mono-entreprise) ; restent le stock, les affectations, la concurrence et le RBAC hors inventaire.

### 2.2. État après le Lot 4 (22 octobre 2026)

| Point | État vérifié |
|---|---|
| Routes | 152 annotations de mapping sur 30 fichiers `*Controller.java` : 150 routes métier sur 28 contrôleurs, plus les 2 routes de test `/` et `/ping`. |
| Routes d'inventaire | 76 routes sur 14 contrôleurs : `Assignment` 11, `Dashboard` 2, `InventoryCount` 7, `Item` 10, `ItemAssignment` 11, `ItemCategory` 6, `ItemType` 10, `Label` 2, `Operation` 3, `QrScan` 1, `Report` 1, `StockBalance` 1, `StockMovement` 5, `Warehouse` 6. |
| Contrat d'affectation | `/api/item-assignments` canonique ; `/api/inventory/assignments` façade dépréciée vers le même `ItemAssignmentService` ; plus de JSON libre ni de soft-200. |
| Cycle de vie | Création active, retour, void motivé, archivage, restauration, listes filtrées ; invariants testés. |
| Mouvements | `OUT` crée l'affectation, `RETURN` la clôture, `IN`/`TRANSFER`/`DESTRUCTION` refusent un article affecté. |
| Archivage | Articles, catégories, types et entrepôts archivés logiquement, restaurables, exclus des listes ; affectations voidées avec motif/acteur/date. |
| Migration | `20_lot4_assignments.sql` additive : `source_warehouse_id`, `voided_at`, `voided_by`, `void_reason`, `archived_by`, deux `CHECK` et trois index. Appliquée, rejouée, idempotente. |
| Lot 5 | Scan QR tenant-scoped, enrichi, journalisé (`qr_scan`) et limité en débit ; code QR immuable ; image PNG en cache ; planches d'étiquettes HTML. Migration `21_lot5_qr.sql` additive et idempotente. |
| Lot 6 | Comptages avec instantané figé (`inventory_count_expected`), périmètre tenant, clôture idempotente/non vide, annulation, lignes paginées, mouvements `ADJUSTMENT`. Migration `22_lot6_inventory_counts.sql` additive et idempotente. |
| Lot 7 | Routes canoniques `/api/v1` et alias `/api` déprécié (`Deprecation`/`Link`) ; errors Problem Details RFC 9457 avec `code`/`traceId`/`timestamp` ; `PageResponse` sur l'inventaire ; 201 + `Location`, 204 ; dates ISO-8601 UTC ; historique article paginé/filtré et 404. Aucune migration SQL. |
| Lot 9 | Préparation frontend documentée (`docs/frontend/`) ; spec OpenAPI des soldes ajoutée (28 fichiers / 149 opérations) et validée ; jeu de démonstration maintenu mono-entreprise. Aucun changement de code, SQL ou donnée. |
| Lot 8 | Tests d'affectation, de mouvement, de concurrence, de login et de latence ; fixtures autonomes ; base dédiée ; garde-fou CI sur les tests ignorés. Aucun changement de code, SQL ou donnée. |
| Tests | 74 tests dans 13 classes (24 octobre 2026) : schéma, QR, comptages, contrat HTTP, affectations, mouvements, concurrence, login et latence. Fixtures autonomes pour les 16 tests du Lot 8. La suite fonctionnelle historique de 150 tests n'existe plus. |
| Reste ouvert | `P1-14` (ETag/`Idempotency-Key`), `P1-08` (N+1), références vers des référentiels archivés, purge physique définitive, réconciliation de la donnée de démonstration, pagination hors inventaire et filtres d'écran. La tenancy (`P0-01`) est un écart accepté mono-entreprise : elle redevient bloquante seulement si une deuxième entreprise est admise. |

---

## 3. Routes existantes et propositions

### 3.1. Routes réellement exposées

| Module | Contrôleurs | Routes |
|---|---:|---:|
| Inventaire | 12 | 57 |
| Authentification / administration | 4 | 23 |
| Organisation | 3 | 15 |
| Personnel | 2 | 10 |
| Clients | 2 | 10 |
| Sites | 3 | 15 |
| **Total métier** | **26** | **130** |

Les deux routes de test `/` et `/ping` sont exclues. `ItemErrorController` expose la route technique `/error`, sans route métier.

### 3.2. Rapport 13

Le rapport 13 contient **33 propositions** (`E-01` à `E-33`). Toutes sont sous `/api/v1`, préfixe absent du code courant. Elles ne doivent donc pas être présentées comme existantes.

Certaines propositions recouvrent fonctionnellement des routes actuelles, mais avec un contrat différent :

- `POST /api/v1/auth/register` propose une inscription encadrée, alors que `POST /api/auth/register` existe déjà en version publique ;
- `POST /api/v1/stock/movements` propose quantité et acteur, alors que `POST /api/inventory/movements` existe sans ces champs ;
- `GET /api/v1/items/{itemId}/history` propose un historique paginé et filtré, alors que la route actuelle est globale et tronquée ;
- `POST /api/v1/inventory-counts/{id}/close` propose une réconciliation, alors que la clôture actuelle ne réconcilie rien.

Le rapport 13 reste donc **une spécification cible**, pas un inventaire de l'existant. Les rapports 02 et 10 décrivent le contrat réel.

---

## 4. Contradictions et corrections documentaires

| ID | Incohérence constatée | Résolution |
|---|---|---|
| C-01 | `README.md` affirmait que le dépôt n'était pas initialisé en Git, alors que les commits `a3f7c56` et `7e3f196` existent. | Corrigé : commits cités et HEAD de revue précisé. |
| C-02 | Les rapports 00, 01, 02 et README annonçaient **14 fichiers OpenAPI d'inventaire** ; `docs/api/11_inventory` en contient **12** (26 au total, dont 14 hors inventaire). | Corrigé dans 00, 01, 02 et README. |
| C-03 | Le rapport 10 annonçait **13 contrôleurs d'inventaire** ; il y en a **12**. Le total de 26 contrôleurs et 130 routes reste correct à l'audit initial. | Corrigé dans 10 ; le rapport 01 est également aligné. Après Lot 1, le total est 131 routes avec `POST /api/auth/logout`. |
| C-04 | Le rapport 06 décrivait un **500 sans corps** pour un QR vide ; le HEAD `7e3f196` renvoie un **500 JSON `QR_CODE_MISSING`**, et un UUID invalide un **400 `BAD_REQUEST`**. | Corrigé dans 06, avec la nouvelle classe de tests. |
| C-05 | Le rapport 02 indiquait seulement `200 PNG / 404` pour l'image QR, sans les 400/500 du correctif. | Corrigé dans 02. |
| C-06 | Le rapport 07 disait qu'un historique inconnu renvoie « probablement 200 » ; 02, 11 et 12 affirmaient 200. Le code courant renvoie toujours 200. | Corrigé : 200 affirmé sans ambiguïté, divergence de test explicitée. |
| C-07 | Le rapport 12 listait 14 P1 dans le registre mais seulement 13 dans la matrice : `P1-02` manquait. | Corrigé : ligne `P1-02` ajoutée (probabilité 4, impact 3, score 12, risque moyen). |
| C-08 | Le rapport 15 comptait 18 critères « Comptages / opérations » alors que `CNT-01..CNT-12` + `OPS-01..OPS-07` = 19 ; le total affiché était 191 au lieu de 192. | Corrigé : ligne à 19 et total explicite de 192. Les statuts ont évolué avec les Lots 1 à 3 ; le bilan courant est dans la section 14 du rapport 15 (68 `PASS`, 29 `PARTIEL`, 92 `FAIL`, 3 `NON VÉRIFIÉ` après le Lot 6). |
| C-09 | Les rapports 00, 01, 11 et README présentaient 21 classes / 105 tests comme l'état courant. L'arbre HEAD contient 22 classes / 112 tests, et le seul rapport Surefire restant couvre 7 tests QR. | Corrigé : 105/21 étiqueté « rapport complet historique », état HEAD et absence de suite complète précisés. |
| C-10 | Le rapport 06 affirmait qu'aucun test ne couvrait l'image d'un QR vide ; `ItemControllerQrCodeImageTest` couvre désormais ce cas, ainsi que le 400, les 500 et `Accept: image/png`. | Corrigé dans 06. |
| C-11 | Les 183 avertissements Redocly sont cités comme un état courant alors que le décompte n'a pas été rejoué après `7e3f196`. | Signalé comme mesure de l'audit initial. **Rejoué au Lot 4** : 179 avertissements, 0 erreur. |

Aucune de ces corrections ne modifie le verdict, les priorités P0 ou les conclusions métier.

---

## 5. Tableau final des anomalies

Les colonnes « Dépendances » indiquent les anomalies ou décisions qui doivent être traitées avant ou avec l'anomalie. « Critère de résolution » décrit une preuve vérifiable, pas une intention.

### 5.1. P0 — bloquants

**Statut après Lot 4 :** `P0-03` corrigé ; `P0-04` corrigé pour les cas d'usage existants (pool Spring, connexion transactionnelle, tests de rollback service et DAO) ; `P0-02` partiel (inscription fermée et RBAC inventaire/administration, mais modules hors inventaire non couverts) ; `P0-01` partiel (colonnes/backfill/index et scope utilisateurs/rôles, mais DAO d'inventaire non filtrés), devenu **écart accepté** par la décision mono-entreprise du 24 octobre 2026 ; `P0-05` corrigé dans le périmètre du Lot 3 (solde calculé, mouvements complets, comptage réconcilié), avec dette historique sur les lignes antérieures au Lot 3, les ajustements de clôture étant journalisés depuis le Lot 6 ; `P0-06` corrigé dans le périmètre du Lot 4 (machine à états unique, retour/void/suppression audités, mouvements `OUT`/`RETURN` raccordés, solde synchronisé), sous réserve de la donnée de démonstration antérieure non réconciliée. Les opérations multi-lignes sont désormais implémentées et atomiques (`P1-06`), avec un test de rollback.

| ID | Preuve | Impact | Dépendances | Critère de résolution |
|---|---|---|---|---|
| **P0-01** | `ItemDAOImpl.getAllItems` et les autres listes n'ont aucun prédicat `company_id` ; `company_id` vient du client ; `SecurityConfig` ne demande qu'une authentification sur `/api/inventory/**`. | Lecture et écriture inter-entreprises ; fuite et corruption de données. | D-02, P0-02 | Toute requête métier est filtrée par une entreprise dérivée du serveur ; tests croisés A/B en lecture et écriture ; 403/404 selon la politique ; aucune donnée d'une autre entreprise retournée. |
| **P0-02** | `SecurityConfig` : seul l'admin est exigé sur users/roles/permissions ; aucun `@PreAuthorize` sur l'inventaire ; `AuthService.register` crée un compte `ACTIVE` sans entreprise ; permissions non consommées. | Tout compte authentifié, y compris public sans entreprise, peut lire ou modifier l'inventaire ; escalade d'administration. | D-03, D-08, P0-01 | Inscription fermée, invitée ou validée ; rôles et permissions appliqués par route et par entreprise ; tests anonyme, `USER`, `ADMIN`, sans entreprise et cross-tenant. |
| **P0-03** | `AuthService.refresh` reprend les rôles du jeton ; `JwtService` n'émet ni `jti`, ni révocation, ni `company_id`. | Un rôle révoqué reste utilisable jusqu'à expiration, et peut être rafraîchi tant que le compte est actif ; pas de logout serveur. | D-03, P0-02 | Rôles rechargés en base au refresh ; version/session ou denylist ; `jti`, `iss`, `aud`, `company_id` ; rotation et révocation testées. |
| **P0-04** | **Constat initial :** 25 DAO utilisent `DatabaseConnection`/`DriverManager` ; aucun ne passe par le `DataSource` Spring ; les services multi-écritures sont annotés `@Transactional` mais hors transaction Spring. | Écritures partielles : article modifié sans mouvement, affectation créée sans état, opération sans ligne, etc. | P1-09 | DAO sur `DataSource`/pool ; une transaction par cas d'usage ; tests d'échec après première écriture sans ligne résiduelle. |
| **P0-05** | `item_type.quantity_total` est manuel ; `stock_movement.quantity` vaut 1 par défaut et n'est pas exposé ; `warehouse_stock_config` n'est pas utilisé ; `last_inventory_at` n'est jamais écrit. | Stock, alertes et dashboard non fiables ; aucun moyen de réconcilier un comptage. | D-01, P0-04 | Source de vérité unique choisie ; quantités, entrepôts source/destination et acteur enregistrés ; solde calculé ou assumé ; comptage réconcilié ; tests de soldes et d'écarts. |
| **P0-06** | `StockMovementService` met `OUT` à `ASSIGNED` sans affectation et `RETURN` sans clôture ; `ItemAssignmentService.delete` laisse l'article `ASSIGNED` ; `AssignmentService.returnItem` sans `returned_at` est un no-op ; création avec `returned_at` passe quand même à `ASSIGNED`. | L'état de l'article ne reflète pas les affectations ; matériel perdu ou doublement affecté. | D-06, P0-04, P1-05 | Une seule machine à états et un seul contrat d'affectation ; `ASSIGNED` implique exactement une affectation active ; retour, suppression et création historiques cohérents ; tests de cycle complet et de concurrence. |

### 5.2. P1 — majeurs

| ID | Preuve | Impact | Dépendances | Critère de résolution |
|---|---|---|---|---|
| **P1-01** | `ErrorMapper.fromSql` et `GlobalExceptionHandler` renvoient 500 avec SQLSTATE et message PostgreSQL pour doublons, FK et NOT NULL. | Fuite de schéma et classification fausse des erreurs client. | — | 400/409/422 selon le cas ; aucun SQLSTATE en production ; tests doublon, FK, NOT NULL et validation. |
| **P1-02** | `ItemTypeDAOImpl.createItemType` insère explicitement `unit`, `quantity_total` et `minimum_stock` avec des `NULL` ; `EmployeeDAOImpl.create` fait de même pour `status`, ce qui neutralise les `DEFAULT`. | Un formulaire minimal échoue en 500 `23502` ; les tests nominaux masquent le cas. | — | Les colonnes nullables sont omises ou les défauts sont appliqués ; tests de création avec champs facultatifs absents. |
| **P1-03** | `StockMovementService.update/delete` modifie ou supprime le journal sans compensation ; le DTO ne contient ni quantité, ni source/destination, ni opération, ni acteur. | Journal non auditable, stock non reconstituable, historique réécrit. | P0-04, P0-05 | Journal append-only ou compensatoire ; champs quantité/source/destination/opération/acteur ; tests d'immuabilité et de compensation. |
| **P1-04** | `QrScanDAOImpl` exécute `SELECT * FROM security.item WHERE qr_code = ?` sans `company_id`, sans enrichissement et sans table de scan. | Scan inter-entreprises, réponse brute, aucune trace d'usage mobile. | P0-01, P0-02, D-05 | Scan filtré par entreprise, réponse enrichie (type, entrepôt, affectation), journal des scans, limitation de débit ; tests A/B et trace. |
| **P1-05** | `/api/item-assignments` (DTO typé) et `/api/inventory/assignments` (JSON libre) manipulent la même table avec des règles, statuts et acteurs différents. | Contrat ambigu, comportements divergents, frontend fragile. | D-06 | Un seul contrat exposé ou dépréciation explicite de l'ancien ; mêmes invariants et mêmes codes ; tests de parité. |
| **P1-06** | Les trois routes `receipt`, `issue`, `transfer` appellent la même sauvegarde et ne créent qu'un en-tête `inventory_operation` ; aucune ligne, aucun mouvement. | Fausse traçabilité des opérations. | P0-04, P0-05 | Opération avec lignes, mouvements atomiques, type imposé par la route, idempotence ; tests de retour arrière. |
| **P1-07** | Les tables de comptage ne figent pas la quantité attendue ; `close` passe le statut à `COMPLETED` sans `completed_at`, sans réconciliation et sans `last_inventory_at` ; l'écart est fourni par le client. | Le comptage ne prouve ni le stock ni l'écart. | P0-04, P0-05 | Quantité attendue figée à l'ouverture ; écart calculé serveur ; clôture atomique avec timestamps et ajustements ; tests de comptage partiel et complet. |
| **P1-08** | `ItemRestMapper` déclenche des requêtes par article (N+1) ; `LIMIT/OFFSET` sans plafond ; tri par `id`. Le pool est désormais fourni par le Lot 2, mais la chaîne N+1 et la pagination ne sont pas corrigées. | Latence et saturation sur les listes ; pagination instable. | P1-09, P2-02 | Requêtes batch/jointes, plafond `pageSize`, tri stable, métadonnées de pagination ; tests de charge avec 1 000 articles. |
| **P1-09** | **Constat initial :** 25 DAO utilisent `DatabaseConnection` ; aucune référence à `DataSource`/`JdbcTemplate` ; `DriverManager` avec valeurs de repli. | Pool inutilisé, timeouts/TLS Spring ignorés, transactions impossibles. | - | `DatabaseConnection` lit le `DataSource`/pool et passe par `DataSourceUtils` ; plus de valeurs de repli ; test de pool et de rollback. L'injection directe dans chaque DAO reste une dette de conception, non nécessaire pour corriger le contournement. |
| **P1-10** | Les réponses de création de mouvement et d'affectation sont construites à partir de l'entrée : `movement_type`, `movement_date` et `assigned_at` peuvent être `null` alors que la base a appliqué un défaut. | Le frontend doit recharger la ligne ; affichage faux après création. | — | Relecture après insertion ou remplissage avec les valeurs réellement stockées ; tests de réponse de création. |
| **P1-11** | `archived_at`/`archived_by` existent mais ne sont jamais renseignés ; `DELETE` est physique ; les FK `RESTRICT` produisent 500 ; `SET NULL` vide des relations ; DELETE inconnu renvoie 204. | Perte d'historique et d'intégrité ; actions destructrices irréversibles. | D-04 | Politique de suppression/archivage décidée ; 409 sur référence, 404 sur inconnu, restauration si archivage ; tests de suppression et de conservation. |
| **P1-12** | `ItemController.getItemHistory` charge 100 mouvements et 100 affectations globaux, filtre en mémoire, ne vérifie pas l'existence et renvoie 200 ; le test attend 404. | Historique incomplet, contrat faux, test en divergence. | P0-01, P1-08, P1-13 | Filtre SQL par article et entreprise, pagination et tri, 404 sur article inconnu ; test aligné et exécuté. |
| **P1-13** | Tests sur base partagée, `Assumptions` sur données de démonstration, jeton synthétique `ADMIN` pour les tests métier, pas de scénario à deux entreprises ni de concurrence. Après le Lot 3, la suite complète compte 140 tests et 0 échec, dont des tests de solde, d'immuabilité, de rollback d'opération et de clôture de comptage. | La suite ne prouve pas encore la tenancy complète, l'intégrité de tous les cas d'usage ou la concurrence. | P1-09 | Base isolée/Testcontainers, rollback ou fixtures, matrice RBAC/tenancy A/B, tests de concurrence et de transaction ; rapport complet vert sur HEAD. |
| **P1-14** | Aucune colonne de version, aucun `ETag`/`If-Match`, aucune `Idempotency-Key` ; la course sur `uq_item_assignment_active` devient un 500. | Écrasements de mise à jour et doublons en cas de double clic ou de retry. | P0-04 | Verrouillage optimiste, idempotence des créations sensibles, 409 explicite sur conflit ; tests concurrents. |

**Statut après Lot 4 :** `P1-01`, `P1-02`, `P1-09` et `P1-10` sont corrigés et vérifiés par la suite complète. `P1-09` conserve un adaptateur statique centralisé au lieu d'une injection dans chaque DAO ; le contournement de Spring et l'absence de pool sont toutefois corrigés. `P1-03`, `P1-06` et `P1-07` sont corrigés dans le périmètre du Lot 3 et couverts par huit tests dédiés. `P1-05` et `P1-11` sont corrigés dans le périmètre du Lot 4 (contrat unique avec façade dépréciée ; archivage logique, restauration, 404 sur inconnu, 409 sur affectation active). `P1-14` reste ouvert : la colonne `version` existe sur `stock_balance`, mais aucun `ETag`/`If-Match` ni `Idempotency-Key` n'est appliqué aux écritures sensibles ; un conflit d'unicité sur `uq_item_assignment_active` est traduit en 409, sans test de course. `P1-12` reste partiellement ouvert (historique filtré par article, mais existence de l'article non vérifiée). Les autres P1 restent ouverts.

**Statut après Lot 7 :** `P1-12` est corrigé (404 sur article inconnu, pagination et filtres SQL, test dédié). `P2-02` est corrigé pour l'inventaire (`PageResponse` avec total, plafond 500) mais reste partiel pour les autres modules. `P2-03` est corrigé pour le versionnement `/api/v1`, la dépréciation de l'alias, les dates ISO-8601 UTC et la documentation des erreurs ; les libellés d'enums restent incomplets (`API-17`). `P1-08` reste partiel : le plafond `pageSize` et la pagination sont en place, mais le N+1 de `ItemRestMapper` et le tri stable ne sont pas corrigés. `P1-14` reste ouvert : `API-10` (`Idempotency-Key`) et `API-11` (`If-Match`/ETag) attendent une décision de stockage. Les filtres d'écran (`API-15`) restent ouverts ; la spec des soldes est **résolue au Lot 9**.

### 5.3. P2 — dette structurante

| ID | Preuve | Impact | Dépendances | Critère de résolution |
|---|---|---|---|---|
| **P2-01** | `archived_at`/`archived_by` inertes ; aucun endpoint d'archivage ou de restauration. | Impossible de distinguer suppression, archivage et absence. | D-04, P1-11 | Politique d'archivage documentée et endpoint de restauration ; tests de listes filtrées et de restauration. |
| **P2-02** | Listes sans total ni métadonnées ; filtres limités ; `pageSize` non borné ; tri par `id`. | Le frontend filtre côté client et ne peut pas paginer correctement. | P1-08, P1-12 | `PageResponse` avec total/page/pageSize/totalPages, filtres documentés, plafonds et tests de bornes. |
| **P2-03** | `/api/item-assignments` hors `/api/v1`, deux contrats pour la même table, dates `Timestamp` sans fuseau contractuel, enums sans libellés. | Contrat instable et ambigu pour les clients. | D-07 | Stratégie `/api/v1` et dépréciation, dates ISO-8601 UTC, enums documentés ; tests de contrat. |
| **P2-04** | Dashboard global, low-stock basé sur `quantity_total`, pas de stock par entrepôt ni de période. | Indicateurs non représentatifs de l'activité. | P0-05, P1-08 | Dashboard tenant-scoped, stock réel, période et entrepôt ; tests de cohérence avec les soldes. |
| **P2-05** | Seule l'image PNG existe ; aucun PDF, planche, file d'impression, texte lisible ou cache. | Impression de centaines d'étiquettes non opérationnelle. | D-05, P1-04 | Endpoint d'étiquette PDF et planche, cache et tests de génération. |
| **P2-06** | CI exécute tests, lint et packaging mais sans tenancy/RBAC/transaction/concurrence ni couverture ; 183 avertissements OpenAPI non rejoués. | Faux sentiment de qualité ; dette de contrat non mesurée. | P1-13 | Jobs dédiés, seuil de couverture, tests de sécurité et tenancy, lint OpenAPI sans avertissement bloquant, décompte rejoué. |

**Justification des priorités :** les six P0 ont tous un score de risque de 20 à 25 et bloquent la production. Les P1 ont un impact majeur, même lorsque leur score est de 12 (`P1-02`, `P1-08`, `P1-14`) : ils sont reproductibles, touchent le contrat ou empêchent un pilote élargi. Les P2 sont contournables à court terme mais bloquent une exploitation industrielle. `P2-01` est corrigé pour les référentiels et les affectations (archivage logique et routes de restauration) ; la purge physique définitive reste à trancher avec D-04.

---

## 6. Défauts préexistants et risques introduits par les corrections

### 6.1. Défauts préexistants

Les anomalies P0-01 à P0-06 et la majorité des P1/P2 existent dans le code et le schéma antérieurs à cette revue. Elles ne résultent pas des corrections proposées.

Sont également préexistants :

- la divergence `ItemApiTests.historyUnknownReturnsNotFound` (404 attendu) contre `ItemController` (200) ;
- l'absence de rapport complet pour le HEAD ;
- la documentation initiale en partie périmée par le commit `7e3f196`.

Le correctif QR `7e3f196` est déjà appliqué. Il ne constitue pas une anomalie ouverte. La revue a seulement aligné les rapports 02 et 06 sur son comportement et ajouté sa couverture de test à l'inventaire.

### 6.2. Risques pouvant être introduits par les corrections

| Correction | Risque à maîtriser | Garde-fou |
|---|---|---|
| Filtrage tenant dans les DAO | Des lignes existantes ont un `company_id` nul ou incohérent ; des clients envoient encore `company_id` dans le corps. | Audit/backfill des données, rejet des `company_id` clients, tests A/B, migration réversible. |
| Remplacement de `DriverManager` par `DataSource` | Changement des frontières de transaction et de l'auto-commit ; fermeture de connexions ou pooling mal configuré. | Tests d'échec injecté, pool borné, transactions par cas d'usage, surveillance des connexions. |
| Application du RBAC | Les tests et les clients utilisent un jeton synthétique `ADMIN` ; des comptes sans entreprise risquent d'être bloqués. | Données de test dédiées, rôles de transition, messages 401/403 stables. |
| Journal de stock immuable | Des clients utilisent `PATCH`/`DELETE` sur les mouvements ; les données historiques peuvent être incomplètes. | Dépréciation, compensation, migration des données, tests de compatibilité. |
| Réconciliation des comptages | Les soldes `quantity_total` existants ne correspondent pas aux mouvements ; le dashboard change de sens. | Décision D-01, backfill, double contrôle avant/après, tests de non-régression. |
| Machine à états des affectations | Des articles sont déjà `ASSIGNED` sans affectation active ; des affectations historiques ont des dates incohérentes. | Script de diagnostic, correction manuelle encadrée, invariant `ASSIGNED` ⇔ une affectation active. |
| Historique 404 et pagination | Des clients peuvent dépendre du 200 avec listes vides ; le changement de contrat casse le frontend. | Versionnement ou dépréciation, tests de contrat, communication explicite. |
| Gestion locale des erreurs QR | Un `IllegalArgumentException` d'une autre origine pourrait être classé à tort en 400 ou en `QR_CODE_MISSING`. | Exception typée, périmètre du `try` limité au contenu QR, tests des cas voisins. |
| Idempotence et verrouillage optimiste | Des retries légitimes peuvent recevoir 409 ; des conflits non expliqués augmentent le support. | Contrat d'idempotence, codes d'erreur stables, tests de double soumission. |

---

## 7. Preuve d'exécution des tests

### 7.1. Rapport historique

L'audit initial s'appuie sur un rapport complet de **21 classes et 105 tests**, avec **0 échec, 0 erreur, 0 ignoré**. Ce rapport n'est plus présent dans `target/surefire-reports` et ne couvre pas le HEAD `7e3f196`.

### 7.2. État après le Lot 1 (historique)

| Élément | Constat |
|---|---|
| Classes de test dans `src/test` | 23 |
| Tests dans l'arbre | 120 |
| Suite complète exécutée | **OUI**, `.\mvnw.cmd -q test` |
| Résultat | 119 succès, 1 échec, 0 erreur, 0 ignoré |
| Tests ciblés auth/RBAC | 29 succès |
| Échec | `ItemApiTests.historyUnknownReturnsNotFound` : 404 attendu, 200 obtenu |
| Rapport complet pour le HEAD de travail | Oui, `target/surefire-reports` |

### 7.3. État après le Lot 2 (22 octobre 2026)

| Élément | Constat |
|---|---|
| Suites de test dans `src/test` | 27 |
| Tests exécutés | 132 |
| Résultat | **132 succès, 0 échec, 0 erreur, 0 ignoré** |
| Commande | `.\mvnw.cmd -q test` |
| Tests ajoutés au Lot 2 | rollback DAO, rollback service mouvement, pool Hikari, défauts type/employé, mapping SQL |
| Nettoyage | requêtes `phase2-*`/`phase5-*` : 0 ligne résiduelle |

### 7.4. État après le Lot 3 (22 octobre 2026)

| Élément | Constat |
|---|---|
| Suites de test dans `src/test` | 28 |
| Tests exécutés | 140 |
| Résultat | **140 succès, 0 échec, 0 erreur, 0 ignoré** |
| Commande | `.\mvnw.cmd -q test` |
| Tests dédiés | `Lot3StockModelTests` : 8 tests (solde/API, acteur JWT, stock négatif, immuabilité, rollback multi-lignes, comptage/clôture, rollback de clôture, 404) |
| Migration | `19_lot3_stock.sql` appliquée puis rejouée : `INSERT 0 0`, soldes inchangés |
| Nettoyage | `phase2-*`, `phase3-*`, `phase3-lot3-*`, `phase5-*` : 0 ligne résiduelle |
| Cohérence des soldes | `mismatches = 0` après réconciliation ponctuelle des données de démonstration |

### 7.5. État après le Lot 4 (22 octobre 2026)

| Élément | Constat |
|---|---|
| Suites de test dans `src/test` | 28 |
| Tests exécutés | 150 |
| Résultat | **150 succès, 0 échec, 0 erreur, 0 ignoré** |
| Commande | `.\mvnw.cmd -q test` |
| Tests dédiés | `AssignmentApiTests` : 14 ; `ItemAssignmentStateTests` : 5 ; `ItemAssignmentTimestampTests` : 3 |
| Tests adaptés | `ItemApiTests` 16, `ItemCategoryApiTests` 6, `ItemTypeApiTests` 10, `WarehouseApiTests` 5, `OperationApiTests` 4, `StockMovementBusinessRulesTests` 9, `StockMovementTimestampTests` 4, `Lot3StockModelTests` 8 |
| Migration | `20_lot4_assignments.sql` appliquée puis rejouée : colonnes, `CHECK` et index présents, aucune ligne supprimée |
| Base de démonstration | 1 entreprise, 13 articles, 13 mouvements, 6 affectations actives ; 1 affectation active antérieure pointe vers un article `AVAILABLE` (non corrigée) |

### 7.6. Divergence historique retirée

`ItemApiTests.historyUnknownReturnsNotFound` n'existe plus dans l'arbre courant. Le contrôleur d'historique conserve toutefois ses limites fonctionnelles (absence de contrôle d'existence, chargement global et filtrage en mémoire) ; elles relèvent de `P1-12` et ne sont pas corrigées par le Lot 2. L'ancien échec ne doit donc plus être présenté comme un échec courant.

### 7.7. Conséquence pour les statuts `PASS`

Les `PASS` du rapport 15 restent des constats statiques ou historiques, sauf pour les tests d'authentification/RBAC exécutés au Lot 1, les tests de transaction du Lot 2, les tests de stock/opération/comptage du Lot 3, les tests de cycle de vie des affectations exécutés au Lot 4 et les tests de contrat HTTP du Lot 7 (versionnement, Problem Details, pagination, `Location`, dates UTC). Les critères qui dépendent d'un scénario à deux entreprises ou d'un test de concurrence restent `NON VÉRIFIÉ` ou `PARTIEL`.

---

## 8. Ordre de correction recommandé

L'ordre reprend les lots du rapport 14, avec les dépendances vérifiées. Le **Lot 1 a été exécuté partiellement** le 21 octobre 2026. Le **Lot 2 a été exécuté** le 22 octobre 2026 dans son périmètre P1-09, P0-04, P1-01, P1-02 et P1-10. Le **Lot 3 a été exécuté** le 22 octobre 2026 dans son périmètre P0-05, P1-03, P1-06 et P1-07. Le **Lot 4 a été exécuté** le 22 octobre 2026 dans son périmètre P0-06, P1-05 et P1-11 ; le **Lot 5 a été exécuté** le 22 octobre 2026 (P1-04), le **Lot 6** le 23 octobre 2026 (P1-07 complété), le **Lot 7** le 23 octobre 2026 (contrat HTTP), le **Lot 9** le 23 octobre 2026 (documentation de préparation frontend, sans code) et le **Lot 8** le 24 octobre 2026 (tests, CI et performance, sans code applicatif). Le Lot 8 ne débloque pas la production.

| Étape | Lot | Contenu | Dépend de | Critère de sortie |
|---:|---|---|---|---|
| 0 | L0 | Décisions D-01 à D-08, gel du périmètre, base de test isolée, rapport complet de référence sur HEAD. | — | Décisions D-02, D-03 et D-08 prises ; suite complète exécutée sur la base de démonstration ; échec connu documenté. |
| 1 | L1 | Identité, tenancy, RBAC : P0-02, P0-03, P0-01. | L0 | **Exécuté partiellement** : inscription fermée, entreprise dans le jeton, rôles rechargés, RBAC inventaire/administration, révocation testée. Restent les tests A/B, les filtres DAO d'inventaire et le RBAC hors inventaire. |
| 2 | L2 | DataSource, pool, transactions : P1-09, P0-04, P1-01, P1-02, P1-10. | L1 | **Exécuté le 22 octobre 2026** : DAO sur `DataSource`/HikariCP, transactions effectives, erreurs client 400/409/422/503 sans SQLSTATE, créations avec défauts PostgreSQL relus. Suite : 132 tests verts. Restent `P0-01`, la concurrence (`P1-14`) et le RBAC hors inventaire. |
| 3 | L3 | Modèle de stock : P0-05, P1-03, P1-06, P1-07. | L0, L2 | **Exécuté le 22 octobre 2026** : `stock_balance`, migration `19_lot3_stock.sql`, journal append-only, opérations multi-lignes atomiques, comptages avec écart serveur et clôture réconciliée. Suite : 140 tests verts. Restent les affectations (`P0-06`), la tenancy (`P0-01`) et la concurrence (`P1-14`). |
| 4 | L4 | Affectations et suppressions : P0-06, P1-05, P1-11. | L2, L3, D-04, D-06 | **Exécuté le 22 octobre 2026** : contrat canonique `/api/item-assignments` avec façade dépréciée, invariant `ASSIGNED` testée, retour/void/archivage/restauration audités, `OUT`/`RETURN` raccordés au stock, archivage logique des référentiels, migration `20_lot4_assignments.sql`. Suite à cette date : 150 tests verts (suite depuis réduite). Restent la tenancy (`P0-01`), la concurrence (`P1-14`) et la réconciliation de la donnée de démonstration. |
| 5 | L6 | Compléments de comptage : annulation, pagination, journalisation des ajustements. | L3 | **Exécuté le 23 octobre 2026** : instantané figé à l'ouverture (`22_lot6_inventory_counts.sql`), périmètre et cloisonnement par entreprise, clôture idempotente et non vide, annulation, pagination des lignes et mouvements `ADJUSTMENT`. Suite : 49 tests verts. Restent `P0-01` (tenancy générale) et `P1-14` (concurrence). |
| 6 | L5 | QR, scan et étiquettes : P1-04, P2-05. | L1, L3, L4, D-05 | Scan tenant-scoped et enrichi, journal, étiquettes. |
| 7 | L7 | Contrat HTTP : P1-12, P2-02, P2-03, P1-08, P1-14. | L1, L3 | **Exécuté le 23 octobre 2026** : routes `/api/v1` et alias déprécié, Problem Details RFC 9457, `PageResponse` sur l'inventaire, 201 + `Location`, 204, dates ISO-8601 UTC, historique article paginé/filtré et 404, OpenAPI aligné. Suite : 58 tests verts. `API-10`/`API-11` (idempotence, ETag) restent différés ; `P1-08` (N+1) et `API-02` hors inventaire restent partiels. |
| 8 | L8 | Tests, CI, performance : P1-13, P2-06. | L1 à L7 | **Exécuté le 24 octobre 2026** : 74 tests verts sur une base dédiée, fixtures autonomes, concurrence et latence testées, rollback de mouvement vérifié, garde-fou CI sur les tests ignorés. Restent la tenancy A/B (`P0-01`), l'idempotence (`P1-14`), la matrice RBAC et un seuil de couverture. |
| 9 | L9 | Préparation frontend. | L7, L8 | Contrat validé, environnements, stockage du jeton, catalogue d'erreurs, critères du rapport 15. |

### 8.1. Pourquoi cet ordre

1. **Les décisions précèdent le code.** Le modèle de stock, la tenancy, l'inscription, la suppression, le QR et l'affectation changent les tables et les contrats.
2. **L'accès précède les fonctionnalités.** Corriger le stock sans fermer l'accès laisserait une API inter-entreprises.
3. **La transaction précède le stock et les affectations.** Sans transaction réelle, les invariants métier ne peuvent pas être garantis.
4. **Le stock précède les affectations.** `OUT`, `RETURN`, `DESTRUCTION` et les comptages doivent s'appuyer sur le même modèle.
5. **Le contrat et les tests ferment la marche.** Ils vérifient les invariants et préparent le frontend sans le démarrer.

### 8.2. Portes Go/No-Go

- **Prototype interne mono-entreprise :** possible avec un utilisateur de confiance, un stock fiable pour les mouvements/opérations/comptages et un cycle de vie des affectations cohérent, mais sans multi-entreprise et sans promesse d'audit complet.
- **Pilote élargi :** exige les P0 et les P1 de sécurité, tenancy, transaction, stock et affectations.
- **Production :** exige la fermeture des six P0, des P1 applicables au périmètre, les critères du rapport 15 et un rapport de tests complet vert sur une base isolée.
- **Frontend de production :** ne doit pas démarrer avant la stabilisation du contrat, de la tenancy, du RBAC, du stock et des affectations.

---

## 9. Limites de la revue

- **Suite réduite le 22 octobre 2026** : la suite fonctionnelle (150 tests, 28 suites) a été remplacée par 20 tests de conformité schéma/application (4 suites), puis complétée par les contrats QR (Lot 5), comptage (Lot 6) et HTTP (Lot 7) : **58 tests dans 8 suites au 23 octobre 2026**, puis **74 tests dans 13 classes au 24 octobre 2026** après le Lot 8 (affectations, mouvements, concurrence, login et latence, avec fixtures autonomes sur base dédiée). Les conclusions de ce rapport sur la couverture fonctionnelle restent donc partielles. Les tests ont été exécutés sur la base isolée `sssm_ci_lot6` ; aucun reset ni migration destructive n'a été effectué. Les migrations additives `19_lot3_stock.sql`, `20_lot4_assignments.sql`, `21_lot5_qr.sql` et `22_lot6_inventory_counts.sql` ont été appliquées puis rejouées pour vérifier leur idempotence. Les données de test marquées `phase2-*`, `phase3-*`, `phase3-lot3-*`, `phase5-*` et `LOT7-*` ont été supprimées.
- **Donnée de démonstration incohérente non corrigée.** Une affectation active antérieure au Lot 4 (`f7223d51-39cc-4e3e-8736-24d32d3289ad`, article `54488506-9b2d-4430-b126-7b33f7b784c5`) pointe vers un article `AVAILABLE`. Aucune correction automatique n'a été appliquée : la réconciliation est une décision de gestion de données.
- **Pas de test multi-entreprise réel.** La base de démonstration ne contient qu'une entreprise. Le scope entreprise des utilisateurs/rôles est testé, mais l'isolation des DAO d'inventaire n'est pas garantie. Depuis la décision mono-entreprise du 24 octobre 2026, ce point est hors périmètre et `P0-01` est un écart accepté.
- **Pas de test de concurrence.** Les conclusions sur les transactions et les verrous reposent sur le mode de connexion et l'absence de version.
- **OpenAPI rejoué au Lot 9.** Les **28 fichiers** ont été revalidés : 0 erreur de parsing ruamel, 0 `$ref` orphelin, `servers`/`security`/`tags` à la racine ; `scripts/validate_specs.py` rapporte 0 problème sur 14 fichiers / 76 opérations d'inventaire. Le lint Redocly a été rejoué au Lot 9 : **0 erreur, 175 avertissements** (license, localhost, operationId, exemples).
- **Pas de SAST/DAST, de test de pénétration ni d'audit d'infrastructure.**
- **Le Lot 1 a modifié le code, les tests, la configuration et le SQL.** La revue initiale ne modifiait que les Markdown ; le Lot 1 est une passe d'exécution ultérieure, sans commit à ce stade.

---

## 10. Conclusion

La revue confirme le verdict et les priorités. Les rapports 13 à 15 restent des **propositions**, pas des fonctionnalités livrées. Les corrections documentaires lèvent les contradictions de comptage, de routes, d'OpenAPI et de tests, sans masquer les anomalies de code. Le Lot 1 a corrigé l'identité et une partie du RBAC/tenancy ; le Lot 2 a rétabli un pool Spring et des transactions réelles ; le Lot 3 a posé un stock calculé, un journal immuable et des comptages réconciliés ; le Lot 4 a unifié le contrat d'affectation, fiabilisé son cycle de vie et rendu l'archivage logique effectif ; le Lot 6 a figé les attendus de comptage à l'ouverture, cloisonné les comptages par entreprise et journalisé leurs ajustements ; le Lot 7 a stabilisé le contrat HTTP (`/api/v1`, Problem Details, pagination, `Location`, dates UTC, historique 404/paginé). Le Lot 9 fournit le dossier de préparation frontend et comble la spec OpenAPI des soldes, sans changer le verdict : la démonstration mono-entreprise est possible, la production reste bloquée. Le verdict de production reste inchangé.

Le point le plus dangereux n'est plus l'absence de machine à états : c'est la combinaison **tenancy d'inventaire incomplète + RBAC partiel hors inventaire + concurrence/idempotence non maîtrisée + incohérence de données antérieure au Lot 4**. Elle interdit un frontend de production ; l'usage reste mono-entreprise par décision (24 octobre 2026). Depuis le Lot 6, les ajustements de clôture de comptage écrivent un mouvement `ADJUSTMENT` par écart non nul : le journal peut reconstituer les corrections de comptage. Il reste une divergence possible entre le solde (corrigé depuis l'état courant de l'article) et le journal (écart figé à l'ouverture) si l'article bouge pendant le comptage.

**Décision :** les Lots 4, 5, 6, 7 et 9 sont terminés dans leur périmètre documenté (`P0-06`, `P1-05`, `P1-11`, `P1-04`, `P2-05` partiel, `P1-07`, `P1-12`, `P2-02` partiel, `P2-03`, documentation frontend et spec des soldes). Toute modification supplémentaire de code, SQL, test, configuration, dépendance ou OpenAPI doit attendre une validation explicite, en particulier `API-10`/`API-11`. `P0-01` et `P1-14` restent ouverts et ne doivent pas être considérés comme couverts.

Les preuves détaillées restent dans les rapports 00 à 15 ; le registre consolidé est dans [`12-anomalies-risques-priorites.md`](12-anomalies-risques-priorites.md), le plan dans [`14-plan-corrections.md`](14-plan-corrections.md) et les critères dans [`15-criteres-validation-api.md`](15-criteres-validation-api.md).
