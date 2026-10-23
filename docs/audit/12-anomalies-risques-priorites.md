# 12 — Anomalies, risques et priorités

**Objectif :** centraliser les constats dans un registre unique, les qualifier par gravité et fournir l'ordre d'attaque du plan de correction. Les preuves détaillées restent dans les rapports 01 à 11 ; ce document ne les remplace pas.

**Portée :** audit statique initial, puis exécution des Lots 1 à 4 (identité, tenancy mono-entreprise, RBAC, transactions, stock calculé, cycle de vie des affectations et archivage logique). Les constats restent la photographie de l'audit initial ; chaque P0/P1 concerné porte désormais un statut d'exécution daté.

---

## 1. Méthode de priorisation

### 1.1. Niveaux

| Niveau | Définition | Conséquence |
|---|---|---|
| **P0** | Bloquant pour une mise en production, sécurité ou intégrité des données | Ne pas ouvrir le frontend de production |
| **P1** | Fonctionnalité majeure incomplète, contrat instable ou risque d'exploitation | À corriger avant pilote élargi |
| **P2** | Dette structurante, mais contournable à court terme | À planifier après les P0/P1 |
| **P3** | Confort, cohérence, documentation | Peut attendre |

### 1.2. Cotation des risques

- **Probabilité** : 5 certain, 4 probable, 3 possible, 2 improbable, 1 rare.
- **Impact** : 5 critique, 4 majeur, 3 modéré, 2 mineur, 1 négligeable.
- **Score** = probabilité × impact.

La cotation est qualitative. Elle sert à ordonner les travaux, pas à produire une mesure actuarielle.

---

## 2. Registre P0 — bloquants

### P0-01 — Absence d'isolation multi-entreprise

**Constat :** toutes les requêtes d'inventaire sont globales. Aucune ne filtre `company_id`. Le `company_id` des créations et modifications vient du client.

**Preuves :**

- `ItemDAOImpl.getAllItems` : `SELECT * FROM security.item ORDER BY id`.
- `ItemTypeDAOImpl`, `ItemCategoryDAOImpl`, `WarehouseDAOImpl` : mêmes listes globales.
- `StockMovementDAOImpl`, `ItemAssignmentDAOImpl`, `InventoryCountDAOImpl`, `ReportDAOImpl`, `DashboardDAOImpl` : aucun prédicat d'entreprise.
- `SecurityConfig` : `/api/inventory/**` exige seulement `authenticated()`.
- Aucune FK composite `(id, company_id)` dans le schéma.

**Impact :** un utilisateur authentifié peut lire, modifier et supprimer les données de toutes les entreprises. C'est une faille de confidentialité et d'intégrité.

**Probabilité :** 5 — certain dès qu'une deuxième entreprise existe.
**Impact :** 5 — fuite et corruption de données.
**Score :** 25.

**Références :** rapports 02, 03, 04, 09.

**Statut au 21 octobre 2026 (Lot 1) : PARTIEL.** La décision D-02 retient le mono-entreprise pour le runtime. La migration `18_lot1_security.sql` ajoute `company_id` à `stock_movement`, `item_assignment` et `inventory_count_scan`, backfille les lignes existantes et ajoute les index. `app_user.company_id` est backfillé pour les comptes non-plateforme de l'unique entreprise. En revanche, les DAO d'inventaire ne filtrent pas encore par `company_id`, les FK/contraintes composites et le `NOT NULL` ne sont pas ajoutés, et aucune isolation multi-entreprise n'est garantie. Le risque est neutralisé uniquement tant que la base ne contient qu'une entreprise.

---

### P0-02 — RBAC absent et inscription publique

**Constat :** les permissions en base ne sont consommées par aucun contrôle. Seuls `/api/users/**`, `/api/roles/**` et `/api/permissions/**` exigent `ADMIN`. L'inventaire, le personnel, les clients et les sites exigent seulement un JWT.

`POST /api/auth/register` est public et crée un utilisateur :

- sans entreprise ;
- `ACTIVE` ;
- avec le rôle global `USER` s'il existe ;
- sans jeton.

Si le rôle `USER` n'existe pas, l'utilisateur n'a aucun rôle mais reste authentifié : le filtre crée une authentification avec zéro autorité, et les routes `authenticated()` restent accessibles.

**Impact :** toute personne peut s'inscrire et accéder à l'inventaire global. Un compte sans rôle n'est pas bloqué.

**Probabilité :** 5.
**Impact :** 5.
**Score :** 25.

**Références :** rapports 09 et 11.

**Statut au 21 octobre 2026 (Lot 1) : PARTIEL.** `POST /api/auth/register` n'est plus public : il exige `ROLE_ADMIN` ou `users.manage`, et un administrateur d'entreprise ne peut créer que dans sa propre entreprise. Les routes `/api/inventory/**`, `/api/users/**` et `/api/roles/**` consomment désormais les permissions (`inventory.manage`, `users.manage`, `roles.manage`) ou les rôles correspondants ; `/api/permissions/**` reste réservé à l'admin plateforme. Les utilisateurs et rôles sont filtrés par entreprise. Restent hors Lot 1 : le RBAC des modules organisation, personnel, clients et sites, le rate limiting, la vérification d'email et l'alignement des statuts 400/401/403 du login.

---

### P0-03 — JWT sans révocation et refresh avec rôles figés

**Constat :**

