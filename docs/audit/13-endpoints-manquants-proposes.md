# 13 — Endpoints manquants proposés

**Note Lot 7 (23 octobre 2026) :** les routes canoniques sont désormais préfixées `/api/v1` ; l'alias `/api` reste accepté avec `Deprecation`/`Link`. Les chemins cités ci-dessous reflètent l'état de l'audit au moment de sa rédaction, sauf mise à jour explicite.

**Objectif :** spécifier les endpoints qui manquent pour rendre le backend utilisable par un frontend web et mobile, sans les implémenter. Ce document est une **proposition de contrat**, pas une description de l'existant.

**Avertissement :** aucun endpoint de ce rapport n'existe aujourd'hui. Les chemins, DTO, codes et règles ci-dessous sont des cibles. Ils ne doivent pas être consommés avant implémentation et validation.

---

## 1. Conventions proposées

### 1.1. Base et version

Toutes les nouvelles routes seraient sous :

```text
/api/v1
```

Les routes actuelles `/api/...` resteraient temporairement comme alias dépréciés, avec une date de retrait documentée.

### 1.2. Entreprise

`company_id` ne serait **jamais** lu dans le corps pour une route métier. Il serait :

- soit extrait d'un claim `company_id` du JWT ;
- soit chargé côté serveur depuis l'utilisateur authentifié.

Un administrateur de plateforme pourrait utiliser un en-tête explicite, mais uniquement avec une permission dédiée et un journal d'audit.

### 1.3. Autorisation

Permissions proposées :

| Permission | Usage |
|---|---|
| `inventory.read` | consulter articles, stocks, mouvements |
| `inventory.write` | créer/modifier le catalogue |
| `inventory.stock` | créer des mouvements et opérations |
| `inventory.assign` | affecter et restituer |
| `inventory.count` | créer, scanner et clôturer un comptage |
| `inventory.admin` | archivage, rotation QR, corrections |
| `users.manage` | administrer les utilisateurs |
| `roles.manage` | administrer les rôles |

`ADMIN` implique toutes les permissions dans son entreprise. Aucune permission globale ne traverse les entreprises.

### 1.4. Pagination

Réponse standard :

```json
{
  "items": [],
  "page": 1,
  "pageSize": 20,
  "total": 0,
  "totalPages": 0
}
```

- `page` commence à 1 ;
- `pageSize` est plafonné à 100 ;
- `total` est calculé côté serveur ;
- les filtres sont conservés dans les liens de navigation si un jour nécessaire.

### 1.5. Erreurs

Format `application/problem+json` :

```json
{
  "type": "https://sssm.example/errors/item-already-assigned",
  "title": "Article déjà affecté",
  "status": 409,
  "code": "ITEM_ALREADY_ASSIGNED",
  "detail": "L'article est déjà couvert par une affectation active.",
  "instance": "/api/v1/item-assignments",
  "traceId": "..."
}
```

- pas de SQLSTATE en production ;
- `code` stable et documenté ;
- `traceId` pour la corrélation ;
- 400 pour un corps invalide, 401 non authentifié, 403 interdit, 404 inconnu, 409 conflit, 422 règle métier, 500 panne interne.

### 1.6. Idempotence et concurrence

- `Idempotency-Key` obligatoire sur les `POST` qui créent un mouvement, une affectation, un comptage, une opération ou un scan.
- `If-Match` / `ETag` sur les `PATCH` sensibles.
- Une clé rejouée avec le même corps renvoie la même réponse ; avec un corps différent, renvoie 409 `IDEMPOTENCY_KEY_REUSED`.
- La durée de conservation des clés doit être documentée.

---

## 2. Tableau des endpoints proposés

**Note Lot 1 :** les routes `/api/auth/**` existantes ont été durcies (inscription fermée, refresh rotatif, logout, claims), mais aucun endpoint `/api/v1` n'a été créé à ce moment. Depuis le Lot 7, `/api/v1` est le préfixe canonique de tous les contrôleurs et `/api` reste un alias déprécié. E-01 à E-05 restent des propositions de contrat cible.

