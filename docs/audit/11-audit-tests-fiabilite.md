# 11 — Audit des tests et de la fiabilité

**Note Lot 7 (23 octobre 2026) :** les routes canoniques sont désormais préfixées `/api/v1` ; l'alias `/api` reste accepté avec `Deprecation`/`Link`. Les chemins cités ci-dessous reflètent l'état de l'audit au moment de sa rédaction, sauf mise à jour explicite.

**Objectif :** évaluer ce que les tests prouvent réellement, ce qu'ils ne prouvent pas, et dans quelle mesure ils peuvent servir de garde-fou avant correction.

**Mise à jour du 22 octobre 2026 (suite de tests réduite) :** la suite automatisée a été ramenée à **4 classes et 20 tests** (`PingControllerTest`, `SssmBackendApplicationTests`, `DatabaseSchemaConformanceTests`, `SchemaScriptsConformanceTests`). Ces tests vérifient uniquement que le schéma SQL appliqué est conforme à ce que l'application attend (tables, colonnes, types, clés étrangères, index, énumérations, scripts de migration). Les classes de tests fonctionnels citées dans les sections suivantes ont été supprimées le 22 octobre 2026 (commit `453c82f`) : les constats qu'elles étayaient restent **vérifiés par lecture de code**, mais ne sont plus couverts par un test automatisé. Les critères `PASS` correspondants du rapport 15 se lisent désormais « vérifié statiquement ».

La suite exécutée le 22 octobre 2026 après le Lot 4 comptait **150 tests dans 28 suites** ; cette suite a ensuite été remplacée par la suite de conformité (20 tests, 4 suites).

**Mise à jour du 23 octobre 2026 (Lots 5 à 7) :** la suite compte désormais **8 classes et 58 tests**. Trois contrats métier s'y ajoutent : le scan QR (13 tests et 4 tests de limiteur, Lot 5), les comptages (10 tests, Lot 6) et le contrat HTTP (9 tests, Lot 7 : versionnement, Problem Details, pagination, `Location`, dates UTC, historique 404). Les tests de conformité de schéma restent à 20 tests. Le reste des constats (tenancy générale, concurrence, matrice RBAC, base isolée par test) reste ouvert.

---

## 1. Chiffres disponibles

| Indicateur | Valeur |
|---|---|
| Classes de tests | 21 (audit initial) |
| Tests | 105 (rapport complet historique) |
| Échecs | 0 |
| Erreurs | 0 |
| Tests ignorés | 0 |
| Source | Rapport complet `target/surefire-reports` de l'audit initial ; ce rapport n'est plus présent dans le `target` courant |

Répartition :

| Classe | Tests |
|---|---:|
| `ErrorFormatTests` | 11 |
| `ItemApiTests` | 17 |
| `AssignmentApiTests` | 9 |
| `ItemTypeApiTests` | 7 |
| `StockMovementBusinessRulesTests` | 7 |
| `InventoryCountScanCloseTests` | 6 |
| `ReportApiTests` | 5 |
| `WarehouseApiTests` | 5 |
| `ItemAssignmentStateTests` | 4 |
| `ItemCategoryApiTests` | 4 |
| `OperationApiTests` | 4 |
| `QrScanApiTests` | 4 |
| `RefreshTokenApiTests` | 4 |
| `AppUserApiTests` | 3 |
| `DashboardApiTests` | 3 |
| `ItemAssignmentTimestampTests` | 3 |
| `StockMovementTimestampTests` | 3 |
| `WhoAmIApiTests` | 3 |
| `CompanySearchApiTests` | 1 |
| `PingControllerTest` | 1 |
| `SssmBackendApplicationTests` | 1 |

**Limite :** ces résultats sont ceux du dernier rapport complet historique. Ils ne garantissent pas que la suite passerait sur l'état actuel du code, notamment à cause de la divergence décrite au point 6.

### 1.1. État vérifié au HEAD `7e3f196` (revue de cohérence)

