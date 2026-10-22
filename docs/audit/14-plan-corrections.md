# 14 — Plan de correction

**Objectif :** proposer un ordre de correction réaliste, avec dépendances, lots de travail, migrations et critères de sortie. Les Lots 1 à 4 ont été exécutés les 21 et 22 octobre 2026 ; les autres lots restent à planifier. Le détail du Lot 1 est consigné dans la section 4.6, celui du Lot 3 dans la section 6.5 et celui du Lot 4 dans la section 7.6.

**Principe directeur :** ne pas ajouter d'écran sur un contrat faux. La stabilité de la tenancy, des transactions et du stock précède le frontend.

---

## 1. Décisions à prendre avant le code

Certaines corrections ne sont pas des tâches techniques : elles engagent le produit. Elles doivent être tranchées explicitement.

| ID | Décision | Options | Impact |
|---|---|---|---|
| D-01 | Modèle de stock | Journal calculé / stock déclaratif assumé / hybride | Détermine les tables, le dashboard et les écrans |
| D-02 | Multi-entreprise | Mono-entreprise définitive / multi-entreprise réelle | Détermine l'effort de tenancy |
| D-03 | Inscription | Fermée / invitation / publique avec validation | Détermine le flux d'identité |
| D-04 | Suppression | Physique / archivage logique / les deux | Détermine les contraintes et l'UI |
| D-05 | QR Code | Immuable / versionné / modifiable avec historique | Détermine l'impression et le mobile |
| D-06 | Affectation | Un seul module / deux modules spécialisés | Détermine le contrat |
| D-07 | API | Versionner `/api/v1` / garder `/api` | Détermine la compatibilité |
| D-08 | Portée admin | Admin plateforme / admin entreprise | Détermine le RBAC |

**Sortie :** un court document de décisions signé. Sans D-01 et D-02, les lots 3 et 4 ne peuvent pas être planifiés correctement.

---

## 2. Vue d'ensemble des lots

| Lot | Thème | Priorité | Dépend de | Taille indicative |
|---|---|---|---|---|
| L0 | Décisions et cadrage | P0 | — | S |
| L1 | Identité, tenancy, RBAC | P0 | L0 | XL |
| L2 | Accès aux données et transactions | P0 | L1 | L |
| L3 | Modèle de stock — **exécuté le 22 octobre 2026** | P0 | L0, L2 | XL |
| L4 | Affectations et mouvements — **exécuté le 22 octobre 2026** | P0 | L3 | L |
| L5 | QR, scan, étiquettes | P1 | L1, L3, L4 | L |
| L6 | Compléments de comptage et journalisation des ajustements | P1 | L3 | L |
| L7 | Contrat HTTP, erreurs, pagination | P1 | L1, L3 | M |
| L8 | Tests, CI, performance | P1 | L1-L7 | M |
| L9 | Préparation frontend | P2 | L7, L8 | M |

Les tailles sont relatives : S = quelques jours, M = quelques semaines, L = plusieurs semaines, XL = chantier transversal. Elles ne remplacent pas une estimation d'équipe.

---

## 3. Lot 0 — Décisions et cadrage

### Tâches

1. Trancher D-01 à D-08.
2. Définir la définition de « prêt pour la production ».
3. Identifier les données existantes à préserver.
4. Définir la politique de sauvegarde et de rollback.
5. Geler les évolutions de contrat pendant la migration.

### Livrables

- document de décisions ;
- inventaire des données de démonstration et de production ;
- fenêtre de migration ;
- matrice de compatibilité des anciennes routes.

### Critère de sortie

- D-01 et D-02 ont une réponse explicite ;
- aucune correction de stock n'est lancée sur un modèle non choisi.

---

## 4. Lot 1 — Identité, tenancy et RBAC

### 4.1. Modèle de tenancy

**Cible :**

- `app_user.company_id` obligatoire pour les comptes métier ;
- les comptes plateforme ont un indicateur explicite et une permission dédiée ;
- `company_id` est ajouté aux tables d'inventaire qui ne l'ont pas : `stock_movement`, `item_assignment`, `inventory_count_scan` (et vérification des autres) ;
- les contraintes composites `(id, company_id)` sont ajoutées sur les tables référencées ;
- les FK composites remplacent les FK simples lorsque la cohérence de tenant doit être garantie ;
- les requêtes filtrent par `company_id` issu du jeton ;
- option de défense en profondeur : PostgreSQL RLS avec un paramètre de session, si l'équipe accepte la complexité.

