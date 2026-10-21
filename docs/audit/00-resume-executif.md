# 00 — Résumé exécutif

**Date :** 21 octobre 2026  
**Verdict global :** **NON PRÊT POUR LA PRODUCTION**  
**Nuance :** **PARTIELLEMENT PRÊT** pour un prototype mono-entreprise, en lecture et CRUD encadré, sans promesse de stock fiable ni de traçabilité complète.

---

## 1. Décision

Le backend possède un socle fonctionnel réel : les articles, catégories, types, entrepôts, QR Codes, mouvements, affectations, comptages et tableaux de bord ont des routes et des implémentations JDBC. Le dernier rapport complet historique mentionne 105 tests sans échec, et plusieurs parcours nominaux sont couverts ; la suite complète n'a pas été relancée pendant l'audit. Le HEAD courant (`7e3f196`) contient 22 classes et 112 tests dans l'arbre, mais aucun rapport complet ne couvre ce HEAD et un test d'historique est en divergence avec le contrôleur.

En revanche, quatre garanties fondamentales manquent :

1. **Aucune isolation entre entreprises.** Les requêtes d'inventaire ne filtrent pas par `company_id` ; l'entreprise est fournie dans le corps de la requête ou déduite d'un objet chargé globalement.
2. **Aucun contrôle d'accès métier.** Un utilisateur authentifié, même inscrit publiquement sans entreprise, peut lire et modifier tout l'inventaire. Les permissions existent en base mais ne sont pas consommées.
3. **Aucune transaction réelle.** Les DAO ouvrent chacun leur propre connexion via `DatabaseConnection` ; les annotations `@Transactional` ne couvrent pas les écritures. Les opérations composées peuvent laisser des états partiels.
4. **Aucun stock fiable.** La quantité `item_type.quantity_total` est saisie manuellement et n'est jamais dérivée des mouvements. La quantité d'un mouvement vaut toujours `1`, faute de champ exposé. Les opérations et comptages ne produisent ni lignes détaillées ni écritures de stock.

Le backend peut donc alimenter une démonstration ou un pilote mono-entreprise, avec des données contrôlées et un utilisateur de confiance. Il ne peut pas être exposé à plusieurs entreprises, à des utilisateurs non administrés ou à des décisions de stock sans corrections préalables.

---

## 2. Synthèse par domaine

| Domaine | Statut | Risque | Constat principal |
|---|---|---|---|
| Authentification | **PARTIEL** | Élevé | JWT et BCrypt présents, mais inscription publique sans entreprise, pas de révocation, rôles figés dans le jeton. |
| Autorisations / RBAC | **ABSENT** | Critique | Aucun `@PreAuthorize` ; seul `/api/users`, `/api/roles`, `/api/permissions` exige `ADMIN`. |
| Isolation multi-entreprise | **ABSENT** | Critique | Aucun filtre `company_id` dans les DAO d'inventaire ; `company_id` vient du client. |
| Catalogue matériel | **PARTIEL** | Élevé | CRUD présent, mais intégrité inter-entreprises non contrôlée et création de type fragile. |
| Stock / quantités | **ABSENT** | Critique | Pas de solde de stock, pas de journal de quantités, `quantity_total` manuel. |
| Mouvements | **PARTIEL** | Critique | Les statuts changent, mais les quantités, sources, destinations et acteurs ne sont pas enregistrés. |
| Opérations | **PARTIEL** | Critique | En-tête seul ; aucune ligne d'article, aucun mouvement, `operation_type` non déduit de la route. |
| Affectations | **PARTIEL** | Critique | Deux modules divergents ; retour partiel possible ; suppression laissant l'article `ASSIGNED`. |
| QR Code | **PARTIEL** | Élevé | Génération PNG et scan fonctionnent, mais résolution globale, code modifiable, pas de contrat mobile enrichi. |
| Comptages d'inventaire | **PARTIEL** | Élevé | Saisie d'écarts sans calcul, clôture sans réconciliation, `last_inventory_at` jamais mis à jour. |
| Contrat API | **PARTIEL** | Élevé | Routes cohérentes dans l'ensemble, mais erreurs douces en 200, conflits en 500, pagination non bornée. |
| Performance | **PARTIEL** | Élevé | Chaîne N+1 sur chaque article, aucun pool de connexions utilisé par les DAO, pas de plafond de pagination. |
| Tests | **PARTIEL** | Élevé | 105 tests verts dans le dernier rapport complet historique, mais aucune couverture multi-entreprise, RBAC, concurrence, quantités ou réconciliation ; suite non relancée et état complet de `7e3f196` non vérifié. |
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

| Rang | Anomalie | Référence |
|---|---|---|
| 1 | Absence d'isolation multi-entreprise | 09, 12 |
| 2 | Inscription publique + RBAC inopérant | 09, 12 |
| 3 | Transactions inopérantes et écritures non atomiques | 01, 03, 12 |
| 4 | Stock non dérivé des mouvements, quantités fixes | 05, 07, 12 |
| 5 | Machines à états des affectations et retours incohérentes | 08, 12 |
| 6 | Opérations d'inventaire creuses | 05, 07, 12 |
| 7 | QR Code global et modifiable | 06, 12 |
| 8 | Fuite de SQLSTATE et de messages PostgreSQL | 02, 09, 12 |

### 3.7. Quelles sont les cinq actions à mener avant toute nouvelle phase ?

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
- Authentification JWT, BCrypt, `whoami`, refresh.
- Enveloppe d'erreur homogène `{"errors": {...}}` pour les exceptions.
- 105 tests sans échec dans le dernier rapport complet historique, dont des tests d'intégration sur base réelle ; ce rapport ne couvre pas le HEAD `7e3f196`.

Ces acquis sont réels, mais ils ne compensent pas les quatre garanties manquantes.

---

## 5. Ce qui ne doit pas être exposé en l'état

- Un écran d'administration multi-entreprise.
- Un écran de stock avec quantités présentées comme fiables.
- Un écran d'affectation avec suppression ou retour partiel.
- Un parcours mobile de scan avec action directe.
- Un portail public d'inscription.
- Un tableau de bord de stock basé sur `quantity_total`.
- Toute API accessible avec un simple compte `USER` non rattaché à une entreprise.

---

## 6. Indicateurs de l'audit

| Indicateur | Valeur |
|---|---|
| Contrôleurs REST | 26 |
| Routes applicatives | 130 |
| Routes d'inventaire | 57 |
| Fichiers OpenAPI | 26 |
| Fichiers OpenAPI inventaire | 12 |
| Scripts SQL | 18 |
| Tests | 105 (rapport complet historique) ; 112 dans l'arbre `7e3f196`, suite complète non rejouée |
| Échecs de tests (dernier rapport complet) | 0 ; état de `7e3f196` **NON VÉRIFIÉ** |
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

L'audit ne demande pas de choisir immédiatement une solution technique. Il demande de valider trois orientations avant toute correction :

1. **Tenancy :** mono-entreprise assumée ou multi-entreprise stricte ?
2. **Stock :** journal de mouvements avec solde calculé, ou stock déclaratif manuel ?
3. **Accès :** RBAC par permission, ou rôles simples suffisamment encadrés ?

Tant que ces trois orientations ne sont pas explicites, les corrections risques d'être incompatibles entre elles. Le plan du fichier 14 est structuré pour permettre cette décision avant les développements.