- le JWT contient `roles` mais pas de `jti`, `iss`, `aud` ni `company_id` ;
- aucun logout JWT avec révocation, aucune liste de révocation, aucune rotation ;
- `/api/auth/refresh` recharge l'utilisateur et son statut, mais **réutilise les rôles de l'ancien jeton** ;
- un `ADMIN` dont le rôle est retiré peut donc rafraîchir et conserver `ADMIN` tant qu'il rafraîchit avant expiration, que le compte existe et reste `ACTIVE`.

**Impact :** une révocation d'accès ne prend effet qu'à l'expiration du jeton, et peut être contournée par refresh. Élévation de privilèges persistante.

**Probabilité :** 5 pour un utilisateur révoqué qui connaît `/refresh`.
**Impact :** 5.
**Score :** 25.

**Références :** rapport 09.

**Statut au 21 octobre 2026 (Lot 1) : CORRIGÉ.** Le JWT porte désormais `jti`, `iss`, `aud`, `company_id`, `roles`, `permissions` et `platform_admin`. Le refresh token est distinct de l'access token, stocké haché dans `security.refresh_token`, à usage unique et rotation à chaque refresh. Les rôles et permissions sont rechargés en base au refresh. `POST /api/auth/logout` révoque le refresh token et le `jti` de l'access token ; le filtre refuse tout `jti` présent dans `security.revoked_access_token`. Les jetons émis avant ce changement sont invalidés par l'exigence de `jti`, `iss` et `aud`.

---

### P0-04 — Transactions inopérantes et écritures non atomiques

**Statut au 22 octobre 2026 (Lot 2) : CORRIGÉ pour les cas d'usage existants.** `DatabaseConnection` fournit une connexion issue du `DataSource` Spring/HikariCP et transactionnelle. Les services annotés `@Transactional` couvrent désormais réellement les DAO ; un rollback DAO et un rollback du service mouvement sont testés. L'atomicité des opérations multi-lignes reste dépendante de `P1-06`, qui n'implémente pas encore les lignes d'opération.

**Constat initial :** les services sont annotés `@Transactional`, mais les DAO ouvrent leurs connexions via `DatabaseConnection.getConnection()` et `DriverManager`. Ils ne participent pas à la transaction Spring. Il n'y a pas de pool partagé par les DAO.

Les opérations suivantes écrivent sur plusieurs lignes sans atomicité réelle :

- création d'un mouvement + mise à jour de l'article ;
- création d'une affectation + passage de l'article à `ASSIGNED` ;
- restitution + passage à `AVAILABLE` ;
- clôture ou scan d'un comptage ;
- création d'une opération et, à terme, de ses lignes.

**Impact :** un échec partiel laisse des états incohérents : mouvement sans effet, affectation sans statut, article `ASSIGNED` sans ligne, etc. Le rollback annoncé par les annotations est faux.

**Probabilité :** 4 — dépend des erreurs, mais toute erreur après la première écriture suffit.
**Impact :** 5.
**Score :** 20.

**Références :** rapports 03, 05, 07, 08, 11.

---

### P0-05 — Stock non calculé et non réconcilié

**Statut au 22 octobre 2026 (Lot 3) : CORRIGÉ DANS LE PÉRIMÈTRE DU LOT 3.** La migration additive `19_lot3_stock.sql` crée `security.stock_balance` (clé unique entreprise/entrepôt/type, quantité et version), ajoute les quantités de comptage et backfille les soldes depuis les articles disponibles. `GET /api/inventory/stock-balances` expose le solde réel. Les mouvements `IN`, `OUT`, `TRANSFER`, `RETURN`, `DESTRUCTION` et `ADJUSTMENT` mettent à jour ce solde dans la même transaction ; un solde négatif est refusé en `409 INSUFFICIENT_STOCK`. Le dashboard low-stock lit désormais `stock_balance` et le seuil effectif `warehouse_stock_config`/`item_type.minimum_stock`. La clôture d'un comptage réconcilie l'article, son emplacement et le solde, et écrit `last_inventory_at`. **Limites résiduelles :** les lignes de mouvement antérieures au Lot 3 n'ont pas de source/destination/acteur ; `item_type.quantity_total` existe encore et reste modifiable mais n'est plus le critère low-stock ; les ajustements de clôture ne créent pas de mouvement compensatoire dans le journal. Depuis le Lot 4, les affectations synchronisent aussi `stock_balance` à la création, au retour et au void actif.

**Constat initial :**

- `item_type.quantity_total` est un champ manuel, modifiable par `PATCH`, jamais mis à jour par un mouvement, une affectation, une destruction ou un comptage.
- `stock_movement.quantity` vaut toujours `1` : le DTO ne l'expose pas.
- `source_warehouse_id`, `destination_warehouse_id`, `operation_id` et `performed_by` ne sont jamais alimentés par l'API.
- `warehouse_stock_config` n'est lu par aucun code.
- `inventory_count_scan` ne contient ni quantité attendue ni quantité comptée.
- `item.last_inventory_at` n'est jamais écrit.

**Impact :** le backend ne peut pas répondre à « combien d'unités sont disponibles dans cet entrepôt ? », « quel est l'écart du comptage ? », « quelle est la valeur du stock ? ». Le dashboard et les alertes low-stock reposent sur un champ déclaratif.

**Probabilité :** 5 — le comportement est certain.
**Impact :** 5 — le stock est le cœur du domaine.
**Score :** 25.