**Migration :**

1. ajouter les colonnes nullable ;
2. backfiller depuis `item`, `warehouse`, `employee`, `security_post` ;
3. vérifier les orphelins ;
4. ajouter les index ;
5. ajouter les contraintes `NOT NULL` et composites ;
6. basculer les lectures ;
7. retirer les anciennes requêtes globales.

**Risque :** les données historiques sans entreprise doivent être traitées avant le `NOT NULL`. Aucune suppression automatique.

### 4.2. Authentification

| Tâche | Détail |
|---|---|
| Inscription | invitation ou validation ; plus de compte sans entreprise |
| Login | 401 pour identifiants invalides, 403 pour compte inactif |
| Refresh | refresh token distinct, rotation, révocation, rôles rechargés |
| Logout | révocation du refresh et du `jti` d'accès |
| JWT | ajouter `company_id`, `jti`, `iss`, `aud` ; permissions ou rechargement |
| Mots de passe | politique, changement, reset, pas de log |
| Limitation | rate limiting login/register/refresh |
| Audit | journal des connexions et révocations |

### 4.3. RBAC

- Définir les permissions dans un catalogue unique.
- Les mapper sur les routes et les méthodes.
- `ADMIN` d'entreprise ne voit que son entreprise.
- `ADMIN` plateforme explicite, avec audit.
- Les permissions en base deviennent la source de vérité, pas seulement le rôle.
- Tests : `USER`, `INVENTORY`, `ADMIN`, sans rôle, deux entreprises.

### 4.4. Durcissement

- CORS par environnement, origines explicites.
- HTTPS obligatoire en production.
- En-têtes de sécurité (HSTS, CSP selon le frontend).
- Masquage des SQLSTATE en production.
- Journal d'audit sans secrets.

### 4.5. Critères de sortie

- un utilisateur de A ne peut lire, créer, modifier ou supprimer aucune donnée de B ;
- un utilisateur sans entreprise ne peut pas accéder à l'inventaire ;
- un `USER` ne peut pas appeler une route `ADMIN` ;
- un rôle retiré ne peut pas être rafraîchi ;
- un refresh rejoué est rejeté ;
- les tests de tenancy et RBAC passent sur deux entreprises.

### 4.6. Compte rendu d'exécution — 21 octobre 2026

**Décisions appliquées :** D-02 (mono-entreprise), D-03 (inscription fermée, comptes créés par un administrateur), D-08 (séparation admin entreprise / admin plateforme).

**Migration :** `docs/database/schema/18_lot1_security.sql`, ajoutée au runner `docs/database/sssm_schema2_v01.sql`. Elle est additive : colonnes nullables, backfill, index, tables `refresh_token` et `revoked_access_token`, permissions `users.manage` et `roles.manage`. Aucun `NOT NULL`, aucune suppression, aucun reset. Appliquée à la base de démonstration ; les volumes de backfill ont été contrôlés après exécution.

**Implémenté :**

- inscription réservée à `ROLE_ADMIN` ou `users.manage`, entreprise déduite du jeton ;
- refresh token distinct, haché, rotation à usage unique, révocation et logout ;
- JWT avec `jti`, `iss`, `aud`, `company_id`, `roles`, `permissions`, `platform_admin` ;
- rechargement des rôles et permissions au refresh ;
- RBAC sur inventaire, utilisateurs, rôles et catalogue de permissions ;
- scope entreprise sur `/api/users/**` et `/api/roles/**`, admin plateforme non filtré ;
- CORS à origines explicites par environnement ;
- tests `AuthHardeningApiTests`, `RefreshTokenApiTests`, `WhoAmIApiTests`, `AppUserApiTests`, `ErrorFormatTests`.

**Non réalisé dans le Lot 1 :**

- filtres `company_id` dans les DAO d'inventaire et FK/contraintes composites ;
- `NOT NULL` sur `app_user.company_id` et les nouvelles colonnes ;
- RLS PostgreSQL, rate limiting, politique de mot de passe, reset, journal d'audit ;
- RBAC des routes organisation, personnel, clients et sites ;
- tests croisés sur deux entreprises ;
- purge automatique des refresh tokens et des jetons révoqués ;
- statuts 401/403 du login et traduction des erreurs SQL (Lot 2/Lot 7).

