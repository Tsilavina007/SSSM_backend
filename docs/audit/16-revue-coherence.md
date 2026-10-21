# 16 — Revue de cohérence et vérification des preuves

**Date :** 21 octobre 2026  
**Objet :** deuxième passe de cohérence sur les rapports 00 à 15, vérification des affirmations contre le code, le schéma PostgreSQL, les scripts SQL, les tests et OpenAPI.  
**HEAD examiné :** `7e3f196` (`fix(inventory): guarantee ItemError envelope on QR code image endpoint`).  
**Audit initial :** commit `a3f7c56` (`docs(audit): add backend readiness and security audit`).  
**Nature :** revue documentaire et statique, sans exécution de la suite complète et sans modification de données lors de la revue initiale.

**Mise à jour du 21 octobre 2026 (Lot 1) :** le code d'authentification, de tenancy et de RBAC a été corrigé, la migration `18_lot1_security.sql` a été appliquée et la suite complète a été exécutée : **120 tests, 119 succès, 1 échec préexistant**. Les sections ci-dessous distinguent l'état de la revue initiale et l'état après Lot 1.

**Verdict :** inchangé — **NON PRÊT POUR LA PRODUCTION**. Le backend reste **PARTIELLEMENT PRÊT** pour un prototype mono-entreprise, en lecture et CRUD encadré, avec un utilisateur de confiance et sans promesse de stock fiable ni de traçabilité complète.

---

## 1. Résultat de la revue

Les conclusions structurantes de l'audit initial sont confirmées :

- aucune isolation multi-entreprise effective ;
- aucun contrôle d'accès métier sur l'inventaire ;
- transactions Spring inopérantes sur les DAO `DriverManager` ;
- stock non calculé et non réconcilié ;
- cycle de vie des affectations incohérent ;
- erreurs de saisie souvent transformées en 500 avec SQLSTATE ;
- QR Code fonctionnel pour un prototype, mais scan global et brut ;
- tests insuffisants pour la tenancy, le RBAC, les transactions, la concurrence et la réconciliation.