**Références :** rapports 03, 05, 07.

---

### P0-06 — Cycle de vie des affectations incohérent

**Constat :** le statut `ASSIGNED` a plusieurs sources qui ne se synchronisent pas.

| Chemin | Effet | Ligne d'affectation |
|---|---|---|
| `POST /api/item-assignments` | article `ASSIGNED` | créée |
| `POST /api/inventory/assignments` | article `ASSIGNED` | créée, JSON libre |
| `POST /movements` type `OUT` | article `ASSIGNED` | **aucune** |
| `POST /movements` type `RETURN` | article `AVAILABLE` | **non clôturée** |
| `PATCH /item-assignments/{id}` sans `returned_at` | article `ASSIGNED` | toujours active |
| `POST /inventory/assignments/{id}/return` sans `returned_at` | article `ASSIGNED` | toujours active, réponse 200 |
| `DELETE /item-assignments/{id}` | article reste `ASSIGNED` | supprimée |
| `POST /item-assignments` avec `returned_at` | article `ASSIGNED` | déjà retournée |

**Impact :** un article peut être `ASSIGNED` sans détenteur, `AVAILABLE` avec une affectation active, ou historique et marqué affecté. Le mobile et le web ne peuvent pas afficher un état fiable.

**Probabilité :** 5.
**Impact :** 5.
**Score :** 25.

**Références :** rapports 07, 08.

**Statut au 22 octobre 2026 (Lot 4) : CORRIGÉ DANS LE PÉRIMÈTRE DU LOT 4.** `/api/item-assignments` devient le contrat canonique et `/api/inventory/assignments` une façade dépréciée déléguant au même service, avec les mêmes validations, DTO typés et codes HTTP. Une affectation active exige un article `AVAILABLE` sans affectation active, passe l'article `ASSIGNED`, vide son emplacement et synchronise le solde dans la même transaction. `POST /{id}/return` clôt l'affectation et restaure l'article ; un mouvement `OUT` crée l'affectation et un `RETURN` la clôture ; `IN`, `TRANSFER` et `DESTRUCTION` refusent une affectation active. La migration additive `20_lot4_assignments.sql` ajoute `source_warehouse_id`, `voided_at`, `voided_by` et `void_reason` ainsi que les index/contraintes associés. Le void, l'archivage et la restauration sont audités ; une suppression active renvoie 409. **Limites résiduelles :** les contrôles de cohérence entreprise/article/employé/poste restent absents (`P0-01`), et la concurrence n'est pas idempotente (`P1-14`). Une affectation active préexistante de la base de démonstration pointe encore vers un article `AVAILABLE` ; elle n'a pas été corrigée automatiquement.

---

## 3. Registre P1 — majeurs

### P1-01 — Erreurs client renvoyées en 500 avec détails SQL

**Statut au 22 octobre 2026 (Lot 2) : CORRIGÉ.** Les erreurs SQL sont traduites en 400 (NOT NULL, format), 409 (unicité, conflit de transaction), 422 (FK, contrainte `CHECK`) ou 503 (base indisponible). Le corps ne contient plus ni SQLSTATE, ni message PostgreSQL, ni nom de contrainte.

**Constat initial :** doublons, FK, NOT NULL et contraintes produisent `PSQLException`, converties en **500**. Le corps contient le message PostgreSQL, le détail et le `SQLState`.

**Impact :** le frontend ne peut pas distinguer 400/409/422/500 ; la structure de la base est exposée ; l'utilisateur voit une erreur technique.

**Exemples :** doublon de QR Code, code de type, nom de catégorie, `operation_type` manquant, `unit`/`quantity_total`/`minimum_stock` manquants, `status` d'employé manquant, FK inconnue.

**Références :** rapports 02, 04, 05, 09, 10.

---

### P1-02 — Création de type et d'employé fragile

**Statut au 22 octobre 2026 (Lot 2) : CORRIGÉ.** Les colonnes absentes sont omises de l'`INSERT`; PostgreSQL applique `unit = 'piece'`, les quantités à 0 et `status = 'ACTIVE'` pour l'employé. La ligne persistée est relue avant la réponse. Des tests couvrent les formulaires minimaux.

**Constat initial :** `ItemTypeDAOImpl.createItemType` insère explicitement `unit`, `quantity_total` et `minimum_stock` même s'ils sont nuls. `EmployeeDAOImpl.create` fait de même pour `status`. Les `DEFAULT` SQL ne s'appliquent pas à un `NULL` explicite.

**Impact :** un formulaire minimal échoue en 500 `23502`. Les tests nominaux fournissent les valeurs et masquent le cas.

**Références :** rapports 04 et 08.

---

### P1-03 — Journal de mouvements mutable et incomplet

**Statut au 22 octobre 2026 (Lot 3) : CORRIGÉ DANS LE PÉRIMÈTRE DU LOT 3.** `PATCH` et `DELETE` sur un mouvement renvoient `409 IMMUTABLE_RESOURCE` après vérification de l'existence ; aucune ligne n'est modifiée ou supprimée. Les réponses exposent `quantity`, `source_warehouse_id`, `destination_warehouse_id`, `operation_id`, `performed_by` et `created_at`. L'acteur est résolu depuis le JWT lorsqu'il porte un UUID, sinon depuis l'UUID fourni ; à défaut il reste nul. Les mouvements créés par une opération portent `operation_id`. **Limites résiduelles :** les mouvements historiques restent incomplets ; l'acteur peut être nul pour les appels sans JWT UUID ni UUID explicite ; aucune compensation automatique n'est exposée, seule la création d'un mouvement inverse manuel est possible.