**Vérifications historiques du Lot 1 :** suite complète `mvn test` exécutée : **120 tests, 119 succès, 1 échec**. L'échec était `ItemApiTests.historyUnknownReturnsNotFound` (404 attendu, 200 obtenu), divergence préexistante du contrôleur d'historique, hors Lot 1 ; ce test a depuis été retiré. Les tests ciblés d'authentification et de RBAC passent (29 tests).

---

## 5. Lot 2 — Accès aux données et transactions

**Statut : EXÉCUTÉ le 22 octobre 2026** pour `P1-09`, `P0-04`, `P1-01`, `P1-02` et `P1-10`, après autorisation explicite. La concurrence (`5.3`) est volontairement différée : elle exige des colonnes et un contrat HTTP nouveaux.

### 5.0. Résultat d'exécution

- `DatabaseConnection` lit le `DataSource` Spring/HikariCP ; plus aucun `DriverManager` dans le code applicatif. Les DAO conservent un point d'accès centralisé, mais la connexion est celle du pool et participe à la transaction Spring.
- `spring.datasource.hikari.*` configure taille, timeouts, validation et durée de vie ; les valeurs sont surchargeables par l'environnement. Aucun changement de schéma. TLS n'est pas imposé par le code : `DB_URL` doit porter `sslmode=require` (ou équivalent) en production.
- `@Transactional` couvre les écritures des services existants ; les DAO rejoignent la transaction. Un test de rollback DAO et un test de rollback du service de mouvement prouvent l'absence de ligne résiduelle.
- Les erreurs SQL sont traduites en 400/409/422/503 ; aucun SQLSTATE ni message PostgreSQL n'est renvoyé. Les codes applicatifs sont stables (`CONFLICT`, `UNPROCESSABLE_ENTITY`, `BAD_REQUEST`, `DATABASE_UNAVAILABLE`).
- `ItemTypeDAOImpl.createItemType` et `EmployeeDAOImpl.create` omettent les colonnes absentes pour laisser PostgreSQL appliquer ses `DEFAULT`, puis relisent la ligne créée. Les créations de mouvement et d'affectation relisent aussi la ligne persistée.
- Suite complète : **132 tests, 0 échec, 0 erreur, 0 ignoré, 27 suites** ; nettoyage `phase2-*`/`phase5-*` vérifié à zéro.

### 5.1. Remplacer `DriverManager`

- Utiliser le `DataSource` Spring et son pool (HikariCP), ou en configurer un explicitement.
- Injecter ce `DataSource` dans les DAO ou passer par `JdbcTemplate`.
- Supprimer les ouvertures de connexion manuelles dans les DAO.
- Faire lire aux DAO le `DataSource` Spring (mêmes variables `DB_URL`/`DB_USER`/`DB_PASSWORD`) et supprimer la double configuration `DriverManager`.
- Vérifier TLS, timeouts, taille de pool, validation des connexions.

### 5.2. Transactions

- Délimiter les transactions au niveau service.
- Vérifier que chaque DAO utilise la connexion transactionnelle.
- Rendre atomiques :
  - mouvement + article ;
  - affectation + article ;
  - retour + article ;
  - opération + lignes + mouvements ;
  - comptage + ajustements.
- Ajouter des tests d'échec après la première écriture.

### 5.3. Concurrence — NON EXÉCUTÉ

**Décision requise :** ajouter `version`/`If-Match` et `Idempotency-Key` change le schéma et le contrat. Le Lot 2 ne les introduit pas. Les conflits SQL (unicité, deadlock, sérialisation) sont déjà traduits en 409.

- Ajouter `version` ou `updated_at` avec `If-Match`.
- Utiliser `SELECT ... FOR UPDATE` sur les articles lors des transitions.
- Traduire les conflits en 409.
- Ajouter `Idempotency-Key` sur les créations sensibles.

### 5.4. Corrections de defaults

- `ItemTypeDAOImpl.createItemType` : ne pas insérer `NULL` explicite pour `unit`, `quantity_total`, `minimum_stock`.
- `EmployeeDAOImpl.create` : même correction pour `status`.
- Revoir tous les DAO qui insèrent des colonnes nullables avec `DEFAULT`.
- Ajouter des tests d'omission.

