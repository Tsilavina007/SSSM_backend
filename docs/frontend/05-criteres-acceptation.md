# Critères d'acceptation frontend

**Décision du 23 octobre 2026 :** No-Go pour la production, Go pour une
démonstration **mono-entreprise** en lecture et CRUD encadré.

**Mise à jour du 24 octobre 2026 — mono-entreprise par conception :** le backend
exploite une seule entreprise. Les portes `G0-1` et `G0-2` sont `N/A` (et non
`FAIL`), `FE-11`/`FE-12` aussi, et l'isolation A/B sort des blocages de
production. Le No-Go production subsiste pour le login `400` (`A-02`, `A-03`),
l'absence de rate limit sur login/refresh (`A-18`) et l'idempotence (`P1-14`).

## 1. Portes Go/No-Go backend

| Porte | Critère | Statut | Conséquence frontend |
|---|---|---|---|
| `G0-1` | A ne peut pas lire les données de B | **N/A** | Mono-entreprise : aucune donnée d'une deuxième entreprise ne doit exister |
| `G0-2` | A ne peut pas écrire dans B | **N/A** | Pas de saisie multi-entreprise |
| `G0-3` | Compte sans entreprise/rôle bloqué | **PARTIEL** | L'admin plateforme reste autorisé par conception |
| `G0-4` | Écritures multi-lignes atomiques | **PASS** | Opérations, affectations et comptages fiables |
| `G0-5` | Source de vérité du stock | **PASS** | Les soldes peuvent être affichés |
| `G0-6` | `ASSIGNED` ⇒ une affectation active | **PASS** | Le badge d'état est exploitable |
| `G0-7` | Pas de 500 avec SQLSTATE | **PASS** | Les erreurs sont mappables |
| `G0-8` | Rôle révoqué non rafraîchi | **PASS** | Le refresh recharge les rôles |

## 2. Critères par écran

### Catalogue et articles

| ID | Critère d'acceptation | Statut backend |
|---|---|---|
| FE-01 | Lister, filtrer et paginer sans total manquant | **PARTIEL** : total présent sur les articles ; filtres limités, référentiels non paginés |
| FE-02 | Détail avec type, catégorie, entrepôt et statut | **PARTIEL** : relations parfois nulles |
| FE-03 | Créer un type sans champs facultatifs | **PASS** |
| CAT-01 | CRUD nominal article, type, catégorie, entrepôt | **PARTIEL** : non rejoué par la suite courante |
| CAT-09 | Transitions de statut contrôlées | **FAIL** : `PATCH` libre |
| CAT-17 | Filtres type, catégorie, entrepôt, statut, dates | **FAIL** : partiels |
| API-15 | Filtres couvrant les besoins des écrans | **FAIL** |

Recette minimale :

1. créer un type sans `unit`, `quantity_total`, `minimum_stock` → `201` ;
2. créer un article dans un entrepôt existant → `201` + `Location` ;
3. lister les articles avec `page=1&pageSize=20` → `total` cohérent ;
4. filtrer par `warehouseId` et `status` ;
5. archiver un article avec affectation active → `409 ACTIVE_ASSIGNMENT` ;
6. restaurer un article archivé → `200`.

### Stock et mouvements

| ID | Critère d'acceptation | Statut backend |
|---|---|---|
| STK-01 | Quantité > 0 | **PASS** |
| STK-02 | Source, destination, opération et acteur | **PARTIEL** : acteur possiblement nul |
| STK-03 | Stock par entrepôt et type | **PASS** |
| STK-04 | Stock négatif refusé | **PASS** |
| STK-05 | Mouvement immuable | **PASS** |
| STK-06 | Correction par mouvement inverse | **PARTIEL** : manuel uniquement |
| STK-16 | Rapport filtrable par date, type, acteur | **FAIL** |
| STK-17 | Dashboard sur le stock réel | **PARTIEL** |
| FE-06 | Mouvement avec quantité et acteur | **PARTIEL** |

Recette minimale :

1. créer un mouvement `IN` de 5 unités → solde +5 ;
2. créer un `OUT` de 2 unités → solde +3, article `ASSIGNED`, affectation créée ;
3. créer un `OUT` de 4 unités → `409 INSUFFICIENT_STOCK`, solde inchangé ;
4. `PATCH`/`DELETE` un mouvement → `409 IMMUTABLE_RESOURCE` ;
5. afficher le solde `GET /inventory/stock-balances?warehouseId=…` ;
6. vérifier la pagination et le tri stable.