**Constat initial :**

- `PATCH` et `DELETE` sont exposés sans compensation ni contrôle d'état ;
- la réponse standard n'expose ni quantité, ni source/destination, ni opération, ni acteur ;
- la quantité est toujours 1 ;
- les colonnes d'audit restent nulles ;
- un `OUT` crée un état `ASSIGNED` sans affectation ;
- un `RETURN` ne clôt aucune affectation ;
- un `ADJUSTMENT` n'a aucun effet ;
- la réponse de création peut renvoyer `movement_type: null` et `movement_date: null` alors que la base a appliqué les défauts.

**Impact :** le journal n'a pas de valeur d'audit et ne permet pas de reconstituer un stock ou un transfert.

**Références :** rapports 05, 07.

---

### P1-04 — Scan QR global, brut et sans journal

**Constat :** `POST /api/inventory/scans/resolve` interroge `item` par `qr_code` sans filtre d'entreprise. La réponse contient la ligne brute, sans type, catégorie, entrepôt lisible, affectation ou détenteur. Le code est modifiable par `PATCH`. Aucun scan n'est journalisé. Aucune étiquette PDF n'existe.

**Impact :** le mobile ne peut pas construire un workflow fiable à partir du scan ; les codes dupliqués entre entreprises sont ambigus ; les étiquettes peuvent être invalidées.

**Références :** rapports 06 et 10.