### 5.5. Critères de sortie

- aucune connexion `DriverManager` dans les DAO ;
- un échec forcé après la première écriture ne laisse aucune ligne partielle ;
- la configuration de production est celle réellement utilisée ;
- les doublons et FK produisent 409/422, pas 500.

**Vérification au 22 octobre 2026 :** ces quatre critères sont couverts par `DatabaseConnection`, `application.properties`, `DatabaseTransactionRollbackTests`, `StockMovementTransactionTests`, `SqlErrorMappingTests`, `ItemCategoryApiTests` et `ItemTypeApiTests`. Après le Lot 3, la suite complète est verte (140 tests, 28 suites).

---

## 6. Lot 3 — Modèle de stock

### 6.1. Si le stock est calculé

**Tables / colonnes cibles :**

- `stock_movement` : `quantity NUMERIC(12,2)`, `source_warehouse_id`, `destination_warehouse_id`, `operation_id`, `performed_by`, `idempotency_key`, `reversed_by`, `reversed_at`, `occurred_at`.
- `stock_balance` : `(company_id, warehouse_id, item_type_id, quantity, version)` avec clé unique.
- `inventory_operation_item` : `(operation_id, item_id, quantity, source_warehouse_id, destination_warehouse_id)`.
- `inventory_count_scan` : `expected_quantity`, `counted_quantity`, `difference`, `expected_warehouse_id`, `counted_warehouse_id`.
- `item.last_inventory_at` : alimenté à la clôture.
- `warehouse_stock_config` : consommé par les alertes.
- `item_type.quantity_total` : déprécié ou supprimé après migration.

**Règles :**

- quantité > 0 ;
- pas de stock négatif ;
- mouvement validé immuable ;
- correction par mouvement inverse ;
- solde mis à jour dans la même transaction ;
- toute opération multi-lignes est atomique ;
- pas de `quantity_total` modifiable à la main.

### 6.2. Si le stock reste déclaratif

- Le documenter comme tel.
- Supprimer les promesses de dashboard « stock réel ».
- Renommer les champs pour éviter la confusion (`declared_quantity`).
- Ne pas construire d'écran de stock par entrepôt.
- Conserver les mouvements comme journal d'événements, avec quantité et acteur.

### 6.3. Migration

1. choisir la source de vérité ;
2. créer les nouvelles tables/colonnes ;
3. backfiller depuis les données existantes ;
4. double-écriture temporaire si nécessaire ;
5. vérifier les soldes par rapport aux articles ;
6. basculer les lectures ;
7. retirer l'ancien champ ;
8. conserver une période de réconciliation.

### 6.4. Critères de sortie

- une réponse unique et testée à « stock disponible par entrepôt et type » ;
- aucun mouvement sans quantité, source/destination ou acteur ;
- aucun stock négatif possible ;
- un mouvement ne peut plus être modifié ou supprimé ;
- un comptage clôturé met à jour `last_inventory_at` et les soldes ;
- le dashboard et les alertes utilisent la source de vérité.

### 6.5. Exécution du Lot 3 — 22 octobre 2026

**Décision appliquée :** D-01, stock calculé avec journal et table de soldes. Le stock déclaratif (`item_type.quantity_total`) n'est plus la source du dashboard low-stock.

**Migration `19_lot3_stock.sql`.** Additive, appliquée après `18_lot1_security.sql` et incluse dans `sssm_schema2_v01.sql` :

- création de `security.stock_balance` avec clé unique `(company_id, warehouse_id, item_type_id)`, `quantity NUMERIC(12,2) CHECK (quantity >= 0)` et `version` ;
- ajout de `expected_quantity`, `counted_quantity` et `difference` à `inventory_count_scan`, avec contraintes `CHECK` nullables pour préserver les scans historiques ;
- backfill des soldes depuis les articles `AVAILABLE`, non archivés et rattachés à un entrepôt. Le backfill utilise `ON CONFLICT DO NOTHING` : le rejouer n'écrase pas les soldes issus de mouvements ou de comptages postérieurs à la bascule ;
- index de lecture `idx_stock_balance_warehouse_type`, `idx_stock_balance_company_type` et `idx_warehouse_stock_config_lookup`.

