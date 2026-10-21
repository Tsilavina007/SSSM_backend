# 12 — Anomalies, risques et priorités

**Objectif :** centraliser les constats dans un registre unique, les qualifier par gravité et fournir l'ordre d'attaque du plan de correction. Les preuves détaillées restent dans les rapports 01 à 11 ; ce document ne les remplace pas.

**Portée :** audit statique et lecture seule de la base de démonstration. Aucune anomalie n'a été corrigée pendant cette passe.

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

---

### P0-04 — Transactions inopérantes et écritures non atomiques

**Constat :** les services sont annotés `@Transactional`, mais les DAO ouvrent leurs connexions via `DatabaseConnection.getConnection()` et `DriverManager`. Ils ne participent pas à la transaction Spring. Il n'y a pas de pool partagé par les DAO.

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

**Constat :**

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

---

## 3. Registre P1 — majeurs

### P1-01 — Erreurs client renvoyées en 500 avec détails SQL

**Constat :** doublons, FK, NOT NULL et contraintes produisent `PSQLException`, converties en **500**. Le corps contient le message PostgreSQL, le détail et le `SQLState`.

**Impact :** le frontend ne peut pas distinguer 400/409/422/500 ; la structure de la base est exposée ; l'utilisateur voit une erreur technique.

**Exemples :** doublon de QR Code, code de type, nom de catégorie, `operation_type` manquant, `unit`/`quantity_total`/`minimum_stock` manquants, `status` d'employé manquant, FK inconnue.

**Références :** rapports 02, 04, 05, 09, 10.

---

### P1-02 — Création de type et d'employé fragile

**Constat :** `ItemTypeDAOImpl.createItemType` insère explicitement `unit`, `quantity_total` et `minimum_stock` même s'ils sont nuls. `EmployeeDAOImpl.create` fait de même pour `status`. Les `DEFAULT` SQL ne s'appliquent pas à un `NULL` explicite.

**Impact :** un formulaire minimal échoue en 500 `23502`. Les tests nominaux fournissent les valeurs et masquent le cas.

**Références :** rapports 04 et 08.

---

### P1-03 — Journal de mouvements mutable et incomplet

**Constat :**

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

---

### P1-06 — Opérations d'inventaire vides

**Constat :** `/receipt`, `/issue` et `/transfer` appellent la même méthode. Elles insèrent un en-tête `inventory_operation` avec les colonnes fournies, sans ligne d'article, sans mouvement, sans modification. Le type n'est pas déduit de la route. S'il est absent, `23502` → 500.

**Impact :** une opération ne prouve aucune réception, sortie ou transfert. Le dashboard compte des en-têtes vides.

**Références :** rapport 05.

---

### P1-07 — Comptages sans réconciliation

**Constat :** `inventory_count_scan` ne contient pas de quantité attendue ou comptée. La clôture ne met à jour ni `item.current_warehouse_id`, ni `item.status`, ni `item.last_inventory_at`, ni `completed_at` si le corps est vide. Les scans ne sont pas contrôlés par entreprise. `GET /inventory-counts/{id}` est absent ; `GET /{id}/results` renvoie 200 `[]` pour un comptage inconnu.

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

**Constat :** `application-prod.properties` déclare `DB_URL`, `DB_USER`, `DB_PASSWORD` et un `DataSource`, et `DatabaseConnection` lit les mêmes variables. Mais les DAO passent par `DriverManager`, avec des valeurs de repli locales, et n'utilisent pas le `DataSource` Spring.

**Impact :** pool Spring non utilisé, réglages (timeouts, TLS, validation) ignorés, et valeurs de repli locales codées en dur qui peuvent masquer une configuration incomplète hors profil prod. Ce n'est pas une divergence de nom de variable, mais la couche JDBC n'applique pas la configuration de production.

**Références :** rapports 03 et 09.

---

### P1-10 — Réponses de création avec valeurs nulles

**Constat :** le DAO construit la réponse avec les valeurs d'entrée, pas avec les valeurs relues :

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

---

### P1-12 — Historique d'article global, tronqué et non testé correctement

**Constat :** `GET /items/{itemId}/history` charge les 100 premiers mouvements **globaux** et les 100 premières affectations **globales**, puis filtre en mémoire. Il ne vérifie pas l'existence de l'article. Un UUID inconnu renvoie 200 avec des listes vides.

Le test `ItemApiTests.historyUnknownReturnsNotFound` attend 404. Le code actuel ne peut pas produire ce 404.

**Impact :** historique incomplet, réponse 200 pour un article inexistant, et divergence code/test qui invalide la confiance dans la suite verte.

**Références :** rapports 02, 07 et 11.

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

---

### P1-14 — Absence de concurrence et d'idempotence

**Constat :** aucun `version`, `ETag`, `If-Match` ou `Idempotency-Key`. Deux utilisateurs peuvent écraser la même modification. Un double clic peut créer deux opérations ou deux affectations. La course sur l'affectation active est rejetée par l'index unique en 500, pas en 409.

