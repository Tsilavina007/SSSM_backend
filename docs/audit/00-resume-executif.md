# 00 — Résumé exécutif

**Date :** 21 octobre 2026, mise à jour du 22 octobre 2026 (Lots 2 et 3)  
**Verdict global :** **NON PRÊT POUR LA PRODUCTION**  
**Nuance :** **PARTIELLEMENT PRÊT** pour un prototype mono-entreprise, en lecture et CRUD encadré, avec un stock calculé pour les flux mouvement/opération/comptage, mais sans tenancy complète, sans affectations synchronisées ni concurrence maîtrisée.

**Mise à jour du 21 octobre 2026 (Lot 1) :** inscription publique fermée, comptes créés par un administrateur ; entreprise et permissions dans le JWT ; refresh distinct, rotatif et révocable ; logout ; RBAC sur l'inventaire et l'administration ; scope entreprise sur les utilisateurs et rôles ; CORS explicite. **P0-03 est corrigé**, **P0-02 est partiel** (RBAC non étendu aux modules hors inventaire) et **P0-01 reste partiel** sous l'hypothèse mono-entreprise. Les autres P0 restent ouverts.

**Mise à jour du 22 octobre 2026 (Lot 2) :** accès JDBC via le `DataSource` Spring/HikariCP, transactions de service effectives, erreurs SQL traduites en 400/409/422/503 sans SQLSTATE, créations avec défauts PostgreSQL relus. **P1-09, P0-04, P1-01, P1-02 et P1-10 sont corrigés** dans leur périmètre. Suite complète : **132 tests, 0 échec, 0 erreur, 0 ignoré, 27 suites**. `P0-01` (filtres `company_id` d'inventaire) et la concurrence (`P1-14`) restent ouverts. Aucune migration SQL n'a été exécutée.

**Mise à jour du 22 octobre 2026 (Lot 3) :** stock disponible calculé dans `security.stock_balance` et exposé par `GET /api/inventory/stock-balances` ; journal append-only avec quantité, source/destination, opération et acteur ; opérations multi-lignes atomiques ; comptages avec écart serveur et clôture réconciliée. **P0-05, P1-03, P1-06 et P1-07 sont corrigés** dans le périmètre documenté. La migration additive `19_lot3_stock.sql` a été appliquée et rejouée sans écraser les soldes. Suite complète : **140 tests, 0 échec, 0 erreur, 0 ignoré, 28 suites**. `P0-01`, `P0-06` et `P1-14` restent ouverts ; les affectations peuvent encore désynchroniser le solde et les ajustements de comptage ne sont pas journalisés.

---

## 1. Décision

Le backend possède un socle fonctionnel réel : les articles, catégories, types, entrepôts, QR Codes, mouvements, affectations, comptages et tableaux de bord ont des routes et des implémentations JDBC. Après le Lot 3, la suite complète compte **140 tests, 140 succès, 0 échec, 0 erreur, 0 ignoré, 28 suites**. Les 29 tests ciblés d'authentification et de RBAC passent, ainsi que les tests de pool, de transaction, de mapping SQL et les huit tests dédiés au stock.

En revanche, quatre garanties fondamentales restent incomplètes :

1. **Isolation mono-entreprise seulement.** Les colonnes `company_id` ont été ajoutées et backfillées, mais les requêtes d'inventaire ne filtrent pas encore par entreprise ; l'entreprise est fournie dans le corps de la requête ou déduite d'un objet chargé globalement. Aucune seconde entreprise ne doit être exposée tant que ce filtrage n'est pas fait.
2. **RBAC partiel.** L'inscription publique est fermée et l'inventaire, les utilisateurs, les rôles et le catalogue des permissions sont protégés. Les modules organisation, personnel, clients et sites restent seulement `authenticated`, sans permission métier.
3. **Transactions rétablies, concurrence non traitée.** Les DAO utilisent une connexion transactionnelle issue du pool Spring/HikariCP ; les annotations `@Transactional` couvrent les écritures des services existants, y compris les opérations multi-lignes du Lot 3. Il reste à traiter la concurrence (version/`If-Match`, idempotence).
4. **Stock calculé, mais pas encore universel.** `stock_balance` est la source du stock disponible et le dashboard low-stock lit ce solde. `item_type.quantity_total` existe encore pour compatibilité mais n'est plus le critère. Les affectations (`P0-06`) peuvent encore changer l'état d'un article sans mettre à jour le solde, et les ajustements de clôture de comptage ne créent pas de mouvement compensatoire.

Le backend peut donc alimenter une démonstration ou un pilote mono-entreprise, avec des données contrôlées et un utilisateur de confiance. Il ne peut pas être exposé à plusieurs entreprises, à des utilisateurs non administrés ou à des décisions de stock sans corrections préalables.

---

## 2. Synthèse par domaine

| Domaine | Statut | Risque | Constat principal |
|---|---|---|---|
| Authentification | **PARTIEL / DURCI** | Moyen | JWT avec `jti`/`iss`/`aud`/entreprise/permissions, refresh rotatif, logout et révocation. Restent rate limiting, verrouillage et statuts 401/403 du login. |
| Autorisations / RBAC | **PARTIEL** | Élevé | RBAC sur inventaire, utilisateurs, rôles et permissions ; modules organisation/personnel/clients/sites encore seulement authentifiés. |
| Isolation multi-entreprise | **PARTIEL — MONO-ENTREPRISE** | Critique | Colonnes et index ajoutés, scope entreprise sur utilisateurs/rôles ; DAO d'inventaire non filtrés. |
| Catalogue matériel | **PARTIEL** | Élevé | CRUD présent et création de type corrigée (défauts PostgreSQL) ; intégrité inter-entreprises non contrôlée. |
| Stock / quantités | **CORRIGÉ — LOT 3** | Critique | `stock_balance` calculé et exposé ; dashboard low-stock sur le solde réel. Dette historique et affectations non synchronisées. |
| Mouvements | **CORRIGÉ — LOT 3** | Critique | Quantités, source/destination, opération et acteur exposés ; journal append-only ; stock négatif refusé. |
| Opérations | **CORRIGÉ — LOT 3** | Critique | Type imposé par la route, lignes et mouvements créés atomiquement, statut `COMPLETED`. |
| Affectations | **PARTIEL** | Critique | Deux modules divergents ; retour partiel possible ; suppression laissant l'article `ASSIGNED`. |
| QR Code | **PARTIEL** | Élevé | Génération PNG et scan fonctionnent, mais résolution globale, code modifiable, pas de contrat mobile enrichi. |
| Comptages d'inventaire | **CORRIGÉ — LOT 3** | Élevé | Quantité attendue figée, écart calculé serveur, clôture réconciliée avec `last_inventory_at`. Ajustements non journalisés. |
| Contrat API | **PARTIEL** | Élevé | Erreurs SQL corrigées (400/409/422/503) ; restent des erreurs douces en 200 et une pagination non bornée. |
| Performance | **PARTIEL** | Élevé | Pool HikariCP désormais utilisé par les DAO ; chaîne N+1 sur chaque article et pagination non bornée restent ouvertes. |
| Tests | **PARTIEL** | Élevé | Suite complète exécutée : 140 tests, 0 échec, 28 suites. Couverture RBAC, pool, rollback transactionnel, mapping SQL, soldes, opérations atomiques et comptages ; pas de scénario à deux entreprises ni de concurrence. |
| Documentation OpenAPI | **PARTIEL** | Moyen | 26 fichiers cohérents avec les routes, mais 183 avertissements non bloquants relevés à l'audit initial (non rejoués après `7e3f196`) et plusieurs dettes documentées. |

---

## 3. Réponses aux questions de décision

### 3.1. Peut-on démarrer le frontend web maintenant ?

**Non, pas pour un frontend de production.**  
Un frontend de démonstration peut consommer le catalogue, les articles, les QR Codes et les listes en lecture. Il ne doit pas exposer des écrans de stock, d'affectation ou d'administration multi-entreprise tant que les garanties de tenancy, de concurrence et de stock ne sont pas posées.

### 3.2. Peut-on démarrer l'application mobile maintenant ?

**Non.** Le scan QR existe, mais il ne renvoie qu'une ligne d'article brute, sans type lisible, sans entrepôt lisible, sans affectation active, sans détenteur et sans action possible. Il n'existe pas de endpoint de scan tracé, d'action mobile idempotente ni de contrat de synchronisation hors ligne. Le mobile ne peut pas construire un parcours fiable avec l'API actuelle.

### 3.3. La génération et la lecture des QR Codes sont-elles prêtes ?

**La génération et l'image sont prêtes ; le reste est partiel.**

- Génération automatique d'un code `QR-XXXXXXXX` à la création d'un article, ou code fourni par le client.
- Image PNG 300 × 300 générée à la demande avec ZXing.
- Unicité en base par `(company_id, qr_code)`.
- Lecture d'un article par identifiant, et résolution d'un code par `POST /api/inventory/scans/resolve`.

Limites : le code est modifiable par `PATCH`, aucune étiquette PDF n'est produite, l'image n'est pas stockée, le scan est global à toutes les entreprises, renvoie une liste et n'inclut ni type, ni entrepôt, ni affectation, ni détenteur.

### 3.4. Les mouvements de stock sont-ils fiables ?

**Non.** Cinq raisons suffisent :

1. `stock_movement.quantity` n'est jamais exposé par l'API et vaut toujours `1` par défaut.
2. `item_type.quantity_total` est un champ manuel, jamais recalculé à partir des mouvements.
3. `source_warehouse_id`, `destination_warehouse_id`, `operation_id` et `performed_by` existent en base mais ne sont jamais renseignés par l'API.
4. `PATCH` et `DELETE` sur un mouvement modifient ou suppriment le journal sans corriger l'état de l'article.
5. `warehouse_stock_config` existe mais n'est utilisé par aucun code.

### 3.5. Les affectations sont-elles cohérentes ?

**Partiellement.** Le parcours typé nominal fonctionne : affectation d'un article `AVAILABLE`, passage à `ASSIGNED`, restitution avec `returned_at` qui remet l'article `AVAILABLE`. Mais :

- deux modules différents exposent des sémantiques divergentes ;
- le retour générique sans `returned_at` ne fait rien, tout en renvoyant 200 ;
- `DELETE /api/item-assignments/{id}` supprime la ligne sans remettre l'article `AVAILABLE` ;
- les champs `assigned_by` et `returned_by` ne sont pas exposés par le module typé ;
- un mouvement `OUT` passe aussi l'article `ASSIGNED` sans créer d'affectation ;
- aucun contrôle d'employé actif, de cohérence d'entreprise ou de poste n'est effectué.

### 3.6. Quelles sont les anomalies les plus critiques ?

| Rang | Anomalie | Référence | Statut après Lots 1 à 3 |
|---|---|---|---|
| 1 | Isolation multi-entreprise incomplète | 09, 12 | Partiel — mono-entreprise assumée ; filtres DAO ouverts (`P0-01`) |
| 2 | Inscription publique + RBAC inopérant | 09, 12 | Inscription corrigée ; RBAC partiel |
| 3 | Transactions inopérantes et écritures non atomiques | 01, 03, 12 | Corrigé (`P0-04`) ; opérations multi-lignes atomiques (`P1-06`) |
| 4 | Stock non dérivé des mouvements, quantités fixes | 05, 07, 12 | Corrigé dans le périmètre du Lot 3 (`P0-05`), avec dette historique |
| 5 | Machines à états des affectations et retours incohérentes | 08, 12 | Ouvert (`P0-06`, Lot 4) |
| 6 | Opérations d'inventaire creuses | 05, 07, 12 | Corrigé (`P1-06`) |
| 7 | QR Code global et modifiable | 06, 12 | Ouvert |
| 8 | Fuite de SQLSTATE et de messages PostgreSQL | 02, 09, 12 | Corrigé (`P1-01`) |

### 3.7. Quelles sont les cinq actions à mener avant toute nouvelle phase ?

**État après Lot 3 :** les actions 2 et 3 sont réalisées dans leur périmètre documenté. L'action 1 reste partielle (mono-entreprise, filtres DAO d'inventaire non généralisés), et les actions 4 et 5 restent à mener. Le Lot 4 ne doit pas démarrer sans autorisation.