| ID | Priorité | Méthode | Chemin | Remplace / comble |
|---|---|---|---|---|
| E-01 | P0 | POST | `/api/v1/auth/register` | Inscription actuelle, désormais réservée aux administrateurs |
| E-02 | P0 | POST | `/api/v1/auth/login` | Login actuel, contrat durci |
| E-03 | P0 | POST | `/api/v1/auth/refresh` | Refresh actuel, rôles rechargés |
| E-04 | P0 | POST | `/api/v1/auth/logout` | Pas de logout JWT avec révocation |
| E-05 | P0 | GET | `/api/v1/auth/me` | `whoami`, avec entreprise et permissions |
| E-06 | P0 | GET | `/api/v1/stock/balances` | Absent |
| E-07 | P0 | GET | `/api/v1/stock/items/{itemId}` | Absent |
| E-08 | P0 | POST | `/api/v1/stock/movements` | Mouvement actuel, avec quantité et acteur |
| E-09 | P0 | GET | `/api/v1/stock/movements` | Liste actuelle, filtrée et paginée |
| E-10 | P1 | POST | `/api/v1/stock/movements/{id}/reverse` | `DELETE` actuel |
| E-11 | P1 | POST | `/api/v1/inventory-operations` | Opérations vides actuelles |
| E-12 | P1 | GET | `/api/v1/inventory-operations/{id}` | Absent |
| E-13 | P0 | POST | `/api/v1/item-assignments` | Deux modules actuels |
| E-14 | P0 | GET | `/api/v1/item-assignments` | Listes actuelles |
| E-15 | P0 | POST | `/api/v1/item-assignments/{id}/return` | Retour no-op actuel |
| E-16 | P1 | POST | `/api/v1/item-assignments/{id}/void` | `DELETE` actuel |
| E-17 | P1 | GET | `/api/v1/items/{itemId}/assignment` | Absent |
| E-18 | P0 | POST | `/api/v1/qr/resolve` | Scan global actuel |
| E-19 | P1 | POST | `/api/v1/qr/scans` | Journal de scan absent |
| E-20 | P1 | POST | `/api/v1/items/{itemId}/qr/rotate` | `PATCH qr_code` libre |
| E-21 | P2 | GET | `/api/v1/items/{itemId}/label` | PNG seul |
| E-22 | P2 | POST | `/api/v1/labels/batch` | Absent |
| E-23 | P0 | POST | `/api/v1/inventory-counts` | Comptage sans attendu |
| E-24 | P0 | GET | `/api/v1/inventory-counts/{id}` | Absent |
| E-25 | P0 | GET | `/api/v1/inventory-counts/{id}/lines` | Résultats non structurés |
| E-26 | P0 | POST | `/api/v1/inventory-counts/{id}/scans` | Scan sans quantité |
| E-27 | P0 | POST | `/api/v1/inventory-counts/{id}/close` | Clôture sans réconciliation |
| E-28 | P1 | POST | `/api/v1/inventory-counts/{id}/cancel` | Absent |
| E-29 | P1 | GET | `/api/v1/items/{itemId}/history` | Historique global/tronqué |
| E-30 | P1 | POST | `/api/v1/items/bulk` | Bulk partiel non transactionnel |
| E-31 | P2 | GET | `/api/v1/reference-data` | Absent |
| E-32 | P1 | GET | `/api/v1/dashboard/summary` | Dashboard global |
| E-33 | P1 | GET | `/api/v1/alerts/low-stock` | Alerte sur champ manuel |

---

## 3. Authentification

### E-01 — Inscription encadrée

```http
POST /api/v1/auth/register
```

**Corps :**