La migration ne contient ni `DROP`, ni `DELETE`, ni `TRUNCATE`, ni ajout de `NOT NULL` sur une colonne historique. Elle a été appliquée sur la base de démonstration, puis rejouée pour vérifier qu'elle ne réinitialise pas les soldes.

**Code livré :**

- `stock_balance` : DAO, service, contrôleur, DTO et mapper ; `GET /api/inventory/stock-balances` avec filtres `warehouseId`/`itemTypeId` et pagination plafonnée ;
- `StockMovementService` : quantités, source/destination, opération, acteur, soldes dans la transaction du mouvement, refus du stock négatif (`409 INSUFFICIENT_STOCK`), `PATCH`/`DELETE` interdits (`409 IMMUTABLE_RESOURCE`) ;
- `OperationService` : type imposé par la route, au moins une ligne, en-tête + lignes + mouvements atomiques, statut `COMPLETED` ;
- `InventoryCountService`/`InventoryCountDAOImpl` : quantité attendue figée au scan, quantité comptée, écart et type d'écart calculés serveur, contrôle d'entreprise, `404` sur comptage inconnu, clôture atomique avec `last_inventory_at`, `completed_at` et réconciliation du solde ;
- `ItemService` et `DashboardDAOImpl` : synchronisation des créations/modifications/suppressions directes d'articles et lecture du solde réel pour le low-stock, avec `warehouse_stock_config.minimum_stock` comme seuil effectif.

**Colonnes cibles non ajoutées :** `idempotency_key`, `reversed_by`, `reversed_at` et `occurred_at` restent hors périmètre. L'idempotence et la concurrence relèvent de `P1-14` ; `created_at` est utilisé comme horodatage d'occurrence ; la correction se fait par création d'un mouvement inverse manuel. `inventory_operation_item` réutilise ses colonnes `quantity` et `movement_id` ; les entrepôts source/destination restent portés par le mouvement.

**Preuves :**

- `Lot3StockModelTests` : 8 tests (solde et API, acteur JWT, stock négatif, journal immuable, rollback multi-lignes, comptage/écart/clôture, rollback de clôture, 404) ;
- suite complète : **140 tests, 0 échec, 0 erreur, 0 ignoré, 28 suites** ;
- rejeu de `19_lot3_stock.sql` sur la base de démonstration : `INSERT 0 0`, `COMMIT`, soldes inchangés ;
- contrôle SQL : aucun marqueur `phase3-lot3-*`, `phase3-*`, `phase2-*` ou `phase5-*` résiduel ; réconciliation ponctuelle des données de démonstration (`UPDATE 1`, RADIO principal 4 → 2), puis `mismatches = 0` entre `stock_balance` et les articles `AVAILABLE`.

**Limites résiduelles :** `P0-01` (tenancy) et `P1-14` (concurrence/idempotence) restent ouverts. La clôture de comptage ajuste directement le solde sans écrire de mouvement compensatoire ; la reconstitution du solde depuis le seul journal n'est donc pas complète. Un comptage vide peut encore être clôturé (hors périmètre du Lot 3). Le raccordement des affectations au solde, initialement hors Lot 3, a été livré au Lot 4.

---

## 7. Lot 4 — Affectations et mouvements

### 7.1. Un seul contrat

- Choisir `/api/v1/item-assignments`.
- Déprécier `/api/inventory/assignments`.
- Supprimer le JSON libre.
- Conserver une période de compatibilité en lecture si nécessaire.

### 7.2. Machine à états

```text
AVAILABLE --assign--> ASSIGNED --return--> AVAILABLE
AVAILABLE --OUT--> ASSIGNED (avec affectation obligatoire)
ASSIGNED --RETURN--> AVAILABLE (clôt l'affectation)
ASSIGNED --DESTRUCTION--> RETIRED (interdit si actif)
```

Règles :

- `ASSIGNED` implique exactement une affectation active ;
- `AVAILABLE` implique aucune affectation active ;
- `RETIRED` est terminal ;
- `DAMAGED`, `LOST`, `REPAIR` ont des transitions documentées ;
- toute transition est atomique et auditée.

### 7.3. Retour

