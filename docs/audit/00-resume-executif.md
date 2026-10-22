# 00 — Résumé exécutif

**Date :** 21 octobre 2026, mise à jour du 22 octobre 2026 (Lots 2, 3 et 4)  
**Verdict global :** **NON PRÊT POUR LA PRODUCTION**  
**Nuance :** **PARTIELLEMENT PRÊT** pour un prototype mono-entreprise, en lecture et CRUD encadré, avec un stock calculé pour les flux mouvement/opération/comptage et un cycle de vie des affectations unifié, mais sans tenancy complète ni concurrence maîtrisée.

**Mise à jour du 21 octobre 2026 (Lot 1) :** inscription publique fermée, comptes créés par un administrateur ; entreprise et permissions dans le JWT ; refresh distinct, rotatif et révocable ; logout ; RBAC sur l'inventaire et l'administration ; scope entreprise sur les utilisateurs et rôles ; CORS explicite. **P0-03 est corrigé**, **P0-02 est partiel** (RBAC non étendu aux modules hors inventaire) et **P0-01 reste partiel** sous l'hypothèse mono-entreprise. Les autres P0 restent ouverts.

**Mise à jour du 22 octobre 2026 (Lot 2) :** accès JDBC via le `DataSource` Spring/HikariCP, transactions de service effectives, erreurs SQL traduites en 400/409/422/503 sans SQLSTATE, créations avec défauts PostgreSQL relus. **P1-09, P0-04, P1-01, P1-02 et P1-10 sont corrigés** dans leur périmètre. Suite complète : **132 tests, 0 échec, 0 erreur, 0 ignoré, 27 suites**. `P0-01` (filtres `company_id` d'inventaire) et la concurrence (`P1-14`) restent ouverts. Aucune migration SQL n'a été exécutée.

**Mise à jour du 22 octobre 2026 (Lot 3) :** stock disponible calculé dans `security.stock_balance` et exposé par `GET /api/inventory/stock-balances` ; journal append-only avec quantité, source/destination, opération et acteur ; opérations multi-lignes atomiques ; comptages avec écart serveur et clôture réconciliée. **P0-05, P1-03, P1-06 et P1-07 sont corrigés** dans le périmètre documenté. La migration additive `19_lot3_stock.sql` a été appliquée et rejouée sans écraser les soldes. Suite complète : **140 tests, 0 échec, 0 erreur, 0 ignoré, 28 suites**. `P0-01`, `P0-06` et `P1-14` restaient ouverts à cette date ; les affectations pouvaient encore désynchroniser le solde et les ajustements de comptage ne sont pas journalisés.

**Mise à jour du 22 octobre 2026 (Lot 4) :** cycle de vie des affectations unifié derrière le contrat canonique `/api/item-assignments` ; `/api/inventory/assignments` devient une façade dépréciée déléguant au même service, avec les mêmes DTO et les mêmes codes HTTP, plus aucun JSON libre ni soft-200. `OUT` exige `employee_id`/`post_id` et crée une affectation ; `RETURN` la clôt ; `IN`, `TRANSFER` et `DESTRUCTION` refusent un article affecté. Le retour (`POST /{id}/return`) pose date et acteur côté serveur, restaure l'entrepôt et l'article, et synchronise le solde. Articles, catégories, types et entrepôts sont archivés logiquement et restaurables ; les affectations sont archivées par un void motivé et audité, et une affectation active ne peut pas être supprimée. **P0-06, P1-05 et P1-11 sont corrigés** dans le périmètre documenté. La migration additive `20_lot4_assignments.sql` a été appliquée, rejouée et vérifiée idempotente. Suite complète : **150 tests, 0 échec, 0 erreur, 0 ignoré, 28 suites**. `P0-01` et `P1-14` restent ouverts ; une affectation active antérieure de la base de démonstration pointe encore vers un article `AVAILABLE` et n'a pas été corrigée automatiquement.

---

## 1. Décision

Le backend possède un socle fonctionnel réel : les articles, catégories, types, entrepôts, QR Codes, mouvements, affectations, comptages et tableaux de bord ont des routes et des implémentations JDBC. Après le Lot 4, la suite complète compte **150 tests, 150 succès, 0 échec, 0 erreur, 0 ignoré, 28 suites**. Les 29 tests ciblés d'authentification et de RBAC passent, ainsi que les tests de pool, de transaction, de mapping SQL, les huit tests dédiés au stock et les 22 tests dédiés au cycle de vie des affectations.

En revanche, quatre garanties fondamentales restent incomplètes :

1. **Isolation mono-entreprise seulement.** Les colonnes `company_id` ont été ajoutées et backfillées, mais les requêtes d'inventaire ne filtrent pas encore par entreprise ; l'entreprise est fournie dans le corps de la requête ou déduite d'un objet chargé globalement. Aucune seconde entreprise ne doit être exposée tant que ce filtrage n'est pas fait.
2. **RBAC partiel.** L'inscription publique est fermée et l'inventaire, les utilisateurs, les rôles et le catalogue des permissions sont protégés. Les modules organisation, personnel, clients et sites restent seulement `authenticated`, sans permission métier.
3. **Transactions rétablies, concurrence non traitée.** Les DAO utilisent une connexion transactionnelle issue du pool Spring/HikariCP ; les annotations `@Transactional` couvrent les écritures des services existants, y compris les opérations multi-lignes du Lot 3 et les affectations du Lot 4. Il reste à traiter la concurrence (version/`If-Match`, idempotence).
4. **Stock calculé, mais pas encore universel.** `stock_balance` est la source du stock disponible et le dashboard low-stock lit ce solde. `item_type.quantity_total` existe encore pour compatibilité mais n'est plus le critère. Les affectations sont désormais synchronisées avec le solde ; en revanche, les ajustements de clôture de comptage ne créent pas de mouvement compensatoire et les lignes de mouvement antérieures au Lot 3 restent incomplètes.

Le backend peut donc alimenter une démonstration ou un pilote mono-entreprise, avec des données contrôlées et un utilisateur de confiance. Il ne peut pas être exposé à plusieurs entreprises, à des utilisateurs non administrés ou à des décisions de stock sans corrections préalables.

---

## 2. Synthèse par domaine

| Domaine | Statut | Risque | Constat principal |
|---|---|---|---|
| Authentification | **PARTIEL / DURCI** | Moyen | JWT avec `jti`/`iss`/`aud`/entreprise/permissions, refresh rotatif, logout et révocation. Restent rate limiting, verrouillage et statuts 401/403 du login. |
| Autorisations / RBAC | **PARTIEL** | Élevé | RBAC sur inventaire, utilisateurs, rôles et permissions ; modules organisation/personnel/clients/sites encore seulement authentifiés. |
| Isolation multi-entreprise | **PARTIEL — MONO-ENTREPRISE** | Critique | Colonnes et index ajoutés, scope entreprise sur utilisateurs/rôles ; DAO d'inventaire non filtrés. |
| Catalogue matériel | **PARTIEL** | Élevé | CRUD présent, création de type corrigée (défauts PostgreSQL), archivage logique et restauration des articles, catégories, types et entrepôts ; intégrité inter-entreprises non contrôlée. |
| Stock / quantités | **CORRIGÉ — LOT 3** | Critique | `stock_balance` calculé et exposé ; dashboard low-stock sur le solde réel. Dette historique sur les lignes antérieures ; ajustements de clôture non journalisés. |
| Mouvements | **CORRIGÉ — LOT 3** | Critique | Quantités, source/destination, opération et acteur exposés ; journal append-only ; stock négatif refusé. |
| Opérations | **CORRIGÉ — LOT 3** | Critique | Type imposé par la route, lignes et mouvements créés atomiquement, statut `COMPLETED`. |
| Affectations | **CORRIGÉ — LOT 4** | Critique | Contrat canonique unique (façade dépréciée) ; création, retour, void, archivage et restauration audités ; `OUT`/`RETURN` raccordés au stock. Restent la cohérence entreprise et l'idempotence. |
| QR Code | **PARTIEL** | Élevé | Génération PNG et scan fonctionnent, mais résolution globale, code modifiable, pas de contrat mobile enrichi. |
| Comptages d'inventaire | **CORRIGÉ — LOT 3** | Élevé | Quantité attendue figée, écart calculé serveur, clôture réconciliée avec `last_inventory_at`. Ajustements non journalisés. |
| Contrat API | **PARTIEL** | Élevé | Erreurs SQL corrigées (400/409/422/503) ; le contrat d'affectation est unifié et sans erreur douce ; restent les erreurs douces des autres modules et une pagination non bornée. |
| Performance | **PARTIEL** | Élevé | Pool HikariCP désormais utilisé par les DAO ; chaîne N+1 sur chaque article et pagination non bornée restent ouvertes. |
| Tests | **PARTIEL** | Élevé | Suite complète exécutée : 150 tests, 0 échec, 28 suites. Couverture RBAC, pool, rollback transactionnel, mapping SQL, soldes, opérations atomiques, comptages et cycle de vie des affectations ; pas de scénario à deux entreprises ni de concurrence. |
| Documentation OpenAPI | **PARTIEL** | Moyen | 26 fichiers cohérents avec les routes ; lint Redocly rejoué au Lot 4 : **179 avertissements, 0 erreur** (contre 183 à l'audit initial). Restent les exemples, les `license` et les réponses d'erreur incomplètes. |

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

**Oui, dans le périmètre documenté (Lots 3 et 4).** Le journal est append-only, `quantity`, `source_warehouse_id`, `destination_warehouse_id`, `operation_id` et `performed_by` sont exposés, et `stock_balance` est la source du stock disponible. Un `OUT` crée désormais une affectation et un `RETURN` la clôture ; `IN`, `TRANSFER` et `DESTRUCTION` refusent un article affecté.

Restent des limites : les lignes de mouvement antérieures au Lot 3 n'ont ni source, ni destination, ni acteur ; les ajustements de clôture de comptage modifient le solde sans écrire de mouvement compensatoire ; `item_type.quantity_total` subsiste pour compatibilité sans être le critère du dashboard.

### 3.5. Les affectations sont-elles cohérentes ?

**Oui, dans le périmètre documenté (Lot 4).** Le contrat canonique est `/api/item-assignments` ; `/api/inventory/assignments` est une façade dépréciée qui délègue au même service, avec les mêmes DTO, les mêmes codes HTTP et des en-têtes `Deprecation`. Une affectation active exige un article `AVAILABLE` sans affectation active, le passe à `ASSIGNED`, vide son emplacement et synchronise le solde. `POST /{id}/return` clôt l'affectation, pose la date et l'acteur côté serveur, restaure l'entrepôt d'origine et l'article. Le void est motivé et audité, la suppression d'une affectation active renvoie 409, et les listes excluent les lignes voidées.

Restent des limites : aucun contrôle d'employé actif, de cohérence d'entreprise ni de poste (`P0-01`) ; l'idempotence n'est pas implémentée (`P1-14`) ; les références vers un référentiel archivé ne sont pas toutes refusées ; une affectation active antérieure de la base de démonstration pointe vers un article `AVAILABLE` et n'a pas été corrigée automatiquement.

### 3.6. Quelles sont les anomalies les plus critiques ?

| Rang | Anomalie | Référence | Statut après Lots 1 à 4 |
|---|---|---|---|
| 1 | Isolation multi-entreprise incomplète | 09, 12 | Partiel — mono-entreprise assumée ; filtres DAO ouverts (`P0-01`) |
| 2 | Inscription publique + RBAC inopérant | 09, 12 | Inscription corrigée ; RBAC partiel |
| 3 | Transactions inopérantes et écritures non atomiques | 01, 03, 12 | Corrigé (`P0-04`) ; opérations multi-lignes atomiques (`P1-06`) |
| 4 | Stock non dérivé des mouvements, quantités fixes | 05, 07, 12 | Corrigé dans le périmètre du Lot 3 (`P0-05`), avec dette historique |
| 5 | Machines à états des affectations et retours incohérentes | 08, 12 | Corrigé dans le périmètre du Lot 4 (`P0-06`) |
| 6 | Opérations d'inventaire creuses | 05, 07, 12 | Corrigé (`P1-06`) |
| 7 | QR Code global et modifiable | 06, 12 | Ouvert |
| 8 | Fuite de SQLSTATE et de messages PostgreSQL | 02, 09, 12 | Corrigé (`P1-01`) |

### 3.7. Quelles sont les cinq actions à mener avant toute nouvelle phase ?

**État après Lot 4 :** les actions 2, 3 et 4 sont réalisées dans leur périmètre documenté. L'action 1 reste partielle (mono-entreprise, filtres DAO d'inventaire non généralisés) et l'action 5 reste à mener. Le Lot 5 ne doit pas démarrer sans autorisation.

1. **Trancher le modèle de tenancy et le contrôle d'accès.**  
   Décider si l'entreprise est un tenant strict, comment elle est portée par le jeton, et quelles permissions sont réellement appliquées. Fermer ou encadrer l'inscription publique. Tant que ce point n'est pas décidé, tout développement frontend reposera sur un contrat faux.

2. **Rétablir des transactions et des connexions maîtrisées — fait au Lot 2.**  
   L'accès `DriverManager` a été remplacé par le `DataSource` Spring/HikariCP et les DAO participent aux transactions. Les opérations composées (mouvement + état, affectation + état, opération multi-lignes) sont atomiques et testées ; le Lot 3 a implémenté les lignes d'opération et les mouvements associés (`P1-06`).

3. **Construire un vrai journal de stock — fait au Lot 3.**  
   `quantity`, `source_warehouse_id`, `destination_warehouse_id`, `operation_id` et `performed_by` sont exposés ; `stock_balance` est alimenté par les mouvements, opérations, articles et comptages. `PATCH`/`DELETE` sont interdits sur le journal. `quantity_total` existe encore pour compatibilité mais n'est plus le critère du dashboard low-stock. Les ajustements de comptage ne sont pas encore journalisés ; les affectations ne désynchronisent plus le solde depuis le Lot 4.

4. **Unifier les machines à états — fait au Lot 4.**  
   Une seule vérité pour l'affectation, le retour, la sortie et la restitution : contrat canonique `/api/item-assignments` avec façade dépréciée, retour serveur daté et journalisé, entrepôt d'origine restauré, void motivé et audité, aucune suppression d'historique actif, aucun `OUT` sans affectation, et archivage logique avec restauration des référentiels. Restent l'idempotence (`P1-14`) et la cohérence d'entreprise (`P0-01`).

5. **Définir le contrat mobile et QR avant de coder le mobile.**  
   Résolution tenant-scoped, réponse enrichie, scan tracé, étiquette imprimable, actions idempotentes et gestion des conflits. Sans ce contrat, le mobile ne pourra pas être testé de bout en bout.

---

## 4. Ce qui fonctionne déjà

- CRUD des articles, catégories, types et entrepôts.
- Filtres articles par recherche, entrepôt et statut.
- Génération automatique de QR Code et image PNG.
- Résolution d'un QR Code vers une ligne d'article.
- Mouvements `IN`, `OUT`, `TRANSFER`, `RETURN`, `DESTRUCTION`, `ADJUSTMENT` avec effets d'état nominaux.
- Affectation typée avec contrôle de disponibilité, index unique d'affectation active et contrat canonique unique.
- Restitution typée avec `returned_at` et remise à `AVAILABLE`, entrepôt d'origine restauré et acteur journalisé.
- Comptage : création d'en-tête, scan d'article, clôture de statut.
- Authentification JWT, BCrypt, `whoami`, refresh rotatif, logout et révocation.
- Inscription fermée aux administrateurs, scope entreprise sur les utilisateurs et rôles, RBAC sur l'inventaire et l'administration.
- Enveloppe d'erreur homogène `{"errors": {...}}` pour les exceptions.
- 150 tests exécutés après le Lot 4 : 150 succès, 0 échec, 0 erreur, 0 ignoré, 28 suites.

Ces acquis sont réels, mais ils ne compensent pas les garanties encore incomplètes : tenancy d'inventaire, RBAC métier hors inventaire et concurrence.

---

## 5. Ce qui ne doit pas être exposé en l'état

- Un écran d'administration multi-entreprise.
- Un parcours mobile de scan avec action directe.
- Un portail public d'inscription (fermé au Lot 1).
- Un tableau de bord de stock basé sur `quantity_total` (le dashboard lit désormais `stock_balance`).
- Un écran d'administration multi-entreprise tant que les DAO d'inventaire ne filtrent pas par `company_id`.
- Les routes organisation/personnel/clients/sites avec un simple compte authentifié, sans RBAC métier.
- Un parcours de double soumission sur une affectation : l'idempotence (`P1-14`) n'est pas implémentée et un second retour reçoit 409.

---

## 6. Indicateurs de l'audit

| Indicateur | Valeur |
|---|---|
| Contrôleurs REST | 27 contrôleurs métier (+ 2 contrôleurs de test) |
| Routes applicatives | 147 après le Lot 4 (130 à l'audit initial, plus logout, soldes, lecture d'un comptage et le cycle de vie des affectations) |
| Routes d'inventaire | 73 sur 13 contrôleurs |
| Fichiers OpenAPI | 26 |
| Fichiers OpenAPI inventaire | 12 |
| Scripts SQL | 21 (dont `18_lot1_security.sql`, `19_lot3_stock.sql` et `20_lot4_assignments.sql`) |
| Tests | 150 après le Lot 4 (28 suites) |
| Échecs de tests | 0 |
| Pool de connexions | HikariCP via le `DataSource` Spring |
| Entreprises en base de démonstration | 1 |
| Articles en base de démonstration | 13 (0 archivé) |
| Mouvements en base de démonstration | 13 |
| Affectations actives en base de démonstration | 6 (dont 1 antérieure incohérente avec le statut de son article) |
| Affectations voidées en base de démonstration | 0 |
| Mouvements avec quantité différente de 1 | 0 |
| Mouvements avec source/destination/acteur renseigné | 2 |
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
