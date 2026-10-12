# Inventory security

Every route under `/api/inventory/**` requires a valid JWT bearer token. The
same protection covers the inventory aliases (`/api/items`, `/api/warehouses`,
and related administrative routes); callers must authenticate before using
them. Role-specific business policies can be added with
`@PreAuthorize("hasRole('...')")` as roles are provisioned.

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"operator","email":"operator@example.test","password":"change-me-123"}'
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"login":"operator","password":"change-me-123"}'
curl http://localhost:8080/api/inventory/inventory-counts \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

Configure `JWT_SECRET`, `JWT_EXPIRATION_MS`, and `AUTH_DEFAULT_ROLE` through
environment variables; the checked-in local secret is only a development
placeholder.