```json
{
  "invitationToken": "...",
  "username": "jdupont",
  "email": "jdupont@example.com",
  "password": "...",
  "firstName": "Jean",
  "lastName": "Dupont",
  "phone": "+33..."
}
```

**Règles :**

- `invitationToken` obligatoire, à usage unique, lié à une entreprise et à un rôle ;
- pas de `company_id` dans le corps ;
- le compte est créé `PENDING` ou `ACTIVE` selon la politique ;
- le rôle est celui de l'invitation, jamais `ADMIN` par défaut ;
- l'email est vérifié avant activation ;
- réponse **201** avec l'utilisateur, **sans jeton** ;
- **409** si username/email déjà utilisé ;
- **422** si l'invitation est expirée ou consommée.

**Décision appliquée au Lot 1 :** l'inscription publique n'est pas conservée. Les comptes sont créés par un administrateur d'entreprise ou un admin plateforme, avec une entreprise explicite. Un compte sans rôle ne peut pas accéder à l'inventaire.

### E-02 — Connexion

```http
POST /api/v1/auth/login
```

**Corps :**

```json
{
  "login": "jdupont",
  "password": "..."
}
```

**Réponse 200 :**

```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": "...",
    "username": "jdupont",
    "companyId": "...",
    "roles": ["INVENTORY"],
    "permissions": ["inventory.read", "inventory.assign"]
  }
}
```

**Erreurs :**

- **401** identifiants invalides ;
- **403** compte inactif ou verrouillé ;
- **429** trop de tentatives.

Le mot de passe ne doit jamais être renvoyé, ni dans un log, ni dans une erreur.

### E-03 — Refresh avec rotation

```http
POST /api/v1/auth/refresh
```

**Corps :**

```json
{ "refreshToken": "..." }
```

**Règles :**

- le refresh token est distinct de l'access token ;
- il est stocké hashé côté serveur, avec expiration, appareil et date de dernière utilisation ;
- chaque utilisation le révoque et en émet un nouveau ;
- les rôles et permissions sont **rechargés depuis la base** ;
- un utilisateur inactif ou supprimé est refusé ;
- réponse **200** avec nouveau access token et nouveau refresh token ;
- **401** si le refresh token est inconnu, expiré, révoqué ou rejoué.

### E-04 — Déconnexion

```http
POST /api/v1/auth/logout
Authorization: Bearer ...
```

**Règles :**

- révoque le refresh token courant ;
- ajoute le `jti` de l'access token à une liste de révocation jusqu'à son expiration ;
- réponse **204** ;
- idempotent : un second appel renvoie 204.

### E-05 — Profil courant

```http
GET /api/v1/auth/me
```

**Réponse 200 :**

```json
{
  "id": "...",
  "username": "jdupont",
  "company": { "id": "...", "name": "..." },
  "roles": ["INVENTORY"],
  "permissions": ["inventory.read", "inventory.assign"],
  "status": "ACTIVE"
}
```

**Erreurs :** **401** sans jeton, **404** si le compte n'existe plus.

---

## 4. Stock

### E-06 — Soldes par entrepôt et type

```http
GET /api/v1/stock/balances?warehouseId=...&itemTypeId=...&page=1&pageSize=20
```

**Réponse 200 :**

```json
{
  "items": [
    {
      "companyId": "...",
      "warehouse": { "id": "...", "name": "Entrepôt A" },
      "itemType": { "id": "...", "code": "PERFO", "name": "Perforateur" },
      "available": 12,
      "assigned": 5,
      "damaged": 1,
      "lost": 0,
      "retired": 2,
      "total": 20,
      "minimumStock": 4,
      "belowMinimum": false,
      "lastMovementAt": "2026-10-20T10:00:00Z",
      "lastInventoryAt": "2026-10-01T08:00:00Z"
    }
  ],
  "page": 1,
  "pageSize": 20,
  "total": 1,
  "totalPages": 1
}
```

