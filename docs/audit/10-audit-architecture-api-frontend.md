# 10 — Audit de l'architecture API et de l'adéquation frontend

**Note Lot 7 (23 octobre 2026) :** les routes canoniques sont désormais préfixées `/api/v1` ; l'alias `/api` reste accepté avec `Deprecation`/`Link`. Les chemins cités ci-dessous reflètent l'état de l'audit au moment de sa rédaction, sauf mise à jour explicite.

**Objectif :** évaluer si le contrat HTTP actuel permet de construire un frontend web et une application mobile sans masquer des comportements faux. Ce rapport ne propose pas de code frontend ; il identifie les écarts entre l'API et les besoins d'interface.

---

## 1. Surface d'API

| Catégorie | Contrôleurs | Routes |
|---|---|---|
| Inventaire | 12 | 57 |
| Authentification / administration | 4 | 23 |
| Organisation | 3 | 15 |
| Personnel | 2 | 10 |
| Clients | 2 | 10 |
| Sites | 3 | 15 |
| **Total métier** | **26** | **130** |

L'API est large. Elle couvre le catalogue, les mouvements, les affectations, les comptages, les QR Codes, le dashboard et l'administration des utilisateurs.

---

## 2. Forme des ressources

### 2.1. Deux styles de réponse

**Style imbriqué** — `ItemRest` :

```json
{
  "id": "...",
  "company": { "...": "..." },
  "category": { "...": "..." },
  "item_type": { "...": "..." },
  "current_warehouse": { "...": "..." },
  "qr_code": "QR-...",
  "status": "AVAILABLE"
}
```

**Style plat** — `StockMovementRest`, `ItemAssignmentRest`, `AssignmentRest`, `QrScanItemRest` :

```json
{
  "id": "...",
  "item_id": "...",
  "warehouse_id": "...",
  "employee_id": "...",
  "movement_type": "IN"
}
```

Le frontend doit donc gérer deux conventions :

- parfois un objet complet ;
- parfois un identifiant à résoudre par un second appel.

### 2.2. Conséquences

- Les listes d'articles déclenchent le N+1 côté serveur.
- Les listes de mouvements et d'affectations obligent le frontend à charger les articles, employés et entrepôts séparément.
- Les réponses de scan ne contiennent aucun libellé.
- Les réponses d'affectation typée n'exposent pas les acteurs.
- Les rapports de mouvements exposent plus de champs que la ressource standard, ce qui crée deux contrats pour un même objet.

### 2.3. Nommage

| Ressource | Route | Cohérence |
|---|---|---|
| Articles | `/api/inventory/items` | préfixe inventaire |
| Types | `/api/inventory/item-types` | préfixe inventaire |
| Catégories | `/api/inventory/item-categories` | préfixe inventaire |
| Entrepôts | `/api/inventory/warehouses` | préfixe inventaire |
| Mouvements | `/api/inventory/movements` | préfixe inventaire |
| Affectations typées | `/api/item-assignments` | **hors préfixe** |
| Affectations génériques | `/api/inventory/assignments` | **deuxième contrat** |
| Employés | `/api/employees` | hors préfixe |
| Postes | `/api/security-posts` | hors préfixe |

Aucune version d'API (`/api/v1`) n'est présente. Aucun mécanisme de dépréciation n'existe.

---

## 3. Pagination et filtres

### 3.1. Pagination

Les listes typées renvoient un **tableau brut** :

```json
[
  { "...": "..." },
  { "...": "..." }
]
```

Elles ne renvoient pas :

- le total ;
- le nombre de pages ;
- la page courante ;
- la taille de page ;
- un curseur.

Les paramètres varient :

- `page` / `pageSize` pour l'inventaire typé ;
- `page` / `pageSize` pour les rapports, avec plafond 500 ;
- `page` / `pageSize` pour les modules génériques, avec plafond 500 ;
- défaut 10 pour les listes typées ;
- défaut 50 pour les modules génériques.

`page` est 1-based. `page=0` n'est pas normalisé partout : les DAO typés calculent `(page - 1) * pageSize` et peuvent produire un `OFFSET` négatif.

### 3.2. Filtres

| Liste | Filtres disponibles | Filtres manquants |
|---|---|---|
| Articles | `search`, `warehouseId`, `status` | type, catégorie, entreprise, dates |
| Types | `search` | catégorie, entreprise, statut |
| Catégories | `search` | entreprise |
| Entrepôts | `search` | entreprise, agence, actif |
| Mouvements | `search` (référence/notes) | article, entrepôt, type, dates, entreprise |
| Affectations typées | `search` (conditions/notes) | article, employé, poste, actif |
| Affectations génériques | actives uniquement | article, employé, poste |
| Comptages | aucun filtre exposé | entrepôt, statut, dates, entreprise |
| Rapport mouvements | `itemId`, `warehouseId` | type, dates, entreprise, acteur |

