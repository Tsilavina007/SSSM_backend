# Préparation frontend — Lot 9

**Date :** 23 octobre 2026
**Objet :** fournir au frontend web et mobile un contrat stable à consommer, sans
commencer les écrans.
**État backend :** 74 tests verts (13 classes) ; `GET /api/v1/inventory/stock-balances`
est désormais documenté dans OpenAPI ; `P0-01` (isolation multi-entreprise) et
`P1-14` (concurrence/idempotence) restent ouverts.

## Verdict

**No-Go pour un frontend de production**. `G0-1` et `G0-2` du rapport 15
(isolation A/B en lecture et en écriture) sont toujours `FAIL`.

**Go limité à une démonstration mono-entreprise**, avec des données contrôlées et
un utilisateur de confiance. Cet usage ne doit pas exposer une deuxième
entreprise, ni des décisions de stock non surveillées.

Les critères détaillés et les écarts acceptés sont dans
[`05-criteres-acceptation.md`](05-criteres-acceptation.md).

## Livrables du lot

| Livrable | Fichier |
|---|---|
| Environnements, URLs et conventions | ce document |
| Stockage des jetons, refresh et logout | [`01-authentification-jetons.md`](01-authentification-jetons.md) |
| Catalogue des erreurs et mapping UI | [`02-erreurs.md`](02-erreurs.md) |
| Pagination et filtres | [`03-pagination-filtres.md`](03-pagination-filtres.md) |
| Parcours de scan et étiquettes (maquettes) | [`04-parcours-scan-etiquettes.md`](04-parcours-scan-etiquettes.md) |
| Critères d'acceptation frontend | [`05-criteres-acceptation.md`](05-criteres-acceptation.md) |
| Données de démonstration | [`06-donnees-demonstration.md`](06-donnees-demonstration.md) |
| Contrat OpenAPI | [`../api/README.md`](../api/README.md) et les 28 fichiers `docs/api/**/*.yaml` |

## Environnements et URLs

| Environnement | URL backend | Origines frontend autorisées | Statut |
|---|---|---|---|
| Développement local | `http://localhost:8080` | `http://localhost:3000`, `http://localhost:5173`, `http://localhost:4200` | Disponible |
| CI / tests | Service PostgreSQL éphémère, backend démarré par la suite Maven | Non applicable | Disponible |
| Recette | Non définie dans le dépôt | À fournir par l'exploitation | Absent |
| Production | Non définie dans le dépôt | À fournir par l'exploitation | Absent ; HTTPS non imposé (`NFR-08`) |

Règles de configuration :

- une seule variable côté frontend, par exemple `API_BASE_URL` ;
- les appels métier utilisent **`${API_BASE_URL}/api/v1`** ;
- ne pas utiliser l'alias `/api` : il est accepté mais déprécié
  (`Deprecation: true`, `Link` vers `/api/v1`) ;
- les origines autorisées se règlent côté backend avec
  `CORS_ALLOWED_ORIGINS` (aucun `*`, `allowCredentials = true`) ;
- en production, `DB_URL` doit porter `sslmode=require` et le backend doit être
  servi en HTTPS ; ce n'est pas encore garanti par le code.

## Conventions de contrat

- **Version :** `/api/v1`.
- **Authentification :** `Authorization: Bearer <access token>` sur toutes les
  routes métier.
- **Dates :** ISO-8601 UTC (`Z` ou `+00:00`) ; les champs `date` restent
  `yyyy-MM-dd`.
- **Identifiants :** UUID en `string`.
- **Erreurs :** `application/problem+json` (RFC 9457), avec `code`, `traceId`
  et `timestamp`.
- **Listes d'inventaire :** `PageResponse` (`items`, `page`, `pageSize`,
  `total`, `totalPages`).
- **Créations :** `201` + `Location` lorsqu'une ressource adressable est créée.
- **Suppressions :** `204` sans corps.

## Limites connues à ne pas masquer

| Limite | Effet pour le frontend | Référence |
|---|---|---|
| Aucun filtrage `company_id` dans les DAO d'inventaire | Ne jamais exposer deux entreprises ; les listes peuvent contenir des données d'autres entreprises si la base en contient | `P0-01`, `G0-1`, `G0-2` |
| Pas d'`Idempotency-Key` ni d'`If-Match`/ETag | Double-clic et écrasement concurrent non détectés ; désactiver le bouton pendant l'appel, recharger après conflit | `P1-14`, `API-10`, `API-11` |
| Login invalide en `400` | Ne pas déclencher un refresh sur un `400` de login ; afficher le message | `A-02` |
| Listes hors inventaire non paginées avec total | Référentiels types/catégories/entrepôts et modules 02 à 06 : tableaux bruts, plafond absent | `API-02`, `NFR-05` |
| Filtres d'écran partiels | Filtrer côté serveur quand le paramètre existe, sinon paginer et filtrer côté client avec prudence | `API-15` |
| Libellés d'enums non fournis | Maintenir une table de libellés côté frontend | `API-17` |
| Pas de PDF serveur | Imprimer l'étiquette HTML via le navigateur | `QR-12` |
| Pas de politique de mot de passe oublié | Prévoir un support administrateur | rapport 09 |
| `traceId` non affiché par défaut | L'afficher dans le détail d'erreur pour le support | `API-16` |

## Ce que ce lot ne fait pas

- Aucun écran, composant ou projet frontend.
- Aucune modification de schéma SQL ni de données.
- Aucun jeu de données multi-entreprise (voir
  [`06-donnees-demonstration.md`](06-donnees-demonstration.md)).
- Aucune correction de `P0-01` ou `P1-14` : ces corrections appartiennent aux
  lots de sécurité/concurrence et restent bloquantes pour la production.
