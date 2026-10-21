# 09 — Audit de l'authentification et de la sécurité

**Objectif :** évaluer l'inscription, la connexion, les JWT, le refresh, les rôles, les permissions, le CORS et la protection des données. Les routes d'inventaire sont supposées authentifiées ; ce rapport vérifie ce que « authentifié » signifie réellement.

---

## 1. Modèle de sécurité réel

| Élément | Implémentation | Fichier |
|---|---|---|
| Authentification | JWT porté par `Authorization: Bearer` | `JwtAuthenticationFilter`, `JwtService` |
| Hash des mots de passe | BCrypt | `SecurityConfig.passwordEncoder` |
| Autorisation | Règles d'URL dans `SecurityConfig` | `SecurityConfig` |
| Rôles | table `role`, `user_role` | `03_security.sql` |
| Permissions | table `permission`, `role_permission` | `03_security.sql` |
| Sessions | stateless | `SessionCreationPolicy.STATELESS` |
| CSRF | désactivé | `SecurityConfig` |
| CORS | toutes origines, credentials autorisés | `SecurityConfig.corsConfigurationSource` |

### 1.1. Règles d'accès exactes

```text
/                         permitAll
/ping                     permitAll
/error                    permitAll
/api/auth/register        permitAll
/api/auth/login           permitAll
/api/auth/refresh         permitAll
/api/inventory/**         authenticated
/api/users/**             hasRole("ADMIN")
/api/roles/**             hasRole("ADMIN")
/api/permissions/**       hasRole("ADMIN")
anyRequest                authenticated
```

### 1.2. Conséquences immédiates

- Toutes les routes d'inventaire exigent seulement un JWT valide, **sans rôle ni permission**.
- Toutes les routes d'organisation, de personnel, de clients et de sites exigent seulement un JWT valide.
- Les permissions stockées en base ne sont consommées par **aucun** contrôle.
- Aucun `@PreAuthorize`, `@PostAuthorize` ou `@Secured` n'existe dans le code, malgré `@EnableMethodSecurity`.
- Un utilisateur sans entreprise peut accéder à tout l'inventaire.

---

## 2. Inscription

### 2.1. Route

```http
POST /api/auth/register
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

Route publique, sans JWT.

### 2.2. Comportement

`AuthService.register` :

1. vérifie que `email` et `username` ne sont pas nuls ;
2. vérifie qu'aucun utilisateur n'existe avec ce username ou cet email ;
3. crée un `AppUser` avec :
   - `company_id = null` ;
   - `status = ACTIVE` ;
   - mot de passe BCrypt ;
4. cherche le rôle global `USER` ;
5. l'affecte si trouvé ;
6. renvoie un `AuthResponse` avec `token = null`.

Validation Jakarta :

- `username` obligatoire, max 100 ;
- `email` obligatoire, format email, max 180 ;
- `password` obligatoire, 8 à 128 caractères ;
- `firstName`, `lastName`, `phone` facultatifs.

### 2.3. Problèmes

| Problème | Impact |
|---|---|
| Inscription publique | N'importe qui peut créer un compte. |
| Aucune entreprise | Le compte n'est rattaché à aucun tenant. |
| Aucun jeton retourné | Le client doit appeler `/login` juste après. |
| Rôle par défaut `USER` | Ce rôle donne accès à tout l'inventaire, car aucune permission n'est vérifiée. |
| Aucun contrôle d'email | Aucune vérification d'adresse. |
| Aucun rate limiting | Création massive de comptes. |
| Aucun CAPTCHA | Automatisation facile. |
| Doublon en course | `23505` non intercepté → 500. |
| Si le rôle `USER` n'existe pas | L'utilisateur est créé sans rôle, mais reste authentifié et accède quand même à l'inventaire. |

### 2.4. Rôle absent

La recherche du rôle est un `ifPresent`. Si le rôle global `USER` n'existe pas :

- l'utilisateur est créé ;
- aucun rôle ne lui est affecté ;
- il peut se connecter ;
- le JWT a une liste de rôles vide ;
- le filtre crée une authentification avec zéro autorité ;
- les routes `authenticated()` restent accessibles.

C'est une faille de conception : l'absence de rôle ne bloque pas l'accès.

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
| Identifiants invalides → **400** | Un client s'attend à 401 ; le 400 mélange validation et authentification. |
| Compte inactif → **400** | Même remarque. |
| Aucun rate limiting | Brute force possible. |
| Aucun verrouillage après N échecs | `UserStatus.LOCKED` n'est jamais appliqué automatiquement. |
| Message identique pour utilisateur inconnu / mauvais mot de passe | Bon point pour l'énumération, mais le 400 reste ambigu. |
| Aucun test de login | La route n'est pas couverte par les tests. |
| Aucun test d'inscription | La route n'est pas couverte par les tests. |

---

## 4. JWT

### 4.1. Structure

`JwtService.createToken` crée un jeton avec :

- `sub` = username ;
- `uid` = identifiant utilisateur ;
- `roles` = liste des noms de rôles ;
- `iat` = date d'émission ;
- `exp` = date d'expiration.

Il ne contient pas :

- `company_id` ;
- `tenant` ;
- `iss` (émetteur) ;
- `aud` (audience) ;
- `jti` (identifiant unique du jeton) ;
- `scope` ou permissions fines.

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
- parse le jeton ;
- lit `roles` ;
- crée les autorités `ROLE_<role>` ;
- place l'authentification dans le contexte.

Si le jeton est invalide, expiré, mal formé ou mal signé, le filtre renvoie **401** avec une enveloppe JSON.

### 4.5. Absence de révocation

Il n'existe :

- aucun `jti` ;
- aucune liste de révocation ;
- aucun logout JWT avec révocation côté serveur ;
- aucun refresh token persistant ;
- aucun moyen d'invalider un jeton avant son expiration.

Un jeton volé reste valide jusqu'à `exp`.

### 4.6. Rôles figés

Les rôles sont dans le jeton. Si un administrateur retire un rôle :

- le jeton existant conserve l'ancien rôle ;
- l'utilisateur garde l'accès jusqu'à expiration ;
- `/api/auth/refresh` **réutilise les rôles du jeton**, sans les recharger depuis la base.

Conséquence : un `ADMIN` révoqué peut continuer à rafraîchir son jeton et conserver ses droits tant qu'il rafraîchit avant expiration, que l'utilisateur existe et reste `ACTIVE`. C'est une faille d'autorisation majeure.

---

## 5. Refresh

### 5.1. Route

```http
POST /api/auth/refresh
Content-Type: application/json

