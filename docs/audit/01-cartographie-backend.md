# 01 — Cartographie du backend

**Objectif :** décrire l'architecture réelle du backend, ses couches, ses modules, ses dépendances et le chemin d'une requête HTTP, afin de situer les anomalies dans le code.

---

## 1. Vue d'ensemble

Le backend est une application Spring Boot monolithique, organisée par domaines métier, avec un accès direct à PostgreSQL via JDBC. Il n'utilise ni JPA, ni Hibernate, ni MyBatis. Les contrôleurs exposent du JSON, les services portent une partie des règles métier, et les DAO écrivent le SQL à la main.

```text
Client HTTP
   |
   v
Spring Security (JwtAuthenticationFilter)
   |
   v
@RestController  (28 contrôleurs, 149 routes après le Lot 5)
   |
   v
@Service         (règles métier, parfois @Transactional)
   |
   v
@Repository      (DAO JDBC, SQL manuel)
   |
   v
DatabaseConnection.getConnection()  (DataSourceUtils)
   |
   v
DataSource Spring / HikariCP
   |
   v
PostgreSQL — base sssm_db, schéma security
```

Depuis le Lot 2, `DatabaseConnection` est un adaptateur statique qui lit le `DataSource` Spring et obtient une connexion via `DataSourceUtils`. Les DAO participent donc à la transaction Spring et utilisent le pool HikariCP. Le point critique restant est le couplage statique à ce `DataSource` et l'absence d'injection directe dans les DAO.

---

## 2. Stack technique

| Composant | Version / choix | Fichier de référence |
|---|---|---|
| Framework | Spring Boot `4.1.1` | `pom.xml` |
| Langage | Java `17` | `pom.xml` |
| Web | `spring-boot-starter-web` | `pom.xml` |
| Sécurité | `spring-boot-starter-security` | `pom.xml` |
| Validation | `spring-boot-starter-validation` | `pom.xml` |
| JDBC | `spring-boot-starter-jdbc` | `pom.xml` |
| Driver PostgreSQL | `42.7.13` | `pom.xml` |
| JWT | JJWT `0.12.6` (`api`, `impl`, `jackson`) | `pom.xml` |
| QR Code | ZXing `core` et `javase` `3.5.3` | `pom.xml` |
| Hash de mot de passe | BCrypt via Spring Security | `SecurityConfig` |
| Mapping | Lombok, mappers manuels | `pom.xml`, packages `dto.mapper` |
| Formatage | `fmt-maven-plugin` `2.9.1` | `pom.xml` |
| Tests | `spring-boot-starter-test` | `pom.xml` |

Le starter JDBC est présent, mais il n'est pas utilisé pour les DAO : il sert uniquement à l'auto-configuration d'un `DataSource` que le code applicatif contourne.

---

## 3. Organisation des packages

Le code est rangé sous `sssm.dev.sssm_backend`, avec une convention de préfixes numériques.

| Package | Rôle | Contrôleurs |
|---|---|---|
| `a00_infrastructure` | connexion base, exceptions techniques | 0 |
| `a01_common` | énumérations, enveloppe d'erreur, gestion globale des exceptions | 1 (`ItemErrorController`, sans route) |
| `a02_organisation` | entreprises, agences, succursales | 3 |
| `a03_authentication` | authentification, utilisateurs, rôles, permissions | 4 |
| `a04_personnel` | employés, documents employés | 2 |
| `a05_clients` | clients, contacts clients | 2 |
| `a06_sites` | sites, postes de sécurité, exigences de poste | 3 |
| `a11_inventory` | inventaire, stock, QR, affectations, étiquettes, comptages, dashboard, rapports | 14 |
| `controller_test` | contrôleurs de test (`Hello`, `Ping`) | 2 |