Le frontend devra souvent filtrer côté client, ce qui est incompatible avec de gros volumes.

---

## 4. Codes HTTP et erreurs

### 4.1. Codes de succès

| Situation | Codes observés |
|---|---|
| Lecture | 200 |
| Création typée | 201 |
| Création générique | 200 |
| Mise à jour | 200 |
| Suppression | 204 |
| Création d'opération | 200 |
| Création de comptage | 200 |
| Retour d'affectation | 200 |

L'absence de `Location` sur les 201 et l'absence de 204 sur certaines suppressions/retours rendent le contrat imprévisible.

### 4.2. Erreurs douces

Certains modules renvoient **200** avec :

```json
{
  "errors": {
    "message": "Affectation d'article introuvable",
    "detail": "...",
    "code": "NOT_FOUND"
  }
}
```

Cela concerne notamment :

- `GET /api/inventory/assignments/{id}` ;
- `PATCH /api/inventory/assignments/{id}` ;
- `POST /api/inventory/assignments/{id}/return` ;
- `POST /api/inventory/assignments` avec `{}` ;
- `POST /api/inventory/operations/*` avec `{}` ;
- `POST /api/inventory/inventory-counts` avec `{}` ;
- `POST /api/inventory/inventory-counts/{id}/close` sur un identifiant inconnu.

Un frontend standard qui teste `response.ok` considérera ces erreurs comme des succès.

### 4.3. Erreurs métier en 500

Les cas suivants produisent **500** :

- doublon de QR Code ;
- doublon de code de type ;
- doublon de nom de catégorie ;
- doublon d'affectation active en course ;
- clé étrangère inexistante ;
- `operation_type` manquant ;
- `unit`/`quantity_total`/`minimum_stock` manquants à la création d'un type ;
- `status` d'employé manquant ;
- statut de comptage invalide ;
- référence protégée par RESTRICT.

Le frontend ne peut pas distinguer :

- une erreur de saisie ;
- un conflit de données ;
- une panne serveur.

### 4.4. Détails SQL

Le corps d'erreur contient le SQLSTATE et un message PostgreSQL. En production, cela expose la structure de la base et donne une expérience utilisateur inadaptée.

### 4.5. Format d'erreur

L'enveloppe `{"errors": {"message", "detail", "code"}}` est cohérente pour les exceptions. Elle ne l'est pas pour :

- les erreurs de validation de `@Valid` (même enveloppe, mais code `VALIDATION_ERROR`) ;
- les erreurs de filtre de sécurité (même enveloppe, codes `UNAUTHORIZED`/`FORBIDDEN`) ;
- les erreurs douces en 200 ;
- les erreurs 500 SQL.

Il n'y a pas de `Problem Details` (RFC 9457), pas de `traceId`, pas de `timestamp`, pas de liste de champs structurée.

---

## 5. Dates, identifiants et enums

### 5.1. Dates

Les DTO utilisent `java.sql.Timestamp`. La sérialisation JSON est déléguée à Jackson. Le contrat ne précise pas :

- le fuseau ;
- le format ;
- le comportement en cas d'absence.

Certaines réponses de création renvoient `null` alors que la base a appliqué `now()` :

- `assigned_at` d'une affectation ;
- `movement_date` d'un mouvement.

Le frontend doit recharger la ressource pour afficher la date réelle.

### 5.2. Identifiants

- Les ID sont des UUID sous forme de chaînes.
- Les réponses imbriquées contiennent parfois l'ID et l'objet complet.
- Les réponses plates contiennent l'ID sans libellé.
- Aucun endpoint de résolution en lot (`POST /items/by-ids`) n'existe.

### 5.3. Enums

Les enums sont exposés en majuscules (`AVAILABLE`, `IN`, `ASSIGNED`). Le frontend doit maintenir une table de libellés. Aucun endpoint ne renvoie les valeurs possibles avec leur libellé.

---

## 6. Authentification côté frontend

### 6.1. Flux disponible

1. `POST /api/auth/register` (public, sans jeton).
2. `POST /api/auth/login` → jeton + profil.
3. Appels avec `Authorization: Bearer`.
4. `GET /api/auth/whoami` pour le profil.
5. `POST /api/auth/refresh` pour prolonger.

### 6.2. Ce qui manque

- pas de refresh token distinct ;
- pas de logout JWT avec révocation ;
- pas de révocation ;
- pas de expiration glissante contrôlée ;
- pas de claim d'entreprise ;
- pas de permissions dans le jeton ;
- pas de gestion de session multi-appareils ;
- pas de mot de passe oublié ;
- pas de changement de mot de passe exposé.

### 6.3. Stockage du jeton

Le backend ne dicte pas le stockage. Pour un frontend web, un jeton dans `localStorage` est exposé au XSS ; dans un cookie, il faut CSRF et SameSite. Le CORS permissif et l'absence de CSP explicite augmentent le risque. Ce sont des décisions à prendre avant le frontend, pas après.