| Indicateur | Valeur |
|---|---|
| Classes de tests dans l'arbre | 22 |
| Tests dans l'arbre | 112 (105 historiques + 7 tests QR) |
| Rapport complet pour `7e3f196` | **ABSENT** |
| Artefact Surefire courant | `ItemControllerQrCodeImageTest` uniquement : 7 tests, 0 échec, 0 erreur, 0 ignoré |
| Exécution complète sur HEAD | **NON VÉRIFIÉE** (aucune suite complète relancée ; base de démonstration partagée non modifiée) |
| Divergence connue (test depuis retiré) | `ItemApiTests.historyUnknownReturnsNotFound` attendait 404 ; `ItemController` renvoyait 200 avec des listes vides |

Le commit `7e3f196` ajoute `ItemControllerQrCodeImageTest` et son rapport de 7 tests. Il ne fournit pas de rapport pour les 21 autres classes. On ne peut donc pas conclure que les 105 tests historiques passent sur HEAD, et la lecture statique indique au moins un test qui devrait échouer.

### 1.2. État après le Lot 1 (21 octobre 2026)

| Indicateur | Valeur |
|---|---|
| Classes de tests | 23 |
| Tests exécutés | 120 |
| Succès | 119 |
| Échecs | 1 |
| Erreurs | 0 |
| Ignorés | 0 |
| Commande | `.\mvnw.cmd -q test` |
| Échec (historique, test depuis retiré) | `ItemApiTests.historyUnknownReturnsNotFound` : 404 attendu, 200 obtenu |

Le HEAD de travail n'est plus `7e3f196` : le Lot 1 ajoute `AuthHardeningApiTests` (7 tests) et un test de rotation/révocation dans `RefreshTokenApiTests` (5 tests). Les 29 tests ciblés d'authentification et de RBAC passent. L'unique échec est une divergence **préexistante** du contrôleur d'historique, reproduite sur le HEAD initial et hors périmètre du Lot 1.

### 1.3. État après le Lot 2 (22 octobre 2026)

| Indicateur | Valeur |
|---|---|
| Suites de tests | 27 |
| Tests exécutés | 132 |
| Succès | 132 |
| Échecs | 0 |
| Erreurs | 0 |
| Ignorés | 0 |
| Commande | `.\mvnw.cmd -q test` |
| Nettoyage | `phase2-*`/`phase5-*` : 0 ligne résiduelle |

Le Lot 2 ajoute des tests de pool HikariCP, de rollback DAO, de rollback du service de mouvement, de défauts de type/employé, de réponse de création, de doublon 409 et de FK 422. L'ancien test `historyUnknownReturnsNotFound` n'est plus présent dans l'arbre ; l'anomalie fonctionnelle de l'historique reste décrite en `P1-12`, mais elle n'est plus un échec de suite.

### 1.4. État après le Lot 3 (22 octobre 2026)

| Indicateur | Valeur |
|---|---|
| Suites de tests | 28 |
| Tests exécutés | 140 |
| Succès | 140 |
| Échecs | 0 |
| Erreurs | 0 |
| Ignorés | 0 |
| Commande | `.\mvnw.cmd -q test` |
| Tests dédiés | `Lot3StockModelTests` : 8 tests |
| Nettoyage | `phase2-*`, `phase3-*`, `phase3-lot3-*`, `phase5-*` : 0 ligne résiduelle |

Le Lot 3 ajoute les tests de solde calculé, d'immuabilité du journal, de stock négatif, d'acteur JWT, de rollback des opérations multi-lignes, de comptage avec écart serveur, de clôture réconciliée et de rollback de clôture. La suite reste exécutée sur la base de démonstration partagée : elle ne prouve ni la tenancy A/B, ni la concurrence, ni la synchronisation des affectations.

### 1.5. État après le Lot 4 (22 octobre 2026)

