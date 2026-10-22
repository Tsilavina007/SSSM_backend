# Documentation API — SSSM Backend

Spécifications OpenAPI 3.0.3 générées à partir des contrôleurs Spring du projet,
module par module. Chaque fichier est autonome et peut être importé directement
dans Swagger Editor, Postman, Insomnia ou Redoc.

## Organisation

| Module | Dossier | Spécifications | Opérations |
|---|---|---|---|
| `a02_organisation` | [`02_organisation/`](02_organisation/) | company, agency, branch | 15 |
| `a03_authentication` | [`03_authentication/`](03_authentication/) | auth, user, role, permission | 22 |
| `a04_personnel` | [`04_personnel/`](04_personnel/) | employee, employee_document | 10 |
| `a05_clients` | [`05_clients/`](05_clients/) | client, client_contact | 10 |
| `a06_sites` | [`06_sites/`](06_sites/) | site, security_post, post_requirement | 15 |
| `a11_inventory` | [`11_inventory/`](11_inventory/) | 12 ressources (voir ci-dessous) | 55 |
| **Total** | | **26 fichiers** | **127** |

### Détail des fichiers

- **02_organisation**
  - `company-openapi.yaml` — `/api/company`
  - `agency-openapi.yaml` — `/api/agency`
  - `branch-openapi.yaml` — `/api/branch`
- **03_authentication**
  - `auth-openapi.yaml` — `/api/auth` (register/login publics, whoami authentifié)
  - `user-openapi.yaml` — `/api/users` (ADMIN)
  - `role-openapi.yaml` — `/api/roles` (ADMIN)
  - `permission-openapi.yaml` — `/api/permissions` (ADMIN)
- **04_personnel**
  - `employee-openapi.yaml` — `/api/employees`
  - `employee_document-openapi.yaml` — `/api/employee-documents`
- **05_clients**
  - `client-openapi.yaml` — `/api/clients`
  - `client_contact-openapi.yaml` — `/api/client-contacts`
- **06_sites**
  - `site-openapi.yaml` — `/api/sites`
  - `security_post-openapi.yaml` — `/api/security-posts`
  - `post_requirement-openapi.yaml` — `/api/post-requirements`
- **11_inventory** (existant, vérifié : 55 opérations alignées sur les 12 contrôleurs)
  - `assignment-openapi.yaml`, `dashboard-openapi.yaml`, `inventory_count-openapi.yaml`,
    `item-openapi.yaml`, `item_assignment-openapi.yaml`, `item_category-openapi.yaml`,
    `item_type-openapi.yaml`, `operation-openapi.yaml`, `qr_scan-openapi.yaml`,
    `report-openapi.yaml`, `stock_movement-openapi.yaml`, `warehouse-openapi.yaml`

Aucune spécification n'est fournie pour `a00_infrastructure`, `a01_common` et
`controller_test` : ces packages n'exposent pas d'API métier.

## Conventions communes

### Serveur et authentification

- Base URL : `http://localhost:8080`.
- Toutes les routes exigent `Authorization: Bearer <JWT>`, sauf :
  - `/`, `/ping`, `/error`, `POST /api/auth/register` et `POST /api/auth/login`
    (publics) ;
  - `GET /api/auth/whoami` et toutes les autres routes protégées restent
    soumises au filtre JWT.
- JWT obtenu via `POST /api/auth/login` (durée par défaut : 1 heure).
- `/api/users/**`, `/api/roles/**` et `/api/permissions/**` exigent en plus le
  rôle `ADMIN`.
- Un JWT absent, invalide ou expiré renvoie `401` avec l'enveloppe
  `ItemError`. Un JWT valide sans le rôle requis renvoie `403`.

### Pagination et recherche

- Paramètres : `page` (défaut `1`) et `pageSize` (défaut `10`), sans maximum
  pour les modules 02 à 06.
- Le module `11_inventory` utilise `pageSize=50` par défaut et un maximum de
  `500` sur certaines ressources.
- `search` est une recherche libre `ILIKE`, dont les colonnes varient selon la
  ressource (documenté dans chaque fichier).
- Les listes par parent (`/employee/{id}`, `/client/{id}`, `/site/{id}`,
  `/post/{id}`) ne sont pas paginées.

### Formats et validation

- Dates : `date` (`yyyy-MM-dd`), horodatages : `date-time` (ISO-8601), heures :
  `time` (`HH:mm:ss`).
- Identifiants : UUID en `string`.
- Enums : `ACTIVE`/`INACTIVE`, `ACTIVE`/`INACTIVE`/`LOCKED`,
  `ACTIVE`/`INACTIVE`/`SUSPENDED`/`LEFT`.
