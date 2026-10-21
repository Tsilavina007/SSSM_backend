# Audit technique et fonctionnel — Backend SSSM

**Date de l'audit :** 21 octobre 2026  
**Périmètre :** backend Spring Boot, API HTTP, PostgreSQL, modules inventaire / stock / QR Code / affectations / authentification  
**Nature :** audit statique et vérifications en lecture seule  
**Statut de la mission :** audit initial terminé, aucune correction applicative effectuée par l'audit ou la revue, aucun frontend démarré. Le correctif QR `7e3f196` est postérieur à l'audit initial.  
**Revue de cohérence :** 21 octobre 2026 — voir [`16-revue-coherence.md`](16-revue-coherence.md)

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
| API | 26 contrôleurs REST, 130 routes applicatives, codes HTTP, validation, erreurs, pagination |
| Base de données | schéma `security`, scripts `00_init.sql` à `17_seed_permissions.sql`, contraintes, index, triggers |
| Catalogue | catégories, types de matériel, entrepôts, articles |
| Stock | mouvements, opérations, comptages, tableau de bord, rapports |
| QR Code | génération, image PNG, résolution, scan |
| Affectations | deux modules distincts (`item_assignment` et `assignment`) |
| Authentification | inscription, connexion, JWT, refresh, rôles, permissions, CORS |
| Tests | 21 classes / 105 tests lors de l'audit initial ; 22 classes / 112 tests dans l'arbre courant, suite complète non rejouée |
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
| Base contrôlée | `sssm_db`, schéma `security`, en lecture seule |
| Rapports de tests | Rapport complet historique : 105 tests, 0 échec, 0 erreur, 0 ignoré. Artefact courant : `ItemControllerQrCodeImageTest`, 7 tests, 0 échec. Aucun rapport complet pour `7e3f196`. |
| Documentation API | `doc/api/**/*.yaml` — 26 fichiers |
| Scripts SQL | `doc/database/schema/00_init.sql` à `17_seed_permissions.sql` — 18 fichiers |

Le dépôt est initialisé en tant que dépôt Git. L'audit initial correspond au commit `a3f7c56` (`docs(audit): add backend readiness and security audit`). Le code a ensuite évolué avec le commit `7e3f196` (`fix(inventory): guarantee ItemError envelope on QR code image endpoint`), qui ajoute notamment `ItemControllerQrCodeImageTest`. La revue de cohérence porte sur ce HEAD `7e3f196` et signale explicitement les constats qui datent de l'audit initial.

---

## 5. Méthode

1. **Lecture statique** des contrôleurs, services, DAO, DTO, mappers, filtres de sécurité et fichiers de configuration.
2. **Lecture croisée** du schéma PostgreSQL, des scripts SQL, des index, des triggers et des spécifications OpenAPI.
3. **Vérifications en lecture seule** sur la base locale : tables, colonnes, contraintes, index, triggers, valeurs d'énumérations et comptages agrégés.
4. **Analyse des rapports de tests existants** dans `target/surefire-reports`, sans relancer la suite.
5. **Consolidation** des anomalies, de leur impact et de leur priorité, puis rédaction des propositions et des critères de validation.

Aucun test destructif, aucune migration, aucune insertion, aucune modification de données n'a été effectué pendant cette passe.

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
| [`08-audit-affectations-employes.md`](08-audit-affectations-employes.md) | Deux modules d'affectation, cohérence d'état, restitution, suppressions | Statique + tests |
| [`09-audit-authentification-securite.md`](09-audit-authentification-securite.md) | Inscription, JWT, refresh, RBAC, CORS, fuites d'erreurs | Statique + tests |
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

- **Aucune suite complète n'a été relancée pendant l'audit initial ni pendant la revue.** Les résultats complets cités (105 tests) proviennent d'un rapport Surefire historique. L'artefact encore présent dans `target/surefire-reports` ne contient que les 7 tests QR de `ItemControllerQrCodeImageTest`, exécutés lors du correctif. Aucune exécution complète n'a été faite sur `7e3f196`, et le test `ItemApiTests.historyUnknownReturnsNotFound` attend un 404 alors que le contrôleur courant renvoie 200. L'état de la suite complète sur HEAD est donc **NON VÉRIFIÉ**.
- **L'isolation multi-entreprise n'a pas pu être testée en exécution** : la base de démonstration ne contient qu'une entreprise et il était interdit de créer des données.
- **Aucun test de concurrence réel** n'a été exécuté. Les conclusions sur les transactions et les verrous sont déduites du code et du mode de connexion JDBC.
- **Aucun scan SAST/DAST, test de pénétration ou audit d'infrastructure** n'a été réalisé.
- **Les secrets réels n'ont pas été lus.** Les conclusions portent sur la présence de valeurs par défaut et sur la configuration, pas sur la divulgation d'un secret d'exploitation.
- **OpenAPI** a été vérifié structurellement. Les 183 avertissements Redocly relevés lors de l'audit initial ne sont pas corrigés dans ce dossier ; ils sont signalés comme dette de documentation. Ce décompte n'a pas été rejoué après le commit `7e3f196`.
- **Les propositions** des fichiers 13 à 15 sont des spécifications. Elles ne constituent ni du code livré, ni un engagement de délai.

---

## 9. Verdict en une phrase

**Le backend n'est pas prêt pour la production.** Il est partiellement utilisable pour un prototype mono-entreprise, en lecture et CRUD encadré, mais il ne permet pas aujourd'hui de garantir un stock fiable, une isolation entre entreprises, une traçabilité des affectations ou un contrôle d'accès par rôle.

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

## 11. Déclaration de non-modification

Cette passe d'audit n'a modifié :

- aucun fichier Java applicatif ;
- aucun fichier de test ;
- aucun script SQL ;
- aucune migration ;
- aucune configuration ;
- aucune dépendance ;
- aucune donnée en base ;
- aucun fichier OpenAPI.

Les seuls fichiers créés ou modifiés sont les fichiers Markdown de ce dossier `docs/audit/` (dix-huit après ajout du rapport 16).

Toute correction et tout démarrage de frontend doivent faire l'objet d'un accord explicite distinct.