1. **Trancher le modèle de tenancy et le contrôle d'accès.**  
   Décider si l'entreprise est un tenant strict, comment elle est portée par le jeton, et quelles permissions sont réellement appliquées. Fermer ou encadrer l'inscription publique. Tant que ce point n'est pas décidé, tout développement frontend reposera sur un contrat faux.

2. **Rétablir des transactions et des connexions maîtrisées — fait au Lot 2.**  
   L'accès `DriverManager` a été remplacé par le `DataSource` Spring/HikariCP et les DAO participent aux transactions. Les opérations composées (mouvement + état, affectation + état, opération multi-lignes) sont atomiques et testées ; le Lot 3 a implémenté les lignes d'opération et les mouvements associés (`P1-06`).

3. **Construire un vrai journal de stock — fait au Lot 3.**  
   `quantity`, `source_warehouse_id`, `destination_warehouse_id`, `operation_id` et `performed_by` sont exposés ; `stock_balance` est alimenté par les mouvements, opérations, articles et comptages. `PATCH`/`DELETE` sont interdits sur le journal. `quantity_total` existe encore pour compatibilité mais n'est plus le critère du dashboard low-stock. Les ajustements de comptage ne sont pas encore journalisés et les affectations (`P0-06`) peuvent désynchroniser le solde.