### Affectations

| ID | Critère d'acceptation | Statut backend |
|---|---|---|
| ASG-01 | Un seul contrat exposé | **PARTIEL** : façade dépréciée encore active |
| ASG-02 | Article non disponible → `409` | **PASS** |
| ASG-03 | Employé inactif → `422` | **FAIL** |
| ASG-04/05 | Employé/poste d'une autre entreprise refusé | **FAIL** |
| ASG-07/08/09 | Retour daté, entrepôt restauré, acteur | **PASS** |
| ASG-10 | Retour idempotent | **PARTIEL** : `409` déterministe, sans clé |
| ASG-12 | Suppression active refusée | **PASS** |
| ASG-13 | Void audité et article restauré | **PASS** |
| ASG-14 | `ASSIGNED` ⇒ une affectation active | **PASS** |
| ASG-17 | Retards identifiables | **FAIL** : aucune route dédiée |
| FE-07 | Créer, retourner et auditer | **PASS** (réserve donnée de démo) |

Recette minimale :

1. affecter un article `AVAILABLE` à un employé et un poste → `201` ;
2. vérifier `assigned_by`, `assigned_at`, entrepôt vidé, solde décrémenté ;
3. retourner via `POST /item-assignments/{id}/return` → article `AVAILABLE`,
   entrepôt restauré, `returned_at` et `returned_by` renseignés ;
4. second retour → `409 ASSIGNMENT_ALREADY_RETURNED` ;
5. annuler une affectation avec motif → void audité ;
6. supprimer une affectation active → `409 ACTIVE_ASSIGNMENT` ;
7. utiliser uniquement `/api/v1/item-assignments`, pas la façade.

### Comptages et opérations

| ID | Critère d'acceptation | Statut backend |
|---|---|---|
| CNT-01/02/03 | Attendu figé, écart serveur, hors périmètre refusé | **PASS** |
| CNT-05 | Comptage vide non clôturable | **PASS** |
| CNT-07/08 | Clôture réconciliée et `last_inventory_at` | **PASS** |
| CNT-10 | Lignes paginées et filtrables | **PARTIEL** : filtres absents |
| CNT-11 | Annulation | **PASS** |
| OPS-06 | Opération inconnue → `404` | **FAIL** |
| OPS-07 | Opération idempotente | **FAIL** |

Recette minimale :

1. ouvrir un comptage → instantané figé, statut `IN_PROGRESS` ;
2. scanner un article attendu avec une quantité → `expected`, `counted`,
   `difference` calculés ;
3. scanner un article hors périmètre → `409 ITEM_OUT_OF_SCOPE` ;
4. clôturer → `completed_at`, ajustements, soldes, `last_inventory_at` ;
5. reclôturer → `200` sans nouvel ajustement ;
6. clôturer un comptage vide → `409 COUNT_EMPTY` ;
7. annuler un comptage ouvert → `CANCELLED` sans ajustement.

### QR, scan et étiquettes

| ID | Critère d'acceptation | Statut backend |
|---|---|---|
| QR-01 | Code stable après impression | **PASS** |
| QR-04/05/06/07 | Scan tenant-scoped, 404, enrichi, sans champ interne | **PASS** |
| QR-08 | Chaque scan journalisé | **PASS** |
| QR-09 | Limite de débit | **PARTIEL** : en mémoire, non partagée |
| QR-12 | Étiquette PDF | **PARTIEL** : HTML imprimable |
| QR-13 | Planche d'étiquettes | **PASS** : planche HTML de 100 articles depuis le Lot 5 |
| FE-08 | Scan fournit assez d'informations pour agir | **PASS** |
| FE-09 | Étiquette PDF en lot | **PARTIEL** : HTML imprimable, pas de PDF serveur |

Recette minimale :

1. scanner un code connu → réponse enrichie, journal `RESOLVED` ;
2. scanner un code inconnu → `404`, journal `NOT_FOUND` ;
3. scanner un code d'une autre entreprise → `404` (test backend existant) ;
4. tenter `PATCH qr_code` → `400 IMMUTABLE_QR_CODE` ;
5. demander l'image avec `If-None-Match` → `304` ;
6. ouvrir l'étiquette et la planche de 100 articles ;
7. dépasser la limite de scans → `429`.

