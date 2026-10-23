# 09 — Audit de l'authentification et de la sécurité

**Note Lot 7 (23 octobre 2026) :** les routes canoniques sont désormais préfixées `/api/v1` ; l'alias `/api` reste accepté avec `Deprecation`/`Link`. Les chemins cités ci-dessous reflètent l'état de l'audit au moment de sa rédaction, sauf mise à jour explicite.

**Objectif :** évaluer l'inscription, la connexion, les JWT, le refresh, les rôles, les permissions, le CORS et la protection des données. Les routes d'inventaire sont supposées authentifiées ; ce rapport vérifie ce que « authentifié » signifie réellement.

**Mise à jour du 21 octobre 2026 (Lot 1) :** l'inscription publique est fermée, le refresh token est distinct et rotatif, le logout et la révocation sont implémentés, les rôles sont rechargés au refresh, le JWT porte le tenant et les permissions, et le CORS est explicite. Les sections ci-dessous décrivent l'état initial puis signalent l'état courant. Les points non corrigés restent listés.

---

## 1. Modèle de sécurité réel

| Élément | Implémentation | Fichier |
|---|---|---|
| Authentification | JWT porté par `Authorization: Bearer` | `JwtAuthenticationFilter`, `JwtService` |
| Hash des mots de passe | BCrypt | `SecurityConfig.passwordEncoder` |
| Autorisation | Règles d'URL + permissions du jeton | `SecurityConfig`, `JwtAuthenticationFilter` |
| Rôles | table `role`, `user_role` | `03_security.sql` |
| Permissions | table `permission`, `role_permission`, claims `permissions` | `03_security.sql`, `JwtService` |
| Refresh | table `refresh_token` (haché, rotation, révocation) | `18_lot1_security.sql`, `RefreshTokenDAOImpl` |
| Révocation access token | table `revoked_access_token`, contrôle du `jti` | `18_lot1_security.sql`, filtre JWT |
| Sessions | stateless | `SessionCreationPolicy.STATELESS` |
| CSRF | désactivé | `SecurityConfig` |
| CORS | origines explicites par environnement | `SecurityConfig.corsConfigurationSource` |

### 1.1. Règles d'accès exactes

```text
/                         permitAll
/ping                     permitAll
/error                    permitAll
/api/auth/login           permitAll
/api/auth/refresh         permitAll
/api/auth/logout          permitAll
/api/auth/register        ROLE_ADMIN ou users.manage
/api/inventory/**         ROLE_ADMIN, ROLE_INVENTORY ou inventory.manage
/api/users/**             ROLE_ADMIN ou users.manage
/api/roles/**             ROLE_ADMIN ou roles.manage
/api/permissions/**       ROLE_ADMIN (admin plateforme)
anyRequest                authenticated
```

### 1.2. Conséquences immédiates

- L'inscription n'est plus publique : elle exige une autorité d'administration et rattache le compte à l'entreprise du jeton.
- L'inventaire exige désormais `ROLE_ADMIN`, `ROLE_INVENTORY` ou la permission `inventory.manage`.
- Les permissions `users.manage` et `roles.manage` sont consommées par les routes correspondantes.
- Les rôles et permissions sont embarqués dans le JWT et rechargés au refresh.
- Les routes d'organisation, de personnel, de clients et de sites restent seulement `authenticated` : le RBAC doit y être étendu dans un lot ultérieur.
- Aucun `@PreAuthorize`, `@PostAuthorize` ou `@Secured` n'existe encore ; le contrôle est centralisé dans `SecurityConfig`.

---

## 2. Inscription

### 2.1. Route

```http
POST /api/auth/register
Authorization: Bearer <access token admin>
Content-Type: application/json

{
  "username": "...",
  "email": "...",
  "password": "...",
  "firstName": "...",
  "lastName": "...",
  "phone": "..."
}
```

La route est réservée aux porteurs de `ROLE_ADMIN` ou `users.manage`. Un administrateur d'entreprise ne peut créer que dans son entreprise ; un administrateur plateforme sans entreprise doit utiliser `POST /api/users`.

### 2.2. Comportement

`AuthService.register` :

1. exige un principal JWT et une entreprise non nulle ;
2. vérifie que `email` et `username` ne sont pas nuls ;
3. vérifie qu'aucun utilisateur n'existe avec ce username ou cet email ;
4. crée un `AppUser` avec :
   - `company_id` = entreprise du jeton ;
   - `status = ACTIVE` ;
   - mot de passe BCrypt ;
5. cherche le rôle global `USER` et l'affecte si trouvé ;
6. renvoie un `AuthResponse` avec `token = null` et le profil, dont `companyId`.

