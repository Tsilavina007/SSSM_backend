# 00 — Résumé exécutif

**Date :** 21 octobre 2026  
**Verdict global :** **NON PRÊT POUR LA PRODUCTION**  
**Nuance :** **PARTIELLEMENT PRÊT** pour un prototype mono-entreprise, en lecture et CRUD encadré, sans promesse de stock fiable ni de traçabilité complète.

**Mise à jour du 21 octobre 2026 (Lot 1) :** inscription publique fermée, comptes créés par un administrateur ; entreprise et permissions dans le JWT ; refresh distinct, rotatif et révocable ; logout ; RBAC sur l'inventaire et l'administration ; scope entreprise sur les utilisateurs et rôles ; CORS explicite. **P0-03 est corrigé**, **P0-02 est partiel** (RBAC non étendu aux modules hors inventaire) et **P0-01 reste partiel** sous l'hypothèse mono-entreprise. Les autres P0 restent ouverts.

---

## 1. Décision

Le backend possède un socle fonctionnel réel : les articles, catégories, types, entrepôts, QR Codes, mouvements, affectations, comptages et tableaux de bord ont des routes et des implémentations JDBC. Après le Lot 1, la suite complète compte **120 tests, 119 succès et 1 échec préexistant** (`ItemApiTests.historyUnknownReturnsNotFound`, hors périmètre). Les 29 tests ciblés d'authentification et de RBAC passent.

En revanche, quatre garanties fondamentales restent incomplètes :

1. **Isolation mono-entreprise seulement.** Les colonnes `company_id` ont été ajoutées et backfillées, mais les requêtes d'inventaire ne filtrent pas encore par entreprise ; l'entreprise est fournie dans le corps de la requête ou déduite d'un objet chargé globalement. Aucune seconde entreprise ne doit être exposée tant que ce filtrage n'est pas fait.
2. **RBAC partiel.** L'inscription publique est fermée et l'inventaire, les utilisateurs, les rôles et le catalogue des permissions sont protégés. Les modules organisation, personnel, clients et sites restent seulement `authenticated`, sans permission métier.
3. **Aucune transaction réelle.** Les DAO ouvrent chacun leur propre connexion via `DatabaseConnection` ; les annotations `@Transactional` ne couvrent pas les écritures. Les opérations composées peuvent laisser des états partiels.
4. **Aucun stock fiable.** La quantité `item_type.quantity_total` est saisie manuellement et n'est jamais dérivée des mouvements. La quantité d'un mouvement vaut toujours `1`, faute de champ exposé. Les opérations et comptages ne produisent ni lignes détaillées ni écritures de stock.

Le backend peut donc alimenter une démonstration ou un pilote mono-entreprise, avec des données contrôlées et un utilisateur de confiance. Il ne peut pas être exposé à plusieurs entreprises, à des utilisateurs non administrés ou à des décisions de stock sans corrections préalables.

---

## 2. Synthèse par domaine

| Domaine | Statut | Risque | Constat principal |
|---|---|---|---|
| Authentification | **PARTIEL / DURCI** | Moyen | JWT avec `jti`/`iss`/`aud`/entreprise/permissions, refresh rotatif, logout et révocation. Restent rate limiting, verrouillage et statuts 401/403 du login. |
| Autorisations / RBAC | **PARTIEL** | Élevé | RBAC sur inventaire, utilisateurs, rôles et permissions ; modules organisation/personnel/clients/sites encore seulement authentifiés. |
| Isolation multi-entreprise | **PARTIEL — MONO-ENTREPRISE** | Critique | Colonnes et index ajoutés, scope entreprise sur utilisateurs/rôles ; DAO d'inventaire non filtrés. |
| Catalogue matériel | **PARTIEL** | Élevé | CRUD présent, mais intégrité inter-entreprises non contrôlée et création de type fragile. |
| Stock / quantités | **ABSENT** | Critique | Pas de solde de stock, pas de journal de quantités, `quantity_total` manuel. |
| Mouvements | **PARTIEL** | Critique | Les statuts changent, mais les quantités, sources, destinations et acteurs ne sont pas enregistrés. |
| Opérations | **PARTIEL** | Critique | En-tête seul ; aucune ligne d'article, aucun mouvement, `operation_type` non déduit de la route. |
| Affectations | **PARTIEL** | Critique | Deux modules divergents ; retour partiel possible ; suppression laissant l'article `ASSIGNED`. |
| QR Code | **PARTIEL** | Élevé | Génération PNG et scan fonctionnent, mais résolution globale, code modifiable, pas de contrat mobile enrichi. |
| Comptages d'inventaire | **PARTIEL** | Élevé | Saisie d'écarts sans calcul, clôture sans réconciliation, `last_inventory_at` jamais mis à jour. |
| Contrat API | **PARTIEL** | Élevé | Routes cohérentes dans l'ensemble, mais erreurs douces en 200, conflits en 500, pagination non bornée. |
| Performance | **PARTIEL** | Élevé | Chaîne N+1 sur chaque article, aucun pool de connexions utilisé par les DAO, pas de plafond de pagination. |
| Tests | **PARTIEL** | Élevé | Suite complète exécutée : 120 tests, 119 succès, 1 échec préexistant. Couverture RBAC, scope entreprise et révocation ajoutée ; pas de scénario à deux entreprises, de concurrence, de quantités ou de réconciliation. |
| Documentation OpenAPI | **PARTIEL** | Moyen | 26 fichiers cohérents avec les routes, mais 183 avertissements non bloquants relevés à l'audit initial (non rejoués après `7e3f196`) et plusieurs dettes documentées. |