---

## 7. Adéquation par écran

| Écran | Faisabilité actuelle | Blocages |
|---|---|---|
| Liste des articles | **Bonne** | pagination sans total, N+1, filtres limités |
| Détail d'un article | **Bonne** | relations parfois nulles, pas d'historique fiable |
| Création d'un article | **Moyenne** | type/catégorie/entrepôt cross-company, QR modifiable |
| Création d'un type | **Faible** | omission de `unit`/quantités → 500 |
| Liste des mouvements | **Moyenne** | champs incomplets, filtres limités, tri par ID |
| Création d'un mouvement | **Faible** | pas de quantité, pas de source/destination, pas d'acteur |
| Détail d'une affectation | **Moyenne** | acteurs absents, deux API |
| Création d'une affectation | **Moyenne** | employé inactif/cross-company accepté |
| Retour d'affectation | **Faible** | no-op si `returned_at` absent, pas de restauration d'entrepôt |
| Scan QR | **Faible** | réponse non enrichie, globale, pas d'action |
| Étiquette QR | **Moyenne** | PNG seulement, pas de PDF/lot |
| Dashboard stock | **Faible** | compteurs globaux, stock non fiable |
| Alertes low-stock | **Faible** | `quantity_total` manuel |
| Comptage d'inventaire | **Faible** | pas de quantité attendue, pas de réconciliation |
| Opérations | **Très faible** | en-tête seul, type non déduit, 500 si absent |
| Administration utilisateurs | **Moyenne** | ADMIN global, pas de tenant |
| Gestion des rôles/permissions | **Moyenne** | permissions non consommées |

---

## 8. Performance et exploitation

### 8.1. N+1

`ItemRestMapper.toRest` charge plusieurs relations par article. Une liste de 10 articles peut générer plusieurs dizaines de requêtes. Depuis le Lot 2, ces requêtes utilisent le pool Spring/HikariCP, mais le N+1 reste une source de latence.

### 8.2. Pool de connexions

Le pool HikariCP est désormais utilisé par les DAO via `DataSourceUtils`. La saturation par absence de pool est corrigée ; la charge reste à mesurer et la pagination non bornée peut encore concentrer les requêtes.

### 8.3. Listes non bornées

Les modules typés n'appliquent aucun plafond de `pageSize`. Un client peut demander toute la table.

### 8.4. Images QR

L'image est régénérée à chaque appel, sans cache. Pour une planche de 100 étiquettes, cela signifie 100 générations ZXing et 100 appels HTTP.

### 8.5. Absence de versionnement

Aucun `ETag`, `If-Match` ou champ `version`. Deux utilisateurs peuvent modifier le même article et écraser mutuellement leurs changements.

### 8.6. Absence d'idempotence

Aucun `Idempotency-Key` :

- un double clic sur « créer une affectation » peut produire un conflit ou un doublon ;
- un double clic sur « créer une opération » crée deux en-têtes ;
- un double clic sur « retour » peut être accepté deux fois ;
- un scan réseau peut être rejoué.

---

## 9. Conventions recommandées pour le futur contrat

Ces recommandations sont des propositions, pas des changements appliqués.

1. **Versionner** : `/api/v1/...`.
2. **Tenant dans le jeton** : ne jamais accepter `company_id` du client pour les routes métier.
3. **Pagination normalisée** :
   ```json
   {
     "items": [],
     "page": 1,
     "pageSize": 20,
     "total": 0,
     "totalPages": 0
   }
   ```
4. **Erreurs stables** : `application/problem+json`, codes métier indépendants de SQLSTATE.
5. **Statuts corrects** : 201 + `Location`, 204 pour les suppressions, 409 pour les conflits, 422 pour les règles métier, 404 pour les inconnus.
6. **Idempotence** : `Idempotency-Key` sur les créations sensibles.
7. **Verrouillage optimiste** : `version` ou `ETag`.
8. **Ressources plates enrichies** : inclure les libellés nécessaires ou fournir un endpoint de résolution en lot.
9. **Dates normalisées** : ISO-8601 UTC, jamais `null` après création.
10. **OpenAPI complet** : exemples, erreurs, enums, pagination.
11. **Pas de détails SQL** en production.
12. **Un seul module d'affectation**.
13. **Scan QR enrichi** et tenant-scoped.
14. **Étiquette PDF** et impression en lot.

---

## 10. Verdict

L'API est **suffisante pour un frontend de démonstration en lecture** sur le catalogue et les QR Codes. Elle n'est pas **suffisante pour un frontend de production** sur le stock, les affectations, les opérations, les comptages et le mobile.

Le frontend ne doit pas être démarré avant que les contrats de tenancy, de stock, d'affectation et de scan soient stabilisés. Sinon, il devra être réécrit en même temps que le backend.

Les endpoints proposés sont dans le rapport 13 ; les priorités dans le rapport 12 ; les critères d'acceptation dans le rapport 15.