Validation Jakarta inchangée : `username` obligatoire max 100, `email` obligatoire au format email max 180, `password` obligatoire de 8 à 128 caractères, autres champs facultatifs.

### 2.3. Problèmes restants

| Problème | Impact |
|---|---|
| Aucun jeton retourné | Le client doit appeler `/login` juste après. |
| Aucun contrôle d'email | Aucune vérification d'adresse. |
| Aucun rate limiting | Création massive de comptes par un administrateur compromis. |
| Pas de CAPTCHA | Automatisation possible si un administrateur est compromis. |
| Doublon en course | `23505` traduit en 409 depuis le Lot 2 ; la course reste possible mais n'expose plus de 500. |
| Rôle `USER` global absent | Le compte est créé sans rôle ; il peut se connecter mais n'a pas les permissions métier. |

### 2.4. Rôle absent

La recherche du rôle est un `ifPresent`. Si le rôle global `USER` n'existe pas :

- l'utilisateur est créé ;
- aucun rôle ne lui est affecté ;
- il peut se connecter ;
- le JWT a une liste de rôles vide ;
- les routes d'inventaire, d'administration et de rôles sont refusées, car elles exigent une autorité explicite.

Un compte sans rôle n'accède donc plus à l'inventaire. Il ne conserve que les routes qui restent volontairement `authenticated()` (organisation, personnel, clients, sites), ce qui reste une dette RBAC à traiter.

---

## 3. Connexion

### 3.1. Route

```http
POST /api/auth/login
Content-Type: application/json

{
  "login": "username ou email",
  "password": "..."
}
```

### 3.2. Comportement

- recherche par username exact ou email insensible à la casse ;
- filtre `status == ACTIVE` ;
- vérifie le hash BCrypt ;
- met à jour `last_login_at` ;
- charge les rôles ;
- crée un JWT ;
- renvoie `token`, `type = Bearer` et le profil.

### 3.3. Problèmes

| Problème | Impact |
|---|---|
| Identifiants invalides → **400** | Un client s'attend à 401 ; le 400 mélange validation et authentification. Non corrigé au Lot 1. |
| Compte inactif → **400** | Même remarque. Non corrigé au Lot 1. |
| Aucun rate limiting | Brute force possible. |
| Aucun verrouillage après N échecs | `UserStatus.LOCKED` n'est jamais appliqué automatiquement. |
| Message identique pour utilisateur inconnu / mauvais mot de passe | Bon point pour l'énumération, mais le 400 reste ambigu. |

Le login est désormais couvert par les tests d'intégration (`AuthHardeningApiTests`, `RefreshTokenApiTests`) : compte créé par un administrateur, login valide, refresh, logout. Les cas « mauvais mot de passe » et « compte inactif » ne sont pas encore testés.

---

## 4. JWT

### 4.1. Structure

`JwtService.createAccessToken` crée un jeton avec :

- `sub` = username ;
- `uid` = identifiant utilisateur ;
- `company_id` = entreprise du compte (ou `null` pour un admin plateforme) ;
- `roles` = liste des noms de rôles ;
- `permissions` = liste des codes de permissions ;
- `platform_admin` = booléen ;
- `jti` = identifiant unique du jeton ;
- `iss` = `app.jwt.issuer` (défaut `sssm-api`) ;
- `aud` = `app.jwt.audience` (défaut `sssm-client`) ;
- `iat` = date d'émission ;
- `exp` = date d'expiration.

La méthode `createToken(userId, username, roles)` est conservée pour les tests et les outils ; elle ne porte pas d'entreprise et marque `platform_admin` à vrai si le rôle `ADMIN` est présent. Les nouveaux flux utilisent `createAccessToken`.

### 4.2. Signature

- clé HMAC dérivée d'un secret de texte ;
- le secret doit contenir au moins 32 caractères, sinon l'application refuse de démarrer ;
- l'algorithme n'est pas fixé explicitement dans `createToken` : JJWT le déduit de la clé ;
- aucune rotation de clé n'est prévue.

En production, `app.jwt.secret` est obligatoire via `JWT_SECRET`. En développement, une valeur par défaut existe. Elle ne doit pas être utilisée en production.

### 4.3. Expiration

- défaut : 3 600 000 ms, soit 1 heure ;
- configurable par `JWT_EXPIRATION_MS`.

### 4.4. Validation

`JwtAuthenticationFilter` :

- lit l'en-tête `Bearer` ;
- parse le jeton en exigeant `iss` et `aud` ;
- refuse un `jti` absent ou présent dans `security.revoked_access_token` ;
- lit `roles`, `permissions` et `platform_admin` ;
- crée les autorités `ROLE_<role>`, les permissions brutes et `PLATFORM_ADMIN` ;
- place un `JwtPrincipal` (userId, username, companyId, platformAdmin) dans le contexte.