**Statut au 22 octobre 2026 (Lot 5) : CORRIGÉ DANS LE PÉRIMÈTRE DU LOT 5.** La résolution est scoped à l'entreprise de l'appelant (`company_id` du JWT) : un code d'une autre entreprise est traité comme inconnu (404). La réponse est enrichie (type, catégorie, entrepôt, affectation active, détenteur, poste) et ne contient plus de champ technique interne. Un code inconnu renvoie 404 au lieu de 200 `[]`. Chaque tentative alimente la table `qr_scan` (acteur, horodatage, terminal, résultat `RESOLVED`/`NOT_FOUND`). Une limitation de débit en mémoire protège l'énumération (429 `RATE_LIMITED`). Le code QR est rendu immuable (`PATCH` refusé en 400 `IMMUTABLE_QR_CODE`, décision D-05) et l'image PNG est mise en cache avec `ETag`. Une planche d'étiquettes HTML est disponible, sans dépendance PDF. **Limites résiduelles :** le journal n'est pas purgé (pas de politique de rétention), le limiteur n'est pas partagé entre instances, et la résolution reste mono-entreprise (le contrôle `company_id` des autres modules n'est pas généralisé, `P0-01`).

---

### P1-05 — Deux modules d'affectation divergents

**Constat :** `/api/item-assignments` et `/api/inventory/assignments` manipulent la même table avec des contrats différents :

- validation Jakarta d'un côté, JSON libre de l'autre ;
- 201 contre 200 ;
- 404 contre 200 avec `errors` ;
- champs d'audit dans un seul module ;
- retour no-op dans le module générique ;
- suppression exposée dans un seul module.

**Impact :** le frontend doit choisir un contrat incomplet ; les données peuvent être modifiées différemment selon la route.

**Références :** rapport 08.

**Statut au 22 octobre 2026 (Lot 4) : CORRIGÉ.** Le contrat canonique est `/api/item-assignments`. `/api/inventory/assignments` reste une façade dépréciée qui délègue au même `ItemAssignmentService`, utilise les mêmes DTO et renvoie les mêmes statuts (201, 404, 409). Les réponses de succès portent `Deprecation: true` et `Link` vers le successeur ; il n'y a plus de JSON libre ni de soft-200. Les deux préfixes restent exposés jusqu'au retrait planifié de la façade.

---

### P1-06 — Opérations d'inventaire vides

**Statut au 22 octobre 2026 (Lot 3) : CORRIGÉ DANS LE PÉRIMÈTRE DU LOT 3.** `/receipt`, `/issue` et `/transfer` déterminent le type par la route, refusent un corps sans ligne, créent l'en-tête, les lignes `inventory_operation_item` et les mouvements dans une seule transaction, et renvoient `201` avec les lignes et le statut `COMPLETED`. Un test vérifie qu'une erreur sur la deuxième ligne annule l'en-tête, les lignes, le mouvement et le solde de la première. **Limite résiduelle :** l'idempotence de création (`Idempotency-Key`) reste ouverte (`P1-14`).

**Constat initial :** `/receipt`, `/issue` et `/transfer` appellent la même méthode. Elles insèrent un en-tête `inventory_operation` avec les colonnes fournies, sans ligne d'article, sans mouvement, sans modification. Le type n'est pas déduit de la route. S'il est absent, `23502` → 500.

**Impact :** une opération ne prouve aucune réception, sortie ou transfert. Le dashboard compte des en-têtes vides.

**Références :** rapport 05.

---

### P1-07 — Comptages sans réconciliation

**Statut au 22 octobre 2026 (Lot 3) : CORRIGÉ DANS LE PÉRIMÈTRE DU LOT 3.** Le scan fige `expected_quantity` à partir de l'état de l'article et de l'entrepôt du comptage, enregistre `counted_quantity`, calcule `difference` et `discrepancy_type` côté serveur, et refuse un article d'une autre entreprise. `GET /api/inventory/inventory-counts/{id}` et `GET /{id}/results` renvoient `404` pour un comptage inconnu. La clôture est transactionnelle : elle réconcilie chaque ligne scannée, met à jour le statut et l'emplacement de l'article, ajuste le solde, écrit `last_inventory_at` et `completed_at`, puis refuse une seconde clôture en `409 COUNT_NOT_OPEN`. Un test vérifie le rollback complet lorsqu'une réconciliation échoue après la première ligne. **Limites résiduelles :** les scans historiques sans quantité sont horodatés mais ne produisent aucun ajustement inventé ; les ajustements de clôture ne créent pas de mouvement compensatoire ; un comptage vide peut encore être clôturé (hors périmètre du Lot 3).

**Statut au 23 octobre 2026 (Lot 6) : CORRIGÉ.** L'attendu est figé à l'ouverture dans `security.inventory_count_expected` (migration additive `22_lot6_inventory_counts.sql`) ; le scan lit cet instantané et refuse un article hors périmètre en `409 ITEM_OUT_OF_SCOPE`. La clôture est idempotente, refuse un comptage vide (`409 COUNT_EMPTY`), écrit `completed_at`/`completed_by`, réconcilie articles et soldes, renseigne `last_inventory_at` et journalise un mouvement `ADJUSTMENT` par écart non nul. `POST /{id}/cancel` annule sans ajustement. Les lignes sont paginées avec `X-Total-Count`. Création, liste, lecture, scan, clôture et annulation sont cloisonnés par l'entreprise du jeton. **Limites résiduelles :** les articles attendus non scannés ne sont pas marqués manquants, le solde suit l'état courant de l'article alors que le journal suit l'écart figé, et les filtres des lignes restent absents. Test : `InventoryCountApiTests` (10 tests).

**Constat initial :** `inventory_count_scan` ne contient pas de quantité attendue ou comptée. La clôture ne met à jour ni `item.current_warehouse_id`, ni `item.status`, ni `item.last_inventory_at`, ni `completed_at` si le corps est vide. Les scans ne sont pas contrôlés par entreprise. `GET /inventory-counts/{id}` est absent ; `GET /{id}/results` renvoie 200 `[]` pour un comptage inconnu.

**Impact :** un comptage est une collecte d'observations, pas une réconciliation d'inventaire.

**Références :** rapport 05.

---

### P1-08 — Performance et pagination non maîtrisées

**Constat :**

- `pageSize` non plafonné sur les modules typés ;
- `page=0` peut produire un `OFFSET` négatif ;
- pas de total ni de métadonnées de pagination ;
- N+1 dans `ItemRestMapper` ;
- pool de connexions non utilisé par les DAO ;
- listes globales non filtrées ;
- image QR régénérée à chaque appel ;
- rapports plafonnés à 500 mais sans agrégation.

**Impact :** latence et charge base ; l'interface ne peut pas afficher un paginateur fiable.

**Références :** rapports 04, 10 et 11.

---

### P1-09 — `DataSource` Spring contourné et pool non utilisé par les DAO

**Statut au 22 octobre 2026 (Lot 2) : CORRIGÉ pour le contournement de Spring et l'absence de pool.** `DatabaseConnection` ne lit plus `DriverManager` ni de valeurs de repli. Il reçoit le `DataSource` Spring, ouvre les connexions via `DataSourceUtils` et les libère correctement. HikariCP est configuré dans `application.properties` (taille, timeouts, validation, durée de vie). L'injection directe du `DataSource` dans chaque DAO n'a pas été faite : l'adaptateur statique reste une dette de conception, sans impact sur le pool ni sur les transactions.

**Constat initial :** `application-prod.properties` déclare `DB_URL`, `DB_USER`, `DB_PASSWORD` et un `DataSource`, et `DatabaseConnection` lit les mêmes variables. Mais les DAO passent par `DriverManager`, avec des valeurs de repli locales, et n'utilisent pas le `DataSource` Spring.

**Impact :** pool Spring non utilisé, réglages (timeouts, TLS, validation) ignorés, et valeurs de repli locales codées en dur qui peuvent masquer une configuration incomplète hors profil prod. Ce n'est pas une divergence de nom de variable, mais la couche JDBC n'applique pas la configuration de production.

**Références :** rapports 03 et 09.

---

### P1-10 — Réponses de création avec valeurs nulles

**Statut au 22 octobre 2026 (Lot 2) : CORRIGÉ.** Les DAO de mouvement et d'affectation relisent la ligne après insertion ; `movement_type`, `movement_date` et `assigned_at` reflètent les valeurs PostgreSQL. Les tests de réponse de création ne tolèrent plus `null`.

**Constat initial :** le DAO construit la réponse avec les valeurs d'entrée, pas avec les valeurs relues :

- `assigned_at: null` pour une affectation créée sans date ;
- `movement_date: null` et `movement_type: null` pour un mouvement créé sans date/type.

Les tests correspondants vérifient la base **et** acceptent `null` dans la réponse.

**Impact :** le frontend affiche une date vide après une création réussie et doit recharger la ressource.

**Références :** rapports 07, 08 et 11.

---

### P1-11 — Suppression physique et archivage inopérant

**Constat :**

- `DELETE` est physique pour les articles, types, entrepôts, catégories, mouvements et affectations ;
- les FK `RESTRICT` produisent 500 si l'objet est référencé ;
- les catégories utilisent `SET NULL`, ce qui vide silencieusement des relations ;
- `archived_at` et `archived_by` existent mais ne sont jamais renseignés ;
- les listes ne filtrent pas les archivés ;
- `DELETE` sur un UUID inconnu renvoie 204.

**Impact :** perte d'historique, blocages d'écran, impossibilité de distinguer suppression et absence.

**Références :** rapports 03, 04, 07 et 08.

**Statut au 22 octobre 2026 (Lot 4) : CORRIGÉ DANS LE PÉRIMÈTRE DU LOT 4.** Les articles, catégories, types et entrepôts sont archivés logiquement (`archived_at`, `archived_by`), exclus des listes et restaurables par `/restore`. Un UUID inconnu ou déjà archivé renvoie 404. L'archivage d'un article avec affectation active renvoie 409. Les affectations utilisent un void motivé et audité, et ne sont supprimables que lorsqu'elles sont déjà retournées ; une affectation active renvoie 409. Les mouvements restent append-only. **Limites résiduelles :** les références vers un type, une catégorie ou un entrepôt archivé ne sont pas toutes refusées, et la purge physique définitive n'est pas modélisée.

---

### P1-12 — Historique d'article global, tronqué et non testé correctement

**Constat :** `GET /items/{itemId}/history` charge les 100 premiers mouvements **globaux** et les 100 premières affectations **globales**, puis filtre en mémoire. Il ne vérifie pas l'existence de l'article. Un UUID inconnu renvoie 200 avec des listes vides.

L'ancien test `ItemApiTests.historyUnknownReturnsNotFound` attendait 404 ; il a été retiré de l'arbre. Le code actuel ne produit toujours pas ce 404 et l'anomalie n'est plus couverte par un test négatif.

**Impact :** historique incomplet, réponse 200 pour un article inexistant, et absence de garde-fou sur ce cas.

**Références :** rapports 02, 07 et 11.

**Statut au 22 octobre 2026 (Lot 4) : PARTIEL.** L'historique lit désormais les mouvements et affectations filtrés par article (jusqu'à 500 lignes chacun) au lieu de charger 100 lignes globales puis filtrer en mémoire. L'existence de l'article n'est toutefois toujours pas vérifiée : un UUID inconnu renvoie encore 200 avec des listes vides.