**Règle :** les soldes sont calculés à partir d'une source unique (mouvements validés ou état des articles), pas de `quantity_total` saisi à la main.

### E-07 — État d'un article

```http
GET /api/v1/stock/items/{itemId}
```

**Réponse 200 :**

```json
{
  "item": { "id": "...", "qrCode": "...", "status": "ASSIGNED" },
  "itemType": { "id": "...", "code": "PERFO", "name": "Perforateur" },
  "warehouse": null,
  "activeAssignment": {
    "id": "...",
    "employee": { "id": "...", "firstName": "...", "lastName": "..." },
    "post": { "id": "...", "name": "..." },
    "assignedAt": "2026-10-20T09:00:00Z",
    "expectedReturnAt": "2026-10-27T09:00:00Z"
  },
  "lastMovement": { "id": "...", "type": "OUT", "occurredAt": "..." },
  "lastInventoryAt": "..."
}
```

**Erreurs :** **404** si l'article n'existe pas ou n'appartient pas à l'entreprise.

### E-08 — Créer un mouvement

```http
POST /api/v1/stock/movements
Idempotency-Key: ...
```

**Corps :**

```json
{
  "itemId": "...",
  "movementType": "OUT",
  "quantity": 1,
  "sourceWarehouseId": "...",
  "destinationWarehouseId": null,
  "employeeId": "...",
  "operationId": null,
  "reference": "BL-2026-001",
  "occurredAt": "2026-10-21T10:00:00Z",
  "notes": "..."
}
```

**Règles :**

- `movementType` ∈ `IN`, `OUT`, `TRANSFER`, `RETURN`, `DESTRUCTION`, `ADJUSTMENT` ;
- `quantity` > 0 ;
- `IN` exige `destinationWarehouseId` ;
- `OUT` exige `sourceWarehouseId` et un article `AVAILABLE` ;
- `TRANSFER` exige source et destination différentes, même entreprise ;
- `RETURN` exige une affectation active et la clôt ;
- `DESTRUCTION` exige un motif et interdit un article affecté ;
- `ADJUSTMENT` exige `adjustmentDirection` (`INCREASE`/`DECREASE`) et une raison ;
- `performedBy` est déduit du jeton, jamais du corps ;
- la quantité est décrémentée/incrémentée de façon atomique ;
- réponse **201** avec le mouvement **relu en base**, y compris `occurredAt`, `createdAt`, `performedBy` ;
- **409** stock insuffisant ou article déjà affecté ;
- **422** transition interdite ;
- **404** article/entrepôt/employé inconnu dans l'entreprise.

### E-09 — Lister les mouvements

```http
GET /api/v1/stock/movements?itemId=...&warehouseId=...&movementType=...&from=...&to=...&page=1&pageSize=20
```

**Réponse 200 :** `PageResponse<Movement>` avec les champs complets :

- `id`, `itemId`, `itemType`, `warehouse`, `employee` ;
- `movementType`, `quantity` ;
- `sourceWarehouse`, `destinationWarehouse` ;
- `operationId`, `performedBy` ;
- `occurredAt`, `createdAt`, `reversedBy`, `reversedAt`.

**Règles :** tri par `occurredAt` décroissant, filtres combinables, total calculé.

### E-10 — Annuler par compensation

```http
POST /api/v1/stock/movements/{id}/reverse
```

**Corps :**

```json
{ "reason": "Erreur de saisie", "occurredAt": "..." }
```

**Règles :**

- ne modifie ni ne supprime le mouvement d'origine ;
- crée un mouvement inverse de même quantité ;
- marque l'original `reversedBy` ;
- **409** si déjà annulé ;
- **422** si l'annulation produirait un stock négatif ;
- **201** avec le mouvement de compensation.

---

## 5. Opérations d'inventaire

### E-11 — Créer une opération avec lignes

```http
POST /api/v1/inventory-operations
Idempotency-Key: ...
```