---

## 3. Réponses aux questions de décision

### 3.1. Peut-on démarrer le frontend web maintenant ?

**Non, pas pour un frontend de production.**  
Un frontend de démonstration peut consommer le catalogue, les articles, les QR Codes et les listes en lecture. Il ne doit pas exposer des écrans de stock, d'affectation ou d'administration multi-entreprise tant que les garanties de tenancy, de transaction et de stock ne sont pas posées.

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

| Rang | Anomalie | Référence | Statut Lot 1 |
|---|---|---|---|
| 1 | Isolation multi-entreprise incomplète | 09, 12 | Partiel — mono-entreprise assumée |
| 2 | Inscription publique + RBAC inopérant | 09, 12 | Inscription corrigée ; RBAC partiel |
| 3 | Transactions inopérantes et écritures non atomiques | 01, 03, 12 | Ouvert |
| 4 | Stock non dérivé des mouvements, quantités fixes | 05, 07, 12 | Ouvert |
| 5 | Machines à états des affectations et retours incohérentes | 08, 12 | Ouvert |
| 6 | Opérations d'inventaire creuses | 05, 07, 12 | Ouvert |
| 7 | QR Code global et modifiable | 06, 12 | Ouvert |
| 8 | Fuite de SQLSTATE et de messages PostgreSQL | 02, 09, 12 | Ouvert |

### 3.7. Quelles sont les cinq actions à mener avant toute nouvelle phase ?

**État Lot 1 :** l'action 1 est partiellement réalisée (mono-entreprise, inscription fermée, RBAC inventaire/administration, jetons durcis). Les actions 2 à 5 restent à mener ; le Lot 2 ne doit pas démarrer sans autorisation.

1. **Trancher le modèle de tenancy et le contrôle d'accès.**  
   Décider si l'entreprise est un tenant strict, comment elle est portée par le jeton, et quelles permissions sont réellement appliquées. Fermer ou encadrer l'inscription publique. Tant que ce point n'est pas décidé, tout développement frontend reposera sur un contrat faux.

2. **Rétablir des transactions et des connexions maîtrisées.**  
   Remplacer l'accès `DriverManager` par une source de données gérée par Spring, ou assumer explicitement l'absence de transactions. Les opérations composées (article + affectation, mouvement + état, comptage + clôture) doivent être atomiques.

3. **Construire un vrai journal de stock.**  
   Exposer `quantity`, `source_warehouse_id`, `destination_warehouse_id`, `operation_id` et `performed_by`, puis calculer les soldes à partir des mouvements. Interdire la modification manuelle de `quantity_total` ou la remplacer par une vue calculée.

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
- 120 tests exécutés après le Lot 1 : 119 succès, 1 échec préexistant sur l'historique d'article.

Ces acquis sont réels, mais ils ne compensent pas les quatre garanties manquantes.

---

## 5. Ce qui ne doit pas être exposé en l'état

- Un écran d'administration multi-entreprise.
- Un écran de stock avec quantités présentées comme fiables.
- Un écran d'affectation avec suppression ou retour partiel.
- Un parcours mobile de scan avec action directe.
- Un portail public d'inscription (fermé au Lot 1).
- Un tableau de bord de stock basé sur `quantity_total`.
- Un écran d'administration multi-entreprise tant que les DAO d'inventaire ne filtrent pas par `company_id`.
- Les routes organisation/personnel/clients/sites avec un simple compte authentifié, sans RBAC métier.

---

## 6. Indicateurs de l'audit

| Indicateur | Valeur |
|---|---|
| Contrôleurs REST | 26 |
| Routes applicatives | 131 après le Lot 1 (130 à l'audit initial, plus `POST /api/auth/logout`) |
| Routes d'inventaire | 57 |
| Fichiers OpenAPI | 26 |
| Fichiers OpenAPI inventaire | 12 |
| Scripts SQL | 19 (dont `18_lot1_security.sql`) |
| Tests | 120 après le Lot 1 (23 classes) |
| Échecs de tests | 1 préexistant (`ItemApiTests.historyUnknownReturnsNotFound`) |
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
3. **Stock :** décision toujours ouverte. Le journal de mouvements avec solde calculé n'est pas encore implémenté.

Les corrections du Lot 2 (DataSource, transactions, filtres `company_id` dans les DAO d'inventaire) ne doivent pas démarrer sans validation explicite. Le plan du fichier 14 est structuré pour cette décision.