| Indicateur | Valeur |
|---|---|
| Suites de tests | 28 |
| Tests exécutés | 150 |
| Succès | 150 |
| Échecs | 0 |
| Erreurs | 0 |
| Ignorés | 0 |
| Commande | `.\mvnw.cmd -q test` |
| Tests dédiés | `AssignmentApiTests` : 14, `ItemAssignmentStateTests` : 5, `ItemAssignmentTimestampTests` : 3 |
| Tests adaptés | `ItemApiTests` 16, `ItemCategoryApiTests` 6, `ItemTypeApiTests` 10, `WarehouseApiTests` 5, `OperationApiTests` 4, `StockMovementBusinessRulesTests` 9, `StockMovementTimestampTests` 4, `Lot3StockModelTests` 8 |
| Migration | `20_lot4_assignments.sql` appliquée puis rejouée : aucun reset, aucune donnée supprimée |

Le Lot 4 avait ajouté la couverture du cycle de vie des affectations (création, retour, void motivé, archivage, restauration), de la cohérence entre les deux préfixes, du rejet des articles affectés sur `IN`/`TRANSFER`/`DESTRUCTION`, de l'archivage et de la restauration des référentiels, et des 404 sur UUID inconnu. Cette couverture a été retirée avec la suite ; les comportements décrits restent vérifiés par lecture de code et par la suite de conformité, qui contrôle les colonnes, contraintes et index qui les portent. Restent non prouvés : la tenancy A/B, la concurrence réelle et l'idempotence.

### 1.6. État après la réduction de la suite (22 octobre 2026)

| Indicateur | Valeur |
|---|---|
| Suites de tests | 6 |
| Tests exécutés | 38 |
| Succès | 38 |
| Échecs | 0 |
| Erreurs | 0 |
| Ignorés | 0 |
| Commande | `.\mvnw.cmd -B -q test` |
| Durée | quelques secondes : aucun contexte Spring dans les tests de conformité |

| Classe | Tests | Objet |
|---|---|---|
| `DatabaseSchemaConformanceTests` | 13 | Tables lues par les DAO, colonnes exposées par l'API, types énumérés Java ↔ PostgreSQL, types des colonnes du cycle de vie, colonnes d'archivage, tables du stock, clés étrangères, index unique partiel, application des règles de void, tables applicatives, clés primaires, tables de la base ↔ scripts |
| `SchemaScriptsConformanceTests` | 5 | Inclusion de tous les scripts par le runner, absence d'opération destructive, ordre et unicité des scripts, tables ciblées par les données de démonstration, contenu de la migration Lot 4 |
| `QrScanApiTests` | 13 | Résolution enrichie, absence de fuite, 404 inconnu, 404 code d'une autre entreprise, journal (résolu et 404), code QR immuable, cache et 304, étiquette, planche, code vide, code au format UUID |
| `QrScanRateLimiterTests` | 4 | Limite de débit, dépassement, cloisonnement par utilisateur, désactivation |
| `SssmBackendApplicationTests` | 1 | Démarrage du contexte Spring |
| `PingControllerTest` | 1 | Endpoint technique `/ping` |

La suite de conformité s'exécute sur la base de démonstration et n'y écrit que ce qu'elle annule (transaction annulée après chaque tentative d'insertion invalide).

---

## 2. Nature des tests

Les tests sont des tests d'intégration Spring Boot :

- `@SpringBootTest` ;
- `MockMvc` avec la chaîne de filtres Spring Security ;
- accès direct à `DatabaseConnection` ;
- base PostgreSQL réelle `sssm_db` ;
- création d'un jeton synthétique avec `JwtService.createToken(..., List.of("ADMIN"))` ;
- données de démonstration lues dans les tables ;
- nettoyage par `DELETE` SQL avec des marqueurs `phase5-*`.

Ils ne sont pas des tests unitaires isolés. Ils dépendent :

- d'une base accessible ;
- d'un schéma initialisé ;
- de données de démonstration ;
- d'un état de base non parallèle ;
- des droits SQL de l'utilisateur de connexion.

---

## 3. Hypothèses et skip

`ItemApiTests.setUp` cherche :

- une entreprise ;
- un type ;
- une catégorie ;
- un entrepôt ;
- un article ;
- un QR Code.