**Après le Lot 1 :** l'inscription publique est fermée, le JWT porte l'entreprise et les permissions, le refresh est distinct/rotatif/révocable, le logout est implémenté, le RBAC couvre l'inventaire et l'administration, et les utilisateurs/rôles sont filtrés par entreprise. Les points encore ouverts sont l'isolation complète des DAO d'inventaire, le RBAC des modules hors inventaire, les transactions, le stock, les affectations et la concurrence.

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
| OpenAPI : 26 fichiers, dont 12 d'inventaire | `doc/api/**/*.yaml` contient 26 fichiers ; `doc/api/11_inventory` en contient 12. | Fichiers, vérifié |
| Les 33 endpoints du rapport 13 sont des propositions | Tous les chemins proposés commencent par `/api/v1` ; aucune occurrence de `/api/v1` dans `src/main/java`. | Statique, vérifié |
| Aucune isolation entre entreprises | `ItemDAOImpl.getAllItems` exécute `SELECT * FROM security.item ORDER BY id LIMIT ? OFFSET ?` sans `company_id` ; mêmes absences dans les listes de mouvements, affectations, comptages, opérations, dashboard, rapports et scan. `company_id` est fourni dans les DTO de création. | Code, vérifié |
| Aucun RBAC métier | `SecurityConfig` protège `/api/inventory/**` par `authenticated()` seulement ; `hasRole("ADMIN")` ne couvre que `/api/users/**`, `/api/roles/**` et `/api/permissions/**`. Aucun `@PreAuthorize`, `@Secured` ou `@RolesAllowed` dans l'inventaire. Les tables `permission` et `role_permission` ne sont pas consommées. | Code, vérifié |
| Inscription publique non encadrée | `AuthService.register` met le compte à `ACTIVE`, ne renseigne pas d'entreprise et affecte au mieux le rôle global par défaut. Un compte sans entreprise peut appeler l'inventaire. | Code, vérifié |
| Le refresh conserve les rôles du jeton | `AuthService.refresh` lit `claims.get("roles", List.class)` et recrée un jeton avec cette liste ; il recharge l'utilisateur et vérifie son statut `ACTIVE`, mais ne recharge pas ses rôles en base. | Code, vérifié |
| JWT sans révocation ni contexte tenant | `JwtService.createToken` pose `sub`, `uid`, `roles` et `exp` ; il n'émet ni `jti`, ni `iss`, ni `aud`, ni `company_id`. Aucun denylist ni session serveur. | Code, vérifié |
| Les DAO contournent Spring et le pool | 25 classes de type DAO/répository utilisent `DatabaseConnection.getConnection()` ; aucune ne référence `DataSource` ou `JdbcTemplate`. `DatabaseConnection` ouvre une connexion `DriverManager` avec des valeurs de repli locales. | Code, vérifié |
| Les transactions ne couvrent pas les écritures multi-tables | Les services portent des `@Transactional`, mais les connexions sont ouvertes hors du `DataSource` Spring. Les écritures article + mouvement ou article + affectation ne partagent pas la transaction Spring. | Code, vérifié |
| Stock non fiable | `item_type.quantity_total` est un champ manuel (`NUMERIC(12,2) NOT NULL DEFAULT 0`) ; `stock_movement.quantity` a un défaut à 1 et n'est pas exposé par le DTO ; `warehouse_stock_config` existe mais n'est pas utilisé ; `last_inventory_at` est lu mais jamais écrit. | Schéma + code, vérifié |
| Cycle d'affectation incohérent | `StockMovementService` : `OUT` passe l'article à `ASSIGNED` sans créer d'affectation ; `RETURN` ne clôt aucune affectation. `AssignmentService.returnItem` sans `returned_at` est un no-op ; `ItemAssignmentService.delete` supprime l'affectation sans remettre l'article à `AVAILABLE` ; une création avec `returned_at` déjà renseigné passe quand même l'article à `ASSIGNED`. L'index `uq_item_assignment_active` protège seulement l'unicité active. | Code + schéma, vérifié |
| Historique global et tronqué | `ItemController.getItemHistory` appelle `getStockMovements(1, 100)` et `getItemAssignments(1, 100)` sans filtre SQL, puis filtre en mémoire. Aucun contrôle d'existence : UUID inconnu → **200** avec listes vides. | Code, vérifié |
| Erreurs QR de l'image corrigées | `ItemController.getQrCodeImage` mappe UUID invalide → 400 `BAD_REQUEST`, article inconnu → 404 `NOT_FOUND`, contenu absent → 500 `QR_CODE_MISSING`, échec d'encodage/flux → 500 `QR_CODE_GENERATION_FAILED`, et sérialise les erreurs en JSON même avec `Accept: image/png` strict. | Code + test, vérifié |
| Suite de tests non probante sur HEAD | 22 classes et 112 tests dans l'arbre ; le seul rapport Surefire courant couvre 7 tests QR, tous verts. Aucun rapport complet pour `7e3f196`. `ItemApiTests.historyUnknownReturnsNotFound` attend 404 alors que le contrôleur renvoie 200. | Fichiers + lecture statique |
| CI présente mais incomplète | `.github/workflows/ci.yml` initialise PostgreSQL 16, applique le schéma et les données de démonstration, exécute `./mvnw -B -q test`, un lint Java/OpenAPI et un packaging. Aucun test de tenancy, RBAC, transaction, concurrence ou réconciliation. | CI, vérifié |
| Schéma et triggers | 18 scripts `00_init.sql` à `17_seed_permissions.sql` lors de la revue ; 19 après ajout de `18_lot1_security.sql`. Les triggers `updated_at` couvrent notamment `company`, `branch`, `agency`, `app_user`, `employee`, `client`, `site`, `contract`, `incident`, `invoice` ; `item_category`, `warehouse` et `item_assignment` n'en ont pas. | SQL, vérifié |
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
| C-02 | Les rapports 00, 01, 02 et README annonçaient **14 fichiers OpenAPI d'inventaire** ; `doc/api/11_inventory` en contient **12** (26 au total, dont 14 hors inventaire). | Corrigé dans 00, 01, 02 et README. |
| C-03 | Le rapport 10 annonçait **13 contrôleurs d'inventaire** ; il y en a **12**. Le total de 26 contrôleurs et 130 routes reste correct à l'audit initial. | Corrigé dans 10 ; le rapport 01 est également aligné. Après Lot 1, le total est 131 routes avec `POST /api/auth/logout`. |
| C-04 | Le rapport 06 décrivait un **500 sans corps** pour un QR vide ; le HEAD `7e3f196` renvoie un **500 JSON `QR_CODE_MISSING`**, et un UUID invalide un **400 `BAD_REQUEST`**. | Corrigé dans 06, avec la nouvelle classe de tests. |
| C-05 | Le rapport 02 indiquait seulement `200 PNG / 404` pour l'image QR, sans les 400/500 du correctif. | Corrigé dans 02. |
| C-06 | Le rapport 07 disait qu'un historique inconnu renvoie « probablement 200 » ; 02, 11 et 12 affirmaient 200. Le code courant renvoie toujours 200. | Corrigé : 200 affirmé sans ambiguïté, divergence de test explicitée. |
| C-07 | Le rapport 12 listait 14 P1 dans le registre mais seulement 13 dans la matrice : `P1-02` manquait. | Corrigé : ligne `P1-02` ajoutée (probabilité 4, impact 3, score 12, risque moyen). |
| C-08 | Le rapport 15 comptait 18 critères « Comptages / opérations » alors que `CNT-01..CNT-12` + `OPS-01..OPS-07` = 19 ; le total affiché était 191 au lieu de 192. | Corrigé : ligne à 19 et total explicite de 192 (7 `PASS`, 16 `PARTIEL`, 165 `FAIL`, 4 `NON VÉRIFIÉ`). |
| C-09 | Les rapports 00, 01, 11 et README présentaient 21 classes / 105 tests comme l'état courant. L'arbre HEAD contient 22 classes / 112 tests, et le seul rapport Surefire restant couvre 7 tests QR. | Corrigé : 105/21 étiqueté « rapport complet historique », état HEAD et absence de suite complète précisés. |
| C-10 | Le rapport 06 affirmait qu'aucun test ne couvrait l'image d'un QR vide ; `ItemControllerQrCodeImageTest` couvre désormais ce cas, ainsi que le 400, les 500 et `Accept: image/png`. | Corrigé dans 06. |
| C-11 | Les 183 avertissements Redocly sont cités comme un état courant alors que le décompte n'a pas été rejoué après `7e3f196`. | Signalé comme mesure de l'audit initial, non rejouée. |