Si le jeton est invalide, expiré, mal formé, mal signé ou révoqué, le filtre renvoie **401** avec une enveloppe JSON.

### 4.5. Révocation

- `jti` est émis pour chaque access token ;
- `security.revoked_access_token` conserve les `jti` révoqués jusqu'à leur expiration ;
- `POST /api/auth/logout` révoque le refresh token fourni et, si l'en-tête `Authorization` est présent, le `jti` de l'access token courant ;
- un refresh token est stocké haché, à usage unique, et révoqué après rotation.

Un access token volé reste techniquement valide jusqu'à `exp` s'il n'a pas été révoqué. La fenêtre est d'une heure par défaut (`JWT_EXPIRATION_MS`). Les tables de révocation ne sont pas encore purgées automatiquement.

### 4.6. Rôles rechargés

Les rôles et permissions sont dans le jeton pour la durée de vie de l'access token. Au refresh, `AuthService` recharge l'utilisateur, son statut, ses rôles, ses permissions et son statut plateforme depuis la base. Un rôle retiré n'est donc plus présent dans le nouveau jeton. L'ancien access token reste valide jusqu'à expiration ou révocation explicite.

---

## 5. Refresh

### 5.1. Route

```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "<refresh token>"
}
```

Route publique. L'ancien champ `token` reste accepté en transition, mais il doit contenir un refresh token ; un access token est refusé.

### 5.2. Comportement

1. hache le refresh token reçu ;
2. consomme atomiquement la ligne `security.refresh_token` non révoquée et non expirée ;
3. charge l'utilisateur et vérifie qu'il est `ACTIVE` ;
4. recharge les rôles et permissions depuis la base ;
5. émet un nouvel access token et un nouveau refresh token ;
6. marque l'ancien refresh token comme remplacé ;
7. renvoie `token`, `refreshToken`, `tokenType` et le profil.

### 5.3. Problèmes restants