{
  "token": "<JWT>"
}
```

Route publique.

### 5.2. Comportement

1. parse le jeton ;
2. extrait `uid` ;
3. charge l'utilisateur ;
4. vérifie qu'il est `ACTIVE` ;
5. recrée un jeton avec les **mêmes rôles que l'ancien jeton** ;
6. renvoie le nouveau jeton.

### 5.3. Problèmes

- Pas de refresh token distinct : l'access token sert à se rafraîchir.
- Pas de rotation ni de révocation.
- Rôles non rechargés : une révocation est ignorée.
- Si l'utilisateur est supprimé : **404**.
- Si l'utilisateur est inactif : **400**.
- Si le jeton est invalide : **401**.
- Aucune limitation de débit.

Un utilisateur peut prolonger sa session tant qu'il rafraîchit avant expiration, même après un changement de rôle.

---

## 6. Rôles et permissions

### 6.1. Ce qui existe

- table `role` avec des rôles globaux (`company_id = null`) et des rôles d'entreprise ;
- table `permission` ;
- table `role_permission` ;
- table `user_role` ;
- endpoints CRUD `/api/roles` et `/api/permissions`, réservés à `ADMIN` ;
- endpoints d'affectation de rôle et de permission.

### 6.2. Ce qui est réellement appliqué

- `hasRole("ADMIN")` sur `/api/users/**`, `/api/roles/**`, `/api/permissions/**` ;
- rien d'autre.

Les permissions (`inventory.manage`, `employee.read`, etc.) ne sont jamais vérifiées. Le rôle `INVENTORY` existe en base mais n'a aucun effet sur les routes d'inventaire.

### 6.3. Portée des rôles

Un rôle peut être global ou lié à une entreprise via `role.company_id`, mais le contrôle `hasRole` ne tient compte ni de l'entreprise du rôle, ni de l'entreprise de l'utilisateur. Un `ADMIN` d'une entreprise peut administrer les utilisateurs de toutes les entreprises.

### 6.4. Escalade

Un `ADMIN` peut :

- créer un utilisateur ;
- lui affecter n'importe quel rôle ;
- modifier les rôles et permissions ;
- supprimer des utilisateurs.

Il n'existe aucune séparation entre administrateur de plateforme et administrateur d'entreprise.

---

## 7. CORS

```java
configuration.setAllowedOriginPatterns(List.of("*"));
configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
configuration.setAllowedHeaders(List.of("*"));
configuration.setAllowCredentials(true);
configuration.setMaxAge(3600L);
```

- toute origine est acceptée ;
- les credentials sont autorisés ;
- tous les en-têtes sont acceptés ;
- toutes les méthodes sont acceptées.

Avec un jeton porté par `Authorization`, le navigateur n'attache pas automatiquement le jeton comme il le ferait avec un cookie. Le risque dépend donc de la façon dont le frontend stocke le jeton. Mais la politique reste trop permissive pour une API de production : elle ne restreint aucune origine et ne permet pas de distinguer les environnements.

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

Elles sont renvoyées avec un statut **500**, ce qui est doublement trompeur : l'erreur est souvent une erreur client (doublon, FK, champ manquant), pas une panne serveur.

### 8.2. Erreurs JWT

Le filtre renvoie des messages distincts pour :

- jeton expiré ;
- jeton mal formé ;
- signature invalide ;
- jeton non supporté ;
- autre erreur JWT.

C'est acceptable pour l'utilisateur, mais peut aider un attaquant à comprendre pourquoi un jeton échoue. Ce n'est pas le problème principal.

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
| Utilisation réelle | Les DAO utilisent `DatabaseConnection` avec les mêmes variables `DB_URL`/`DB_USER`/`DB_PASSWORD`, mais via `DriverManager` et des valeurs de repli, pas le `DataSource` Spring. |
| HTTPS | Aucune règle `requiresChannel()` ni redirection HTTP → HTTPS. |
| En-têtes de sécurité | Les en-têtes par défaut de Spring Security sont conservés, mais aucune politique HSTS/CSP explicite n'est définie. |
| Actuator | Non exposé dans le périmètre examiné. |

Le risque n'est pas une divergence de nom de variable : `application-prod.properties` et `DatabaseConnection` lisent bien `DB_URL`, `DB_USER` et `DB_PASSWORD`. Le problème est que les DAO contournent le `DataSource` Spring : ils ouvrent leurs propres connexions avec `DriverManager`, sans pool pour ces connexions, sans les réglages Spring de timeouts/TLS/validation, et avec des valeurs de repli locales codées en dur (utilisables hors profil prod). Spring Boot peut auto-configurer un `DataSource`, mais les DAO ne l'utilisent pas : le pool éventuel est donc sans effet sur ces appels. La configuration de production n'est pas réellement appliquée à la couche qui parle à PostgreSQL.

---

## 10. Tests de sécurité

| Classe | Tests | Couverture |
|---|---|---|
| `WhoAmIApiTests` | 3 | whoami valide, sans jeton, utilisateur inconnu |
| `RefreshTokenApiTests` | 4 | refresh sans header, sans token, token invalide, utilisateur inconnu |
| `AppUserApiTests` | 3 | création admin, champs protégés, validation |
| `ItemApiTests.listWithoutTokenIsRejected` | 1 | accès inventaire sans jeton |

### 10.1. Ce qui n'est pas testé

- inscription publique ;
- connexion valide et invalide ;
- mot de passe incorrect ;
- compte inactif ;
- verrouillage ;
- rôle `ADMIN` requis sur `/api/users` ;
- accès d'un `USER` aux routes d'inventaire ;
- accès d'un utilisateur sans entreprise ;
- isolation entre deux entreprises ;
- révocation d'un rôle ;
- refresh après révocation ;
- CORS ;
- rate limiting ;
- expiration réelle d'un jeton ;
- jeton signé avec une mauvaise clé ;
- jeton sans claim `roles`.

### 10.2. Ce que les tests figent

- refresh sans header autorisé ;
- refresh avec token invalide → 401 ;
- refresh avec utilisateur inconnu → 404 ;
- whoami sans token → 401 ;
- whoami avec utilisateur inconnu → 404.

Ils ne prouvent pas que l'autorisation fonctionne.

---

## 11. Verdict

L'authentification est **présente et fonctionnelle** : BCrypt, JWT signé, filtre, rôles, refresh. Elle n'est pas **suffisante** pour une API multi-entreprise :

- inscription publique sans entreprise ;
- aucun rôle ou permission appliqué à l'inventaire ;
- aucun tenant dans le jeton ;
- rôles figés et refresh qui perpétue les rôles révoqués ;
- aucune révocation ;
- CORS permissif ;
- erreurs SQL détaillées ;
- `DataSource` Spring contourné : pool non utilisé par les DAO, réglages de production ignorés.

Les priorités sont :

1. fermer ou encadrer l'inscription publique ;
2. ajouter l'entreprise et les permissions au jeton, ou les recharger à chaque requête ;
3. appliquer un RBAC par permission et par tenant ;
4. recharger les rôles au refresh et révoquer les jetons ;
5. limiter les tentatives de connexion et de scan ;
6. restreindre le CORS ;
7. masquer les détails SQL en production ;
8. faire passer les DAO par le `DataSource` Spring et un pool.

Les critères d'acceptation sont dans le rapport 15.
