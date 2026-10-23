# Documentation API — SSSM Backend

Spécifications OpenAPI 3.0.3 générées à partir des contrôleurs Spring du projet,
module par module. Chaque fichier est autonome et peut être importé directement
dans Swagger Editor, Postman, Insomnia ou Redoc.

**État au 23 octobre 2026 (Lot 7) :** les chemins sont canoniquement préfixés
`/api/v1`. L'alias historique `/api` reste accepté par le backend et renvoie
`Deprecation: true` et `Link` vers `/api/v1`, mais il n'est plus documenté.
Les erreurs utilisent `application/problem+json` (RFC 9457).

## Organisation

| Module | Dossier | Spécifications | Opérations |
|---|---|---:|---:|
| `a02_organisation` | [`02_organisation/`](02_organisation/) | 3 | 15 |
| `a03_authentication` | [`03_authentication/`](03_authentication/) | 4 | 23 |
| `a04_personnel` | [`04_personnel/`](04_personnel/) | 2 | 10 |
| `a05_clients` | [`05_clients/`](05_clients/) | 2 | 10 |
| `a06_sites` | [`06_sites/`](06_sites/) | 3 | 15 |
| `a11_inventory` | [`11_inventory/`](11_inventory/) | 13 | 75 |
| **Total** | | **27** | **148** |

### Détail des fichiers

- **02_organisation**
  - `company-openapi.yaml` — `/api/v1/company`
  - `agency-openapi.yaml` — `/api/v1/agency`
  - `branch-openapi.yaml` — `/api/v1/branch`
- **03_authentication**
  - `auth-openapi.yaml` — `/api/v1/auth` (login/refresh/logout publics, register réservé aux administrateurs)
  - `user-openapi.yaml` — `/api/v1/users` (ADMIN ou `users.manage`)
  - `role-openapi.yaml` — `/api/v1/roles` (ADMIN ou `roles.manage`)
  - `permission-openapi.yaml` — `/api/v1/permissions` (ADMIN)
- **04_personnel**
  - `employee-openapi.yaml` — `/api/v1/employees`
  - `employee_document-openapi.yaml` — `/api/v1/employee-documents`
- **05_clients**
  - `client-openapi.yaml` — `/api/v1/clients`
  - `client_contact-openapi.yaml` — `/api/v1/client-contacts`
- **06_sites**
  - `site-openapi.yaml` — `/api/v1/sites`
  - `security_post-openapi.yaml` — `/api/v1/security-posts`
  - `post_requirement-openapi.yaml` — `/api/v1/post-requirements`
- **11_inventory**
  - `assignment-openapi.yaml`, `dashboard-openapi.yaml`, `inventory_count-openapi.yaml`,
    `item-openapi.yaml`, `item_assignment-openapi.yaml`, `item_category-openapi.yaml`,
    `item_type-openapi.yaml`, `label-openapi.yaml`, `operation-openapi.yaml`,
    `qr_scan-openapi.yaml`, `report-openapi.yaml`, `stock_movement-openapi.yaml`,
    `warehouse-openapi.yaml`

Aucune spécification n'est fournie pour `a00_infrastructure`, `a01_common` et
`controller_test` : ces packages n'exposent pas d'API métier. La route
`GET /api/v1/inventory/stock-balances` n'a pas encore de fichier dédié ; c'est
l'écart connu entre les 148 opérations documentées et les 150 routes métier.

## Conventions communes

### Serveur et authentification

- Base URL : `http://localhost:8080`.
- Toutes les routes exigent `Authorization: Bearer <JWT>`, sauf :
  - `/`, `/ping`, `/error`, `POST /api/v1/auth/login`,
    `POST /api/v1/auth/refresh` et `POST /api/v1/auth/logout` ;
  - `POST /api/v1/auth/register` exige `ROLE_ADMIN` ou `users.manage`.
- JWT obtenu via `POST /api/v1/auth/login` (durée par défaut : 1 heure).
- `/api/v1/users/**` exige `ROLE_ADMIN` ou `users.manage` ; `/api/v1/roles/**`
  exige `ROLE_ADMIN` ou `roles.manage` ; `/api/v1/permissions/**` exige `ADMIN`.
- Un JWT absent, invalide ou expiré renvoie `401` en Problem Details ; un JWT
  valide sans le rôle requis renvoie `403`.

### Pagination et recherche

- Les listes d'inventaire (articles, mouvements, affectations, comptages et
  lignes, soldes, rapports) renvoient `PageResponse` :
  `items`, `page`, `pageSize`, `total`, `totalPages`.
- `page` est 1-based ; `page=0` est normalisé à 1. `pageSize` est plafonné à
  `500` sur l'inventaire ; le défaut varie selon le point d'appel (20 sur les
  listes paginées, 50 sur certaines listes historiques).
- Les modules 02 à 06 utilisent `page` (défaut `1`) et `pageSize` (défaut `10`)
  sans métadonnées de total et sans plafond global : leur pagination n'est pas
  encore normalisée (limite connue).
- `search` est une recherche libre `ILIKE`, dont les colonnes varient selon la
  ressource (documenté dans chaque fichier).
- Les listes par parent (`/employee/{id}`, `/client/{id}`, `/site/{id}`,
  `/post/{id}`) ne sont pas paginées.

### Formats et validation

- Dates : `date` (`yyyy-MM-dd`), horodatages : `date-time` ISO-8601 **UTC**
  (`Z` ou `+00:00`), heures : `time` (`HH:mm:ss`).