Aucune de ces corrections ne modifie le verdict, les priorités P0 ou les conclusions métier.

---

## 5. Tableau final des anomalies

Les colonnes « Dépendances » indiquent les anomalies ou décisions qui doivent être traitées avant ou avec l'anomalie. « Critère de résolution » décrit une preuve vérifiable, pas une intention.

### 5.1. P0 — bloquants

**Statut après Lot 1 :** `P0-03` corrigé ; `P0-02` partiel (inscription fermée et RBAC inventaire/administration, mais modules hors inventaire non couverts) ; `P0-01` partiel (colonnes/backfill/index et scope utilisateurs/rôles, mais DAO d'inventaire non filtrés) ; `P0-04`, `P0-05` et `P0-06` ouverts.

| ID | Preuve | Impact | Dépendances | Critère de résolution |
|---|---|---|---|---|
| **P0-01** | `ItemDAOImpl.getAllItems` et les autres listes n'ont aucun prédicat `company_id` ; `company_id` vient du client ; `SecurityConfig` ne demande qu'une authentification sur `/api/inventory/**`. | Lecture et écriture inter-entreprises ; fuite et corruption de données. | D-02, P0-02 | Toute requête métier est filtrée par une entreprise dérivée du serveur ; tests croisés A/B en lecture et écriture ; 403/404 selon la politique ; aucune donnée d'une autre entreprise retournée. |
| **P0-02** | `SecurityConfig` : seul l'admin est exigé sur users/roles/permissions ; aucun `@PreAuthorize` sur l'inventaire ; `AuthService.register` crée un compte `ACTIVE` sans entreprise ; permissions non consommées. | Tout compte authentifié, y compris public sans entreprise, peut lire ou modifier l'inventaire ; escalade d'administration. | D-03, D-08, P0-01 | Inscription fermée, invitée ou validée ; rôles et permissions appliqués par route et par entreprise ; tests anonyme, `USER`, `ADMIN`, sans entreprise et cross-tenant. |
| **P0-03** | `AuthService.refresh` reprend les rôles du jeton ; `JwtService` n'émet ni `jti`, ni révocation, ni `company_id`. | Un rôle révoqué reste utilisable jusqu'à expiration, et peut être rafraîchi tant que le compte est actif ; pas de logout serveur. | D-03, P0-02 | Rôles rechargés en base au refresh ; version/session ou denylist ; `jti`, `iss`, `aud`, `company_id` ; rotation et révocation testées. |
| **P0-04** | 25 DAO utilisent `DatabaseConnection`/`DriverManager` ; aucun ne passe par le `DataSource` Spring ; les services multi-écritures sont annotés `@Transactional` mais hors transaction Spring. | Écritures partielles : article modifié sans mouvement, affectation créée sans état, opération sans ligne, etc. | P1-09 | DAO sur `DataSource`/pool ; une transaction par cas d'usage ; tests d'échec après première écriture sans ligne résiduelle. |
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
| **P1-08** | `ItemRestMapper` déclenche des requêtes par article (N+1) ; `LIMIT/OFFSET` sans plafond ; tri par `id` ; pas de pool. | Latence et saturation sur les listes ; pagination instable. | P1-09, P2-02 | Requêtes batch/jointes, plafond `pageSize`, tri stable, métadonnées de pagination ; tests de charge avec 1 000 articles. |
| **P1-09** | 25 DAO utilisent `DatabaseConnection` ; aucune référence à `DataSource`/`JdbcTemplate` ; `DriverManager` avec valeurs de repli. | Pool inutilisé, timeouts/TLS Spring ignorés, transactions impossibles. | — | DAO injectés avec `DataSource`/pool ; plus de connexion statique avec repli ; tests de configuration et de fermeture. |
| **P1-10** | Les réponses de création de mouvement et d'affectation sont construites à partir de l'entrée : `movement_type`, `movement_date` et `assigned_at` peuvent être `null` alors que la base a appliqué un défaut. | Le frontend doit recharger la ligne ; affichage faux après création. | — | Relecture après insertion ou remplissage avec les valeurs réellement stockées ; tests de réponse de création. |
| **P1-11** | `archived_at`/`archived_by` existent mais ne sont jamais renseignés ; `DELETE` est physique ; les FK `RESTRICT` produisent 500 ; `SET NULL` vide des relations ; DELETE inconnu renvoie 204. | Perte d'historique et d'intégrité ; actions destructrices irréversibles. | D-04 | Politique de suppression/archivage décidée ; 409 sur référence, 404 sur inconnu, restauration si archivage ; tests de suppression et de conservation. |
| **P1-12** | `ItemController.getItemHistory` charge 100 mouvements et 100 affectations globaux, filtre en mémoire, ne vérifie pas l'existence et renvoie 200 ; le test attend 404. | Historique incomplet, contrat faux, test en divergence. | P0-01, P1-08, P1-13 | Filtre SQL par article et entreprise, pagination et tri, 404 sur article inconnu ; test aligné et exécuté. |
| **P1-13** | Tests sur base partagée, `Assumptions` sur données de démonstration, jeton synthétique `ADMIN` pour les tests métier, pas de scénario à deux entreprises ni de concurrence. Le Lot 1 ajoute login/refresh/logout, RBAC, scope entreprise et révocation, et exécute la suite complète (120 tests, 1 échec préexistant). | La suite ne prouve pas encore la tenancy complète, l'intégrité ou la concurrence. | P1-09 | Base isolée/Testcontainers, rollback ou fixtures, matrice RBAC/tenancy A/B, tests de concurrence et de transaction ; rapport complet vert sur HEAD. |
| **P1-14** | Aucune colonne de version, aucun `ETag`/`If-Match`, aucune `Idempotency-Key` ; la course sur `uq_item_assignment_active` devient un 500. | Écrasements de mise à jour et doublons en cas de double clic ou de retry. | P0-04 | Verrouillage optimiste, idempotence des créations sensibles, 409 explicite sur conflit ; tests concurrents. |

### 5.3. P2 — dette structurante

| ID | Preuve | Impact | Dépendances | Critère de résolution |
|---|---|---|---|---|
| **P2-01** | `archived_at`/`archived_by` inertes ; aucun endpoint d'archivage ou de restauration. | Impossible de distinguer suppression, archivage et absence. | D-04, P1-11 | Politique d'archivage documentée et endpoint de restauration ; tests de listes filtrées et de restauration. |
| **P2-02** | Listes sans total ni métadonnées ; filtres limités ; `pageSize` non borné ; tri par `id`. | Le frontend filtre côté client et ne peut pas paginer correctement. | P1-08, P1-12 | `PageResponse` avec total/page/pageSize/totalPages, filtres documentés, plafonds et tests de bornes. |
| **P2-03** | `/api/item-assignments` hors `/api/v1`, deux contrats pour la même table, dates `Timestamp` sans fuseau contractuel, enums sans libellés. | Contrat instable et ambigu pour les clients. | D-07 | Stratégie `/api/v1` et dépréciation, dates ISO-8601 UTC, enums documentés ; tests de contrat. |
| **P2-04** | Dashboard global, low-stock basé sur `quantity_total`, pas de stock par entrepôt ni de période. | Indicateurs non représentatifs de l'activité. | P0-05, P1-08 | Dashboard tenant-scoped, stock réel, période et entrepôt ; tests de cohérence avec les soldes. |
| **P2-05** | Seule l'image PNG existe ; aucun PDF, planche, file d'impression, texte lisible ou cache. | Impression de centaines d'étiquettes non opérationnelle. | D-05, P1-04 | Endpoint d'étiquette PDF et planche, cache et tests de génération. |
| **P2-06** | CI exécute tests, lint et packaging mais sans tenancy/RBAC/transaction/concurrence ni couverture ; 183 avertissements OpenAPI non rejoués. | Faux sentiment de qualité ; dette de contrat non mesurée. | P1-13 | Jobs dédiés, seuil de couverture, tests de sécurité et tenancy, lint OpenAPI sans avertissement bloquant, décompte rejoué. |

**Justification des priorités :** les six P0 ont tous un score de risque de 20 à 25 et bloquent la production. Les P1 ont un impact majeur, même lorsque leur score est de 12 (`P1-02`, `P1-08`, `P1-14`) : ils sont reproductibles, touchent le contrat ou empêchent un pilote élargi. Les P2 sont contournables à court terme mais bloquent une exploitation industrielle.

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

### 7.2. État après le Lot 1

| Élément | Constat |
|---|---|
| Classes de test dans `src/test` | 23 |
| Tests dans l'arbre | 120 |
| Suite complète exécutée | **OUI**, `.\mvnw.cmd -q test` |
| Résultat | 119 succès, 1 échec, 0 erreur, 0 ignoré |
| Tests ciblés auth/RBAC | 29 succès |
| Échec | `ItemApiTests.historyUnknownReturnsNotFound` : 404 attendu, 200 obtenu |
| Rapport complet pour le HEAD de travail | Oui, `target/surefire-reports` |

### 7.3. Divergence certaine

`ItemApiTests.historyUnknownReturnsNotFound` attend :

```java
get("/api/inventory/items/" + UUID.randomUUID() + "/history")
    .andExpect(status().isNotFound());
```

Or `ItemController.getItemHistory` :

- ne charge pas l'article ;
- charge 100 mouvements et 100 affectations globaux ;
- filtre en mémoire ;
- renvoie toujours 200 avec `movements` et `assignments`.

L'exécution complète du 21 octobre 2026 confirme l'échec avec 200 au lieu de 404. Cette divergence est **préexistante** et hors Lot 1.

### 7.4. Conséquence pour les statuts `PASS`

Les `PASS` du rapport 15 restent des constats statiques ou historiques, sauf pour les tests d'authentification/RBAC exécutés au Lot 1. Les critères qui dépendent d'un scénario à deux entreprises ou d'un test de concurrence restent `NON VÉRIFIÉ` ou `PARTIEL`.

---

## 8. Ordre de correction recommandé

L'ordre reprend les lots du rapport 14, avec les dépendances vérifiées. Le **Lot 1 a été exécuté partiellement** le 21 octobre 2026 ; le Lot 2 ne doit pas démarrer sans validation explicite.

| Étape | Lot | Contenu | Dépend de | Critère de sortie |
|---:|---|---|---|---|
| 0 | L0 | Décisions D-01 à D-08, gel du périmètre, base de test isolée, rapport complet de référence sur HEAD. | — | Décisions D-02, D-03 et D-08 prises ; suite complète exécutée sur la base de démonstration ; échec connu documenté. |
| 1 | L1 | Identité, tenancy, RBAC : P0-02, P0-03, P0-01. | L0 | **Exécuté partiellement** : inscription fermée, entreprise dans le jeton, rôles rechargés, RBAC inventaire/administration, révocation testée. Restent les tests A/B, les filtres DAO d'inventaire et le RBAC hors inventaire. |
| 2 | L2 | DataSource, pool, transactions : P1-09, P0-04, P1-01, P1-02, P1-10. | L1 | DAO sur `DataSource`, transactions effectives, erreurs client sans SQLSTATE, créations avec défauts corrects. |
| 3 | L3 | Modèle de stock : P0-05, P1-03, P1-06, P1-07. | L0, L2 | Source de vérité, quantités et acteurs, journal immuable, opérations et comptages réconciliés. |
| 4 | L4 | Affectations et suppressions : P0-06, P1-05, P1-11. | L2, L3, D-04, D-06 | Un seul contrat, invariant `ASSIGNED`, retours et suppressions cohérents. |
| 5 | L6 | Comptages et réconciliation : P1-07. | L3 | Écarts calculés serveur, clôture atomique, timestamps et ajustements. |
| 6 | L5 | QR, scan et étiquettes : P1-04, P2-05. | L1, L3, L4, D-05 | Scan tenant-scoped et enrichi, journal, étiquettes. |
| 7 | L7 | Contrat HTTP : P1-12, P2-02, P2-03, P1-08, P1-14. | L1, L3 | `/api/v1` ou stratégie de dépréciation, historique filtré/paginé, pagination, erreurs stables, idempotence et concurrence. |
| 8 | L8 | Tests, CI, performance : P1-13, P2-06. | L1 à L7 | Suite isolée verte, tests RBAC/tenancy/transaction/concurrence, couverture et lint OpenAPI. |
| 9 | L9 | Préparation frontend. | L7, L8 | Contrat validé, environnements, stockage du jeton, catalogue d'erreurs, critères du rapport 15. |

### 8.1. Pourquoi cet ordre

1. **Les décisions précèdent le code.** Le modèle de stock, la tenancy, l'inscription, la suppression, le QR et l'affectation changent les tables et les contrats.
2. **L'accès précède les fonctionnalités.** Corriger le stock sans fermer l'accès laisserait une API inter-entreprises.
3. **La transaction précède le stock et les affectations.** Sans transaction réelle, les invariants métier ne peuvent pas être garantis.
4. **Le stock précède les affectations.** `OUT`, `RETURN`, `DESTRUCTION` et les comptages doivent s'appuyer sur le même modèle.
5. **Le contrat et les tests ferment la marche.** Ils vérifient les invariants et préparent le frontend sans le démarrer.

### 8.2. Portes Go/No-Go

- **Prototype interne mono-entreprise :** possible avec un utilisateur de confiance, sans écran de stock fiable, sans multi-entreprise et sans promesse d'audit.
- **Pilote élargi :** exige les P0 et les P1 de sécurité, tenancy, transaction, stock et affectations.
- **Production :** exige la fermeture des six P0, des P1 applicables au périmètre, les critères du rapport 15 et un rapport de tests complet vert sur une base isolée.
- **Frontend de production :** ne doit pas démarrer avant la stabilisation du contrat, de la tenancy, du RBAC, du stock et des affectations.

---

## 9. Limites de la revue

- **Suite complète exécutée après le Lot 1** : 120 tests, 119 succès, 1 échec préexistant (`ItemApiTests.historyUnknownReturnsNotFound`). La base de démonstration a été utilisée ; aucun reset n'a été effectué.
- **Pas de test multi-entreprise réel.** La base de démonstration ne contient qu'une entreprise. Le scope entreprise des utilisateurs/rôles est testé, mais l'isolation des DAO d'inventaire n'est pas garantie.
- **Pas de test de concurrence.** Les conclusions sur les transactions et les verrous reposent sur le mode de connexion et l'absence de version.
- **OpenAPI non rejoué.** Les 183 avertissements sont un constat de l'audit initial ; le décompte courant n'a pas été reproduit.
- **Pas de SAST/DAST, de test de pénétration ni d'audit d'infrastructure.**
- **Le Lot 1 a modifié le code, les tests, la configuration et le SQL.** La revue initiale ne modifiait que les Markdown ; le Lot 1 est une passe d'exécution ultérieure, sans commit à ce stade.

---

## 10. Conclusion

La revue confirme le verdict et les priorités. Les rapports 13 à 15 restent des **propositions**, pas des fonctionnalités livrées. Les corrections documentaires lèvent les contradictions de comptage, de routes, d'OpenAPI et de tests, sans masquer les anomalies de code. Le Lot 1 a corrigé l'identité et une partie du RBAC/tenancy ; il ne change pas le verdict de production.

Le point le plus dangereux n'est pas une erreur de documentation : c'est la combinaison **tenancy d'inventaire incomplète + RBAC partiel + transactions inopérantes + stock déclaratif**. Elle interdit un frontend de production et un usage multi-entreprise.

**Décision :** le Lot 2 et toute modification supplémentaire de code, SQL, test, configuration, dépendance ou OpenAPI doivent attendre une validation explicite. Le Lot 1 est terminé dans son périmètre documenté, avec les réserves du rapport 14.

Les preuves détaillées restent dans les rapports 00 à 15 ; le registre consolidé est dans [`12-anomalies-risques-priorites.md`](12-anomalies-risques-priorites.md), le plan dans [`14-plan-corrections.md`](14-plan-corrections.md) et les critères dans [`15-criteres-validation-api.md`](15-criteres-validation-api.md).