Total : **28 contrôleurs REST métier**, plus deux contrôleurs de test et un contrôleur technique sans route métier (`ItemErrorController`), et **149 routes applicatives après le Lot 5** (130 à l'audit initial, 131 après le Lot 1, 133 après le Lot 3, 147 après le Lot 4 ; 151 annotations de mapping en incluant `Hello` et `Ping`).

### 3.1. Détail du module inventaire

| Sous-module | Contrôleur | Service | DAO | Table(s) principale(s) |
|---|---|---|---|---|
| `item` | `ItemController` | `ItemService`, `QrCodeService` | `ItemDAOImpl` | `item` |
| `item_type` | `ItemTypeController` | `ItemTypeService` | `ItemTypeDAOImpl` | `item_type` |
| `item_category` | `ItemCategoryController` | `ItemCategoryService` | `ItemCategoryDAOImpl` | `item_category` |
| `warehouse` | `WarehouseController` | `WarehouseService` | `WarehouseDAOImpl` | `warehouse` |
| `stock_movement` | `StockMovementController` | `StockMovementService` | `StockMovementDAOImpl` | `stock_movement` |
| `stock_balance` | `StockBalanceController` | `StockBalanceService` | `StockBalanceDAOImpl` | `stock_balance` |
| `item_assignment` | `ItemAssignmentController` | `ItemAssignmentService` | `ItemAssignmentDAOImpl` | `item_assignment` |
| `assignment` | `AssignmentController` (façade dépréciée) | `ItemAssignmentService` | `ItemAssignmentDAOImpl` | `item_assignment` |
| `qr_scan` | `QrScanController` | `QrScanService` | `QrScanDAOImpl` | `item`, `qr_scan` |
| `label` | `LabelController` | `LabelService` | `LabelDAO` | `item`, `item_type`, `item_category`, `warehouse`, `company` |
| `inventory_count` | `InventoryCountController` | `InventoryCountService` | `InventoryCountDAOImpl` | `inventory_count`, `inventory_count_scan` |
| `operation` | `OperationController` | `OperationService` | `OperationDAOImpl` | `inventory_operation` |
| `dashboard` | `DashboardController` | `DashboardService` | `DashboardDAOImpl` | `item`, `warehouse`, `inventory_operation`, `item_type`, `stock_balance`, `warehouse_stock_config` |
| `report` | `ReportController` | `ReportService` | `ReportDAOImpl` | `stock_movement` |

**Mise à jour du 22 octobre 2026 (Lot 4) :** les deux modules historiques ne sont plus deux implémentations. `item_assignment` porte le contrat canonique `/api/item-assignments` ; `assignment` est une façade dépréciée de `/api/inventory/assignments` qui délègue au même `ItemAssignmentService`, avec les mêmes DTO, les mêmes validations et les mêmes codes HTTP. Les réponses de succès de la façade portent `Deprecation: true` et `Link: </api/item-assignments>; rel="successor-version"`. Le JSON libre et les réponses 200 portant une erreur douce ont été supprimés.

Le code historique du module `assignment` (`AssignmentService`, `AssignmentDAO`/`AssignmentDAOImpl`, `AssignmentRest` et ses DTO) subsiste dans l'arborescence mais n'est plus référencé par aucun composant vivant : il est candidat à la suppression à la clôture de la fenêtre de dépréciation (D-07). Le détail du cycle de vie unifié figure dans le rapport 08.

---

## 4. Chemin d'une requête

### 4.1. Requête authentifiée

1. Le client envoie `Authorization: Bearer <JWT>`.
2. `JwtAuthenticationFilter` lit l'en-tête, appelle `JwtService.parse`, extrait `sub`, `uid` et `roles`, puis crée un `UsernamePasswordAuthenticationToken` avec des autorités `ROLE_<role>`.
3. `SecurityConfig` applique les règles d'URL.
4. Le contrôleur reçoit la requête, valide éventuellement le DTO avec `@Valid`.
5. Le service applique les règles métier et appelle un ou plusieurs DAO.
6. Chaque DAO ouvre sa propre connexion avec `DatabaseConnection.getConnection()`.
7. Le DAO exécute le SQL, mappe le `ResultSet` vers un DTO de réponse, puis ferme la connexion.
8. Le service convertit éventuellement le DTO interne en objet REST via un mapper.
9. Le contrôleur renvoie du JSON.

### 4.2. Requête en erreur

- Les exceptions `ClientException` deviennent des **400**.
- Les `NotFoundException` deviennent des **404**.
- Les `ServerException` deviennent des **500**.
- Les erreurs de validation Spring deviennent des **400** avec `VALIDATION_ERROR`.
- Les erreurs PostgreSQL sont converties par `ErrorMapper` en **400**, **409**, **422** ou **503** selon la SQLSTATE, sans exposer le SQLSTATE ni le message PostgreSQL (Lot 2). Les conflits d'unicité, dont la course sur `uq_item_assignment_active`, remontent en **409**.

Le module `assignment` ne lève plus d'exception douce : il délègue au service canonique et renvoie les mêmes 404/409 que `/api/item-assignments` (Lot 4). Les modules `inventory_count` et `operation` conservent des cas d'erreur douce en 200, décrits dans le rapport 02.

---

## 5. Configuration

### 5.1. Fichiers

| Fichier | Contenu | Remarque |
|---|---|---|
| `src/main/resources/application.properties` | port, secret JWT de développement, expiration, rôle par défaut, `spring.datasource.*` et `spring.datasource.hikari.*` | Le `DataSource` et le pool sont utilisés par les DAO depuis le Lot 2. |
| `src/main/resources/application-prod.properties` | secret JWT et identifiants base sans valeur par défaut | Le profil prod exige les variables d'environnement ; les valeurs de repli ont été supprimées de `DatabaseConnection`. |
| `src/main/java/.../a00_infrastructure/database/DatabaseConnection.java` | Adaptateur statique vers le `DataSource` Spring via `DataSourceUtils` | Pool HikariCP, connexion transactionnelle, aucune valeur de repli. |

### 5.2. Conséquence

Le couple `application-prod.properties` + `DatabaseConnection` est désormais aligné depuis le Lot 2 :

- Spring configure un `DataSource` HikariCP avec les variables d'environnement ;
- les DAO obtiennent ce `DataSource` et passent par `DataSourceUtils` ;
- les valeurs de repli locales ont été supprimées ;
- `@Transactional` englobe les écritures des DAO, avec rollback testé.

Le couplage statique reste une dette de conception, mais il ne contourne plus Spring.

---

## 6. Patterns et conventions

### 6.1. Ce qui est homogène

- Les contrôleurs sont fins et délèguent aux services.
- Les DTO de création et de mise à jour sont séparés.
- Les réponses REST sont des classes dédiées (`*Rest`), souvent distinctes des DTO internes (`*Response`).
- Les erreurs utilisent une enveloppe unique `{"errors": {"message", "detail", "code"}}`.
- Les noms de colonnes sont en `snake_case`, y compris dans les DTO Java (`company_id`, `item_type_id`, `qr_code`).
- Les routes d'inventaire sont majoritairement préfixées par `/api/inventory`.
- Les listes paginées utilisent `page` et `pageSize`, avec des valeurs par défaut variables.

### 6.2. Ce qui est hétérogène

| Sujet | Variations observées |
|---|---|
| Pagination | `page`/`pageSize` (inventaire typé), `page`/`size` (modules génériques), valeurs par défaut 10, 50 ou 500 |
| Plafond de pagination | absent sur les modules typés, plafonné à 500 sur les modules génériques |
| Codes de création | `201` pour les modules typés, `200` pour `inventory_count` et `operation` ; la façade `assignment` renvoie `201` comme le contrat canonique (Lot 4) |
| Erreurs « introuvable » | `404` pour les modules typés, `200` avec `errors` pour `inventory_count` ; la façade `assignment` renvoie `404` (Lot 4) |
| Réponses de liste | objets imbriqués pour `ItemRest`, identifiants plats pour les mouvements et affectations |
| Champs d'audit | `assigned_by`, `returned_by` et `voided_by` exposés par le contrat d'affectation canonique et par la façade |
| Nommage | `/api/item-assignments` (canonique) contre `/api/inventory/assignments` (façade dépréciée) |

---

## 7. Accès aux données

### 7.1. Caractéristiques

- SQL écrit à la main dans les DAO.
- `PreparedStatement` utilisé pour les valeurs, ce qui limite l'injection SQL directe.
- certains modules génériques construisent des noms de colonnes à partir des clés JSON, après une regex `[A-Za-z_][A-Za-z0-9_]*` ; les valeurs restent paramétrées, mais le schéma n'est pas filtré par une liste blanche de colonnes.
- Aucune pagination par curseur, aucun `ORDER BY` stable sur les dates (souvent `ORDER BY id`).
- Aucun verrou pessimiste, aucune colonne de version, aucun `SELECT ... FOR UPDATE`.
- Aucune connexion partagée ni pool : chaque appel ouvre et ferme une connexion TCP.

### 7.2. Conséquences

- Les listes d'articles déclenchent une chaîne de requêtes par article (N+1), décrite dans le rapport 10.
- Les opérations qui écrivent dans deux tables (article + affectation, article + mouvement) ne sont pas atomiques.
- Une erreur en cours de chaîne laisse la première écriture validée.
- Le coût d'une liste augmente fortement avec le nombre d'articles et la profondeur des relations.

---

## 8. Sécurité applicative

| Élément | Implémentation | Limite |
|---|---|---|
| Authentification | JWT HMAC, algorithme déduit de la clé par JJWT, filtre `OncePerRequestFilter` | Pas de révocation, pas de refresh token persistant |
| Mots de passe | BCrypt | Pas de politique de complexité au-delà de 8 caractères |
| Autorisation | `SecurityConfig` par URL | Pas de `@PreAuthorize`, pas de permissions fines |
| Rôles | table `role`, `user_role` | Rôles globaux ou par entreprise, non filtrés dans les routes |
| CORS | `allowedOriginPatterns("*")` + `allowCredentials(true)` | Combinaison permissive |
| CSRF | désactivé | Acceptable pour une API stateless, à condition de ne pas utiliser de cookies |
| Erreurs SQL | SQLSTATE et message PostgreSQL renvoyés | Fuite d'information technique |

Le détail est dans le rapport 09.

---

## 9. Documentation et artefacts

| Artefact | Nombre | État |
|---|---|---|
| Scripts SQL | 20 (`00` à `19`) | Présents, non migrés automatiquement ; `19_lot3_stock.sql` est additive |
| Fichiers OpenAPI | 26 | Présents, structure valide ; lint Redocly 2.65.1 rejoué au Lot 4 : **179 avertissements, 0 erreur** (183 à l'audit initial) |
| Fichiers OpenAPI inventaire | 12 | Présents, cohérents avec les routes principales |
| Tests | Audit initial : 21 classes, 105 tests ; HEAD `7e3f196` : 22 classes, 112 tests ; après Lot 1 : 23 classes, 120 tests ; après Lot 2 : 27 suites, 132 tests ; après Lot 3 : 28 suites, 140 tests | Suite courante exécutée : 140 succès, 0 échec, 0 erreur, 0 ignoré. |
| Rapport Surefire | `target/surefire-reports` | Rapport complet courant : 28 suites, 140 tests. |

Les scripts SQL ne sont pas exécutés par un outil de migration (Flyway/Liquibase). Le schéma est créé manuellement ou par un script externe. Il n'existe pas de table de version de schéma applicative.

---

## 10. Dettes architecturales structurantes

| Dette | Preuve | Impact |
|---|---|---|
| Absence de couche de tenancy | Aucun filtre `company_id` dans les DAO d'inventaire | Fuite et mélange de données entre entreprises |
| Couplage statique au `DataSource` | `DatabaseConnection` statique | Tests et injection moins explicites, mais transactions effectives |
| Code mort du module `assignment` | `AssignmentService`, `AssignmentDAOImpl` et DTO associés ne sont plus référencés (Lot 4) | Confusion de lecture ; à supprimer à la clôture de la dépréciation (D-07) |
| DAO génériques à colonnes libres | `InventoryCountDAOImpl.save`, `OperationDAOImpl.save` | Contournement des DTO et des validations |
| Stock déclaratif | `item_type.quantity_total` manuel | Stock non fiable |
| Opérations vides | `inventory_operation` sans lignes ni mouvements | Faux sentiment de traçabilité |
| N+1 | `ItemRestMapper` | Performance dégradée |
| Absence de contrôle de concurrence | Aucune colonne `version`, aucun `If-Match` | Écrasements silencieux |
| Pas de version de schéma | scripts SQL manuels | Déploiements non reproductibles |
| Documentation API partielle | 179 avertissements Redocly au Lot 4 (`license`, `localhost`, `operationId` absents sur les opérations antérieures, exemples manquants) | Contrats incomplets pour les frontends |

**Dettes closes par les Lots 2 à 4 :** `DriverManager` et les connexions hors transaction (Lot 2), le stock déclaratif et les opérations vides (Lot 3), la double implémentation des affectations et les suppressions physiques (Lot 4). Les lignes « deux modules d'affectation », « DAO génériques à colonnes libres » (volet affectations) et « stock déclaratif » décrivent l'état initial ; leur situation courante est détaillée dans les rapports 05, 08 et 12.

---

## 11. Synthèse

Le backend est **structuré** : les responsabilités sont globalement séparées, les modules sont identifiables et les routes sont nombreuses. Il est en revanche **fragile sur les fondations** : tenancy, transactions, stock et autorisations ne sont pas garantis par l'architecture.

Les rapports suivants examinent chaque domaine. Le rapport 12 consolide les risques et le rapport 14 propose un ordre de correction qui respecte les dépendances entre ces fondations.
