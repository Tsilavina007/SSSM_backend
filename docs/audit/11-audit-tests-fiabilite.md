# 11 — Audit des tests et de la fiabilité

**Objectif :** évaluer ce que les tests prouvent réellement, ce qu'ils ne prouvent pas, et dans quelle mesure ils peuvent servir de garde-fou avant correction. La suite complète a été exécutée le 21 octobre 2026 après le Lot 1 : **120 tests, 119 succès, 1 échec préexistant**.

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
| Divergence connue | `ItemApiTests.historyUnknownReturnsNotFound` attend 404 ; `ItemController` renvoie 200 avec des listes vides |

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
| Échec | `ItemApiTests.historyUnknownReturnsNotFound` : 404 attendu, 200 obtenu |

Le HEAD de travail n'est plus `7e3f196` : le Lot 1 ajoute `AuthHardeningApiTests` (7 tests) et un test de rotation/révocation dans `RefreshTokenApiTests` (5 tests). Les 29 tests ciblés d'authentification et de RBAC passent. L'unique échec est une divergence **préexistante** du contrôleur d'historique, reproduite sur le HEAD initial et hors périmètre du Lot 1.

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

## 4. Couverture par domaine

| Domaine | Classes | Ce qui est couvert | Ce qui ne l'est pas |
|---|---|---|---|
| Articles | `ItemApiTests` | CRUD, QR, image, filtres, bulk partiel, historique nominal | cross-company, doublons, suppression référencée, historique inconnu, N+1 |
| Types | `ItemTypeApiTests` | CRUD, liste par catégorie, stubs vides | omission de `unit`/quantités, doublons, archived |
| Catégories | `ItemCategoryApiTests` | CRUD nominal | doublons, suppression utilisée, archived |
| Entrepôts | `WarehouseApiTests` | CRUD nominal, suppression inconnue | doublons de nom, suppression référencée, cross-company |
| Mouvements | `StockMovementBusinessRulesTests`, `StockMovementTimestampTests` | effets d'état, dates | quantité, source/destination, acteur, patch/delete, historique |
| Affectations typées | `ItemAssignmentStateTests`, `ItemAssignmentTimestampTests` | état, retour, date | suppression, returned_at à la création, employé inactif, cross-company |
| Affectations génériques | `AssignmentApiTests` | liste active, création, patch notes, retour condition, soft errors | no-op réel, `returned_at`, suppression, colonnes arbitraires |
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
| `ItemAssignmentTimestampTests.createWithoutAssignedAtAppliesDatabaseDefault` | `assigned_at: null` en réponse | Le client doit recharger. |
| `StockMovementTimestampTests.createWithoutMovementDateAppliesDatabaseDefault` | `movement_date: null` en réponse | Même problème. |
| `InventoryCountScanCloseTests.closeWithoutBodyCompletesCount` | `completed_at` non vérifié | Clôture sans date. |
| `ItemApiTests.listWithoutTokenIsRejected` | n'importe quel 4xx | Ne vérifie pas 401. |
| `ItemApiTests.createWithoutOptionalRelationsSucceeds` | `category_id` absent | Le champ n'existe pas dans `ItemRest` ; assertion faible. |

---

## 6. Divergence code / test sur l'historique

`ItemApiTests.historyUnknownReturnsNotFound` attend :

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

**Conclusion :** la suite complète exécutée le 21 octobre 2026 confirme ce diagnostic : `ItemApiTests.historyUnknownReturnsNotFound` échoue avec 200 au lieu de 404. C'est une **désynchronisation certaine entre le test et le code**, pas une preuve que le test est faux. Le contrôle d'existence a été retiré du contrôleur, ou le test a été ajouté sans être aligné. Cette anomalie est antérieure au Lot 1 et reste ouverte ; elle ne doit pas être corrigée dans le cadre de l'authentification.

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
- la divergence code/test sur l'historique est confirmée par l'exécution complète : `historyUnknownReturnsNotFound` échoue (200 au lieu de 404) et reste à corriger hors Lot 1.

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
5. **Tests de transaction** : provoquer une erreur après la première écriture et vérifier le rollback.
6. **Tests de concurrence** : deux affectations simultanées, deux mouvements simultanés.
7. **Tests de stock** : quantité, solde, mouvement inverse, stock négatif.
8. **Tests de comptage** : écart, réconciliation, `last_inventory_at`.
9. **Tests de contrat** : statuts, erreurs, pagination, dates non nulles.
10. **Tests de non-régression QR** : cross-company, code modifié, code vide.
11. **Tests de performance** : nombre de requêtes, seuil de latence.
12. **Tests de sécurité** : login invalide, compte inactif, révocation d'un rôle, CORS, rate limit (register, refresh et logout sont déjà couverts).

---

## 11. Verdict

Les tests sont **nombreux pour un prototype** et **utiles pour les parcours nominaux**. Le Lot 1 a réellement renforcé la couverture de l'identité, du RBAC et de la révocation : **29 tests ciblés passent**, et la suite complète exécutée donne **120 tests, 119 succès, 1 échec préexistant**.

Ils ne sont pas encore **fiables comme garde-fou de production** :

- ils dépendent d'une base PostgreSQL réelle (locale ou CI) et de données de démonstration ;
- ils ignorent silencieusement les cas non préparés ;
- une grande partie des tests métier historiques utilise encore un jeton `ADMIN` synthétique ;
- ils figent des comportements douteux ;
- ils ne couvrent pas encore la tenancy complète, les transactions, la concurrence, le stock ou la réconciliation ;
- une divergence code/test est confirmée sur l'historique d'article.

La priorité n'est pas d'augmenter le nombre de tests, mais de passer à une **isolation par test** (Testcontainers ou rollback), avec des **fixtures explicites** et des **scénarios de tenancy/stock/transaction**. Les critères sont dans le rapport 15.