Puis :

```java
Assumptions.assumeTrue(
  companyId != null && itemTypeId != null && ...,
  "base initialisée avec des données de démonstration requise");
```

Conséquences :

- sur une base vide, les tests sont **ignorés**, pas échoués ;
- un rapport avec 0 échec peut donc signifier « rien n'a été testé » ;
- le dernier rapport indique 0 ignoré, ce qui suggère que la base de démonstration était présente ;
- ce mécanisme n'est pas un substitut à des fixtures explicites.

---

## 4. Couverture par domaine (état avant la réduction de la suite, 22 octobre 2026)

Le tableau ci-dessous décrit la suite exécutée jusqu'au 22 octobre 2026. Les classes correspondantes ont été supprimées : la couverture décrite n'est plus outillée.

| Domaine | Classes | Ce qui est couvert | Ce qui ne l'est pas |
|---|---|---|---|
| Articles | `ItemApiTests` | CRUD, QR, image, filtres, bulk partiel, historique par article, archivage et restauration, 404 sur UUID inconnu | cross-company, doublons, suppression référencée, historique inconnu, N+1 |
| Types | `ItemTypeApiTests` | CRUD, liste par catégorie, archivage et restauration | omission de `unit`/quantités, doublons |
| Catégories | `ItemCategoryApiTests` | CRUD nominal, archivage et restauration | doublons, suppression utilisée |
| Entrepôts | `WarehouseApiTests` | CRUD nominal, archivage, restauration, 404 sur UUID inconnu | doublons de nom, suppression référencée, cross-company |
| Mouvements | `StockMovementBusinessRulesTests`, `StockMovementTimestampTests` | effets d'état, dates, `OUT`/`RETURN` raccordés aux affectations, refus sur article affecté | quantité multi-unités, patch/delete, historique |
| Affectations (canoniques) | `ItemAssignmentStateTests`, `ItemAssignmentTimestampTests` | machine à états, retour, void, dates | employé inactif, cross-company, concurrence |
| Affectations (contrat et façade) | `AssignmentApiTests` | création, retour, void motivé, archivage, restauration, parité des deux préfixes, 404/409 | idempotence, course sur l'index unique |
| Comptages | `InventoryCountScanCloseTests` | scan, doublon, clôture sans corps | réconciliation, `last_inventory_at`, `completed_at`, cross-company |
| Opérations | `OperationApiTests` | en-têtes, type manquant, soft error | lignes d'articles, mouvements, type déduit |
| QR | `QrScanApiTests`, `ItemApiTests` | résolution connue/inconnue, PNG | cross-company, code modifié, code vide, journal |
| Dashboard | `DashboardApiTests` | compteurs, low-stock | stock réel, tenant, cohérence |
| Rapports | `ReportApiTests` | filtres item/entrepôt | dates, type, acteur, agrégats, tri |
| Auth | `WhoAmIApiTests`, `RefreshTokenApiTests`, `AppUserApiTests`, `AuthHardeningApiTests` | whoami, refresh rotatif, logout/révocation, inscription fermée, scope entreprise, RBAC inventaire/utilisateurs/rôles | login invalide, compte inactif, deux entreprises réelles, CORS, rate limit |
| Erreurs | `ErrorFormatTests` | enveloppe et quelques cas | SQLSTATE en production, codes métier, soft errors |
| Contexte | `SssmBackendApplicationTests` | démarrage | configuration prod |

---

## 5. Tests qui figent des comportements douteux

Ces tests ne sont pas des bugs en eux-mêmes, mais ils **acceptent** un comportement qui devrait changer. Ils devront être réécrits avec les corrections.