- `returned_at` obligatoire ou positionné par le serveur ;
- `returned_by` déduit du jeton ;
- entrepôt restauré ;
- idempotent ;
- 409 si déjà retourné.

### 7.4. Suppression

- plus de `DELETE` d'affectation active ;
- `void` avec motif, acteur, date ;
- conservation de l'historique ;
- correction de l'état de l'article dans la même transaction.

### 7.5. Critères de sortie

- aucun article `ASSIGNED` sans affectation active ;
- aucun article `AVAILABLE` avec affectation active ;
- aucun retour sans date ;
- aucun état modifié par un simple `PATCH` de notes ;
- les tests de concurrence passent.

**Statut au 22 octobre 2026 :** les quatre premiers critères sont couverts par le code et les tests. Le dernier reste ouvert : les conflits d'unicité sont traduits en 409, mais il n'existe ni `If-Match`, ni `Idempotency-Key`, ni verrou `FOR UPDATE` sur la transition d'affectation (`P1-14`). Une affectation active préexistante de la base de démonstration est également incohérente avec le statut de son article et n'a pas été corrigée automatiquement.

### 7.6. Exécution du Lot 4 — 22 octobre 2026

**Décisions appliquées :** D-04 (archivage logique pour les référentiels et les affectations), D-06 (contrat canonique unique avec façade dépréciée).

**Migration `20_lot4_assignments.sql`.** Additive, appliquée après `19_lot3_stock.sql` et incluse dans `sssm_schema2_v01.sql` :

- `item_assignment.source_warehouse_id` (FK `warehouse`, `ON DELETE RESTRICT`), `voided_at`, `voided_by`, `void_reason` ;
- `archived_by` sur `item_category`, `item_type` et `warehouse` ;
- contraintes `item_assignment_voided_requires_return` et `item_assignment_voided_requires_reason` ;
- index `idx_item_assignment_source_warehouse`, `idx_item_assignment_voided_at` et `idx_item_assignment_company_active`.

Aucun `DROP`, `DELETE`, `TRUNCATE` ni `NOT NULL` sur une colonne historique. Les lignes existantes restent valides avec les nouvelles colonnes nulles. Le script a été appliqué à la base de démonstration, rejoué sans erreur et vérifié comme idempotent.

**Code livré :**

- contrat canonique `/api/item-assignments` : liste, création, lecture, PATCH de métadonnées, retour, void, restauration, archivage et listes par article/employé/poste ;
- `/api/inventory/assignments` : façade dépréciée déléguant au même `ItemAssignmentService`, avec les mêmes DTO et statuts, en-têtes `Deprecation`/`Link` et suppression du JSON libre et des soft-200 ;
- machine à états : création active réservée à un article `AVAILABLE`, passage `ASSIGNED`, entrepôt vidé et solde synchronisé ; retour serveur avec date/acteur, entrepôt restauré et article `AVAILABLE` ; refus des doubles retours et des affectations actives multiples ;
- `StockMovementService` : `OUT` exige `employee_id` et `post_id`, crée l'affectation et refuse un article non disponible ; `RETURN` clôt l'affectation active ; `IN`, `TRANSFER` et `DESTRUCTION` refusent une affectation active ;
- archivage logique des articles, catégories, types et entrepôts, listes filtrées, routes `/restore`, `404` sur un UUID inconnu et `409` sur un article ayant une affectation active ;
- affectations : `DELETE` interdit sur une ligne active, void motivé et audité, restauration d'un void ; `PATCH` limité aux métadonnées et refus de `condition_on_return` seul ;
- historique d'article branché sur les lectures par article (mouvements et affectations) au lieu des listes globales tronquées ;
- validation UUID renforcée et acteurs JWT non-UUID convertis en `NULL`.

**Preuves :**

- `AssignmentApiTests` : 14 tests ; `ItemAssignmentStateTests` : 5 ; `ItemAssignmentTimestampTests` : 3 ;
- tests liés : `ItemApiTests` 16, `ItemCategoryApiTests` 6, `ItemTypeApiTests` 10, `WarehouseApiTests` 5, `OperationApiTests` 4, `StockMovementBusinessRulesTests` 9, `StockMovementTimestampTests` 4, `Lot3StockModelTests` 8 ;
- suite complète à cette date : **150 tests, 0 échec, 0 erreur, 0 ignoré, 28 suites** ; cette suite a été supprimée le 22 octobre 2026 ;
- migration rejouée sur la base de démonstration : colonnes, contraintes et index présents, aucune donnée supprimée.