---

### P1-13 — Tests dépendants de la base de démonstration

**Constat :**

- base PostgreSQL réelle partagée ;
- pas de rollback ni de Testcontainers ;
- `Assumptions` ignore les tests sur base vide ;
- jeton synthétique `ADMIN` sans utilisateur réel ;
- aucun test de login, register, RBAC, tenancy, concurrence ou transaction ;
- plusieurs tests figent des comportements douteux (soft 200, 500, no-op).

**Impact :** une suite verte ne prouve ni la sécurité, ni la tenancy, ni la fiabilité du stock.

**Référence :** rapport 11.

**Statut au 23 octobre 2026 (Lots 5 et 6) : AGGRAVÉ, PARTIELLEMENT ATTÉNUÉ.** La suite fonctionnelle de 150 tests reste supprimée ; elle compte désormais **7 classes et 49 tests**, dont 20 de conformité du schéma SQL à l'application, 17 sur le contrat QR (Lot 5) et 10 sur le contrat des comptages (Lot 6). Les comportements métier couverts restent limités au QR et aux comptages : ni la sécurité, ni les affectations, ni la tenancy générale, ni la concurrence, ni les transactions ne sont testés. La suite reste verte, mais elle ne prouve pas le domaine. L'impact reste 5 : **score 25, niveau Critique**. Le critère de résolution (base isolée, fixtures, matrice RBAC/tenancy A/B, tests de concurrence et de transaction) reste à faire.

---

### P1-14 — Absence de concurrence et d'idempotence

**Constat :** aucun `version`, `ETag`, `If-Match` ou `Idempotency-Key`. Deux utilisateurs peuvent écraser la même modification. Un double clic peut créer deux opérations ou deux affectations. La course sur l'affectation active est rejetée par l'index unique en 500, pas en 409.

**Impact :** pertes de mise à jour et doublons en production.

**Références :** rapports 08, 10 et 11.

---

## 4. Registre P2 — dette structurante

### P2-01 — Archivage et suppression non modélisés

**Statut au 22 octobre 2026 (Lot 4) : CORRIGÉ pour les référentiels et les affectations.** Les articles, catégories, types et entrepôts disposent d'un archivage logique et d'une route `/restore` ; les affectations utilisent un void motivé et une restauration. La purge physique définitive et la conservation légale restent à décider (D-04).

### P2-02 — Contrat de pagination et de filtrage insuffisant

Pas de total, pas de métadonnées, filtres limités, tri par `id`, pas de recherche plein texte, pas de filtre par entreprise, type, catégorie, date ou acteur. Le frontend filtrera côté client.

### P2-03 — Nommage, versionnement et dates

Routes hors préfixe (`/api/item-assignments`), absence de `/api/v1`, deux contrats pour un même objet, dates `java.sql.Timestamp` sans fuseau contractuel, pas de format de date documenté, enums sans libellés.