| Test | Comportement figé | Problème |
|---|---|---|
| `AssignmentApiTests.getUnknownReturnsSoftErrorIn200` | 200 + `errors` | Un frontend standard croit à un succès. |
| `AssignmentApiTests.emptyBodyReturnsSoftErrorIn200` | 200 + `errors` | Même problème. |
| `AssignmentApiTests.missingRequiredColumnReturns500Envelope` | 500 `23502` | Devrait être 400/422. |
| `AssignmentApiTests.returnRecordsCondition` | retour 200 sans `returned_at` | L'article reste `ASSIGNED`. |
| `OperationApiTests.missingOperationTypeReturns500Envelope` | 500 `23502` | La route devrait déduire le type ou renvoyer 400. |
| `OperationApiTests.emptyBodyReturnsSoftErrorIn200` | 200 + `errors` | Même problème. |
| `ItemTypeApiTests.lowStockStubReturnsEmptyArray` | `[]` | Consacre un stub comme fonctionnalité. |
| `ItemTypeApiTests.itemsByTypeStubReturnsEmptyArray` | `[]` | `typeId` ignoré. |
| `ItemTypeApiTests.stocksByTypeStubReturnsEmptyArray` | `[]` | Aucun stock. |
| `DashboardApiTests.summaryReturnsArrayOfCounters` | tableau d'un objet | Contrat non standard. |
| `ItemAssignmentTimestampTests.createWithoutAssignedAtAppliesDatabaseDefault` | `assigned_at` non nul depuis le Lot 2 | Corrigé : la réponse relit la ligne persistée. |
| `StockMovementTimestampTests.createWithoutMovementDateAppliesDatabaseDefault` | `movement_date` non nul depuis le Lot 2 | Corrigé : la réponse relit la ligne persistée. |
| `InventoryCountScanCloseTests.closeWithoutBodyCompletesCount` | `completed_at` non vérifié | Clôture sans date. |
| `ItemApiTests.listWithoutTokenIsRejected` | n'importe quel 4xx | Ne vérifie pas 401. |
| `ItemApiTests.createWithoutOptionalRelationsSucceeds` | `category_id` absent | Le champ n'existe pas dans `ItemRest` ; assertion faible. |

---

## 6. Divergence code / test sur l'historique (historique)

`ItemApiTests.historyUnknownReturnsNotFound` n'existe plus dans l'arbre courant. Le constat ci-dessous décrit l'état historique du Lot 1 ; l'anomalie a été corrigée au Lot 7 (`P1-12` : 404, pagination et filtres, test `HttpContractApiTests`).

L'ancien test attendait :

```java
get("/api/inventory/items/" + UUID.randomUUID() + "/history")
  .andExpect(status().isNotFound());
```

Or `ItemController.getItemHistory` :

- ne charge pas l'article ;
- appelle `getStockMovements(1, 100)` ;
- appelle `getItemAssignments(1, 100)` ;
- filtre en mémoire ;
- renvoie toujours 200 avec `movements` et `assignments`.

Aucune exception 404 n'est levée.

**Conclusion :** l'exécution complète du 21 octobre 2026 avait confirmé cette divergence avec 200 au lieu de 404. Le test a depuis été retiré de l'arbre. Le contrôleur a été corrigé au Lot 7 : `GET /api/v1/inventory/items/{id}/history` renvoie 404 pour un article inconnu, pagine et filtre les collections, et `HttpContractApiTests` couvre le cas négatif (`P1-12` corrigé).

---

## 7. Faiblesses structurelles des tests

### 7.1. Isolation partielle

- en local, base réelle partagée `sssm_db` ;
- en CI, un service PostgreSQL 16 éphémère, mais unique pour toute la suite ;
- pas de `@Transactional` de test ;
- pas de rollback ;
- nettoyage par marqueurs ;
- pas de Testcontainers ;
- pas de profil de test dédié ;
- pas de fixtures par test.

Deux exécutions parallèles ou un test interrompu peuvent laisser des données.

### 7.2. Dépendance aux données de démonstration

Les tests supposent :

- une entreprise ;
- un type ;
- une catégorie ;
- un entrepôt ;
- un article.

Ils ne créent pas leurs propres fixtures complètes. Un environnement neuf les ignore.

### 7.3. Jeton synthétique

Les tests historiques créent encore un jeton synthétique :

