# Audit technique et fonctionnel — Backend SSSM

**Date de l'audit :** 21 octobre 2026, mise à jour du 22 octobre 2026 (Lots 2, 3 et 4)  
**Périmètre :** backend Spring Boot, API HTTP, PostgreSQL, modules inventaire / stock / QR Code / affectations / authentification  
**Nature :** audit statique initial, puis exécution des Lots 1 à 4 (identité et RBAC, DataSource et transactions, modèle de stock, cycle de vie des affectations et archivage)
**Statut de la mission :** audit initial et revue de cohérence terminés ; **Lots 1 à 4 exécutés** (21 et 22 octobre 2026) ; lots 5 à 9 non démarrés ; aucun frontend démarré. Le correctif QR `7e3f196` est postérieur à l'audit initial.
**Revue de cohérence :** 21 octobre 2026, mise à jour du 22 octobre 2026 — voir [`16-revue-coherence.md`](16-revue-coherence.md)

---

## 1. Objectif

Ce dossier répond à une question unique : **le backend SSSM peut-il servir de socle fiable à un frontend web et à une application mobile de gestion de matériel, aujourd'hui ?**

L'audit ne se limite pas à vérifier que les routes répondent. Il vérifie également :

- la cohérence entre le code Java, le schéma PostgreSQL, les scripts SQL et les spécifications OpenAPI ;
- la solidité des règles métier (stock, mouvements, affectations, QR Code, comptages) ;
- l'isolation entre entreprises, les droits d'accès et la sécurité des échanges ;
- la capacité des tests à détecter les régressions ;
- les contrats API réellement utilisables par un frontend.

Le verdict global et les actions prioritaires sont dans [`00-resume-executif.md`](00-resume-executif.md). Les preuves détaillées sont réparties dans les rapports spécialisés.

---

## 2. Périmètre inclus