### P2-04 — Dashboard et alertes non représentatifs

Compteurs globaux, low-stock basé sur `quantity_total`, pas de stock par entrepôt, pas de valeur, pas d'écarts, pas de période. Le dashboard ne peut pas piloter l'activité.

### P2-05 — Absence d'étiquettes et d'impression en lot

Le PNG est disponible, mais il n'existe ni PDF, ni planche, ni file d'impression, ni texte lisible, ni cache. Un déploiement réel devra imprimer des centaines d'étiquettes.

**Statut au 22 octobre 2026 (Lot 5) : PARTIELLEMENT CORRIGÉ.** Une étiquette HTML imprimable existe pour un article et pour une planche de 100 articles en un seul appel : le QR code est intégré dans la page et le texte est lisible (code, type, description, catégorie, entrepôt, entreprise). L'image PNG est désormais mise en cache (`ETag`, `Cache-Control`, 304) et accepte une taille configurable. **Limites :** il n'y a toujours pas de PDF généré par le serveur (l'impression passe par le navigateur), pas de file d'impression ni de gestion d'impression en cours.

### P2-06 — CI, couverture et documentation de contrat

Un workflow GitHub Actions existe (`.github/workflows/ci.yml`) : PostgreSQL 16 éphémère, initialisation du schéma et des données de démonstration, tests, lint OpenAPI et packaging. Il ne couvre cependant ni la tenancy, ni le RBAC, ni les transactions, ni la concurrence, et la couverture n'est pas mesurée. Le lint OpenAPI a été rejoué au Lot 4 avec la version de la CI (`@redocly/cli@2.65.1`) : **179 avertissements non bloquants, 0 erreur** sur 26 fichiers (183 à l'audit initial) ; les exemples ne sont toujours pas contractuels.

---

## 5. Matrice de risque

La matrice ci-dessous reste la cotation de l'audit initial. Après le Lot 1 : **P0-03 est corrigé**, **P0-02 est partiellement corrigé** (inscription fermée et RBAC inventaire/utilisateurs/rôles), et **P0-01 reste partiel** sous l'hypothèse mono-entreprise. Après le Lot 2 : **P0-04 est corrigé pour les cas d'usage existants**, **P1-01, P1-02, P1-09 et P1-10 sont corrigés**. Après le Lot 3 : **P0-05, P1-03, P1-06 et P1-07 sont corrigés dans le périmètre documenté**. Après le Lot 4 : **P0-06, P1-05 et P1-11 sont corrigés dans le périmètre documenté**, et **P2-01 est corrigé** pour les articles, catégories, types, entrepôts et affectations. `P0-01`, `P1-04`, `P1-08`, `P1-12` (partiel), `P1-13` (aggravé par la réduction de la suite : 20 tests de conformité seulement, score porté à 25), la concurrence (`P1-14`) et les autres P2 restent ouverts. Après le Lot 6 : **P1-07 est complété** (attendu figé, périmètre, clôture idempotente et non vide, annulation, pagination et journal des ajustements), et les comptages sont cloisonnés par entreprise ; `P0-01` reste ouvert pour les autres modules et `P1-14` pour la concurrence.

| ID | Risque | Probabilité | Impact | Score | Niveau |
|---|---|---:|---:|---:|---|
| P0-01 | Fuite/corruption inter-entreprises | 5 | 5 | 25 | Critique |
| P0-02 | Inscription publique + RBAC absent | 5 | 5 | 25 | Critique |
| P0-03 | Refresh perpétue les rôles révoqués | 5 | 5 | 25 | Critique |
| P0-04 | Écritures partiellement validées | 4 | 5 | 20 | Critique |
| P0-05 | Stock non fiable | 5 | 5 | 25 | Critique |
| P0-06 | États d'affectation incohérents | 5 | 5 | 25 | Critique |
| P1-01 | 500 + SQLSTATE exposés | 5 | 4 | 20 | Élevé |
| P1-02 | Création de type/employé fragile (défauts SQL) | 4 | 3 | 12 | Moyen |
| P1-03 | Journal de mouvements non auditable | 5 | 4 | 20 | Élevé |
| P1-04 | Scan QR global et brut | 5 | 4 | 20 | Élevé |
| P1-05 | Deux contrats d'affectation | 5 | 3 | 15 | Élevé |
| P1-06 | Opérations vides | 5 | 4 | 20 | Élevé |
| P1-07 | Comptages non réconciliés | 5 | 4 | 20 | Élevé |
| P1-08 | Performance non maîtrisée | 4 | 3 | 12 | Moyen |
| P1-09 | `DataSource` contourné / pool non utilisé | 4 | 4 | 16 | Élevé |
| P1-10 | Dates nulles après création | 5 | 3 | 15 | Élevé |
| P1-11 | Suppressions destructrices | 4 | 4 | 16 | Élevé |
| P1-12 | Historique faux/tronqué | 5 | 3 | 15 | Élevé |
| P1-13 | Suite de tests non probante | 5 | 5 | 25 | Critique |
| P1-14 | Concurrence/ idempotence | 4 | 3 | 12 | Moyen |
| P2-01 | Archivage inerte | 4 | 3 | 12 | Moyen |
| P2-02 | Pagination/filtres insuffisants | 5 | 3 | 15 | Moyen |
| P2-03 | Contrat non versionné | 4 | 2 | 8 | Faible |
| P2-04 | Dashboard non représentatif | 5 | 3 | 15 | Moyen |
| P2-05 | Étiquettes non imprimables en lot | 4 | 2 | 8 | Faible |
| P2-06 | CI présent mais insuffisant | 5 | 2 | 10 | Faible |