- Le refresh token est un secret porteur : il doit être stocké côté client de façon sûre.
- Pas encore de rate limiting sur `/refresh`.
- Les refresh tokens expirés/révoqués ne sont pas purgés automatiquement.
- Un refresh rejoué est bien rejeté (consommation atomique de l'ancien token), mais un vol concurrent peut consommer le token avant le client légitime.

Si l'utilisateur a été supprimé, le refresh renvoie **401** (et non plus 404). Un compte inactif renvoie **401**.

---

## 6. Rôles et permissions

### 6.1. Ce qui existe

- table `role` avec des rôles globaux (`company_id = null`) et des rôles d'entreprise ;
- table `permission` ;
- table `role_permission` ;
- table `user_role` ;
- endpoints CRUD `/api/roles` (`ADMIN` ou `roles.manage`) et `/api/permissions` (`ADMIN`) ;
- endpoints d'affectation de rôle et de permission.

### 6.2. Ce qui est réellement appliqué

- `/api/inventory/**` : `ROLE_ADMIN`, `ROLE_INVENTORY` ou `inventory.manage` ;
- `/api/users/**` et `/api/auth/register` : `ROLE_ADMIN` ou `users.manage` ;
- `/api/roles/**` : `ROLE_ADMIN` ou `roles.manage` ;
- `/api/permissions/**` : `ROLE_ADMIN` uniquement (catalogue global, admin plateforme).

Les permissions `users.manage` et `roles.manage` sont attribuées au rôle d'administration de l'entreprise de démonstration. Les routes d'organisation, personnel, clients et sites restent seulement `authenticated` : le RBAC n'y est pas encore appliqué.

### 6.3. Portée des rôles

`role.company_id` distingue les rôles globaux des rôles d'entreprise. Les services `/api/users` et `/api/roles` filtrent désormais par l'entreprise du `JwtPrincipal` :

- un admin d'entreprise ne voit et ne modifie que les utilisateurs et rôles de son entreprise ;
- il ne peut pas affecter un rôle global (y compris `ADMIN`) ni un rôle d'une autre entreprise ;
- un admin plateforme (`platform_admin = true`, rôle global `ADMIN`) n'est pas filtré.

Un admin d'entreprise ne peut pas créer un compte dans une autre entreprise, même en fournissant un `company_id` différent : la valeur est rejetée ou remplacée par celle du jeton.

### 6.4. Escalade

La séparation admin plateforme / admin entreprise est explicite dans le jeton (`platform_admin`) et dans les contrôles de service. Un admin d'entreprise peut créer des utilisateurs et gérer les rôles de son entreprise, mais :

- il ne peut pas modifier le catalogue global des permissions ;
- il ne peut pas affecter un rôle global `ADMIN` ;
- il ne peut pas lire un utilisateur ou un rôle d'une autre entreprise (réponse 404).

Restent à traiter : la politique de suppression, l'audit des actions d'administration et le rate limiting.

---

## 7. CORS

La configuration lit `app.cors.allowed-origins` (`CORS_ALLOWED_ORIGINS`). En développement, la valeur par défaut est `http://localhost:3000,http://localhost:5173,http://localhost:4200`. En production, `application-prod.properties` exige la variable `CORS_ALLOWED_ORIGINS` et refuse une liste vide.

```java
configuration.setAllowedOriginPatterns(origins);
configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
configuration.setAllowedHeaders(List.of("*"));
configuration.setAllowCredentials(true);
configuration.setMaxAge(3600L);
```

- les origines sont explicites et distinctes par environnement ;
- les credentials restent autorisés ;
- tous les en-têtes et toutes les méthodes sont acceptés ;
- une origine absente de la liste est refusée par le navigateur.

Avec un jeton porté par `Authorization`, le navigateur n'attache pas automatiquement le jeton comme il le ferait avec un cookie. La politique est désormais compatible avec une API de production, à condition que `CORS_ALLOWED_ORIGINS` soit correctement renseigné. Il reste recommandé de restreindre les en-têtes et méthodes réellement utilisés.

---

## 8. Erreurs et fuites d'information

### 8.1. Erreurs SQL

`ErrorMapper.fromSql` renvoie :

- un message fonctionnel (`Valeur déjà existante`, `Champ obligatoire manquant`, etc.) ;
- le message PostgreSQL ;
- le détail PostgreSQL ;
- `SQLState : <code>`.

Ces informations sont utiles au développement, mais exposent :

- les noms de contraintes ;
- les noms de colonnes ;
- la structure de la base ;
- des détails d'implémentation.

Elles étaient renvoyées avec un statut **500**, ce qui était doublement trompeur : l'erreur est souvent une erreur client (doublon, FK, champ manquant), pas une panne serveur. **Corrigé au Lot 2** : les erreurs SQL sont traduites en 400/409/422/503 et le corps ne contient plus ni SQLSTATE, ni message PostgreSQL, ni nom de contrainte.

### 8.2. Erreurs JWT

Le filtre renvoie des messages distincts pour :

- jeton expiré ;
- jeton mal formé ;
- signature invalide ;
- jeton non supporté ;
- jeton révoqué ou `jti` absent ;
- autre erreur JWT.

Toutes ces erreurs utilisent le statut **401** et l'enveloppe `{"errors": ...}`. Les messages restent distincts, ce qui peut aider un attaquant à comprendre pourquoi un jeton échoue. Ce n'est pas le problème principal.

### 8.3. Ce qui n'est pas exposé

- le hash du mot de passe n'apparaît pas dans les réponses publiques ;
- le secret JWT n'est pas renvoyé par l'API ;
- les identifiants de base ne sont pas renvoyés.

C'est un point positif à préserver.

---

## 9. Configuration de production

| Élément | Constat |
|---|---|
| Secret JWT | `application-prod.properties` exige `JWT_SECRET`. |
| Identifiants base | `application-prod.properties` exige `DB_URL`, `DB_USER`, `DB_PASSWORD`. |
| Utilisation réelle | Depuis le Lot 2, les DAO utilisent le `DataSource` Spring/HikariCP configuré par `spring.datasource.*` ; `DatabaseConnection` ne contient plus de valeurs de repli. |
| JWT | `JWT_EXPIRATION_MS` (1 h par défaut), `JWT_REFRESH_EXPIRATION_MS` (30 jours par défaut), `JWT_ISSUER`, `JWT_AUDIENCE`. |
| CORS | `CORS_ALLOWED_ORIGINS` obligatoire en production. |
| Refresh et révocation | Tables `security.refresh_token` et `security.revoked_access_token` ; pas de purge automatique. |
| HTTPS | Aucune règle `requiresChannel()` ni redirection HTTP → HTTPS. |
| En-têtes de sécurité | Les en-têtes par défaut de Spring Security sont conservés, mais aucune politique HSTS/CSP explicite n'est définie. |
| Actuator | Non exposé dans le périmètre examiné. |

Le risque était que les DAO contournent le `DataSource` Spring avec `DriverManager`, sans pool, sans les réglages Spring de timeouts/TLS/validation et avec des valeurs de repli locales. **Corrigé au Lot 2** : les DAO passent par `DataSourceUtils` sur le `DataSource` Spring/HikariCP et la configuration de production est réellement appliquée. Le couplage statique via `DatabaseConnection` reste une dette de conception, mais il ne contourne plus Spring.

---

## 10. Tests de sécurité

| Classe | Tests | Couverture |
|---|---|---|
| `AuthHardeningApiTests` | 7 | inscription publique refusée, inscription sans permission, inscription dans l'entreprise du jeton, lecture inter-entreprise refusée, liste filtrée, catalogue des permissions protégé, inventaire soumis à permission |
| `RefreshTokenApiTests` | 5 | nouveaux access/refresh tokens, rotation et rejet de l'ancien refresh, refresh absent, refresh invalide, logout et révocation des deux jetons |
| `WhoAmIApiTests` | 3 | whoami valide, sans jeton, utilisateur inconnu |
| `AppUserApiTests` | 3 | création admin, champs protégés, validation |
| `ErrorFormatTests` | 11 | enveloppe d'erreur, dont les réponses de sécurité |
| `ItemApiTests.listWithoutTokenIsRejected` | 1 | accès inventaire sans jeton |

Total des tests ciblés exécutés : **29 tests, 29 succès** (`AppUserApiTests`, `AuthHardeningApiTests`, `RefreshTokenApiTests`, `WhoAmIApiTests`, `ErrorFormatTests`). `ItemApiTests.listWithoutTokenIsRejected` appartient à la suite complète. Suite complète après le Lot 2 : **132 tests, 132 succès, 0 échec, 27 suites**.

### 10.1. Ce qui n'est pas testé

- mot de passe incorrect ;
- compte inactif ;
- verrouillage automatique après N échecs ;
- isolation entre deux entreprises réelles (une seule entreprise en base) ;
- révocation d'un rôle puis utilisation de l'access token encore valide ;
- CORS ;
- rate limiting ;
- expiration réelle d'un jeton ;
- jeton signé avec une mauvaise clé ;
- purge des refresh tokens et des `jti` révoqués.

### 10.2. Ce que les tests figent

- l'inscription sans jeton ou sans permission → 401/403 ;
- l'inscription dans une autre entreprise → 403/400 ;
- la lecture d'un utilisateur d'une autre entreprise → 404 ;
- la liste des utilisateurs filtrée par entreprise ;
- l'accès au catalogue global des permissions refusé à un admin d'entreprise ;
- l'inventaire refusé sans permission ;
- le refresh avec rotation et rejet de l'ancien token ;
- le logout révoque le refresh et l'access token ;
- whoami sans token → 401 ;
- whoami avec utilisateur inconnu → 404.

Le test de suite complet conserve un échec préexistant sur l'historique d'un article inconnu : le contrôleur renvoie 200 au lieu de 404. Ce point est hors Lot 1 et documenté dans le rapport 11.

---

## 11. Verdict

Après le Lot 1, l'authentification est **présente, durcie et testée** : BCrypt, JWT signé avec `iss`/`aud`/`jti`, entreprise et permissions dans le jeton, refresh distinct et rotatif, logout et révocation, rôles rechargés au refresh, inscription fermée, RBAC sur l'inventaire et l'administration, CORS explicite.

Elle n'est pas encore **suffisante** pour une API multi-entreprise :

- l'hypothèse mono-entreprise est retenue : les DAO d'inventaire ne filtrent pas par `company_id` ;
- les routes organisation, personnel, clients et sites restent seulement `authenticated` ;
- pas de rate limiting, de verrouillage automatique ni de politique de mot de passe ;
- le login renvoie encore 400 pour des identifiants invalides ou un compte inactif ;
- les jetons d'accès volés restent valides jusqu'à expiration s'ils ne sont pas révoqués ;
- les tables de refresh/révocation ne sont pas purgées ;
- les erreurs SQL détaillées sont toujours exposées ;
- le `DataSource` Spring reste contourné par les DAO.

Les priorités restantes sont :

1. ajouter les filtres `company_id` et les contraintes composites dans les DAO d'inventaire (P0-01) ;
2. étendre le RBAC aux modules organisation, personnel, clients et sites ;
3. limiter les tentatives de connexion et de refresh, et verrouiller les comptes ;
4. aligner les statuts du login (401/403) et stabiliser le contrat d'erreur ;
5. **fait au Lot 2** : masquer les détails SQL en production ;
6. **fait au Lot 2** : faire passer les DAO par le `DataSource` Spring et un pool ;
7. ajouter la purge des jetons expirés/révoqués et l'audit des actions d'administration.

Les critères d'acceptation sont dans le rapport 15.