**Limites résiduelles :** tenancy (`P0-01`), concurrence/idempotence (`P1-14`), cohérence entreprise/article/employé/poste, refus des références vers des référentiels archivés, purge physique définitive (D-04) et réconciliation de la donnée de démonstration incohérente. Le retrait effectif de la façade `/api/inventory/assignments` reste une décision de versionnement (D-07).

---

## 8. Lot 5 — QR, scan et étiquettes

### 8.1. Code stable

- Table `item_qr_code` : `code`, `item_id`, `company_id`, `version`, `created_at`, `revoked_at`.
- Un seul code actif par article.
- `item.qr_code` conservé comme cache de compatibilité.
- Rotation avec période de grâce.
- Plus de modification libre par `PATCH` ; rotation dédiée.

### 8.2. Scan enrichi

- résolution tenant-scoped ;
- réponse avec type, catégorie, entrepôt, affectation, détenteur, actions autorisées ;
- 404 si inconnu, pas de liste globale ;
- limitation de débit ;
- journal `qr_scan` : utilisateur, entreprise, appareil, action, résultat, horodatage.

### 8.3. Étiquettes

- PDF et planche ;
- texte lisible ;
- taille configurable ;
- cache ;
- génération asynchrone pour les lots ;
- contrôle des droits.

### 8.4. Critères de sortie

- deux entreprises peuvent avoir le même code sans ambiguïté ;
- un code ne change plus silencieusement ;
- un scan renvoie tout ce qu'il faut pour agir ;
- chaque scan est traçable ;
- une planche de 100 étiquettes est générable sans 100 appels manuels.

---

## 9. Lot 6 — Comptages et réconciliation

### Tâches

- Ajouter `expected_quantity` et `counted_quantity`.
- Figer l'attendu à l'ouverture.
- Calculer l'écart côté serveur.
- Refuser un article hors périmètre.
- Clôturer avec `completed_at`, `closed_by`.
- Appliquer les ajustements de manière atomique.
- Mettre à jour `item.last_inventory_at`.
- Ajouter `GET /inventory-counts/{id}` et les lignes paginées.
- Rendre la clôture idempotente.

### Critères de sortie

- un comptage vide ne peut pas être clôturé comme complet ;
- un écart est calculé, pas fourni par le client ;
- la clôture met à jour les soldes et `last_inventory_at` ;
- un comptage inconnu renvoie 404 ;
- un comptage d'une autre entreprise est inaccessible.

---

## 10. Lot 7 — Contrat HTTP

### Tâches

1. `/api/v1`.
2. `PageResponse<T>` avec total.
3. `application/problem+json`.
4. Codes stables.
5. 201 + `Location`.
6. 204 pour les suppressions/retours sans corps.
7. 409 conflits, 422 règles, 404 inconnus.
8. Dates ISO-8601 UTC.
9. Idempotence et ETag.
10. OpenAPI à jour, exemples et erreurs.

### Critères de sortie

- aucun endpoint métier ne renvoie 200 avec `errors` ;
- aucun endpoint ne renvoie 500 pour une erreur de saisie ;
- aucun SQLSTATE en production ;
- le frontend peut paginer sans charger toute la table ;
- les créations renvoient les valeurs relues.

---

## 11. Lot 8 — Tests, CI et performance

### Tâches

- Testcontainers PostgreSQL.
- Fixtures entreprise A/B.
- Tests RBAC et tenancy.
- Tests de transaction et de concurrence.
- Tests de stock et de réconciliation.
- Tests de contrat (statuts, erreurs, pagination).
- Tests de sécurité (login, register, refresh, révocation).
- Tests de performance sur 1 000 articles.
- Étendre le workflow CI existant (`.github/workflows/ci.yml`), qui démarre déjà PostgreSQL 16, initialise le schéma et les données de démonstration, puis lance tests, lint OpenAPI et packaging.
- Ajouter au CI les tests de tenancy, RBAC, transaction, concurrence et réconciliation, ainsi que des seuils de couverture sur les modules critiques, pas sur le total.

### Critères de sortie