4. **Unifier les machines à états.**  
   Une seule vérité pour l'affectation, le retour, la sortie et la restitution. Aucun `DELETE` d'historique sans remise en état. Aucun retour sans date. Aucun mouvement `OUT` qui crée un état `ASSIGNED` sans affectation.

5. **Définir le contrat mobile et QR avant de coder le mobile.**  
   Résolution tenant-scoped, réponse enrichie, scan tracé, étiquette imprimable, actions idempotentes et gestion des conflits. Sans ce contrat, le mobile ne pourra pas être testé de bout en bout.

---

## 4. Ce qui fonctionne déjà

- CRUD des articles, catégories, types et entrepôts.
- Filtres articles par recherche, entrepôt et statut.
- Génération automatique de QR Code et image PNG.
- Résolution d'un QR Code vers une ligne d'article.
- Mouvements `IN`, `OUT`, `TRANSFER`, `RETURN`, `DESTRUCTION`, `ADJUSTMENT` avec effets d'état nominaux.
- Affectation typée avec contrôle de disponibilité et index unique d'affectation active.
- Restitution typée avec `returned_at` et remise à `AVAILABLE`.
- Comptage : création d'en-tête, scan d'article, clôture de statut.
- Authentification JWT, BCrypt, `whoami`, refresh rotatif, logout et révocation.
- Inscription fermée aux administrateurs, scope entreprise sur les utilisateurs et rôles, RBAC sur l'inventaire et l'administration.
- Enveloppe d'erreur homogène `{"errors": {...}}` pour les exceptions.
- 140 tests exécutés après le Lot 3 : 140 succès, 0 échec, 0 erreur, 0 ignoré, 28 suites.

