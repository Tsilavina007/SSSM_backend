# Catalogue des erreurs et mapping frontend

Toutes les erreurs métier du backend sont des **Problem Details RFC 9457**
(`application/problem+json`). Il n'existe plus de `200` avec `{"errors": ...}` sur
une route atteignable.

## 1. Format

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

| Champ | Usage frontend |
|---|---|
| `status` | Code HTTP, répété dans le corps |
| `title` | Résumé court, utilisable comme libellé générique |
| `detail` | Message contextuel à afficher (ou à traduire) |
| `code` | Clé stable pour la logique et l'i18n ; **ne pas parser `detail`** |
| `traceId` | À afficher dans le détail technique et à joindre au support |
| `timestamp` | Horodatage UTC |
| `errors[]` | Uniquement pour `VALIDATION_ERROR` : `{ field, message }` |
| `instance` | URI de la requête en échec |

Règles :

- ne jamais afficher `detail` brut d'une erreur `INTERNAL_ERROR` ou `DATABASE_*` ;
- toujours proposer « copier l'identifiant de trace » sur les erreurs 5xx ;
- si le corps n'est pas du JSON Problem Details (proxy, panne réseau), fabriquer
  une erreur locale `NETWORK_ERROR` avec le statut HTTP.

## 2. Codes HTTP

| HTTP | Signification | Action frontend |
|---|---|---|
| `200` | Succès | Lire le corps |
| `201` | Création | Lire le corps et `Location` |
| `204` | Succès sans corps | Ne pas parser le corps |
| `400` | Requête invalide, JSON illisible, validation, identifiant mal formé, **login invalide** | Corriger les champs ; afficher `errors[]` ; ne pas refresh |
| `401` | JWT absent/invalide/expiré, refresh rejeté | Refresh unique puis logout si échec |
| `403` | JWT valide mais rôle/permission/entreprise insuffisants | Écran d'accès refusé ; ne pas refresh |
| `404` | Ressource inconnue, archivée, ou d'une autre entreprise | État « introuvable », retour à la liste |
| `405` | Méthode non autorisée | Bug frontend : corriger l'appel |
| `409` | Conflit : doublon, état incompatible, ressource immuable, stock insuffisant | Message métier ; recharger la ressource |
| `415` | `Content-Type` non supporté | Corriger l'en-tête |
| `422` | FK ou contrainte `CHECK` violée | Message métier ; corriger la référence |
| `429` | Limite de débit (scan QR) | Attendre et réessayer ; pas de retry automatique en boucle |
| `500` | Erreur interne | Message générique + `traceId` |
| `503` | Base indisponible | Message « service momentanément indisponible », retry avec backoff |

## 3. Codes applicatifs

### Authentification et contrat