### Contrat HTTP et session

| ID | Critère d'acceptation | Statut backend |
|---|---|---|
| API-01 | Routes `/api/v1` | **PASS** |
| API-03/04/05 | Pas de soft-200, pas de 500 de saisie, pas de SQLSTATE | **PASS** |
| API-06/07 | `201` + `Location`, `204` | **PASS** |
| API-09 | Dates ISO-8601 UTC | **PASS** |
| API-10 | `Idempotency-Key` | **FAIL** |
| API-11 | `If-Match`/ETag | **FAIL** |
| API-12/16 | Code stable et `traceId` | **PASS** |
| API-13 | OpenAPI complet | **PARTIEL** : exemples et licences manquants |
| API-17 | Enums avec libellés | **FAIL** |
| FE-04 | Erreurs mappables | **PASS** |
| FE-13 | Dates affichables après création | **PASS** |
| FE-14 | Actions sensibles idempotentes | **FAIL** |

Recette minimale :

1. appeler une route inconnue → `404` Problem Details ;
2. envoyer un UUID invalide sur les articles → `400 BAD_REQUEST` ;
3. provoquer une validation → `400 VALIDATION_ERROR` avec `errors[]` ;
4. créer une ressource → `201` + `Location` + date non nulle ;
5. supprimer → `204` sans corps ;
6. vérifier le suffixe UTC des dates ;
7. vérifier l'alias `/api` : `Deprecation: true` et `Link`.

### Dashboard

| ID | Critère d'acceptation | Statut backend |
|---|---|---|
| FE-10 | Dashboard avec stock réel | **PARTIEL** |
| FE-11 | Dashboard tenant-scoped | **N/A** : global par conception en mono-entreprise |
| STK-18 | Seuil low-stock par entrepôt | **PASS** |
| FE-12 | Écrans multi-entreprise | **N/A** : hors périmètre |

Le low-stock lit `stock_balance` et le seuil effectif ; les deux routes dashboard
sont globales, ce qui correspond au déploiement mono-entreprise. Elles peuvent
être affichées en production sans mention de cloisonnement.

## 3. Définition de « prêt pour la démonstration »

La démonstration est acceptée si :

- [ ] une seule entreprise existe dans la base ;
- [ ] le login, le refresh et le logout fonctionnent, y compris après
      rechargement de page ;
- [ ] aucune action n'est proposée sur un écran dont la route n'existe pas ;
- [ ] les créations désactivent le bouton pendant l'appel (pas d'idempotence) ;
- [ ] les erreurs `400/401/403/404/409/422/429/500/503` sont traitées ;
- [ ] la pagination utilise `total` et non `items.length` ;
- [ ] les dates sont affichées en heure locale à partir de l'UTC ;
- [ ] les écrans de stock, d'affectation et de comptage sont testés sur les
      scénarios ci-dessus ;
- [ ] le scan et l'étiquette fonctionnent avec un jeton et sans lien partageable ;
- [ ] le `traceId` est visible sur les erreurs 5xx.

## 4. Blocages pour la production

| Blocage | Critère | Correction attendue |
|---|---|---|
| Concurrence | `API-10`, `API-11`, `NFR-12` | `version`/`If-Match`, `Idempotency-Key` |
| RBAC hors inventaire | `A-08`, `P0-02` | Permissions sur organisation, personnel, clients, sites |
| Login 400 | `A-02`, `A-03` | `401`/`403` distincts |
| Rate limit login/refresh | `A-18` | Limitation de débit sur les routes d'authentification (limiteur partagé) |
| Filtres d'écran | `API-15` | Paramètres serveur et index |
| Étiquette PDF | `FE-09` | Génération serveur ou impression HTML validée |

**Hors blocage depuis le 24 octobre 2026 :** l'isolation multi-entreprise
(`G0-1`, `G0-2`, `T-01` à `T-11`, `FE-11`, `FE-12`) et le dashboard global. Ces
points redeviennent bloquants si une deuxième entreprise est admise.

Aucun de ces points n'est corrigé à ce jour (Lots 8 et 9 inclus) : le frontend de
production reste interdit jusqu'à leur fermeture.