| Domaine | Éléments examinés |
|---|---|
| Cartographie | packages, contrôleurs, services, DAO, DTO, mappers, configuration, dépendances |
| API | 27 contrôleurs REST, 147 routes applicatives après le Lot 4 (130 à l'audit initial, 131 après le Lot 1), codes HTTP, validation, erreurs, pagination |
| Base de données | schéma `security`, scripts `00_init.sql` à `20_lot4_assignments.sql`, contraintes, index, triggers |
| Catalogue | catégories, types de matériel, entrepôts, articles |
| Stock | mouvements, opérations, comptages, tableau de bord, rapports |
| QR Code | génération, image PNG, résolution, scan |
| Affectations | un contrat canonique (`item_assignment`) et une façade dépréciée (`assignment`) |
| Authentification | inscription, connexion, JWT, refresh, rôles, permissions, CORS |
| Tests | 4 suites / 20 tests après la réduction du 22 octobre 2026 : conformité schéma ↔ application |
| OpenAPI | 26 fichiers YAML, dont 12 pour l'inventaire |

## 3. Périmètre exclu

- Les frontends web et mobile : aucun code frontend n'a été créé ni modifié.
- Les environnements de production, de recette et de préproduction.
- Les données réelles d'exploitation : seuls des comptages agrégés et des contrôles de structure ont été effectués sur la base locale de démonstration.
- Les tests de charge, de pénétration et de concurrence réelle.
- Les sauvegardes, la haute disponibilité, le monitoring, le déploiement et l'infrastructure.
- Les modules métier hors inventaire (planning, patrouilles, incidents, facturation, etc.), sauf lorsqu'ils interfèrent avec l'authentification ou les autorisations.

---

## 4. Version et environnement examinés

| Élément | Valeur constatée |
|---|---|
| Framework | Spring Boot `4.1.1` |
| Langage | Java `17` (`maven.compiler.release=17`) |
| Accès base | JDBC direct, driver PostgreSQL `42.7.13` |
| Sécurité | Spring Security, JJWT `0.12.6`, BCrypt |
| QR Code | ZXing `3.5.3` |
| Build | Maven, plugin de formatage `fmt-maven-plugin` `2.9.1` |
| Base contrôlée | `sssm_db`, schéma `security` ; lecture seule pour l'audit, migrations Lots 1, 3 et 4 appliquées ensuite, tests des Lots 2 à 4 avec nettoyage des marqueurs |
| Rapports de tests | Exécution du 22 octobre 2026 après réduction de la suite : 20 tests, 20 succès, 0 échec, 4 suites. La suite fonctionnelle de 150 tests a été supprimée. |
| Documentation API | `docs/api/**/*.yaml` — 26 fichiers |
| Scripts SQL | `docs/database/schema/00_init.sql` à `20_lot4_assignments.sql` — 21 fichiers |

Le dépôt est initialisé en tant que dépôt Git. L'audit initial correspond au commit `a3f7c56` (`docs(audit): add backend readiness and security audit`). Le code a ensuite évolué avec le commit `7e3f196` (`fix(inventory): guarantee ItemError envelope on QR code image endpoint`), qui ajoute notamment `ItemControllerQrCodeImageTest`. La revue de cohérence porte sur ce HEAD `7e3f196`. Le Lot 1 a ensuite été exécuté sur le HEAD `13bb79b` (`docs(audit): reconcile reports with current HEAD`), sans commit de correction à ce stade.

---

## 5. Méthode

1. **Lecture statique** des contrôleurs, services, DAO, DTO, mappers, filtres de sécurité et fichiers de configuration.
2. **Lecture croisée** du schéma PostgreSQL, des scripts SQL, des index, des triggers et des spécifications OpenAPI.
3. **Vérifications en lecture seule** sur la base locale : tables, colonnes, contraintes, index, triggers, valeurs d'énumérations et comptages agrégés.
4. **Analyse des rapports de tests existants** dans `target/surefire-reports`, puis **exécution complète** de la suite après les Lots 1, 2, 3 et 4.
5. **Consolidation** des anomalies, de leur impact et de leur priorité, puis rédaction des propositions et des critères de validation.
6. **Exécution du Lot 1** : migration additive, code d'authentification/RBAC, tests ciblés et mise à jour des rapports.
7. **Exécution du Lot 2** : DataSource/HikariCP, transactions effectives, mapping SQL, défauts et réponses de création, tests de non-régression et mise à jour des rapports.
8. **Exécution du Lot 3** : modèle de stock calculé, journal append-only, opérations et comptages, migration `19_lot3_stock.sql` et mise à jour des rapports.
9. **Exécution du Lot 4** : cycle de vie des affectations, contrat canonique et façade dépréciée, archivage logique, migration `20_lot4_assignments.sql` et mise à jour des rapports.

L'audit initial n'a effectué aucun test destructif, aucune migration, aucune insertion, aucune modification de données. Le Lot 1 a appliqué la migration `18_lot1_security.sql` à la base de démonstration (colonnes, backfill, index, tables de jetons, permissions) et exécuté les tests ; aucun reset ni suppression de données n'a été effectué. Le Lot 2 n'a appliqué **aucune migration** : il réutilise le schéma courant et n'a supprimé aucune donnée. Les Lots 3 et 4 ont appliqué des migrations **strictement additives** (`19_lot3_stock.sql`, `20_lot4_assignments.sql`), rejouées pour vérifier leur idempotence ; aucune suppression, aucun `TRUNCATE` et aucun reset n'ont été effectués.

---

## 6. Index des rapports

| Fichier | Objectif | Niveau de preuve |
|---|---|---|
| [`00-resume-executif.md`](00-resume-executif.md) | Verdict, réponses aux questions, risques majeurs, cinq actions prioritaires | Synthèse |
| [`01-cartographie-backend.md`](01-cartographie-backend.md) | Architecture, packages, couches, dépendances, flux d'une requête | Statique |
| [`02-audit-api-endpoints.md`](02-audit-api-endpoints.md) | Inventaire complet des routes, statuts, validation, erreurs, contrats | Statique + tests |
| [`03-audit-modele-donnees-postgresql.md`](03-audit-modele-donnees-postgresql.md) | Tables, colonnes, contraintes, index, triggers, écarts code/schéma | Statique + base |
| [`04-audit-catalogue-materiels.md`](04-audit-catalogue-materiels.md) | Catégories, types, entrepôts, articles, filtres, intégrité | Statique + tests |
| [`05-audit-stock-inventaire.md`](05-audit-stock-inventaire.md) | Quantités, stock réel, opérations, comptages, tableau de bord | Statique + base |
| [`06-audit-qrcode.md`](06-audit-qrcode.md) | Génération, PNG, unicité, scan, limites pour le mobile | Statique + tests |
| [`07-audit-mouvements-stock.md`](07-audit-mouvements-stock.md) | Règles IN/OUT/TRANSFER/RETURN/DESTRUCTION/ADJUSTMENT, journal, erreurs | Statique + tests |
| [`08-audit-affectations-employes.md`](08-audit-affectations-employes.md) | Cycle de vie des affectations, contrat canonique et façade dépréciée, restitution, archivage — état après le Lot 4 | Statique + tests |
| [`09-audit-authentification-securite.md`](09-audit-authentification-securite.md) | Inscription, JWT, refresh, RBAC, CORS, fuites d'erreurs — état initial et corrections du Lot 1 | Statique + tests |
| [`10-audit-architecture-api-frontend.md`](10-audit-architecture-api-frontend.md) | Adéquation de l'API aux frontends web/mobile, conventions, performance | Statique |
| [`11-audit-tests-fiabilite.md`](11-audit-tests-fiabilite.md) | Couverture réelle, limites, tests qui figent des comportements douteux | Rapports + code |
| [`12-anomalies-risques-priorites.md`](12-anomalies-risques-priorites.md) | Registre priorisé des anomalies et matrice de risque | Consolidation |
| [`13-endpoints-manquants-proposes.md`](13-endpoints-manquants-proposes.md) | Contrats proposés, non implémentés, pour combler les manques | Proposition |
| [`14-plan-corrections.md`](14-plan-corrections.md) | Plan par phases, dépendances, critères de sortie | Proposition |
| [`15-criteres-validation-api.md`](15-criteres-validation-api.md) | Critères testables pour déclarer l'API prête | Proposition |
| [`16-revue-coherence.md`](16-revue-coherence.md) | Revue de cohérence, matrice de preuves, contradictions, tableau final des anomalies et ordre de correction | Consolidation |

---

## 7. Conventions de statut

Les statuts suivants sont utilisés dans tous les rapports :

| Statut | Signification |
|---|---|
| **IMPLÉMENTÉ / VÉRIFIÉ** | Le comportement est présent dans le code et couvert par un test automatisé ou une vérification en lecture seule. |
| **IMPLÉMENTÉ / NON VÉRIFIÉ** | Le code est présent, mais aucun test ni aucune exécution n'a confirmé le comportement. |
| **PARTIEL** | Le chemin nominal existe, mais des cas métier, des contrôles ou des erreurs sont absents. |
| **ABSENT** | Aucune route, aucun champ ou aucune logique correspondante n'existe. |
| **BLOQUÉ** | La vérification dépend de données ou d'un environnement non disponibles. |
| **PROPOSÉ** | Spécification de correction ou d'endpoint, non implémentée. |

Les priorités utilisées dans le registre sont :

- **P0** : bloque la mise en production ou expose immédiatement les données ;
- **P1** : rend le métier faux ou dangereux à moyen terme ;
- **P2** : dégrade l'exploitation, la maintenabilité ou l'expérience utilisateur ;
- **P3** : amélioration de confort ou de documentation.

---

## 8. Limites de l'audit

- **La suite complète a été exécutée après le Lot 4** : 150 tests, 150 succès, 0 échec, 0 erreur, 0 ignoré, 28 suites ; elle a depuis été réduite le 22 octobre 2026 à **20 tests de conformité schéma** (4 suites). L'ancien échec `ItemApiTests.historyUnknownReturnsNotFound` n'existe plus dans l'arbre ; l'anomalie fonctionnelle de l'historique reste ouverte (`P1-12`). Les résultats historiques (105 tests, puis 120 après le Lot 1, 132 après le Lot 2, 140 après le Lot 3) proviennent d'exécutions antérieures et ne décrivent plus l'état courant.
- **L'isolation multi-entreprise n'a pas pu être testée en exécution** : la base de démonstration ne contient qu'une entreprise. Le Lot 1 applique l'hypothèse mono-entreprise et ajoute le scope entreprise sur les utilisateurs et rôles, mais les DAO d'inventaire ne filtrent pas encore par `company_id`.
- **Aucun test de concurrence réel** n'a été exécuté. Les conclusions sur les transactions et les verrous sont déduites du code et du mode de connexion JDBC.
- **Aucun scan SAST/DAST, test de pénétration ou audit d'infrastructure** n'a été réalisé.
- **Les secrets réels n'ont pas été lus.** Les conclusions portent sur la présence de valeurs par défaut et sur la configuration, pas sur la divulgation d'un secret d'exploitation.
- **OpenAPI** a été vérifié structurellement et linté avec la version de la CI (`@redocly/cli@2.65.1`) au Lot 4 : **179 avertissements, 0 erreur** sur 26 fichiers (183 à l'audit initial). Les avertissements restants concernent les opérations antérieures et ne sont pas bloquants ; ils restent signalés comme dette de documentation.
- **Les propositions** des fichiers 13 à 15 sont des spécifications. Elles ne constituent ni du code livré, ni un engagement de délai.

---

## 9. Verdict en une phrase

**Le backend n'est pas prêt pour la production.** Il est partiellement utilisable pour un prototype mono-entreprise, en lecture et CRUD encadré. Le Lot 1 a fermé l'inscription publique, ajouté le RBAC sur l'inventaire et l'administration, et durci les jetons (refresh rotatif, révocation, logout). Le Lot 2 a rétabli un pool Spring/HikariCP, des transactions effectives pour les cas d'usage existants, des erreurs SQL maîtrisées et des créations relues depuis la base. Le Lot 3 a posé un stock calculé, un journal immuable et des comptages réconciliés ; le Lot 4 a unifié le contrat d'affectation, fiabilisé son cycle de vie et rendu l'archivage logique effectif. Il ne permet pas encore de garantir une isolation multi-entreprise complète, la concurrence et l'idempotence, ni un RBAC sur tous les modules métier.

Le détail de ce verdict, ses nuances et les cinq actions à mener avant tout frontend sont dans [`00-resume-executif.md`](00-resume-executif.md). Les preuves sont dans les rapports 01 à 11, la revue de cohérence dans le rapport 16, et les corrections proposées dans les rapports 12 à 15.

---

## 10. Ordre de lecture recommandé

1. [`00-resume-executif.md`](00-resume-executif.md) — décision et priorités.
2. [`01-cartographie-backend.md`](01-cartographie-backend.md) — comprendre l'architecture.
3. [`02-audit-api-endpoints.md`](02-audit-api-endpoints.md) — connaître le contrat réel.
4. [`03-audit-modele-donnees-postgresql.md`](03-audit-modele-donnees-postgresql.md) — comprendre les données.
5. Rapports 04 à 09 — dettes métier et sécurité.
6. Rapports 10 et 11 — adéquation frontend et fiabilité.
7. Rapports 12 à 15 — risques, propositions, plan, critères d'acceptation.
8. [`16-revue-coherence.md`](16-revue-coherence.md) — vérification croisée, contradictions, tableau final et ordre corrigé.

---

## 11. Déclaration de modification

L'**audit initial et la revue de cohérence** n'ont modifié aucun fichier Java applicatif, test, script SQL, migration, configuration, dépendance, donnée ou fichier OpenAPI. Leurs seuls fichiers créés ou modifiés étaient les Markdown de ce dossier.

Le **Lot 1**, exécuté après la revue, a modifié :

- des fichiers Java d'authentification, de sécurité, d'utilisateurs et de rôles ;
- des tests d'authentification et de RBAC ;
- la configuration `application.properties` / `application-prod.properties` ;
- le runner SQL, les données de démonstration et la migration additive `18_lot1_security.sql` ;
- les rapports `docs/audit/` concernés et `docs/database/README.md`.

Aucune dépendance n'a été ajoutée. Aucun fichier OpenAPI n'a été modifié. Aucun reset, `DROP`, suppression de colonne ou `NOT NULL` destructif n'a été exécuté.

Le **Lot 2**, exécuté le 22 octobre 2026, a modifié :

- `DatabaseConnection`, `ErrorMapper`, `ItemError` et `GlobalExceptionHandler` pour le DataSource, les transactions et le mapping SQL ;
- les DAO de type d'article, d'employé, de mouvement et d'affectation pour les défauts et la relecture des créations ;
- les services de type, d'employé et de catégorie pour les annotations transactionnelles ;
- `application.properties` pour le pool HikariCP ;
- des tests de pool, transaction, défauts, doublons, FK et mapping SQL ;
- les rapports `docs/audit/` concernés.

Aucune dépendance n'a été ajoutée, aucune migration SQL n'a été appliquée et aucun fichier OpenAPI n'a été modifié. La concurrence (`version`/`If-Match`, `Idempotency-Key`) et `P0-01` n'ont pas été traités.

Le **Lot 3**, exécuté le 22 octobre 2026, a modifié :

- la migration additive `19_lot3_stock.sql` et l'orchestrateur `sssm_schema2_v01.sql` ;
- le module `stock_balance` et l'API `GET /api/inventory/stock-balances` ;
- les services de mouvement, d'opération, de comptage, d'article et le dashboard low-stock ;
- les DTO, DAO, mappers et contrôleurs associés ;
- `Lot3StockModelTests` et les tests existants adaptés.

La suite complète est verte : **140 tests, 0 échec, 0 erreur, 0 ignoré, 28 suites**. La migration a été appliquée puis rejouée sans écraser les soldes existants. Aucune dépendance n'a été ajoutée et aucun fichier OpenAPI n'a été modifié. `P0-01`, `P0-06` et `P1-14` restaient ouverts à l'issue du Lot 3 : la tenancy, les affectations et la concurrence n'étaient pas couvertes par le Lot 3.

Le **Lot 4**, exécuté le 22 octobre 2026, a modifié :

- la migration additive `20_lot4_assignments.sql` et l'orchestrateur `sssm_schema2_v01.sql` ;
- le module `item_assignment` (DTO, DAO, mapper, service, contrôleur) et la façade `assignment` devenue dépréciée ;
- le service de mouvement (`OUT`/`RETURN` raccordés aux affectations, refus sur article affecté) et le DTO de création de mouvement (`post_id`) ;
- les services, DAO et contrôleurs d'article, de catégorie, de type et d'entrepôt (archivage logique, restauration, filtres de listes) ;
- l'historique d'article (lectures par article) et la validation des UUID ;
- les tests du cycle de vie des affectations et les tests existants adaptés ;
- les spécifications OpenAPI `item_assignment`, `assignment`, `stock_movement`, `item`, `item_category`, `item_type` et `warehouse` ;
- les rapports `docs/audit/` concernés.

La suite complète était verte à cette date : **150 tests, 0 échec, 0 erreur, 0 ignoré, 28 suites** (suite depuis réduite à 20 tests de conformité schéma). La migration a été appliquée puis rejouée, sans suppression de ligne ni de colonne. Aucune dépendance n'a été ajoutée. `P0-06`, `P1-05` et `P1-11` sont corrigés dans le périmètre documenté ; `P0-01` (tenancy) et `P1-14` (concurrence et idempotence) restent ouverts, ainsi qu'une affectation active antérieure de la base de démonstration incohérente avec le statut de son article, laissée en l'état.

Le Lot 5 et tout démarrage de frontend doivent faire l'objet d'un accord explicite distinct.
