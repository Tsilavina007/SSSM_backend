# Documentation API — SSSM Backend

Spécifications OpenAPI 3.0.3 générées à partir des contrôleurs Spring du projet,
module par module. Chaque fichier est autonome et peut être importé directement
dans Swagger Editor, Postman, Insomnia ou Redoc.

## Organisation

| Module | Dossier | Spécifications | Opérations |
|---|---|---|---|
| `a02_organisation` | [`02_organisation/`](02_organisation/) | company, agency, branch | 15 |
| `a03_authentication` | [`03_authentication/`](03_authentication/) | auth, user, role, permission | 21 |
| `a04_personnel` | [`04_personnel/`](04_personnel/) | employee, employee_document | 10 |
| `a05_clients` | [`05_clients/`](05_clients/) | client, client_contact | 10 |
| `a06_sites` | [`06_sites/`](06_sites/) | site, security_post, post_requirement | 15 |
| `a11_inventory` | [`11_inventory/`](11_inventory/) | 12 ressources (voir ci-dessous) | 55 |
| **Total** | | **26 fichiers** | **126** |

### Détail des fichiers

- **02_organisation**
  - `company-openapi.yaml` — `/api/company`
  - `agency-openapi.yaml` — `/api/agency`
  - `branch-openapi.yaml` — `/api/branch`
- **03_authentication**
  - `auth-openapi.yaml` — `/api/auth` (public)
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
  - `/`, `/ping`, `/error` et `/api/auth/**` (publics) ;
  - les routes protégées restent soumises au filtre JWT.
- JWT obtenu via `POST /api/auth/login` (durée par défaut : 1 heure).
- `/api/users/**`, `/api/roles/**` et `/api/permissions/**` exigent en plus le
  rôle `ADMIN`.
- Un JWT invalide ou expiré renvoie `401` avec l'enveloppe d'erreur ; un JWT
  absent est refusé par Spring Security (`403`).

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
- Seul le module `a03_authentication/auth` utilise la validation Jakarta
  (`@Valid`). Ailleurs, les champs obligatoires sont imposés par PostgreSQL
  (`NOT NULL` sans défaut) et leur omission peut produire une erreur `500`.

### Format des erreurs

Deux conventions coexistent, fidèlement aux contrôleurs :

1. **Enveloppe métier** : `{"errors": {"message": "...", "detail": "...", "code": "23505"}}`
   pour les contrôleurs qui capturent `ClientException`, `NotFoundException` et
   `ServerException` (company, branch, auth, users, ainsi que l'inventaire).
2. **Erreur Spring par défaut** : `{"timestamp", "status", "error", "path"}`
   pour les contrôleurs sans gestion d'exception (agency, roles, permissions,
   employees, clients, sites, etc.).

Chaque fichier précise les codes réellement produits par ses routes.

## Points d'attention connus

- **Suppressions silencieuses** : plusieurs DAO capturent les `SQLException`
  (`printStackTrace`) et renvoient l'objet d'entrée ou une liste vide. Une
  création invalide peut donc répondre `200` avec un objet sans `id`.
- **Réponses `null`** : les contrôleurs retournant `Optional<T>` renvoient
  `200 null` pour un identifiant absent (et non `404`).
- **Company** : `PATCH` et `DELETE` sont mappés sur `/{CompanyId}` alors que le
  paramètre lié s'appelle `companyId` ; cette incohérence de casse est signalée
  dans `company-openapi.yaml`.
- **Users** : `password_hash` est `@JsonIgnore`, donc `POST /api/users` ne peut
  pas créer de compte avec mot de passe (`500`) ; utiliser
  `POST /api/auth/register`.
- **Roles** : `RoleDAO.create` convertit `company_id` sans garde ; un rôle
  global (`company_id = null`) échoue en `500`.
- **Inventory (11)** : plusieurs ressources utilisent un corps `Map` générique
  et des échecs doux (`200 {"error": ...}`) ; ces comportements sont détaillés
  dans les fichiers concernés.

## Vérification

Les 26 fichiers ont été validés (parsing YAML, résolution des `$ref`, paramètres
de chemin) et les 126 opérations documentées correspondent exactement aux
annotations des contrôleurs.

Pour linter ou prévisualiser les spécifications :

```bash
npx @redocly/cli lint doc/api/**/*.yaml
npx @redocly/cli preview-docs doc/api/11_inventory/item-openapi.yaml
```
