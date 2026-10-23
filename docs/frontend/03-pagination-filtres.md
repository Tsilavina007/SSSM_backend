# Pagination et filtres

## 1. Enveloppe des listes paginées

```json
{
  "items": [],
  "page": 1,
  "pageSize": 10,
  "total": 0,
  "totalPages": 0
}
```

- `page` est **1-based** ; `page=0` ou une valeur négative est normalisée à `1`.
- `pageSize` est plafonné à **500** ; une valeur absente prend le défaut du point
  d'appel ; une valeur `< 1` reprend ce défaut.
- `totalPages` vaut `0` quand `total = 0`.
- Le backend ne fournit ni tri paramétrable ni curseur : le tri est fixé par
  chaque route.
- Les paramètres de pagination ne sont pas obligatoires : ne pas les envoyer
  revient à demander la première page avec le défaut.

Ne pas déduire `total` de `items.length` : `total` couvre l'ensemble des lignes
filtrées.

## 2. Défauts par point d'appel

| Route | Défaut `pageSize` | Plafond | Filtres |
|---|---:|---:|---|
| `GET /inventory/items` | 10 | 500 | `search`, `warehouseId`, `status` |
| `GET /inventory/movements` | 10 | 500 | `search` |
| `GET /item-assignments` | 10 | 500 | `search`, `active` |
| `GET /item-assignments/item/{itemId}` | 10 | 500 | — |
| `GET /item-assignments/employee/{employeeId}` | 10 | 500 | — |
| `GET /item-assignments/post/{postId}` | 10 | 500 | — |
| `GET /inventory/items/{itemId}/history` | 20 | 500 | `movementType`, `active` |
| `GET /inventory/inventory-counts` | 50 | 500 | — |
| `GET /inventory/inventory-counts/{id}/results` | 50 | 500 | — |
| `GET /inventory/stock-balances` | 50 | 500 | `warehouseId`, `itemTypeId` |
| `GET /inventory/reports/movements` | 50 | 500 | `itemId`, `warehouseId` |

Les lignes de comptage renvoient aussi `X-Total-Count`, `X-Page` et
`X-Page-Size`, en plus de l'enveloppe.

## 3. Listes non paginées (sans total)

Ces routes renvoient un **tableau brut**, sans `total` ni plafond :

| Route | Défaut `pageSize` | Filtres |
|---|---:|---|
| `GET /inventory/item-types` | 10 | `search` |
| `GET /inventory/item-types/category/{itemCategoryId}` | 10 | — |
| `GET /inventory/item-categories` | 10 | `search` |
| `GET /inventory/warehouses` | 10 | `search` |
| Modules `02_organisation` à `06_sites` | 10 | selon la ressource |
| `GET /inventory/dashboard/summary` | — | — (tableau global) |
| `GET /inventory/alerts/low-stock` | — | — (tableau global) |
| `GET /inventory/scans/resolve` | — | — (objet unique) |

Conséquences :

- les référentiels ne permettent pas d'afficher un paginateur fiable : utiliser
  `pageSize=500` lorsque c'est possible et prévoir une recherche, ou charger
  progressivement ;
- ne pas croire qu'une liste de types est complète par défaut : elle est limitée
  à 10 ;
- `pageSize` n'est pas plafonné sur ces routes : le frontend doit se fixer sa
  propre limite ;
- les deux routes dashboard sont globales et ne sont pas cloisonnées par
  entreprise (`FE-11`, `P0-01`) : ne pas les exposer en production
  multi-entreprise.

## 4. Filtres disponibles

### Articles — `GET /inventory/items`

| Paramètre | Type | Effet |
|---|---|---|
| `search` | string | Recherche libre (code, description, nom selon le DAO) |
| `warehouseId` | UUID | Entrepôt courant |
| `status` | enum `ItemStatus` | `AVAILABLE`, `ASSIGNED`, `DAMAGED`, `LOST`, `REPAIR`, `RETIRED` |
| `page`, `pageSize` | int | Pagination |

Manquent : type, catégorie, dates, entreprise (`API-15`).

### Mouvements — `GET /inventory/movements`

| Paramètre | Type | Effet |
|---|---|---|
| `search` | string | Référence ou notes |
| `page`, `pageSize` | int | Pagination |

Manquent : article, entrepôt, type, dates, acteur. Pour un historique par
article, utiliser `GET /inventory/items/{id}/history` qui accepte
`movementType` et `active`.

### Affectations — `GET /item-assignments`

| Paramètre | Type | Effet |
|---|---|---|
| `search` | string | Conditions ou notes |
| `active` | boolean | `true` : affectations non retournées et non annulées |
| `page`, `pageSize` | int | Pagination |

Les routes par article, employé et poste sont paginées mais sans filtre
supplémentaire. La façade dépréciée `/inventory/assignments` ne doit pas être
utilisée.

### Comptages

`GET /inventory/inventory-counts` : pagination seule.
`GET /inventory/inventory-counts/{id}/results` : pagination seule ; filtres par
statut ou type d'écart absents (`CNT-10`).

### Soldes — `GET /inventory/stock-balances`

| Paramètre | Type | Effet |
|---|---|---|
| `warehouseId` | UUID | Entrepôt |
| `itemTypeId` | UUID | Type de matériel |
| `page`, `pageSize` | int | Pagination |

Un UUID malformé renvoie `409 INVALID_IDENTIFIER` sur cette route (écart connu).
La liste n'est pas filtrée par entreprise (`P0-01`).

### Rapports — `GET /inventory/reports/movements`

| Paramètre | Type | Effet |
|---|---|---|
| `itemId` | UUID | Article |
| `warehouseId` | UUID | Entrepôt |
| `page`, `pageSize` | int | Pagination |

Manquent : type de mouvement, dates, acteur (`STK-16`).

### Historique article — `GET /inventory/items/{id}/history`

| Paramètre | Type | Effet |
|---|---|---|
| `movementType` | enum `MovementType` | `IN`, `OUT`, `TRANSFER`, `RETURN`, `ADJUSTMENT`, `DESTRUCTION` ; valeur inconnue → `400` |
| `active` | boolean | Affectations actives uniquement |
| `page`, `pageSize` | int | Pagination appliquée aux deux collections |

La réponse contient deux `PageResponse` : `movements` et `assignments`.
Un article inconnu renvoie `404`.

## 5. Recommandations d'écran

- Toujours envoyer `page` et `pageSize` explicitement : les défauts diffèrent
  (10, 20, 50).
- Afficher le paginateur à partir de `total`/`totalPages`, jamais à partir de la
  taille de `items`.
- Désactiver « page suivante » quand `page >= totalPages`.
- Conserver les filtres dans l'URL pour permettre le partage et le retour
  arrière.
- Ne pas charger les référentiels complets en boucle pour résoudre des libellés :
  les réponses plates ne contiennent que des UUID. Prévoir un cache local des
  types, catégories, entrepôts et employés, ou afficher l'UUID en secours.
- Pour les écrans à gros volume, filtrer côté serveur lorsque le paramètre
  existe ; sinon documenter la limite et éviter d'afficher un « total » faux.