```java
jwtService.createToken("test-user", "phase5-tester", List.of("ADMIN"))
```

Les nouveaux tests d'authentification passent en revanche par `/login` avec de vrais comptes, vérifient l'entreprise du jeton, la rotation du refresh et la révocation au logout.

Conséquences restantes :

- le rôle `USER` n'est pas testé sur toutes les routes métier ;
- l'absence de rôle n'est pas testée sur les routes hors inventaire ;
- l'isolation entre deux entreprises réelles n'est pas testée (une seule entreprise en base) ;
- les routes `ADMIN` historiques restent testées avec un jeton synthétique.

### 7.4. Pas de concurrence

Aucun test ne lance deux requêtes simultanées sur :

- la même affectation ;
- le même mouvement ;
- le même comptage ;
- la même mise à jour d'article.

Les problèmes de transaction et d'index uniques ne sont donc pas détectés.

### 7.5. Pas de test de performance

Aucun test ne mesure :

- le nombre de requêtes SQL ;
- le temps de réponse d'une liste ;
- le comportement avec 1 000 articles ;
- le N+1 ;
- l'absence de pool.

### 7.6. Couverture de sécurité partielle

Depuis le Lot 1, les tests vérifient :

- l'inscription publique refusée ;
- l'inscription sans permission refusée ;
- l'inscription dans l'entreprise du jeton ;
- la lecture et la liste des utilisateurs filtrées par entreprise ;
- le catalogue global des permissions protégé ;
- l'inventaire soumis à permission ;
- le login valide via les tests de refresh ;
- la rotation et la révocation du refresh ;
- le logout avec révocation de l'access token ;
- les claims `company_id`, `roles` et `permissions`.

Ils ne vérifient pas encore :

- un mot de passe incorrect ;
- un compte inactif ;
- un verrouillage automatique ;
- deux entreprises réelles ;
- le CORS ;
- le rate limiting ;
- la signature avec une mauvaise clé ;
- la purge des jetons.

---

## 8. Ce que les tests prouvent vraiment

Ils prouvent :

- le contexte Spring démarre ;
- les routes principales répondent ;
- le CRUD nominal fonctionne sur la base de démonstration ;
- les effets d'état des mouvements sont ceux codés ;
- l'image QR est un PNG ;
- les erreurs SQL sont bien enveloppées ;
- le refresh et whoami fonctionnent dans leurs cas nominaux ;
- l'admin peut créer un utilisateur ;
- l'inscription publique est fermée ;
- le refresh tourne et l'ancien token est rejeté ;
- le logout révoque les deux jetons ;
- les utilisateurs et rôles sont filtrés par entreprise ;
- l'inventaire exige une permission.

Ils ne prouvent pas :

- l'isolation multi-entreprise complète (DAO d'inventaire non filtrés) ;
- le contrôle d'accès métier sur les modules hors inventaire ;
- la fiabilité du stock ;
- la cohérence des affectations ;
- la réconciliation des comptages ;
- l'atomicité des écritures ;
- la concurrence ;
- la performance ;
- la sécurité de production (rate limit, CORS réel, HTTPS) ;
- la conformité du contrat frontend.

---

## 9. Fiabilité du build

Le projet utilise :

- Maven ;
- `fmt-maven-plugin` 2.9.1 ;
- `maven-compiler-plugin` avec Java 17 ;
- `spring-boot-maven-plugin`.

Un workflow GitHub Actions existe : `.github/workflows/ci.yml`. Il démarre un service PostgreSQL 16, initialise `docs/database/sssm_schema2_v01.sql` et les données de `docs/database/mock_data/sssm_mock_data.sql`, puis exécute `./mvnw -B -q test`, un lint Java/OpenAPI et un packaging. C'est un acquis réel : le CI ne dépend pas de la base locale du développeur.

Ses limites sont cependant structurantes :