**Impact :** pertes de mise à jour et doublons en production.

**Références :** rapports 08, 10 et 11.

---

## 4. Registre P2 — dette structurante

### P2-01 — Archivage et suppression non modélisés

Les champs `archived_at`/`archived_by` sont inertes. Le produit doit décider entre suppression physique, archivage logique et conservation légale. Aucun endpoint ne permet aujourd'hui d'archiver ou de restaurer.

### P2-02 — Contrat de pagination et de filtrage insuffisant

Pas de total, pas de métadonnées, filtres limités, tri par `id`, pas de recherche plein texte, pas de filtre par entreprise, type, catégorie, date ou acteur. Le frontend filtrera côté client.

### P2-03 — Nommage, versionnement et dates

Routes hors préfixe (`/api/item-assignments`), absence de `/api/v1`, deux contrats pour un même objet, dates `java.sql.Timestamp` sans fuseau contractuel, pas de format de date documenté, enums sans libellés.

### P2-04 — Dashboard et alertes non représentatifs

Compteurs globaux, low-stock basé sur `quantity_total`, pas de stock par entrepôt, pas de valeur, pas d'écarts, pas de période. Le dashboard ne peut pas piloter l'activité.

### P2-05 — Absence d'étiquettes et d'impression en lot

Le PNG est disponible, mais il n'existe ni PDF, ni planche, ni file d'impression, ni texte lisible, ni cache. Un déploiement réel devra imprimer des centaines d'étiquettes.

### P2-06 — CI, couverture et documentation de contrat

Un workflow GitHub Actions existe (`.github/workflows/ci.yml`) : PostgreSQL 16 éphémère, initialisation du schéma et des données de démonstration, tests, lint OpenAPI et packaging. Il ne couvre cependant ni la tenancy, ni le RBAC, ni les transactions, ni la concurrence, et la couverture n'est pas mesurée. L'OpenAPI contient 183 avertissements Redocly non bloquants ; les erreurs et les exemples ne sont pas contractuels.

---

## 5. Matrice de risque

| ID | Risque | Probabilité | Impact | Score | Niveau |
|---|---|---:|---:|---:|---|
| P0-01 | Fuite/corruption inter-entreprises | 5 | 5 | 25 | Critique |
| P0-02 | Inscription publique + RBAC absent | 5 | 5 | 25 | Critique |
| P0-03 | Refresh perpétue les rôles révoqués | 5 | 5 | 25 | Critique |
| P0-04 | Écritures partiellement validées | 4 | 5 | 20 | Critique |
| P0-05 | Stock non fiable | 5 | 5 | 25 | Critique |
| P0-06 | États d'affectation incohérents | 5 | 5 | 25 | Critique |
| P1-01 | 500 + SQLSTATE exposés | 5 | 4 | 20 | Élevé |
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
| P1-13 | Suite de tests non probante | 5 | 4 | 20 | Élevé |
| P1-14 | Concurrence/ idempotence | 4 | 3 | 12 | Moyen |
| P2-01 | Archivage inerte | 4 | 3 | 12 | Moyen |
| P2-02 | Pagination/filtres insuffisants | 5 | 3 | 15 | Moyen |
| P2-03 | Contrat non versionné | 4 | 2 | 8 | Faible |
| P2-04 | Dashboard non représentatif | 5 | 3 | 15 | Moyen |
| P2-05 | Étiquettes non imprimables en lot | 4 | 2 | 8 | Faible |
| P2-06 | CI présent mais insuffisant | 5 | 2 | 10 | Faible |

---

## 6. Chemin critique

L'ordre recommandé n'est pas l'ordre alphabétique des modules. Il suit les dépendances de données.

### Étape 1 — Fermer l'accès

1. Décider du mode d'inscription (fermée, invitation, validation).
2. Ajouter l'entreprise au compte et au jeton, ou la déduire d'une source serveur.
3. Appliquer un RBAC par permission et par tenant.
4. Recharger les rôles au refresh et permettre la révocation.

**Sortie :** un utilisateur A ne peut pas lire ni écrire les données de B ; un rôle retiré ne peut pas être rafraîchi.

### Étape 2 — Rendre les écritures atomiques

1. Remplacer `DriverManager` par un `DataSource` partagé et un pool.
2. Faire participer les DAO à la transaction Spring.
3. Tester un échec après la première écriture.

**Sortie :** aucune écriture partielle en cas d'erreur.

### Étape 3 — Choisir le modèle de stock

1. Décider si le stock est déclaratif ou calculé.
2. Si calculé : ajouter les quantités, les soldes par entrepôt et les mouvements immuables.
3. Si déclaratif : assumer que le dashboard n'est pas un stock réel.

**Sortie :** une réponse unique et documentée à « quel est le stock ? ».

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