Ces acquis sont réels, mais ils ne compensent pas les garanties encore incomplètes : tenancy d'inventaire, machines à états des affectations et concurrence.

---

## 5. Ce qui ne doit pas être exposé en l'état

- Un écran d'administration multi-entreprise.
- Un écran de stock qui ignorerait la désynchronisation possible des affectations (`P0-06`).
- Un écran d'affectation avec suppression ou retour partiel.
- Un parcours mobile de scan avec action directe.
- Un portail public d'inscription (fermé au Lot 1).
- Un tableau de bord de stock basé sur `quantity_total` (le dashboard lit désormais `stock_balance`).
- Un écran d'administration multi-entreprise tant que les DAO d'inventaire ne filtrent pas par `company_id`.
- Les routes organisation/personnel/clients/sites avec un simple compte authentifié, sans RBAC métier.

---

## 6. Indicateurs de l'audit

| Indicateur | Valeur |
|---|---|
| Contrôleurs REST | 27 |
| Routes applicatives | 133 après le Lot 3 (130 à l'audit initial, plus logout, soldes et lecture d'un comptage) |
| Routes d'inventaire | 59 |
| Fichiers OpenAPI | 26 |
| Fichiers OpenAPI inventaire | 12 |
| Scripts SQL | 20 (dont `18_lot1_security.sql` et `19_lot3_stock.sql`) |
| Tests | 140 après le Lot 3 (28 suites) |
| Échecs de tests | 0 |
| Pool de connexions | HikariCP via le `DataSource` Spring |
| Entreprises en base de démonstration | 1 |
| Articles en base de démonstration | 10 |
| Mouvements en base de démonstration | 8 |
| Affectations actives en base de démonstration | 5 |
| Mouvements avec quantité différente de 1 | 0 |
| Mouvements avec source/destination/acteur renseigné | 0 |
| Affectations avec `assigned_by` ou `returned_by` renseigné | 0 |
| Articles avec `last_inventory_at` renseigné | 0 |