| `code` | HTTP | Signification | Réaction UI |
|---|---|---|---|
| `UNAUTHORIZED` | 401 | Jeton absent, invalide, expiré, refresh rejeté | Refresh ou redirection login |
| `FORBIDDEN` | 403 | Permission insuffisante | Accès refusé |
| `VALIDATION_ERROR` | 400 | Validation Jakarta | Mapper `errors[]` sur les champs |
| `INVALID_REQUEST_BODY` | 400 | JSON illisible ou type incorrect | Message générique de saisie |
| `BAD_REQUEST` | 400/503 | Paramètre invalide, UUID malformé, erreur SQL de format | Corriger la saisie |
| `NOT_FOUND` | 404 | Ressource inconnue | État vide / retour |
| `CONFLICT` | 409 | Doublon ou conflit SQL | Recharger et signaler |
| `UNPROCESSABLE_ENTITY` | 422 | FK ou `CHECK` violée | Corriger la référence |
| `METHOD_NOT_ALLOWED` | 405 | Route appelée avec la mauvaise méthode | Bug frontend |
| `UNSUPPORTED_MEDIA_TYPE` | 415 | En-tête `Content-Type` incorrect | Corriger l'appel |
| `RATE_LIMITED` | 429 | Limite de scan atteinte | Attendre, message explicite |
| `SERVER_ERROR` | 500 | Erreur applicative interne | Message générique + trace |
| `INTERNAL_ERROR` | 500 | Erreur inattendue | Message générique + trace |
| `DATABASE_ERROR` | 500 | Erreur SQL non classée | Message générique + trace |
| `DATABASE_UNAVAILABLE` | 503 | Base indisponible | Retry avec backoff |
| `MISSING_IDENTIFIER` | 409 | Identifiant obligatoire absent | Bug frontend ou lien incomplet |
| `INVALID_IDENTIFIER` | 409 | UUID invalide sur les filtres de soldes | Corriger le filtre |
| `MISSING` | 400 | Champ obligatoire absent (selon le point d'appel) | Mapper sur le champ |

### Inventaire

| `code` | HTTP | Signification | Réaction UI |
|---|---|---|---|
| `INSUFFICIENT_STOCK` | 409 | Solde insuffisant | Afficher le solde disponible, bloquer l'action |
| `ITEM_NOT_AVAILABLE` | 409 | Article non `AVAILABLE` | Recharger l'article |
| `ITEM_ASSIGNED` | 409 | Article déjà affecté | Proposer le retour ou le détail de l'affectation |
| `ITEM_NOT_ASSIGNED` | 409 | Aucune affectation active | Recharger |
| `ITEM_STATE_INCOMPATIBLE` | 409 | Transition d'état interdite | Recharger l'article |
| `ITEM_ARCHIVED` | 409/404 | Article archivé | Le retirer des écrans actifs |
| `ITEM_NOT_ARCHIVED` | 409 | Restauration d'un article non archivé | Recharger |
| `ITEM_OUT_OF_SCOPE` | 409 | Article hors du périmètre d'un comptage | Retirer du comptage |
| `ACTIVE_ASSIGNMENT` | 409 | Suppression/archivage avec affectation active | Retourner ou annuler l'affectation d'abord |
| `ASSIGNMENT_ALREADY_ACTIVE` | 409 | Une affectation active existe déjà | Recharger l'article |
| `ASSIGNMENT_ALREADY_RETURNED` | 409 | Retour déjà effectué | Afficher l'état retourné |
| `ASSIGNMENT_ALREADY_VOIDED` | 409 | Annulation déjà faite | Afficher l'état annulé |
| `ASSIGNMENT_NOT_VOIDED` | 409 | Restauration d'une affectation non annulée | Recharger |
| `ASSIGNMENT_VOIDED` | 409 | Opération interdite sur une affectation annulée | Recharger |
| `RETURN_ENDPOINT_REQUIRED` | 400 | `PATCH` avec seulement `condition_on_return` | Utiliser `POST /return` |
| `IMMUTABLE_ASSIGNMENT_FIELDS` | 400/409 | Champ non modifiable envoyé au `PATCH` | Corriger le formulaire |
| `COUNT_EMPTY` | 409 | Clôture d'un comptage vide | Ajouter un scan |
| `COUNT_NOT_OPEN` | 409 | Clôture d'un comptage déjà clos/annulé | Recharger |
| `COUNT_COMPLETED` | 409 | Annulation d'un comptage clos | Action impossible |
| `COUNT_CANCELLED` | 409 | Scan/clôture d'un comptage annulé | Recharger |
| `COUNT_REFERENCE_EXISTS` | 409 | Suppression d'une référence utilisée par un comptage | Ne pas supprimer |
| `IMMUTABLE_RESOURCE` | 409 | Mouvement ou ressource append-only | Aucune action de modification |
| `IMMUTABLE_QR_CODE` | 400 | Tentative de modification du QR | Champ en lecture seule |
| `QR_CODE_MISSING` | 400 | Article sans code QR | Régénérer ou contacter le support |
| `QR_CODE_GENERATION_FAILED` | 500 | Échec de génération | Message générique + trace |
| `ITEM_TYPE_NOT_ARCHIVED` | 409 | Restauration d'un type non archivé | Recharger |
| `ITEM_CATEGORY_NOT_ARCHIVED` | 409 | Restauration d'une catégorie non archivée | Recharger |
| `WAREHOUSE_NOT_ARCHIVED` | 409 | Restauration d'un entrepôt non archivé | Recharger |

## 4. Validation

Exemple de `400 VALIDATION_ERROR` :

```json
{
  "type": "about:blank",
  "title": "Validation échouée",
  "status": 400,
  "detail": "item_id : L'identifiant de l'article est obligatoire",
  "instance": "/api/v1/item-assignments",
  "code": "VALIDATION_ERROR",
  "traceId": "…",
  "timestamp": "2026-10-23T12:34:56.789Z",
  "errors": [
    { "field": "item_id", "message": "L'identifiant de l'article est obligatoire" }
  ]
}
```

Le frontend mappe `errors[].field` sur les champs du formulaire. Les noms sont
les noms Java des DTO (`item_id`, `employee_id`, `post_id`, etc.), pas les
libellés.

## 5. Stratégie de retry

| Cas | Retry ? |
|---|---|
| `401` | Oui, un seul refresh, puis rejouer une fois |
| `429` | Attendre un délai manuel/backoff ; aucun `Retry-After` fourni |
| `503` | Oui, backoff exponentiel borné (par exemple 1 s, 2 s, 4 s) |
| `400`, `403`, `404`, `409`, `422` | Non |
| `500` | Une seule fois pour une lecture idempotente ; jamais pour une création |
| Création de mouvement, d'affectation, de comptage | Jamais de retry automatique : pas d'`Idempotency-Key` (`P1-14`) |

## 6. Écarts connus

- **Login invalide en `400`** au lieu de `401` (`A-02`).
- **Compte inactif en `400`** au lieu de `403` (`A-03`).
- **UUID invalide** : `400` en général, mais `409 INVALID_IDENTIFIER` sur
  `GET /api/v1/inventory/stock-balances` (`warehouseId`/`itemTypeId`).
- **Suppression référencée** : `422` et non `409` (`CAT-11`).
- Aucun `Retry-After` sur `429`.
- `type` reste `about:blank` : il n'existe pas encore de catalogue d'URI public.