- une seule base pour toute la suite, sans rollback ni Testcontainers ;
- dépendance aux données de démonstration ;
- tests RBAC et scope entreprise présents, mais pas de scénario à deux entreprises réelles ;
- aucun test de transaction, concurrence ou réconciliation ;
- aucun seuil de couverture ;
- la suite est verte, mais elle ne couvre que le schéma, le QR, les comptages et le contrat HTTP : la tenancy générale, la concurrence, la matrice RBAC et l'archivage restent non testés (`P1-13`) ; l'historique article est désormais couvert (`P1-12` corrigé).

Une suite verte localement ou en CI ne signifie donc pas :

- que la suite passera sur une base réellement vide ;
- qu'elle passera en parallèle ;
- qu'elle passera sans données de démonstration ;
- qu'elle détectera une régression de tenancy ou de stock.

---

## 10. Recommandations de test

Ces recommandations restent majoritairement des propositions. Le Lot 1 a ajouté les tests RBAC, scope entreprise et révocation ; le reste doit être planifié. Le workflow CI existant est un bon point de départ ; il doit être conservé et étendu, pas remplacé.

1. **Testcontainers PostgreSQL** pour une base isolée et reproductible.
2. **Fixtures explicites** : créer l'entreprise A et B, les utilisateurs, les rôles, les articles.
3. **Tests de tenancy** : A ne voit jamais B, en lecture comme en écriture (partiel au Lot 1 : une seule entreprise).
4. **Tests RBAC** : `USER`, `INVENTORY`, `ADMIN`, sans rôle (première couverture ajoutée au Lot 1).
5. **Tests de transaction** : provoquer une erreur après la première écriture et vérifier le rollback. **Fait au Lot 2** pour un DAO et pour le service de mouvement.
6. **Tests de concurrence** : deux affectations simultanées, deux mouvements simultanés.
7. **Tests de stock** : quantité, solde, mouvement inverse, stock négatif.
8. **Tests de comptage** : écart, réconciliation, `last_inventory_at`.
9. **Tests de contrat** : statuts, erreurs, pagination, dates non nulles.
10. **Tests de non-régression QR** : cross-company, code modifié, code vide.
11. **Tests de performance** : nombre de requêtes, seuil de latence.
12. **Tests de sécurité** : login invalide, compte inactif, révocation d'un rôle, CORS, rate limit (register, refresh et logout sont déjà couverts).

---

## 11. Verdict

La suite qui précédait la réduction était **nombreuse pour un prototype** et **utile pour les parcours nominaux** : elle couvrait l'identité et le RBAC (Lot 1), le pool, le rollback transactionnel et le mapping SQL (Lot 2), les soldes calculés, le journal immuable et les comptages (Lot 3), le cycle de vie des affectations et l'archivage (Lot 4). Elle totalisait **150 tests dans 28 suites**.

**La suite actuelle (58 tests, 8 suites) ne couvre que des périmètres ciblés.** Elle contrôle la conformité du schéma à l'application, les contrats QR, comptage et HTTP ; elle ne couvre ni la tenancy générale, ni la concurrence, ni la matrice RBAC, ni l'archivage. C'est un filet de sécurité sur les migrations et trois contrats, pas une preuve de bon fonctionnement : le rapport 15 doit être relu en conséquence, et `P1-13` reste ouvert avec un niveau de gravité plus élevé qu'avant.

Ils ne sont pas encore **fiables comme garde-fou de production** :

- ils dépendent d'une base PostgreSQL réelle (locale ou CI) et de données de démonstration ;
- ils ignorent silencieusement les cas non préparés ;
- une grande partie des tests métier historiques utilise encore un jeton `ADMIN` synthétique ;
- ils figent des comportements douteux ;
- ils ne couvrent pas encore la tenancy complète, les transactions, la concurrence, le stock ou la réconciliation ;
- une divergence code/test est confirmée sur l'historique d'article.

La priorité n'est pas d'augmenter le nombre de tests, mais de passer à une **isolation par test** (Testcontainers ou rollback), avec des **fixtures explicites** et des **scénarios de tenancy/stock/transaction**. Les critères sont dans le rapport 15.
