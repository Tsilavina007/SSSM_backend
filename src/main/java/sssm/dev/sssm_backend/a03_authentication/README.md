# Authentication and RBAC

The API uses stateless bearer JWTs. `POST /api/auth/register` accepts
`username`, `email`, `password` (at least 8 characters), and optional profile
fields. New users are `ACTIVE` and receive the global role configured by
`app.auth.default-role` (`USER` by default) when that role exists.
Registration does not expose `password_hash` and returns a bearer token.

`POST /api/auth/login` accepts `{ "login": "username-or-email", "password":
"..." }` and returns `{ token, tokenType, user: { id, username, email,
roles } }`. Send the token as `Authorization: Bearer <JWT_TOKEN>`.

Set `JWT_SECRET` to a random value of at least 32 characters and optionally set
`JWT_EXPIRATION_MS` and `AUTH_DEFAULT_ROLE`. Database credentials use `DB_URL`,
`DB_USER`, and `DB_PASSWORD` (local development defaults are retained). Do not
commit real secrets.

`/api/users`, `/api/roles`, and `/api/permissions` require `ADMIN`.