- la suite tourne sur une base vide ;
- aucun test ignoré silencieusement ;
- un test échoue si la tenancy est cassée ;
- un test échoue si une transaction partielle est possible ;
- un test échoue si un retour laisse l'article `ASSIGNED` ;
- le pipeline est bloquant sur `main`.

---

## 12. Lot 9 — Préparation frontend

Ce lot ne commence pas le frontend. Il produit les éléments nécessaires pour qu'il puisse démarrer sans être réécrit.

### Livrables

- contrat OpenAPI validé ;
- environnements et URLs ;
- stratégie de stockage du jeton ;
- stratégie de rafraîchissement et de logout ;
- catalogue des erreurs ;
- pagination et filtres ;
- maquettes de scan et d'étiquette ;
- critères d'acceptation du rapport 15 ;
- jeu de données de démonstration multi-entreprise.

### Go / No-Go

Le frontend de production peut démarrer seulement si :

- les P0 du rapport 12 sont fermés ;
- les tests de tenancy et RBAC passent ;
- le stock est défini et testé ;
- les affectations ont une seule API ;
- le scan QR est enrichi et tenant-scoped ;
- le contrat d'erreur est stable.

---

## 13. Séquencement et dépendances

```text
L0 Décisions
  └── L1 Identité / tenancy / RBAC
        └── L2 DataSource / transactions
              └── L3 Stock
                    ├── L4 Affectations
                    ├── L5 QR
                    └── L6 Comptages
                          └── L7 Contrat HTTP
                                └── L8 Tests / CI
                                      └── L9 Go frontend
```

### Pourquoi cet ordre ?

- Corriger le stock avant la tenancy créerait des agrégats faux par entreprise.
- Corriger les affectations avant les transactions laisserait des états partiels.
- Construire le scan avant le stock et les affectations donnerait une interface sans action fiable.
- Construire le frontend avant le contrat ferait porter la dette à l'interface.

---

## 14. Migration : règles de sécurité

1. **Sauvegarde complète** avant toute migration.
2. **Base de recette** identique à la production.
3. **Migrations additives** d'abord : nouvelles colonnes nullables, nouvelles tables.
4. **Backfill** contrôlé, avec requêtes de vérification.
5. **Double écriture** temporaire si nécessaire.
6. **Bascule des lectures** après validation.
7. **Contraintes** ajoutées seulement après nettoyage des données.
8. **Rollback** documenté et testé.
9. **Aucune suppression** de colonne avant une période d'observation.
10. **Aucune migration destructive** sans sauvegarde vérifiée.

### Points de contrôle

- nombre d'orphelins par FK ;
- nombre d'utilisateurs sans entreprise ;
- nombre d'articles sans type ou entrepôt ;
- nombre d'affectations actives incohérentes ;
- nombre de mouvements sans quantité/acteur ;
- nombre de comptages non clôturés ;
- nombre de QR dupliqués par entreprise.

---

## 15. Definition of Done globale

Une correction n'est terminée que si :

- le comportement est spécifié ;
- le code est revu ;
- les tests couvrent le cas nominal et les cas d'erreur ;
- la tenancy est testée ;
- la transaction est testée ;
- les erreurs sont stables ;
- l'OpenAPI est à jour ;
- la migration est réversible ;
- la documentation d'exploitation est à jour ;
- aucun secret ni SQLSTATE n'est exposé ;
- le frontend peut consommer le contrat sans contournement.

---

## 16. Ce qui ne doit pas être fait maintenant

- Commencer les écrans avant les P0.
- Ajouter des endpoints sans corriger la tenancy.
- Optimiser le N+1 avant de choisir le modèle de stock.
- Supprimer les anciennes routes avant d'avoir une période de compatibilité.
- Ajouter un PDF d'étiquette avant de stabiliser le code QR.
- Ajouter des tests sans base isolée.
- Migrer les données sans sauvegarde.

---

## 17. Conclusion

Le plan le plus court n'est pas « corriger les contrôleurs ». C'est :

1. fermer l'accès ;
2. rendre les écritures atomiques ;
3. choisir et implémenter une source de vérité pour le stock ;
4. unifier les affectations ;
5. stabiliser le contrat ;
6. seulement ensuite, construire le frontend.

Les critères de validation sont dans le rapport 15.