**Corps :**

```json
{
  "type": "RECEIPT",
  "reference": "REC-2026-001",
  "occurredAt": "2026-10-21T10:00:00Z",
  "notes": "...",
  "lines": [
    {
      "itemId": "...",
      "quantity": 1,
      "destinationWarehouseId": "..."
    }
  ]
}
```

**Règles :**

- `type` ∈ `RECEIPT`, `ISSUE`, `TRANSFER`, `RETURN` ;
- au moins une ligne ;
- chaque ligne crée un mouvement lié à l'opération ;
- l'opération, les lignes et les mouvements sont atomiques ;
- **201** avec l'opération et ses lignes ;
- **409** si une ligne est en conflit ;
- **422** si une ligne est invalide ;
- une opération validée n'est pas modifiable ; elle se compense.

### E-12 — Détail d'une opération

```http
GET /api/v1/inventory-operations/{id}
```

**Réponse 200 :** en-tête, lignes, mouvements liés, acteur, dates, statut.

---

## 6. Affectations

### E-13 — Créer une affectation

```http
POST /api/v1/item-assignments
Idempotency-Key: ...
```

**Corps :**

```json
{
  "itemId": "...",
  "employeeId": "...",
  "postId": "...",
  "expectedReturnAt": "2026-10-28T10:00:00Z",
  "conditionOnAssignment": "Bon état",
  "notes": "..."
}
```

**Règles :**

- article existant, `AVAILABLE`, dans l'entreprise ;
- employé existant, `ACTIVE`, dans l'entreprise ;
- poste existant, actif, dans l'entreprise ;
- aucune affectation active pour l'article ;
- `assignedBy` déduit du jeton ;
- l'insertion et le passage à `ASSIGNED` sont atomiques ;
- réponse **201** avec `assignedAt` relu ;
- **409** article déjà affecté ;
- **422** employé inactif, poste inactif, incohérence d'entreprise ;
- **404** référence inconnue.

### E-14 — Lister les affectations

```http
GET /api/v1/item-assignments?itemId=...&employeeId=...&postId=...&active=true&page=1&pageSize=20
```

**Réponse 200 :** `PageResponse<Assignment>` avec :

- article et type ;
- employé et poste ;
- `assignedAt`, `expectedReturnAt`, `returnedAt` ;
- `assignedBy`, `returnedBy` ;
- conditions et notes.

### E-15 — Restituer

```http
POST /api/v1/item-assignments/{id}/return
Idempotency-Key: ...
```

**Corps :**

```json
{
  "returnedAt": "2026-10-21T16:00:00Z",
  "conditionOnReturn": "Bon état",
  "returnWarehouseId": "...",
  "notes": "..."
}
```

**Règles :**

- l'affectation doit être active ;
- `returnedAt` est obligatoire, ou positionné par le serveur à `now()` ; il n'est jamais laissé `NULL` ;
- `returnedBy` déduit du jeton ;
- l'article redevient `AVAILABLE` ;
- `current_warehouse_id` est restauré depuis `returnWarehouseId` ou l'entrepôt d'origine ;
- **200** avec l'affectation et l'article mis à jour ;
- **409** si déjà retournée ;
- **422** si `returnedAt < assignedAt` ou entrepôt invalide.

### E-16 — Annuler une affectation erronée

```http
POST /api/v1/item-assignments/{id}/void
```

**Corps :** `{ "reason": "..." }`

**Règles :**

- réservé à `inventory.admin` ;
- interdit si l'affectation a déjà été retournée ;
- ne supprime pas la ligne : la marque annulée avec acteur, date et motif ;
- remet l'article `AVAILABLE` atomiquement ;
- **409** si déjà annulée ou retournée ;
- **204** ou **200** avec la ligne annulée.

### E-17 — Affectation active d'un article

```http
GET /api/v1/items/{itemId}/assignment
```