- Identifiants : UUID en `string`. Un UUID malformé renvoie `400`, pas `500`.
- Enums : `ACTIVE`/`INACTIVE`, `ACTIVE`/`INACTIVE`/`LOCKED`,
  `ACTIVE`/`INACTIVE`/`SUSPENDED`/`LEFT`, ainsi que les enums métier d'inventaire.
- Les corps de création et de mise à jour passent par des DTO dédiés et la
  validation Jakarta (`@Valid`). Un échec renvoie `400` avec
  `code: VALIDATION_ERROR` et un tableau `errors[{field, message}]`.
- Les champs inconnus des DTO typés sont ignorés, sauf pour les scans
  d'inventaire qui appliquent une liste blanche stricte.

### Politique HTTP

- Création : `201` avec la ressource créée et `Location`. Les créations en lot
  et les actions (`scan`, `close`, `cancel`, étiquettes) restent en `200`.
- Lecture : `200` avec la ressource ; identifiant absent : `404`.
- Mise à jour : `200` avec la ressource mise à jour ; identifiant absent : `404`.
- Suppression : `204` sans corps, y compris pour un identifiant déjà absent
  (suppression idempotente).
- Conflit d'unicité ou de cycle de vie : `409` ; contrainte FK/CHECK : `422`.
- Aucun endpoint atteignable ne renvoie `200` avec `{"errors": ...}`. L'ancien
  schéma `ItemError` ne subsiste que pour les erreurs par ligne des créations
  d'articles en lot.

### Format des erreurs

Toutes les erreurs — exceptions métier, validation, JSON illisible, UUID
malformé, route inconnue, méthode non autorisée, erreur SQL ou erreur Spring
Security — renvoient un Problem Details RFC 9457 :

```json
{
  "type": "about:blank",
  "title": "Valeur déjà existante",
  "status": 409,
  "detail": "Une agence avec ce code existe déjà.",
  "instance": "/api/v1/agency",
  "code": "CONFLICT",
  "traceId": "3f1c6a5e-7b2d-4f9a-9a5e-2f4b8c1d0e77",
  "timestamp": "2026-10-23T12:34:56.789Z"
}
```

- `type` : `about:blank` tant qu'aucun catalogue d'URI public n'est publié.
- `title` : résumé stable du type d'erreur.
- `status` : code HTTP répété dans le corps.
- `detail` : précision contextuelle non technique, toujours renseignée.
- `instance` : URI de la requête en échec.
- `code` : code applicatif stable (`BAD_REQUEST`, `VALIDATION_ERROR`,
  `INVALID_REQUEST_BODY`, `NOT_FOUND`, `CLIENT_ERROR`, `CONFLICT`,
  `UNPROCESSABLE_ENTITY`, `SERVER_ERROR`, `UNAUTHORIZED`, `FORBIDDEN`,
  `METHOD_NOT_ALLOWED`, `UNSUPPORTED_MEDIA_TYPE`, `INTERNAL_ERROR`). Aucun
  SQLSTATE ni message PostgreSQL n'est exposé.
- `traceId` : identifiant de corrélation UUID, présent sur toutes les erreurs.
- `timestamp` : horodatage UTC ISO-8601.
- `errors` : tableau `[{field, message}]`, présent uniquement pour une
  validation.

Le gestionnaire global `GlobalExceptionHandler`, `ProblemResponseWriter` et le
contrôleur d'erreur `ItemErrorController` garantissent ce format. Les codes HTTP
utilisés sont `200`, `201`, `204`, `400`, `401`, `403`, `404`, `405`, `409`,
`415`, `422`, `429` et `500`/`503`. Chaque fichier précise les codes réellement
produits par ses routes.

## Points d'attention connus

- **Erreurs SQL** : elles sont traduites en `400`/`409`/`422`/`503`, sans
  SQLSTATE ni message PostgreSQL. Une base indisponible renvoie `503`.
- **Users** : `POST /api/v1/users` et `PATCH /api/v1/users/{id}` utilisent des
  DTO dédiés. Le mot de passe est obligatoire à la création, haché avant
  insertion et jamais renvoyé ; `id`, `password_hash`, `company_id` (en mise à
  jour) et les horodatages ne sont pas modifiables par le client.
- **Inventory** : les corps sont typés par des DTO dédiés ; les erreurs métier
  sont des exceptions HTTP (`404`, `409`, `422`), plus des soft-200.
- **Stubs `item-types`** : les routes `low-stock`, `items/{typeId}` et
  `stocks/{typeId}` ne sont pas encore implémentées et renvoient un tableau JSON
  vide `[]`.
- **Limites** : la spec des soldes manque ; les listes hors inventaire ne sont
  pas paginées avec total ; les filtres d'écran restent partiels ; les libellés
  d'enums ne sont pas tous documentés. L'idempotence (`Idempotency-Key`) et les
  ETag/`If-Match` ne sont pas encore implémentés.

## Vérification

Les 27 fichiers ont été validés au Lot 7 (parsing YAML ruamel, résolution des
`$ref`, chemins `/api/v1`) : **0 erreur de parsing, 0 `$ref` orphelin**.
`scripts/validate_specs.py` rapporte **0 problème sur 13 fichiers / 75
opérations** d'inventaire. Le lint Redocly n'a pas été rejoué ; les
avertissements du Lot 6 (`operationId` absents, licence, `localhost`, exemples)
restent a priori.

Pour linter ou prévisualiser les spécifications :

```bash
npx @redocly/cli lint docs/api/**/*.yaml
npx @redocly/cli preview-docs docs/api/11_inventory/item-openapi.yaml
```