La matrice contient les **26 anomalies** du registre : 6 P0, 14 P1 et 6 P2. Les identifiants sont uniques. `P1-02` reste classé P1 malgré un score de 12 : la défaillance est reproductible sur un formulaire minimal et bloque un parcours de création courant.

---

## 6. Chemin critique

L'ordre recommandé n'est pas l'ordre alphabétique des modules. Il suit les dépendances de données.

### Étape 1 — Fermer l'accès

**État Lot 1 :** inscription fermée aux administrateurs, entreprise et permissions dans le jeton, rôles rechargés au refresh, rotation et révocation des jetons. Le RBAC reste à étendre aux modules hors inventaire, et les DAO d'inventaire ne filtrent pas encore par entreprise.

1. Décider du mode d'inscription (fermée, invitation, validation).
2. Ajouter l'entreprise au compte et au jeton, ou la déduire d'une source serveur.
3. Appliquer un RBAC par permission et par tenant.
4. Recharger les rôles au refresh et permettre la révocation.

**Sortie :** un utilisateur A ne peut pas lire ni écrire les données de B ; un rôle retiré ne peut pas être rafraîchi.

### Étape 2 — Rendre les écritures atomiques — **FAIT au Lot 2**

1. Remplacer `DriverManager` par un `DataSource` partagé et un pool. **Fait** (HikariCP).
2. Faire participer les DAO à la transaction Spring. **Fait** (`DataSourceUtils`).
3. Tester un échec après la première écriture. **Fait** (rollback DAO et service mouvement).

**Sortie :** aucune écriture partielle en cas d'erreur, pour les cas d'usage existants. Les opérations multi-lignes sont implémentées et atomiques depuis le Lot 3 (`P1-06`).

### Étape 3 — Choisir le modèle de stock — **FAIT au Lot 3**

1. Décider si le stock est déclaratif ou calculé. **Fait : journal calculé + `stock_balance` (D-01).**
2. Si calculé : ajouter les quantités, les soldes par entrepôt et les mouvements immuables. **Fait : migration 19, API de soldes, journal append-only, opérations et comptages réconciliés.**
3. Si déclaratif : assumer que le dashboard n'est pas un stock réel. **Sans objet.**

**Sortie :** `GET /api/inventory/stock-balances` fournit une réponse testée par entrepôt et type ; le dashboard low-stock utilise ce solde. Les affectations sont raccordées au stock depuis le Lot 4 ; seule la concurrence (`P1-14`) reste à traiter.

### Étape 4 - Fiabiliser le cycle de vie des affectations et l'archivage - **FAIT au Lot 4**

1. Unifier les deux contrats d'affectation derrière un service et un contrat canoniques. **Fait** (`/api/item-assignments`, façade dépréciée).
2. Lier `OUT`/`RETURN` aux affectations et synchroniser le solde. **Fait**.
3. Rendre le retour, le void et l'archivage explicites et audités. **Fait** (`source_warehouse_id`, `voided_at`, `voided_by`, `void_reason`).
4. Archiver logiquement les référentiels et les affectations, avec restauration. **Fait**.

**Sortie :** un article `ASSIGNED` possède au plus une affectation active, un retour restaure l'article et son stock, et une suppression active est refusée. **Réserves :** isolation entreprise (`P0-01`), idempotence (`P1-14`) et réconciliation de la donnée de démonstration préexistante.

### Étape 4 — Unifier les affectations

1. Choisir une seule API.
2. Imposer une machine à états.
3. Exiger `returned_at` pour un retour.
4. Interdire la suppression d'une affectation active.
5. Aligner `OUT`/`RETURN`.

**Sortie :** un article `ASSIGNED` a toujours une affectation active, et inversement.

### Étape 5 — Stabiliser le contrat HTTP

1. Pagination normalisée.
2. Erreurs `application/problem+json`, codes stables.
3. 409/422 au lieu de 500.
4. Dates non nulles, ISO-8601.
5. Idempotence et verrouillage optimiste.
6. Scan QR enrichi et tenant-scoped.

**Sortie :** un frontend peut coder contre le contrat sans parser SQLSTATE ni recharger après création.

---

## 7. Ce qui n'est pas prioritaire

- Ajouter des champs cosmétiques au dashboard.
- Générer un PDF d'étiquette avant d'avoir stabilisé le code QR.
- Ajouter des écrans avant d'avoir tranché la tenancy.
- Augmenter le nombre de tests sans base isolée.
- Optimiser le N+1 avant d'avoir corrigé le modèle de stock.
- Renommer les routes avant d'avoir un contrat de version.

---

## 8. Conclusion

Le système n'est pas prêt pour la production. Il est partiellement prêt pour un prototype mono-entreprise, en lecture et en CRUD nominal, avec un utilisateur de confiance et sans enjeu de stock.

Les six P0 doivent être traités avant tout frontend de production. Les P1 doivent être traités avant un pilote multi-utilisateurs. Les P2 peuvent être planifiés après stabilisation.

Le plan détaillé est dans le rapport 14 ; les critères de sortie sont dans le rapport 15.