**Réponse 200 :** affectation active ou `null` documenté ; **404** si l'article n'existe pas.

---

## 7. QR Codes et mobile

### E-18 — Résoudre un code

```http
POST /api/v1/qr/resolve
```

**Corps :**

```json
{ "code": "QR-1A2B3C4D" }
```

**Réponse 200 :**

```json
{
  "item": {
    "id": "...",
    "qrCode": "QR-1A2B3C4D",
    "status": "ASSIGNED",
    "description": "..."
  },
  "itemType": { "id": "...", "code": "...", "name": "..." },
  "category": { "id": "...", "name": "..." },
  "warehouse": { "id": "...", "name": "..." },
  "activeAssignment": {
    "id": "...",
    "employee": { "id": "...", "firstName": "...", "lastName": "..." },
    "post": { "id": "...", "name": "..." },
    "expectedReturnAt": "..."
  },
  "allowedActions": ["RETURN", "TRANSFER", "COUNT"]
}
```

**Règles :**

- filtre strictement sur l'entreprise du jeton ;
- **404** si le code n'existe pas dans l'entreprise ;
- **409** si le code est ambigu (ne devrait pas arriver avec l'unicité par entreprise) ;
- aucun effet de bord : résoudre n'est pas agir ;
- limitation de débit par utilisateur et par appareil ;
- la réponse ne contient ni `archived_by` ni détails internes inutiles.

### E-19 — Journaliser un scan

```http
POST /api/v1/qr/scans
Idempotency-Key: ...
```

**Corps :**

```json
{
  "code": "QR-1A2B3C4D",
  "action": "RETURN",
  "itemId": "...",
  "assignmentId": "...",
  "warehouseId": "...",
  "deviceId": "...",
  "clientTimestamp": "2026-10-21T16:00:00Z"
}
```

**Règles :**

- enregistre l'utilisateur, l'entreprise, l'appareil, l'horodatage serveur et le résultat ;
- **202** si l'action est asynchrone ; **200** si elle est immédiate ;
- ne modifie le stock que si l'action est explicitement une action métier ;
- idempotent ;
- **422** si l'action n'est pas autorisée pour cet article.

### E-20 — Rotation contrôlée du QR

```http
POST /api/v1/items/{itemId}/qr/rotate
```

**Corps :** `{ "reason": "...", "revokeOldAfter": "..." }`

**Règles :**

- réservé à `inventory.admin` ;
- génère un nouveau code et conserve l'ancien avec une date de révocation ;
- les scans de l'ancien code renvoient **410 Gone** après la période de grâce ;
- journalise l'acteur et le motif ;
- **409** si un travail d'impression est en cours.

### E-21 — Étiquette PDF

```http
GET /api/v1/items/{itemId}/label?format=pdf&size=50x30
```

**Réponse 200 :** `application/pdf`, avec QR, nom du type, code lisible, entreprise et entrepôt.

### E-22 — Planche d'étiquettes

```http
POST /api/v1/labels/batch
```

**Corps :**

```json
{
  "itemIds": ["..."],
  "format": "pdf",
  "layout": "A4-3x8",
  "includeText": true
}
```

**Réponse 200 :** `application/pdf` ou archive, avec un rapport des identifiants inconnus.

---

## 8. Comptages d'inventaire

### E-23 — Créer un comptage avec attendu figé

**Statut (Lot 6) :** exécuté sous `POST /api/v1/inventory/inventory-counts` (entreprise issue du jeton, entrepôt validé, statut `IN_PROGRESS`, instantané `inventory_count_expected`). Le `scope` par type ou catégorie reste hors périmètre ; le `201 + Location` est fourni depuis le Lot 7.

```http
POST /api/v1/inventory-counts
Idempotency-Key: ...
```

**Corps :**

```json
{
  "warehouseId": "...",
  "scope": { "itemTypeIds": ["..."], "categoryIds": ["..."] },
  "includeExpected": true,
  "notes": "..."
}
```

**Règles :**

- crée un en-tête et une ligne par article attendu ;
- fige `expectedQuantity`, `expectedWarehouseId` et le statut attendu au moment de l'ouverture ;
- l'entreprise vient du jeton ;
- **201** avec le comptage et le nombre de lignes ;
- **409** si un comptage ouvert existe déjà pour le même périmètre ;
- **422** si l'entrepôt est invalide.

### E-24 — Détail d'un comptage

**Statut (Lot 6) :** exécuté sous `GET /api/inventory/inventory-counts/{id}`, cloisonné par entreprise (`404` si inconnu ou étranger).

```http
GET /api/v1/inventory-counts/{id}
```

**Réponse 200 :** en-tête, statut, dates, créateur, clôturant, totaux d'écart.

### E-25 — Lignes d'un comptage

**Statut (Lot 6) :** exécuté sous `GET /api/inventory/inventory-counts/{id}/results?page=&pageSize=` (le nom `lines` n'a pas été ajouté) avec `X-Total-Count`, `X-Page` et `X-Page-Size`. Les filtres par statut ou type d'écart restent absents.

```http
GET /api/v1/inventory-counts/{id}/lines?status=...&page=1&pageSize=50
```

**Réponse 200 :** lignes avec :

- `itemId`, `itemType`, `qrCode` ;
- `expectedQuantity`, `countedQuantity` ;
- `expectedWarehouse`, `countedWarehouse` ;
- `difference`, `discrepancyType` ;
- `scannedBy`, `scannedAt`, `notes`.

**Erreurs :** **404** si le comptage n'existe pas dans l'entreprise.

### E-26 — Scanner une ligne

**Statut (Lot 6) :** exécuté sous `POST /api/inventory/inventory-counts/{id}/scans` : attendu lu dans l'instantané, écart calculé serveur, article hors périmètre refusé en `409 ITEM_OUT_OF_SCOPE`, doublon refusé en `400`.

```http
POST /api/v1/inventory-counts/{id}/scans
Idempotency-Key: ...
```

**Corps :**

```json
{
  "itemId": "...",
  "countedQuantity": 1,
  "warehouseId": "...",
  "condition": "GOOD",
  "notes": "..."
}
```

**Règles :**

- le comptage doit être `OPEN` ;
- l'article doit appartenir au périmètre et à l'entreprise ;
- le serveur calcule `difference` et `discrepancyType`, jamais le client ;
- un scan déjà enregistré pour le même article est idempotent ou rejeté **409** selon la politique ;
- **201** avec la ligne mise à jour ;
- **422** si l'article est hors périmètre.

### E-27 — Clôturer et réconcilier

**Statut (Lot 6) :** exécuté sous `POST /api/inventory/inventory-counts/{id}/close` : clôture idempotente (200), comptage vide refusé (`409 COUNT_EMPTY`), `completed_at` serveur, mouvements `ADJUSTMENT`, soldes et `last_inventory_at` dans une transaction. `applyAdjustments` n'existe pas : les ajustements sont toujours appliqués.

```http
POST /api/v1/inventory-counts/{id}/close
Idempotency-Key: ...
```

**Corps :**

```json
{
  "applyAdjustments": true,
  "notes": "..."
}
```

**Règles :**

- le comptage doit être `OPEN` ;
- toutes les lignes doivent être traitées ou explicitement marquées non trouvées ;
- `completed_at` est renseigné par le serveur ;
- si `applyAdjustments` : crée les mouvements `ADJUSTMENT` compensatoires, met à jour les soldes et `item.last_inventory_at` ;
- le tout est atomique ;
- **200** avec le bilan ;
- **409** si déjà clôturé ;
- **422** si des lignes sont incomplètes.

### E-28 — Annuler un comptage

**Statut (Lot 6) :** exécuté sous `POST /api/inventory/inventory-counts/{id}/cancel` : idempotent, sans ajustement, `409 COUNT_COMPLETED` après clôture. Le motif libre n'est pas stocké.

```http
POST /api/v1/inventory-counts/{id}/cancel
```

**Corps :** `{ "reason": "..." }`

**Règles :** interdit après clôture ; aucun ajustement ; statut `CANCELLED`.

---

## 9. Catalogue et historique

### E-29 — Historique filtré d'un article

```http
GET /api/v1/items/{itemId}/history?types=MOVEMENT,ASSIGNMENT&from=...&to=...&page=1&pageSize=50
```

**Réponse 200 :** `PageResponse<HistoryEvent>` avec :

- type d'événement ;
- date ;
- acteur ;
- mouvement ou affectation complet ;
- entrepôt source/destination ;
- quantité.

**Règles :**

- **404** si l'article n'existe pas dans l'entreprise ;
- filtres et pagination côté SQL ;
- tri par date décroissante ;
- aucun chargement global.

### E-30 — Création en masse atomique ou explicite

```http
POST /api/v1/items/bulk
Idempotency-Key: ...
```

**Corps :**

```json
{
  "atomic": true,
  "items": [ { "...": "..." } ]
}
```

**Règles :**

- `atomic=true` : tout ou rien, **422** avec la liste des erreurs ;
- `atomic=false` : réponse 200 avec `created`, `failed`, chaque échec indexé ;
- validation Jakarta sur chaque élément ;
- QR unique par entreprise ;
- pas de fuite SQL.

### E-31 — Données de référence

```http
GET /api/v1/reference-data
```

**Réponse 200 :** enums, libellés, statuts, types de mouvement, types d'écart, permissions, tailles d'étiquette. Évite de dupliquer les libellés dans le frontend.

---

## 10. Dashboard et alertes

### E-32 — Dashboard tenant-scoped

```http
GET /api/v1/dashboard/summary?warehouseId=...&from=...&to=...
```

**Réponse 200 :**

```json
{
  "items": { "total": 120, "available": 80, "assigned": 30, "damaged": 5, "lost": 2, "retired": 3 },
  "warehouses": 4,
  "openAssignments": 30,
  "overdueAssignments": 2,
  "openCounts": 1,
  "movements": { "in": 12, "out": 8, "transfer": 3, "return": 5, "adjustment": 1 },
  "generatedAt": "..."
}
```

**Règles :** périmètre entreprise, filtres optionnels, pas de compteur global.

### E-33 — Alertes low-stock réelles

```http
GET /api/v1/alerts/low-stock?warehouseId=...
```

**Réponse 200 :** types sous le seuil, avec stock disponible réel, seuil, entrepôt, nombre d'articles et date du dernier mouvement.

**Règle :** le seuil est une configuration par entrepôt (`warehouse_stock_config`), pas un champ manuel du type.

---

## 11. Ordre de mise en œuvre proposé

1. **E-01 à E-05** : fermer l'accès et stabiliser l'identité.
2. **E-06 à E-10** : rendre le stock calculable et auditable.
3. **E-13 à E-17** : unifier les affectations.
4. **E-18 à E-22** : rendre le mobile utilisable.
5. **E-23 à E-28** : réconcilier les comptages. *Exécuté au Lot 6.*
6. **E-29 à E-33** : contrats de lecture et pilotage.

Les endpoints E-01 à E-18 sont des prérequis au frontend de production. Les autres peuvent suivre.

---

## 12. Ce que ce rapport ne fait pas

- Il ne crée aucune route.
- Il ne modifie aucun contrôleur, service, DAO, DTO, migration ou test.
- Il ne choisit pas à la place du métier entre stock déclaratif et stock calculé.
- Il ne garantit pas que les noms de champs proposés correspondent à un schéma déjà migré : certains nécessitent des colonnes ou tables nouvelles, signalées dans le rapport 14.