- Les corps de création et de mise à jour passent par des DTO dédiés et la
  validation Jakarta (`@Valid`). Les champs obligatoires, tailles maximales,
  formats d'email et valeurs numériques sont contrôlés avant l'accès à la base ;
  un échec renvoie `400` avec l'enveloppe `ItemError`.
- Les champs inconnus des DTO typés sont ignorés, sauf pour les scans
  d'inventaire qui appliquent une liste blanche stricte.

### Politique HTTP

- Création : `201` avec la ressource créée.
- Lecture : `200` avec la ressource ; identifiant absent : `404`.
- Mise à jour : `200` avec la ressource mise à jour ; identifiant absent : `404`.
- Suppression : `204` sans corps, y compris pour un identifiant déjà absent
  (suppression idempotente).
- Les endpoints d'inventaire à erreurs douces (assignations génériques,
  inventaires, opérations) conservent `200` avec `{"errors": ItemError}` pour les
  échecs métier qui ne sont pas des exceptions HTTP.

### Format des erreurs

Toutes les erreurs — exceptions métier, validation, JSON illisible, UUID
malformé, route inconnue, méthode non autorisée, erreur SQL ou erreur Spring
Security — renvoient la même enveloppe :

```json
{
  "errors": {
    "message": "Valeur déjà existante",
    "detail": "duplicate key value violates unique constraint \"uq_company_code\" | Key (code)=(ACME) already exists. | SQLState : 23505",
    "code": "23505"
  }
}
```

- `message` : message lisible en français, toujours renseigné (par exemple
  « Agence introuvable », « Identifiants invalides » ou « Champ obligatoire
  manquant »).
- `detail` : précision contextuelle toujours renseignée (identifiant concerné,
  champ invalide, liste des erreurs de validation, détail PostgreSQL ou cause
  technique de l'échec).
- `code` : code applicatif (`BAD_REQUEST`, `VALIDATION_ERROR`,
  `INVALID_REQUEST_BODY`, `NOT_FOUND`, `CLIENT_ERROR`, `SERVER_ERROR`,
  `UNAUTHORIZED`, `FORBIDDEN`, `METHOD_NOT_ALLOWED`,
  `UNSUPPORTED_MEDIA_TYPE`, `INTERNAL_ERROR`) ou SQLState PostgreSQL
  (par exemple `23505` pour une violation d'unicité).

Le gestionnaire global `GlobalExceptionHandler` et le contrôleur d'erreur
`ItemErrorController` garantissent ce format pour toutes les routes. Les codes
HTTP utilisés sont `200`, `201`, `204`, `400`, `401`, `403`, `404`, `405`, `415`
et `500`. Chaque fichier précise les codes réellement produits par ses routes.

## Points d'attention connus

- **Erreurs SQL** : les DAO ne masquent plus les `SQLException`. Une contrainte
  violée, un UUID invalide côté SQL ou une colonne `NOT NULL` omise renvoie
  désormais `500` avec un message français explicite, le détail PostgreSQL et le
  SQLState.
- **Users** : `POST /api/users` et `PATCH /api/users/{id}` utilisent des DTO
  dédiés. Le mot de passe est obligatoire à la création, haché avant insertion
  et jamais renvoyé ; `id`, `password_hash`, `company_id` (en mise à jour) et
  les horodatages ne sont pas modifiables par le client.
- **Roles** : `RoleDAO.create` convertit `company_id` sans garde ; un rôle
  global (`company_id = null`) échoue en `500`.
- **Inventory (11)** : les corps sont typés par des DTO dédiés. Les endpoints
  d'inventaire à erreurs douces conservent `200` avec l'enveloppe
  `{"errors": ItemError}` (code `NOT_FOUND` ou `BAD_REQUEST`). Chaque échec doux
  renvoie un message français explicite (par exemple « Affectation d'article
  introuvable ») et un détail contenant l'identifiant ou le champ concerné.
- **Stubs `item-types`** : les routes `low-stock`, `items/{typeId}` et
  `stocks/{typeId}` ne sont pas encore implémentées et renvoient un tableau JSON
  vide `[]`.

## Vérification

Les 26 fichiers ont été validés (parsing YAML, résolution des `$ref`, paramètres
de chemin) et les 127 opérations documentées correspondent exactement aux
annotations des contrôleurs. Le lint Redocly ne remonte aucune erreur ; les
avertissements restants concernent uniquement les `operationId` absents, la
licence et l'URL `localhost`.

Pour linter ou prévisualiser les spécifications :

```bash
npx @redocly/cli lint docs/api/**/*.yaml
npx @redocly/cli preview-docs docs/api/11_inventory/item-openapi.yaml
```