---

## 7. Renvois

- Architecture et flux : [`01-cartographie-backend.md`](01-cartographie-backend.md)
- Contrat HTTP réel : [`02-audit-api-endpoints.md`](02-audit-api-endpoints.md)
- Schéma et intégrité : [`03-audit-modele-donnees-postgresql.md`](03-audit-modele-donnees-postgresql.md)
- Catalogue : [`04-audit-catalogue-materiels.md`](04-audit-catalogue-materiels.md)
- Stock et comptages : [`05-audit-stock-inventaire.md`](05-audit-stock-inventaire.md)
- QR Code : [`06-audit-qrcode.md`](06-audit-qrcode.md)
- Mouvements : [`07-audit-mouvements-stock.md`](07-audit-mouvements-stock.md)
- Affectations : [`08-audit-affectations-employes.md`](08-audit-affectations-employes.md)
- Sécurité : [`09-audit-authentification-securite.md`](09-audit-authentification-securite.md)
- API et frontend : [`10-audit-architecture-api-frontend.md`](10-audit-architecture-api-frontend.md)
- Tests : [`11-audit-tests-fiabilite.md`](11-audit-tests-fiabilite.md)
- Registre des risques : [`12-anomalies-risques-priorites.md`](12-anomalies-risques-priorites.md)
- Endpoints proposés : [`13-endpoints-manquants-proposes.md`](13-endpoints-manquants-proposes.md)
- Plan de correction : [`14-plan-corrections.md`](14-plan-corrections.md)
- Critères de validation : [`15-criteres-validation-api.md`](15-criteres-validation-api.md)
- Revue de cohérence et tableau final : [`16-revue-coherence.md`](16-revue-coherence.md)

---

## 8. Décision attendue

Trois orientations ont été tranchées pour le Lot 1 :

1. **Tenancy :** mono-entreprise assumée pour le runtime, avec colonnes et scope entreprise préparés pour la suite.
2. **Accès :** RBAC par permission, inscription fermée, admin entreprise séparé de l'admin plateforme.
3. **Stock :** journal de mouvements avec solde calculé (`D-01`), implémenté au Lot 3. Les affectations ne sont pas encore raccordées au solde et les ajustements de comptage ne sont pas journalisés.

Le Lot 2 est exécuté pour le `DataSource`, les transactions, le mapping SQL, les défauts de création et les réponses de création. Le Lot 3 est exécuté pour le stock, les opérations et les comptages. Les filtres `company_id` des DAO d'inventaire relèvent de `P0-01` et n'ont pas été inclus. Le Lot 4 et les décisions de concurrence (`version`/`If-Match`, `Idempotency-Key`) ne doivent pas démarrer sans validation explicite.
