# Authentification, stockage des jetons, refresh et logout

**Décision retenue pour le frontend web :** access token en mémoire, refresh
token dans `localStorage`. Cette stratégie est compatible avec l'API actuelle,
qui renvoie les deux jetons dans le corps JSON et n'émet aucun cookie. Elle
expose le refresh token au XSS : les contre-mesures sont décrites ci-dessous.

## 1. Contrat d'authentification

| Route | Accès | Corps | Réponse |
|---|---|---|---|
| `POST /api/v1/auth/login` | public | `{ "login": "...", "password": "..." }` | `200` `AuthResponse` |
| `GET /api/v1/auth/whoami` | JWT | — | `200` `UserResponse` |
| `POST /api/v1/auth/refresh` | public | `{ "refreshToken": "..." }` | `200` `AuthResponse` |
| `POST /api/v1/auth/logout` | public | `{ "refreshToken": "..." }` + `Authorization` | `204` sans corps |
| `POST /api/v1/auth/register` | `ROLE_ADMIN` ou `users.manage` | `RegisterRequest` | `201` + `Location`, jeton `null` |

`AuthResponse` :

```json
{
  "token": "<access JWT>",
  "refreshToken": "<jeton opaque>",
  "tokenType": "Bearer",
  "user": {
    "id": "uuid",
    "username": "demo.admin",
    "email": "demo.admin@sssm.test",
    "firstName": "Demo",
    "lastName": "Admin",
    "roles": ["ADMIN", "Administrateur entreprise"],
    "companyId": "uuid"
  }
}
```

Durées par défaut :

- access token : `JWT_EXPIRATION_MS`, **1 heure** ;
- refresh token : `JWT_REFRESH_EXPIRATION_MS`, **30 jours** ;
- le refresh tourne à chaque usage : l'ancien jeton est consommé et remplacé.

Le JWT porte `sub` (nom d'utilisateur), `uid`, `company_id`, `roles`,
`permissions`, `platform_admin`, `jti`, `iss` et `aud`. Le frontend ne doit pas
décoder le jeton pour construire ses autorisations : il utilise `user.roles` et
les réponses `401`/`403`.

### Cas d'erreur à connaître

| Situation | Réponse | Conséquence frontend |
|---|---|---|
| Identifiants invalides | `400` `CLIENT_ERROR` | Afficher le message ; **ne pas** tenter de refresh |
| Compte inactif ou verrouillé | `400` `CLIENT_ERROR` | Afficher le message ; ne pas boucler |
| Access token absent/invalide/expiré | `401` `UNAUTHORIZED` | Tenter un refresh unique |
| Rôle ou permission insuffisants | `403` `FORBIDDEN` | Afficher un accès refusé ; ne pas refresh |
| Refresh inconnu, expiré, rejoué ou révoqué | `401` `UNAUTHORIZED` | Purger la session et rediriger vers le login |
| `company_id` absent du jeton sur une route qui l'exige | `403` `FORBIDDEN` | Message « compte mal configuré », support |

Le login invalide en `400` est une anomalie connue (`A-02` du rapport 15) : le
frontend ne doit pas interpréter un `400` de login comme un jeton expiré.

## 2. Stockage

### Access token

- Conservé **uniquement en mémoire** (store applicatif, variable de module).
- Jamais dans `localStorage`, `sessionStorage`, un cookie ou l'URL.
- Perdu au rechargement de page : le frontend restaure la session via le refresh
  token au démarrage.

### Refresh token

- Conservé dans `localStorage`, sous une clé dédiée (`sssm.refresh_token`).
- Jamais envoyé à une autre origine que `API_BASE_URL`.
- Supprimé à la déconnexion, à l'échec du refresh et sur réponse `401`.
- Aucune donnée métier ni donnée personnelle n'est stockée à côté.

### Risques et contre-mesures

| Risque | Contre-mesure |
|---|---|
| XSS lit le refresh token | CSP stricte, aucun script tiers non épinglé, échappement systématique, dépendances auditées |
| Fuite par un log ou un outil d'analyse | Ne jamais logger les jetons ; masquer `Authorization` dans les traces |
| Vol du refresh token | Rotation à chaque refresh ; une réutilisation échoue en `401` ; logout révoque |
| Multi-onglets | Écouter `storage` pour propager la déconnexion ; un seul refresh à la fois par origine |
| CSRF | Sans cookie d'authentification, le CSRF classique ne s'applique pas ; ne pas ajouter de cookie sans revoir ce point |

Cette stratégie est un compromis pour la démonstration. Un passage en production
exige soit un cookie `HttpOnly` + `SameSite` + protection CSRF côté backend, soit
une revue de sécurité dédiée. Aucun changement backend n'est fait au Lot 9.

## 3. Cycle de session

```text
Login
  └─ stocker access en mémoire + refresh en localStorage
       └─ appel API avec Authorization
            ├─ 2xx : continuer
            ├─ 401 : refresh (une seule fois) → rejouer la requête
            │        └─ refresh 401 : purger, rediriger login
            └─ 403 : accès refusé, ne pas refresh

Rechargement de page
  └─ lire le refresh en localStorage
       ├─ absent : login
       ├─ refresh 200 : reconstruire la session (user + tokens)
       └─ refresh 401 : purger, login

Logout
  └─ POST /auth/logout { refreshToken } + Authorization
       └─ 204 : purger mémoire + localStorage, rediriger
       └─ erreur réseau : purger quand même (logout local)
```

### Refresh à appel unique

Plusieurs appels peuvent recevoir `401` en parallèle. Le frontend doit
mutualiser le refresh :

```ts
let refreshPromise: Promise<boolean> | null = null;

async function ensureRefreshed(): Promise<boolean> {
  if (!refreshPromise) {
    refreshPromise = doRefresh().finally(() => {
      refreshPromise = null;
    });
  }
  return refreshPromise;
}
```

Après un refresh réussi, rejouer la requête initiale **une seule fois**. Un
second `401` sur la requête rejouée signifie que la session est invalide :
purger et rediriger.

### Démarrage de l'application

1. Lire `sssm.refresh_token`.
2. S'il est absent, afficher le login.
3. Sinon appeler `POST /api/v1/auth/refresh`.
4. En cas de succès, conserver le nouveau refresh (rotation) et l'access en
   mémoire, puis charger `user`.
5. En cas de `401`, purger et afficher le login.

Ne pas appeler `whoami` avant le refresh : sans access token, il renvoie `401`.

## 4. Logout

```http
POST /api/v1/auth/logout
Authorization: Bearer <access token>
Content-Type: application/json

{ "refreshToken": "<refresh token>" }
```

- Le backend renvoie toujours `204`, même si le refresh token est inconnu.
- L'en-tête `Authorization` est nécessaire pour révoquer le `jti` de l'access
  token courant ; sans lui, seul le refresh est révoqué.
- Le frontend purge **d'abord** son état local, puis tente l'appel réseau sans
  bloquer la navigation.
- Le logout n'invalide pas les autres sessions : le refresh est propre à chaque
  connexion.

## 5. Ce qui n'existe pas encore

- cookie de refresh, CSRF, `SameSite` ;
- mot de passe oublié, changement de mot de passe exposé ;
- limitation de débit sur `login` et `refresh` (`A-18`) ;
- verrouillage automatique d'un compte après échecs ;
- gestion multi-appareils explicite (le backend stocke plusieurs refresh tokens,
  mais aucune route de liste ou de révocation globale n'est exposée) ;
- synchronisation hors ligne.
