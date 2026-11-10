#!/usr/bin/env python3
"""Génère la collection Postman et les environnements de l'API SSSM.

La collection est dérivée des spécifications OpenAPI du dépôt (``docs/api``) :
chaque opération documentée devient une requête, organisée par module puis par
ressource. Les routes qui ne figurent pas encore dans les spécifications
(``logout``, ``ping``, ``hello``) sont ajoutées explicitement.

Les identifiants de ressources sont des **variables de collection** : elles sont
alimentées par les scripts de test (login, listes, créations) et ne sont jamais
masquées par l'environnement. L'environnement ne porte que la connexion
(``baseUrl``, ``login``, ``password``).

Utilisation :

    python scripts/postman/generate-postman-collection.py
"""

import json
import os
import re
from collections import OrderedDict

from ruamel.yaml import YAML

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
API_DIR = os.path.join(ROOT, "docs", "api")
OUT_DIR = os.path.join(ROOT, "docs", "postman")

yaml = YAML(typ="safe")

# ---------------------------------------------------------------------------
# Organisation de la collection
# ---------------------------------------------------------------------------
# path_vars : correspondance entre le paramètre de chemin OpenAPI et la
#             variable de collection utilisée dans l'URL.
# list_capture : variable alimentée par le premier élément renvoyé par une liste.
# created_var : variable alimentée par l'identifiant renvoyé par une création.
# public : opérations accessibles sans jeton.
# ---------------------------------------------------------------------------
MODULES = [
    (
        "00 — Authentification",
        [
            dict(
                key="auth",
                spec="03_authentication/auth-openapi.yaml",
                subfolder=None,
                path_vars={},
                public={"POST /api/v1/auth/login", "POST /api/v1/auth/refresh"},
                list_capture=None,
                created_var="createdUserId",
            )
        ],
    ),
    (
        "01 — Utilisateurs, rôles et permissions",
        [
            dict(
                key="user",
                spec="03_authentication/user-openapi.yaml",
                subfolder="Utilisateurs",
                path_vars={"id": "userId", "userId": "userId", "roleId": "roleId"},
                public=set(),
                list_capture="userId",
                created_var="createdUserId",
            ),
            dict(
                key="role",
                spec="03_authentication/role-openapi.yaml",
                subfolder="Rôles",
                path_vars={"id": "roleId", "roleId": "roleId", "permissionId": "permissionId"},
                public=set(),
                list_capture="roleId",
                created_var="createdRoleId",
            ),
            dict(
                key="permission",
                spec="03_authentication/permission-openapi.yaml",
                subfolder="Permissions",
                path_vars={"id": "permissionId"},
                public=set(),
                list_capture="permissionId",
                created_var="createdPermissionId",
            ),
        ],
    ),
    (
        "02 — Organisation",
        [
            dict(
                key="company",
                spec="02_organisation/company-openapi.yaml",
                subfolder="Sociétés",
                path_vars={"companyId": "companyId"},
                public=set(),
                list_capture="companyId",
                created_var="createdCompanyId",
            ),
            dict(
                key="agency",
                spec="02_organisation/agency-openapi.yaml",
                subfolder="Agences",
                path_vars={"agencyId": "agencyId"},
                public=set(),
                list_capture="agencyId",
                created_var="createdAgencyId",
            ),
            dict(
                key="branch",
                spec="02_organisation/branch-openapi.yaml",
                subfolder="Succursales",
                path_vars={"branchId": "branchId"},
                public=set(),
                list_capture="branchId",
                created_var="createdBranchId",
            ),
        ],
    ),
    (
        "03 — Personnel",
        [
            dict(
                key="employee",
                spec="04_personnel/employee-openapi.yaml",
                subfolder="Employés",
                path_vars={"id": "employeeId"},
                public=set(),
                list_capture="employeeId",
                created_var="createdEmployeeId",
            ),
            dict(
                key="employee_document",
                spec="04_personnel/employee_document-openapi.yaml",
                subfolder="Documents d'employé",
                path_vars={"id": "employeeDocumentId", "employeeId": "employeeId"},
                public=set(),
                list_capture="employeeDocumentId",
                created_var="createdEmployeeDocumentId",
            ),
        ],
    ),
    (
        "04 — Clients",
        [
            dict(
                key="client",
                spec="05_clients/client-openapi.yaml",
                subfolder="Clients",
                path_vars={"id": "clientId"},
                public=set(),
                list_capture="clientId",
                created_var="createdClientId",
            ),
            dict(
                key="client_contact",
                spec="05_clients/client_contact-openapi.yaml",
                subfolder="Contacts client",
                path_vars={"id": "clientContactId", "clientId": "clientId"},
                public=set(),
                list_capture="clientContactId",
                created_var="createdClientContactId",
            ),
        ],
    ),
    (
        "05 — Sites et postes",
        [
            dict(
                key="site",
                spec="06_sites/site-openapi.yaml",
                subfolder="Sites",
                path_vars={"id": "siteId"},
                public=set(),
                list_capture="siteId",
                created_var="createdSiteId",
            ),
            dict(
                key="security_post",
                spec="06_sites/security_post-openapi.yaml",
                subfolder="Postes de sécurité",
                path_vars={"id": "postId", "siteId": "siteId"},
                public=set(),
                list_capture="postId",
                created_var="createdPostId",
            ),
            dict(
                key="post_requirement",
                spec="06_sites/post_requirement-openapi.yaml",
                subfolder="Exigences de poste",
                path_vars={"id": "postRequirementId", "postId": "postId"},
                public=set(),
                list_capture="postRequirementId",
                created_var="createdPostRequirementId",
            ),
        ],
    ),
    (
        "06 — Inventaire",
        [
            dict(
                key="item_category",
                spec="11_inventory/item_category-openapi.yaml",
                subfolder="Catégories d'articles",
                path_vars={"itemCategoryId": "itemCategoryId"},
                public=set(),
                list_capture="itemCategoryId",
                created_var="createdItemCategoryId",
            ),
            dict(
                key="item_type",
                spec="11_inventory/item_type-openapi.yaml",
                subfolder="Types d'articles",
                path_vars={"itemTypeId": "itemTypeId", "itemCategoryId": "itemCategoryId", "typeId": "itemTypeId"},
                public=set(),
                list_capture="itemTypeId",
                created_var="createdItemTypeId",
            ),
            dict(
                key="warehouse",
                spec="11_inventory/warehouse-openapi.yaml",
                subfolder="Entrepôts",
                path_vars={"warehouseId": "warehouseId"},
                public=set(),
                list_capture="warehouseId",
                created_var="createdWarehouseId",
            ),
            dict(
                key="item",
                spec="11_inventory/item-openapi.yaml",
                subfolder="Articles",
                path_vars={"itemId": "itemId"},
                public=set(),
                list_capture="itemId",
                created_var="createdItemId",
            ),
            dict(
                key="item_assignment",
                spec="11_inventory/item_assignment-openapi.yaml",
                subfolder="Affectations",
                path_vars={
                    "itemAssignmentId": "assignmentId",
                    "itemId": "itemId",
                    "employeeId": "employeeId",
                    "postId": "postId",
                },
                public=set(),
                list_capture="assignmentId",
                created_var="createdAssignmentId",
            ),
            dict(
                key="assignment",
                spec="11_inventory/assignment-openapi.yaml",
                subfolder="Affectations (alias historique)",
                path_vars={
                    "itemAssignmentId": "assignmentId",
                    "itemId": "itemId",
                    "employeeId": "employeeId",
                    "postId": "postId",
                },
                public=set(),
                list_capture="assignmentId",
                created_var="createdAssignmentId",
            ),
            dict(
                key="stock_movement",
                spec="11_inventory/stock_movement-openapi.yaml",
                subfolder="Mouvements de stock",
                path_vars={"movementId": "movementId", "stockMovementId": "movementId"},
                public=set(),
                list_capture="movementId",
                created_var="createdMovementId",
            ),
            dict(
                key="stock_balance",
                spec="11_inventory/stock_balance-openapi.yaml",
                subfolder="Soldes de stock",
                path_vars={},
                public=set(),
                list_capture=None,
                created_var=None,
            ),
            dict(
                key="operation",
                spec="11_inventory/operation-openapi.yaml",
                subfolder="Opérations d'inventaire",
                path_vars={},
                public=set(),
                list_capture=None,
                created_var=None,
            ),
            dict(
                key="inventory_count",
                spec="11_inventory/inventory_count-openapi.yaml",
                subfolder="Inventaires physiques",
                path_vars={"id": "inventoryCountId"},
                public=set(),
                list_capture="inventoryCountId",
                created_var="createdInventoryCountId",
            ),
            dict(
                key="qr_scan",
                spec="11_inventory/qr_scan-openapi.yaml",
                subfolder="QR code et scans",
                path_vars={},
                public=set(),
                list_capture=None,
                created_var=None,
            ),
            dict(
                key="label",
                spec="11_inventory/label-openapi.yaml",
                subfolder="Étiquettes",
                path_vars={"itemId": "itemId"},
                public=set(),
                list_capture=None,
                created_var=None,
            ),
            dict(
                key="report",
                spec="11_inventory/report-openapi.yaml",
                subfolder="Rapports",
                path_vars={},
                public=set(),
                list_capture=None,
                created_var=None,
            ),
            dict(
                key="dashboard",
                spec="11_inventory/dashboard-openapi.yaml",
                subfolder="Tableau de bord et alertes",
                path_vars={},
                public=set(),
                list_capture=None,
                created_var=None,
            ),
        ],
    ),
]

# ---------------------------------------------------------------------------
# Valeurs par défaut des corps de requête
# ---------------------------------------------------------------------------
FIELD_VALUES = {
    "name": "Démo Postman",
    "code": "DEMO-{{$guid}}",
    "email": "demo.{{$guid}}@sssm.test",
    "username": "demo.user.{{$guid}}",
    "password": "Demo@2024",
    "phone": "+261 34 00 00 00",
    "address": "Lot II bis Antananarivo",
    "city": "Antananarivo",
    "country": "Madagascar",
    "currency_code": "MGA",
    "legal_name": "Démo Postman SARL",
    "description": "Créé par la collection Postman SSSM",
    "notes": "Créé par la collection Postman SSSM",
    "registration_number": "REG-{{$guid}}",
    "tax_number": "TAX-{{$guid}}",
    "employee_number": "EMP-{{$guid}}",
    "position": "Agent de démonstration",
    "national_id": "NID-{{$guid}}",
    "hire_date": "2026-01-05",
    "date_of_birth": "1990-01-01",
    "gender": "M",
    "first_name": "Démo",
    "last_name": "Postman",
    "function": "Responsable sécurité",
    "document_type": "CNI",
    "document_number": "DOC-{{$guid}}",
    "file_url": "https://sssm.test/documents/demo.pdf",
    "photo_url": "https://sssm.test/photos/demo.png",
    "logo_url": "https://sssm.test/logo.png",
    "emergency_contact_phone": "+261 34 00 00 00",
    "end_date": None,
    "issue_date": "2024-01-10",
    "expiry_date": "2030-01-10",
    "requirement": "Radio portative",
    "is_primary": True,
    "required_agents": 1,
    "start_time": "08:00:00",
    "end_time": "18:00:00",
    "status": "ACTIVE",
    "active": True,
    "payment_terms_days": 30,
    "client_type": "ENTREPRISE",
    "unit": "piece",
    "quantity_total": 10,
    "minimum_stock": 2,
    "quantity": 1,
    "counted_quantity": 1,
    "discrepancy_type": "NONE",
    "movement_type": "IN",
    "reference": "MVT-{{$guid}}",
    "idempotency_key": "OP-{{$guid}}",
    "latitude": -18.8792,
    "longitude": 47.5079,
    "risk_level": "MEDIUM",
    "access_instructions": "Accès par le portail nord",
    "condition_on_assignment": "Bon état",
    "condition_on_return": "Bon état",
    "expected_return_at": "2026-12-31T12:00:00Z",
    "size": 300,
    "deviceId": "POSTMAN-DEMO",
    "company_id": "{{companyId}}",
    "agency_id": "{{agencyId}}",
    "agence_id": "{{agencyId}}",
    "branch_id": "{{branchId}}",
    "user_id": "{{userId}}",
    "client_id": "{{clientId}}",
    "site_id": "{{siteId}}",
    "post_id": "{{postId}}",
    "employee_id": "{{employeeId}}",
    "role_id": "{{roleId}}",
    "permission_id": "{{permissionId}}",
    "item_type_id": "{{itemTypeId}}",
    "category_id": "{{itemCategoryId}}",
    "warehouse_id": "{{warehouseId}}",
    "current_warehouse_id": "{{warehouseId}}",
    "source_warehouse_id": "{{warehouseId}}",
    "destination_warehouse_id": "{{warehouseId}}",
    "actual_warehouse_id": "{{warehouseId}}",
    "expected_warehouse_id": "{{warehouseId}}",
    "item_id": "{{itemId}}",
    "inventory_count_id": "{{inventoryCountId}}",
    "scanned_by": "{{userId}}",
    "completed_by": "{{userId}}",
    "performed_by": "{{userId}}",
}

# Valeurs spécifiques à une ressource (clé, champ) -> valeur.
RESOURCE_VALUES = {
    ("role", "name"): "Rôle démo {{$guid}}",
    ("permission", "name"): "Permission démo {{$guid}}",
    ("permission", "code"): "demo.{{$guid}}",
    ("item_category", "name"): "Catégorie démo {{$guid}}",
    ("item_type", "code"): "TYPE-{{$guid}}",
    ("item", "qr_code"): "QR-DEMO-{{$guid}}",
    ("item", "item_type_id"): "{{demoApiItemTypeId}}",
    ("item", "current_warehouse_id"): "{{demoApiWarehouseId}}",
    ("client", "code"): "CLI-{{$guid}}",
    ("site", "code"): "SITE-{{$guid}}",
    ("security_post", "code"): "POST-{{$guid}}",
    ("agency", "code"): "AG-{{$guid}}",
    ("branch", "code"): "BR-{{$guid}}",
    ("employee", "employee_number"): "EMP-{{$guid}}",
    ("user", "username"): "demo.user.{{$guid}}",
    ("user", "email"): "demo.user.{{$guid}}@sssm.test",
    # Les écritures d'inventaire visent l'entrepôt isolé de la collection API
    # (fixtures du script docs/database/mock_data/demo_postman.sql) : le solde
    # de démonstration (3 articles DEMO-RADIO) n'est jamais dégradé.
    ("item_assignment", "item_id"): "{{demoApiItemAssignId}}",
    ("item_assignment", "employee_id"): "{{demoEmployeeId}}",
    ("item_assignment", "post_id"): "{{demoPostId}}",
    ("assignment", "item_id"): "{{demoApiItemAliasId}}",
    ("assignment", "employee_id"): "{{demoEmployeeId}}",
    ("assignment", "post_id"): "{{demoPostId}}",
    ("stock_movement", "item_id"): "{{demoApiItemMoveId}}",
    ("stock_movement", "employee_id"): "{{demoEmployeeId}}",
    ("stock_movement", "post_id"): "{{demoPostId}}",
    ("stock_movement", "warehouse_id"): "{{demoApiWarehouseId}}",
    ("stock_movement", "source_warehouse_id"): "{{demoApiWarehouseId}}",
    ("stock_movement", "movement_type"): "OUT",
    ("inventory_count", "warehouse_id"): "{{demoApiWarehouseId}}",
    ("inventory_count", "reference"): "INV-{{$guid}}",
}

# Chemins visés par les requêtes mutantes (PATCH, DELETE, actions) : la
# ressource créée dans le même dossier est ciblée, jamais celle des données de
# démonstration.
MUTATION_TARGETS = {
    "company": {"companyId": "createdCompanyId"},
    "agency": {"agencyId": "createdAgencyId"},
    "branch": {"branchId": "createdBranchId"},
    "user": {"id": "createdUserId", "userId": "createdUserId", "roleId": "roleId"},
    "role": {"id": "createdRoleId", "roleId": "createdRoleId", "permissionId": "permissionId"},
    "permission": {"id": "createdPermissionId"},
    "employee": {"id": "createdEmployeeId"},
    "employee_document": {"id": "createdEmployeeDocumentId"},
    "client": {"id": "createdClientId"},
    "client_contact": {"id": "createdClientContactId"},
    "site": {"id": "createdSiteId"},
    "security_post": {"id": "createdPostId"},
    "post_requirement": {"id": "createdPostRequirementId"},
    "item_category": {"itemCategoryId": "createdItemCategoryId"},
    "item_type": {"itemTypeId": "createdItemTypeId"},
    "warehouse": {"warehouseId": "createdWarehouseId"},
    "item": {"itemId": "createdItemId"},
    "item_assignment": {"itemAssignmentId": "createdAssignmentId"},
    "assignment": {"itemAssignmentId": "createdAssignmentId"},
    "stock_movement": {"movementId": "createdMovementId"},
    "inventory_count": {"id": "createdInventoryCountId"},
}

# Méthodes considérées comme mutantes pour chaque ressource.
MUTATION_METHODS = {
    "item_assignment": {"patch", "delete", "post"},
    "assignment": {"patch", "delete", "post"},
    "inventory_count": {"patch", "delete", "post"},
    "item_category": {"patch", "delete", "post"},
    "item_type": {"patch", "delete", "post"},
    "warehouse": {"patch", "delete", "post"},
    "item": {"patch", "delete", "post"},
    "user": {"patch", "delete", "post"},
    "role": {"patch", "delete", "post"},
}

# Champs exclus des corps de création : ils sont calculés par le serveur ou
# porteraient sur des données de démonstration déjà utilisées.
EXCLUDE_CREATE = {
    "stock_movement": ["operation_id", "movement_date", "destination_warehouse_id"],
    "employee": ["user_id"],
}

# Statuts documentés qui doivent primer sur le premier code d'erreur listé
# dans la spécification (journal append-only).
STATUS_OVERRIDES = {
    "PATCH /api/v1/inventory/movements/{movementId}": "409",
    "DELETE /api/v1/inventory/movements/{movementId}": "409",
}

# Ordre d'exécution imposé à quelques ressources (cycle de vie cohérent).
ROUTE_ORDER = {
    "auth": [
        "POST /api/v1/auth/login",
        "POST /api/v1/auth/register",
        "GET /api/v1/auth/whoami",
        "POST /api/v1/auth/refresh",
    ],
    "user": [
        "GET /api/v1/users",
        "POST /api/v1/users",
        "GET /api/v1/users/{id}",
        "PATCH /api/v1/users/{id}",
        "POST /api/v1/users/{userId}/roles/{roleId}",
        "DELETE /api/v1/users/{userId}/roles/{roleId}",
        "DELETE /api/v1/users/{id}",
    ],
    "role": [
        "GET /api/v1/roles",
        "POST /api/v1/roles",
        "GET /api/v1/roles/{id}",
        "PATCH /api/v1/roles/{id}",
        "POST /api/v1/roles/{roleId}/permissions/{permissionId}",
        "DELETE /api/v1/roles/{roleId}/permissions/{permissionId}",
        "DELETE /api/v1/roles/{id}",
    ],
    "operation": [
        "POST /api/v1/inventory/operations/receipt",
        "POST /api/v1/inventory/operations/transfer",
        "POST /api/v1/inventory/operations/issue",
    ],
    "item_assignment": [
        "GET /api/v1/item-assignments",
        "POST /api/v1/item-assignments",
        "GET /api/v1/item-assignments/{itemAssignmentId}",
        "PATCH /api/v1/item-assignments/{itemAssignmentId}",
        "POST /api/v1/item-assignments/{itemAssignmentId}/return",
        "POST /api/v1/item-assignments/{itemAssignmentId}/void",
        "POST /api/v1/item-assignments/{itemAssignmentId}/restore",
        "DELETE /api/v1/item-assignments/{itemAssignmentId}",
        "GET /api/v1/item-assignments/item/{itemId}",
        "GET /api/v1/item-assignments/employee/{employeeId}",
        "GET /api/v1/item-assignments/post/{postId}",
    ],
    "assignment": [
        "GET /api/v1/inventory/assignments",
        "POST /api/v1/inventory/assignments",
        "GET /api/v1/inventory/assignments/{itemAssignmentId}",
        "PATCH /api/v1/inventory/assignments/{itemAssignmentId}",
        "POST /api/v1/inventory/assignments/{itemAssignmentId}/return",
        "POST /api/v1/inventory/assignments/{itemAssignmentId}/void",
        "POST /api/v1/inventory/assignments/{itemAssignmentId}/restore",
        "DELETE /api/v1/inventory/assignments/{itemAssignmentId}",
        "GET /api/v1/inventory/items/{itemId}/assignments",
        "GET /api/v1/inventory/employees/{employeeId}/assignments",
        "GET /api/v1/inventory/security-posts/{postId}/assignments",
    ],
    "inventory_count": [
        "GET /api/v1/inventory/inventory-counts",
        "POST /api/v1/inventory/inventory-counts",
        "GET /api/v1/inventory/inventory-counts/{id}",
        "POST /api/v1/inventory/inventory-counts/{id}/scans",
        "GET /api/v1/inventory/inventory-counts/{id}/results",
        "POST /api/v1/inventory/inventory-counts/{id}/close",
        "POST /api/v1/inventory/inventory-counts/{id}/cancel",
    ],
}

# Requêtes dont le statut dépend de l'état métier : l'assertion accepte un
# succès ou un conflit métier documenté.
TOLERANT_ROUTES = {
    "POST /api/v1/item-assignments/{itemAssignmentId}/return",
    "POST /api/v1/item-assignments/{itemAssignmentId}/void",
    "POST /api/v1/item-assignments/{itemAssignmentId}/restore",
    "DELETE /api/v1/item-assignments/{itemAssignmentId}",
    "POST /api/v1/inventory/assignments/{itemAssignmentId}/return",
    "POST /api/v1/inventory/assignments/{itemAssignmentId}/void",
    "POST /api/v1/inventory/assignments/{itemAssignmentId}/restore",
    "DELETE /api/v1/inventory/assignments/{itemAssignmentId}",
    "POST /api/v1/inventory/inventory-counts/{id}/close",
    "POST /api/v1/inventory/inventory-counts/{id}/cancel",
    "POST /api/v1/inventory/inventory-counts/{id}/scans",
}

# Champs réellement exigés par les DTO Java mais absents des listes `required`
# des spécifications OpenAPI.
REQUIRED_EXTRA = {
    "company": ["currency_code"],
}

# Champs optionnels retenus dans les corps de création, par ressource.
CREATE_OPTIONAL = {
    "company": ["legal_name", "registration_number", "tax_number", "email", "phone", "address", "city"],
    "agency": ["code", "address", "city", "phone", "email"],
    "branch": ["code", "address", "city", "phone", "email"],
    "user": ["email", "first_name", "last_name", "phone"],
    "role": ["description"],
    "permission": ["description"],
    "employee": ["email", "phone", "position", "gender", "hire_date", "national_id", "agency_id"],
    "employee_document": ["document_number", "issue_date", "expiry_date", "file_url", "notes"],
    "client": ["code", "legal_name", "client_type", "email", "phone", "address", "city", "agency_id"],
    "client_contact": ["email", "phone", "function", "is_primary"],
    "site": ["code", "client_id", "branch_id", "address", "city", "latitude", "longitude", "access_instructions", "risk_level"],
    "security_post": ["code", "description", "required_agents", "start_time", "end_time"],
    "post_requirement": [],
    "item_category": ["description"],
    "item_type": ["description", "unit", "quantity_total", "minimum_stock", "category_id"],
    "warehouse": ["branch_id", "address"],
    "item": ["category_id", "description", "current_warehouse_id"],
    "item_assignment": ["condition_on_assignment", "notes", "expected_return_at"],
    "assignment": ["condition_on_assignment", "notes", "expected_return_at"],
    "stock_movement": ["reference", "notes"],
    "inventory_count": ["notes"],
}

# Champs retenus dans les corps de mise à jour partielle (PATCH).
PATCH_FIELDS = {
    "company": ["legal_name", "email", "phone", "address", "city"],
    "agency": ["address", "city", "phone", "email"],
    "branch": ["address", "city", "phone", "email"],
    "user": ["email", "first_name", "last_name", "phone"],
    "role": ["description"],
    "permission": ["description"],
    "employee": ["phone", "email", "position", "notes"],
    "employee_document": ["document_number", "file_url", "notes"],
    "client": ["legal_name", "email", "phone", "address", "city"],
    "client_contact": ["email", "phone", "function"],
    "site": ["address", "city", "access_instructions", "risk_level"],
    "security_post": ["description", "required_agents", "start_time", "end_time"],
    "post_requirement": ["requirement"],
    "item_category": ["description"],
    "item_type": ["description", "unit", "minimum_stock"],
    "warehouse": ["address"],
    "item": ["description"],
    "item_assignment": ["notes", "expected_return_at"],
    "assignment": ["notes", "expected_return_at"],
    "stock_movement": ["notes"],
}

# Champs gérés par le serveur : jamais envoyés par le client.
SERVER_MANAGED = {
    "id",
    "created_at",
    "updated_at",
    "archived_at",
    "archived_by",
    "assigned_at",
    "returned_at",
    "returned_by",
    "assigned_by",
    "voided_at",
    "voided_by",
    "void_reason",
    "scanned_at",
    "completed_at",
    "performed_by",
    "version",
    "archived",
}

# Corps rédigés à la main lorsque le schéma OpenAPI ne suffit pas.
MANUAL_BODIES = {
    "POST /api/v1/auth/login": {"login": "{{login}}", "password": "{{password}}"},
    "POST /api/v1/auth/refresh": {"refreshToken": "{{refreshToken}}"},
    "POST /api/v1/auth/logout": {"refreshToken": "{{refreshToken}}"},
    "POST /api/v1/auth/register": {
        "username": "demo.register.{{$guid}}",
        "password": "Demo@2024",
        "email": "demo.register.{{$guid}}@sssm.test",
        "firstName": "Démo",
        "lastName": "Postman",
        "phone": "+261 34 00 00 00",
    },
    "POST /api/v1/inventory/items/bulk": [
        {
            "company_id": "{{companyId}}",
            "item_type_id": "{{itemTypeId}}",
            "qr_code": "QR-BULK-{{$guid}}",
            "description": "Article créé en lot",
        },
        {
            "company_id": "{{companyId}}",
            "item_type_id": "{{itemTypeId}}",
            "qr_code": "QR-BULK-{{$guid}}",
            "description": "Second article du lot",
        },
    ],
    "POST /api/v1/inventory/labels/batch": {"item_ids": ["{{itemId}}"], "size": 300},
    "POST /api/v1/inventory/scans/resolve": {"qrCode": "DEMO-POSTMAN-SPARE", "deviceId": "POSTMAN-DEMO"},
    "POST /api/v1/inventory/operations/receipt": {
        "company_id": "{{companyId}}",
        "reference": "OP-{{$guid}}",
        "idempotency_key": "OP-{{$guid}}",
        "destination_warehouse_id": "{{demoApiWarehouseId}}",
        "employee_id": "{{demoEmployeeId}}",
        "post_id": "{{demoPostId}}",
        "notes": "Réception de démonstration",
        "lines": [
            {
                "item_id": "{{demoApiItemOpId}}",
                "quantity": 1,
                "destination_warehouse_id": "{{demoApiWarehouseId}}",
            }
        ],
    },
    "POST /api/v1/inventory/operations/issue": {
        "company_id": "{{companyId}}",
        "reference": "OP-{{$guid}}",
        "idempotency_key": "OP-{{$guid}}",
        "source_warehouse_id": "{{demoApiWarehouseId}}",
        "employee_id": "{{demoEmployeeId}}",
        "post_id": "{{demoPostId}}",
        "notes": "Sortie de démonstration",
        "lines": [
            {
                "item_id": "{{demoApiItemOpId}}",
                "quantity": 1,
                "source_warehouse_id": "{{demoApiWarehouseId}}",
            }
        ],
    },
    "POST /api/v1/inventory/operations/transfer": {
        "company_id": "{{companyId}}",
        "reference": "OP-{{$guid}}",
        "idempotency_key": "OP-{{$guid}}",
        "source_warehouse_id": "{{demoApiWarehouseId}}",
        "destination_warehouse_id": "{{warehouseId}}",
        "employee_id": "{{demoEmployeeId}}",
        "post_id": "{{demoPostId}}",
        "notes": "Transfert de démonstration",
        "lines": [
            {
                "item_id": "{{createdItemId}}",
                "quantity": 1,
                "source_warehouse_id": "{{demoApiWarehouseId}}",
                "destination_warehouse_id": "{{warehouseId}}",
            }
        ],
    },
    "POST /api/v1/inventory/inventory-counts/{id}/scans": {
        "item_id": "{{demoApiItemAssignId}}",
        "counted_quantity": 1,
        "scanned_by": "{{userId}}",
        "actual_warehouse_id": "{{demoApiWarehouseId}}",
        "expected_warehouse_id": "{{demoApiWarehouseId}}",
        "discrepancy_type": "NONE",
        "notes": "Scan de démonstration",
    },
    "POST /api/v1/inventory/inventory-counts/{id}/close": {
        "completed_by": "{{userId}}",
        "notes": "Clôture de démonstration",
    },
    "POST /api/v1/item-assignments/{itemAssignmentId}/return": {"condition_on_return": "Bon état"},
    "POST /api/v1/inventory/assignments/{itemAssignmentId}/return": {"condition_on_return": "Bon état"},
}

# Requêtes hors spécification OpenAPI.
EXTRA_REQUESTS = [
    dict(
        module="00 — Authentification",
        subfolder="00 — Authentification",
        name="5. Déconnexion (logout)",
        method="POST",
        route="/api/v1/auth/logout",
        description=(
            "Révoque le refresh token courant. Envoyée sans en-tête Authorization : "
            "avec l'en-tête, l'access token est lui aussi révoqué et les requêtes "
            "suivantes recevraient un 401."
        ),
        expected="204",
        body=MANUAL_BODIES["POST /api/v1/auth/logout"],
        noauth=True,
    ),
    dict(
        module="07 — Système",
        subfolder="Diagnostics",
        name="1. Sonde de disponibilité (ping)",
        method="GET",
        route="/ping",
        description="Sonde technique du conteneur : renvoie « pong » sans authentification.",
        expected="200",
        body=None,
        noauth=True,
    ),
    dict(
        module="07 — Système",
        subfolder="Diagnostics",
        name="2. Racine du service (hello)",
        method="GET",
        route="/",
        description="Contrôleur de test : renvoie « Hello World » sans authentification.",
        expected="200",
        body=None,
        noauth=True,
    ),
]

# Scripts de test spécifiques.
MANUAL_SCRIPTS = {
    "POST /api/v1/auth/login": [
        "const body = pm.response.json();",
        "pm.test('200 — connexion acceptée', () => pm.response.to.have.status(200));",
        "pm.test('Access token et refresh token émis', () => {",
        "  pm.expect(body.token).to.be.a('string').and.not.empty;",
        "  pm.expect(body.refreshToken).to.be.a('string').and.not.empty;",
        "});",
        "pm.test('Utilisateur attendu', () => pm.expect(body.user.username).to.eql(pm.environment.get('login')));",
        "pm.collectionVariables.set('accessToken', body.token);",
        "pm.collectionVariables.set('refreshToken', body.refreshToken);",
        "pm.collectionVariables.set('userId', body.user.id);",
        "pm.collectionVariables.set('companyId', body.user.companyId);",
        "pm.collectionVariables.set('roles', (body.user.roles || []).join(', '));",
    ],
    "POST /api/v1/auth/refresh": [
        "const body = pm.response.json();",
        "const previous = pm.collectionVariables.get('accessToken');",
        "pm.test('200 — renouvellement accepté', () => pm.response.to.have.status(200));",
        "pm.test('Nouvel access token', () => pm.expect(body.token).to.not.eql(previous));",
        "pm.test('Nouveau refresh token (rotation)', () => pm.expect(body.refreshToken).to.be.a('string').and.not.empty);",
        "pm.collectionVariables.set('accessToken', body.token);",
        "pm.collectionVariables.set('refreshToken', body.refreshToken);",
    ],
    "GET /api/v1/auth/whoami": [
        "const body = pm.response.json();",
        "pm.test('200 — jeton accepté', () => pm.response.to.have.status(200));",
        "pm.test('Utilisateur du jeton', () => pm.expect(body.username).to.be.a('string').and.not.empty);",
    ],
    "POST /api/v1/auth/register": [
        "const body = pm.response.json();",
        "pm.test('201 — compte créé', () => pm.response.to.have.status(201));",
        "pm.test('Utilisateur créé', () => pm.expect(body.user.id).to.be.a('string').and.not.empty);",
        "pm.collectionVariables.set('createdUserId', body.user.id);",
    ],
}

# Paramètres de requête : nom -> (valeur, activé par défaut).
QUERY_VALUES = {
    "search": ("", False),
    "page": ("1", True),
    "pageSize": ("{{pageSize}}", True),
    "warehouseId": ("{{warehouseId}}", False),
    "itemTypeId": ("{{itemTypeId}}", False),
    "itemId": ("{{itemId}}", False),
    "status": ("AVAILABLE", False),
    "active": ("true", False),
    "reason": ("Opération de démonstration Postman", False),
    "size": ("300", False),
    "movementType": ("OUT", False),
}

# Variables de collection : clé -> valeur initiale.
COLLECTION_VARIABLES = OrderedDict(
    [
        ("accessToken", ""),
        ("refreshToken", ""),
        ("userId", "8a1fa87e-33ac-42a5-9696-e013816f8963"),
        ("companyId", "02ced1a2-f121-490a-a114-556f8e18fdd2"),
        ("roles", ""),
        ("agencyId", "a0700115-e32b-4219-bf6b-368a70f93a83"),
        ("branchId", "e5541e50-5934-478b-9dfa-1f058cf77104"),
        ("roleId", "fe07f9a2-90e0-4a5b-a7d4-93ab8f74d928"),
        ("permissionId", "cd3fe5ea-6463-4bbf-b013-348709771643"),
        ("employeeId", "e93a578f-4c5e-4a27-816d-f9bc5349646c"),
        ("employeeDocumentId", "4df30b0a-c3a2-46a4-b7cf-71e564fbe0b8"),
        ("clientId", "8daa9bf3-60fd-4acd-be54-b67baaa7642c"),
        ("clientContactId", "df215416-bd25-4495-89be-890731b1146d"),
        ("siteId", "e0b9dfc4-4ec4-4044-805d-de06693b5cef"),
        ("postId", "972bc7f9-c2f8-40b2-a092-bdff892a5678"),
        ("postRequirementId", "46993518-daa2-47bb-a46e-a84330758d3e"),
        ("itemCategoryId", "65e1c89d-8a3f-4aa0-bbd2-87e4aa0c5eb3"),
        ("itemTypeId", "a03bad81-9e8c-486e-bad4-3e70eb0c4c77"),
        ("warehouseId", "5ec5b62e-bfa0-4276-b429-27c3a829aa8a"),
        ("itemId", "21c68a57-536d-4c4b-b981-aafb49318996"),
        ("assignmentId", ""),
        ("movementId", ""),
        ("inventoryCountId", "132203fa-aefe-4601-a0b6-2d22c4e4878a"),
        ("transferItemId", "{{createdItemId}}"),
        ("pageSize", "50"),
        ("demoWarehouseId", "d0e00000-0000-4000-8000-000000000003"),
        ("demoEmployeeId", "d0e00000-0000-4000-8000-000000000006"),
        ("demoPostId", "d0e00000-0000-4000-8000-000000000005"),
        ("demoItemAssignId", "d0e00000-0000-4000-8000-000000000007"),
        ("demoItemOutId", "d0e00000-0000-4000-8000-000000000008"),
        ("demoItemSpareId", "d0e00000-0000-4000-8000-000000000009"),
        ("demoApiItemTypeId", "d0e00000-0000-4000-8000-00000000000b"),
        ("demoApiWarehouseId", "d0e00000-0000-4000-8000-000000000010"),
        ("demoApiItemAssignId", "d0e00000-0000-4000-8000-00000000000d"),
        ("demoApiItemAliasId", "d0e00000-0000-4000-8000-00000000000e"),
        ("demoApiItemOpId", "d0e00000-0000-4000-8000-000000000011"),
        ("demoApiItemMoveId", "d0e00000-0000-4000-8000-000000000012"),
        ("createdCompanyId", ""),
        ("createdAgencyId", ""),
        ("createdBranchId", ""),
        ("createdUserId", ""),
        ("createdRoleId", ""),
        ("createdPermissionId", ""),
        ("createdEmployeeId", ""),
        ("createdEmployeeDocumentId", ""),
        ("createdClientId", ""),
        ("createdClientContactId", ""),
        ("createdSiteId", ""),
        ("createdPostId", ""),
        ("createdPostRequirementId", ""),
        ("createdItemCategoryId", ""),
        ("createdItemTypeId", ""),
        ("createdWarehouseId", ""),
        ("createdItemId", ""),
        ("createdAssignmentId", ""),
        ("createdMovementId", ""),
        ("createdInventoryCountId", ""),
    ]
)


# ---------------------------------------------------------------------------
# Helpers OpenAPI
# ---------------------------------------------------------------------------
def resolve(doc, node):
    """Résout une référence $ref (locale) et fusionne les allOf."""
    if not isinstance(node, dict):
        return node
    if "$ref" in node:
        ref = node["$ref"]
        if ref.startswith("#/"):
            target = doc
            for part in ref[2:].split("/"):
                target = target[part]
            merged = dict(target)
            merged.update({k: v for k, v in node.items() if k != "$ref"})
            return resolve(doc, merged)
    if "allOf" in node:
        merged = {}
        for part in node["allOf"]:
            resolved = resolve(doc, part)
            if isinstance(resolved, dict):
                merged.update(resolved)
        merged.update({k: v for k, v in node.items() if k != "allOf"})
        return merged
    return node


def property_schema(doc, schema, name):
    schema = resolve(doc, schema)
    return resolve(doc, schema.get("properties", {}).get(name, {}))


def is_required(doc, schema, name):
    schema = resolve(doc, schema)
    return name in schema.get("required", [])


def field_value(doc, resource_key, field, schema):
    """Valeur d'un champ de corps de requête."""
    override = RESOURCE_VALUES.get((resource_key, field))
    if override is not None:
        return override
    schema = resolve(doc, schema) if isinstance(schema, dict) else {}
    if "example" in schema:
        return schema["example"]
    if "default" in schema:
        return schema["default"]
    if "enum" in schema and schema["enum"]:
        return schema["enum"][0]
    if field in FIELD_VALUES:
        return FIELD_VALUES[field]
    kind = schema.get("type")
    if kind == "integer" or kind == "number":
        return 1
    if kind == "boolean":
        return True
    if kind == "array":
        return []
    if schema.get("format") == "date":
        return "2026-01-05"
    if schema.get("format") == "date-time":
        return "2026-01-05T09:00:00Z"
    return "Postman"


def build_body(doc, resource_key, schema, method, path_key):
    """Construit le corps JSON d'une requête à partir du schéma OpenAPI."""
    if path_key in MANUAL_BODIES:
        return MANUAL_BODIES[path_key]

    schema = resolve(doc, schema)
    if schema.get("type") == "array":
        item = build_body(doc, resource_key, schema.get("items", {}), method, path_key)
        return [item]

    properties = schema.get("properties", {})
    if not properties:
        return {}

    if method == "patch":
        wanted = PATCH_FIELDS.get(resource_key, [])
    else:
        # Création : tous les champs acceptés par l'API, hors champs gérés par
        # le serveur et hors exclusions explicites.
        wanted = [
            name
            for name in properties
            if name not in SERVER_MANAGED and name not in EXCLUDE_CREATE.get(resource_key, [])
        ]

    body = OrderedDict()
    for name, prop in properties.items():
        if name in SERVER_MANAGED:
            continue
        keep = is_required(doc, schema, name) or name in wanted
        if not keep:
            continue
        value = field_value(doc, resource_key, name, prop)
        if value is None:
            continue
        body[name] = value
    return body


def expected_status(operation, path_key=""):
    """Statut de succès documenté pour l'opération (2xx prioritaire)."""
    if path_key in STATUS_OVERRIDES:
        return STATUS_OVERRIDES[path_key]
    responses = operation.get("responses", {}) or {}
    for code in sorted(responses.keys()):
        if code.startswith("2"):
            return code
    for code in sorted(responses.keys()):
        if code.startswith(("3", "4", "5")):
            return code
    return "200"


def status_label(status):
    return f"{status} — " + {
        "200": "lecture réussie",
        "201": "création réussie",
        "202": "requête acceptée",
        "204": "opération effectuée",
        "400": "requête invalide",
        "409": "conflit métier attendu",
    }.get(status, "réponse attendue")


# ---------------------------------------------------------------------------
# Construction des requêtes
# ---------------------------------------------------------------------------
def build_url(resource, route, method):
    """Chemin Postman : route dont les paramètres sont remplacés par des
    variables de collection.

    Les requêtes mutantes (PATCH, DELETE, actions) ciblent la ressource créée
    dans le même dossier : les données de démonstration ne sont jamais
    dégradées par la collection.
    """
    mutating = method.lower() in MUTATION_METHODS.get(resource["key"], {"patch", "delete"})
    targets = MUTATION_TARGETS.get(resource["key"], {}) if mutating else {}
    mapping = {
        placeholder: targets.get(placeholder, variable)
        for placeholder, variable in resource["path_vars"].items()
    }

    # Substitution en une seule passe : un remplacement par chaîne peut
    # réintroduire des accolades déjà substituées.
    return re.sub(
        r"\{([A-Za-z0-9_]+)\}",
        lambda match: "{{" + mapping.get(match.group(1), match.group(1)) + "}}",
        route,
    )


def build_url_object(resource, route, method, query):
    """Objet URL Postman : raw, host, path et query.

    L'hôte est la variable ``{{baseUrl}}`` (qui contient déjà le schéma et le
    port) : aucun champ ``protocol`` n'est ajouté pour éviter de le dupliquer.
    """
    path = build_url(resource, route, method)
    raw = "{{baseUrl}}" + path
    enabled = [entry for entry in query if not entry.get("disabled")]
    if enabled:
        raw += "?" + "&".join(f"{entry['key']}={entry['value']}" for entry in enabled)
    return {
        "raw": raw,
        "host": ["{{baseUrl}}"],
        "path": [segment for segment in path.split("/") if segment],
        "query": query,
    }


def build_query(doc, resource, operation, path_item):
    parameters = list(path_item.get("parameters", [])) + list(operation.get("parameters", []))
    query = []
    for parameter in parameters:
        parameter = resolve(doc, parameter)
        if parameter.get("in") != "query":
            continue
        name = parameter["name"]
        value, enabled = QUERY_VALUES.get(name, ("", False))
        if name not in QUERY_VALUES:
            schema = resolve(doc, parameter.get("schema", {}))
            if "default" in schema:
                value = str(schema["default"])
            elif schema.get("type") == "integer":
                value = "1"
            elif schema.get("type") == "boolean":
                value = "true"
        entry = {"key": name, "value": value}
        if not enabled:
            entry["disabled"] = True
        if parameter.get("description"):
            entry["description"] = parameter["description"]
        query.append(entry)
    return query


def build_tests(resource, method, route, status, is_list, is_create):
    path_key = f"{method.upper()} {route}"
    script = MANUAL_SCRIPTS.get(path_key)
    if script:
        return script

    if path_key in TOLERANT_ROUTES:
        lines = [
            "pm.test('Réponse métier attendue (succès ou conflit documenté)', () => {",
            "  pm.expect([200, 201, 204, 400, 409]).to.include(pm.response.code);",
            "});"
        ]
    else:
        lines = [f"pm.test('{status_label(status)}', () => pm.response.to.have.status({status}));"]

    if is_create and resource.get("created_var"):
        lines += [
            "const body = pm.response.json();",
            "pm.test('Identifiant créé', () => pm.expect(body.id).to.be.a('string').and.not.empty);",
            f"pm.collectionVariables.set('{resource['created_var']}', body.id);",
        ]
    elif is_list and resource.get("list_capture"):
        lines += [
            "const body = pm.response.json();",
            "const items = Array.isArray(body) ? body : (body.items || []);",
            "pm.test('Réponse paginée exploitable', () => pm.expect(items).to.be.an('array'));",
            "if (items.length && items[0].id) {",
            f"  pm.collectionVariables.set('{resource['list_capture']}', items[0].id);",
            "}",
        ]
    return lines


def build_request(doc, resource, route, method, operation, path_item, index):
    path_key = f"{method.upper()} {route}"
    status = expected_status(operation, path_key)
    summary = operation.get("summary", route)
    description = operation.get("description", "").strip()

    is_list = method == "get" and "{" not in route and summary.lower().startswith("lister")
    is_create = (
        method == "post"
        and "{" not in route
        and not route.endswith("/bulk")
        and not route.endswith("/batch")
        and not route.endswith("/resolve")
    )

    request = OrderedDict()
    request["method"] = method.upper()
    request["header"] = []

    body = None
    content = operation.get("requestBody", {}).get("content", {}) or {}
    for media_type, media in content.items():
        if media_type.startswith("application/json"):
            body = build_body(doc, resource["key"], media.get("schema", {}), method, path_key)
        break

    if body is not None:
        request["header"].append({"key": "Content-Type", "value": "application/json"})
        request["body"] = {
            "mode": "raw",
            "raw": json.dumps(body, ensure_ascii=False, indent=2),
            "options": {"raw": {"language": "json"}},
        }

    request["url"] = build_url_object(
        resource, route, method, build_query(doc, resource, operation, path_item)
    )

    if path_key in resource.get("public", set()):
        request["auth"] = {"type": "noauth"}

    item = OrderedDict()
    item["name"] = f"{index}. {summary}"
    item["event"] = [
        {
            "listen": "test",
            "script": {
                "type": "text/javascript",
                "exec": build_tests(resource, method, route, status, is_list, is_create),
            },
        }
    ]
    item["request"] = request

    doc_lines = [f"Route : {method.upper()} {route}", f"Statut attendu : {status}"]
    if description:
        doc_lines.append("")
        doc_lines.append(description)
    item["description"] = "\n".join(doc_lines)
    return item


def build_extra_request(extra):
    request = OrderedDict()
    request["method"] = extra["method"]
    request["header"] = []
    if extra.get("body") is not None:
        request["header"].append({"key": "Content-Type", "value": "application/json"})
        request["body"] = {
            "mode": "raw",
            "raw": json.dumps(extra["body"], ensure_ascii=False, indent=2),
            "options": {"raw": {"language": "json"}},
        }
    request["url"] = {
        "raw": "{{baseUrl}}" + extra["route"],
        "host": ["{{baseUrl}}"],
        "path": [segment for segment in extra["route"].split("/") if segment],
        "query": [],
    }
    if extra.get("noauth"):
        request["auth"] = {"type": "noauth"}

    item = OrderedDict()
    item["name"] = extra["name"]
    item["event"] = [
        {
            "listen": "test",
            "script": {
                "type": "text/javascript",
                "exec": [
                    f"pm.test('{status_label(extra['expected'])}', () => pm.response.to.have.status({extra['expected']}));"
                ],
            },
        }
    ]
    item["request"] = request
    item["description"] = f"Route : {extra['method']} {extra['route']}\n\n{extra['description']}"
    return item


def build_collection():
    modules = OrderedDict()

    for module_name, resources in MODULES:
        folders = OrderedDict()
        for resource in resources:
            spec_path = os.path.join(API_DIR, resource["spec"])
            doc = yaml.load(open(spec_path, encoding="utf-8"))
            subfolder = resource["subfolder"] or module_name
            items = folders.setdefault(subfolder, [])

            index = 0
            operations = []
            for route, path_item in doc.get("paths", {}).items():
                for method, operation in path_item.items():
                    if method not in ("get", "post", "put", "patch", "delete"):
                        continue
                    operations.append((route, method, operation, path_item))

            order = ROUTE_ORDER.get(resource["key"])
            if order:
                def sort_key(entry):
                    route, method = entry[0], entry[1]
                    key = f"{method.upper()} {route}"
                    if key in order:
                        return (0, order.index(key))
                    return (1, 0)

                operations.sort(key=sort_key)

            for route, method, operation, path_item in operations:
                index += 1
                items.append(build_request(doc, resource, route, method, operation, path_item, index))
            resource["_count"] = index

        modules[module_name] = folders

    # Dossier « 07 — Système » créé à la volée pour les routes hors spécification.
    for extra in EXTRA_REQUESTS:
        modules.setdefault(extra["module"], OrderedDict()).setdefault(extra["subfolder"], []).append(
            build_extra_request(extra)
        )

    collection = OrderedDict()
    collection["info"] = OrderedDict(
        [
            ("_postman_id", "d0e00000-0000-4000-8000-0000000000c2"),
            (
                "name",
                "SSSM — API complète (Auth, Organisation, Personnel, Clients, Sites, Inventaire)",
            ),
            (
                "description",
                "Collection couvrant **toutes** les routes HTTP exposées par le backend SSSM "
                "(API v1), organisée par module puis par ressource.\n\n"
                "Utilisation :\n"
                "1. sélectionner l'environnement « SSSM — API (local) » ;\n"
                "2. exécuter le dossier « 00 — Authentification » (le Bearer est capturé "
                "automatiquement) ;\n"
                "3. exécuter les autres dossiers dans l'ordre.\n\n"
                "Les identifiants de ressources sont des variables de collection : les scripts "
                "de test les alimentent à partir des listes et des créations. L'environnement ne "
                "contient que la connexion (baseUrl, login, password).\n\n"
                "Les requêtes destructrices (PATCH, DELETE) visent l'identifiant de la "
                "ressource créée dans le même dossier et jamais celui des données de "
                "démonstration. Les affectations et les mouvements visent les fixtures "
                "dédiées `DEMO-POSTMAN`, réinitialisées par "
                "`scripts/demo/reset-postman-demo.ps1`.",
            ),
            ("schema", "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"),
        ]
    )
    collection["auth"] = {
        "type": "bearer",
        "bearer": [{"key": "token", "value": "{{accessToken}}", "type": "string"}],
    }

    items = []
    for module_name, folders in modules.items():
        module_description = f"Module {module_name}."
        subfolders = []
        for subfolder, folder_items in folders.items():
            subfolders.append(
                OrderedDict(
                    [
                        ("name", subfolder),
                        ("description", module_description),
                        ("item", folder_items),
                    ]
                )
            )
        items.append(OrderedDict([("name", module_name), ("item", subfolders)]))

    collection["item"] = items
    collection["variable"] = [
        {"key": key, "value": value, "type": "string"} for key, value in COLLECTION_VARIABLES.items()
    ]
    return collection


def build_environment(name, base_url, login, password, environment_id):
    return OrderedDict(
        [
            ("id", environment_id),
            ("name", name),
            (
                "values",
                [
                    {"key": "baseUrl", "value": base_url, "type": "default", "enabled": True},
                    {"key": "login", "value": login, "type": "default", "enabled": True},
                    {"key": "password", "value": password, "type": "secret", "enabled": True},
                ],
            ),
            ("_postman_variable_scope", "environment"),
            ("_postman_exported_at", "2026-11-10T00:00:00.000Z"),
            ("_postman_exported_using", "OpenCode"),
        ]
    )


def write_json(path, data):
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(data, handle, ensure_ascii=False, indent=2)
        handle.write("\n")
    print(f"écrit : {os.path.relpath(path, ROOT)}")


def main():
    collection = build_collection()
    write_json(
        os.path.join(OUT_DIR, "SSSM-API.postman_collection.json"),
        collection,
    )
    write_json(
        os.path.join(OUT_DIR, "SSSM-API.postman_environment.json"),
        build_environment(
            "SSSM — API (local)",
            "http://localhost:8080",
            "demo.admin",
            "Demo@2024",
            "d0e00000-0000-4000-8000-0000000000e2",
        ),
    )
    write_json(
        os.path.join(OUT_DIR, "SSSM-API.postman_environment.recette.json"),
        build_environment(
            "SSSM — API (recette)",
            "https://api.sssm.example",
            "",
            "",
            "d0e00000-0000-4000-8000-0000000000e3",
        ),
    )


if __name__ == "__main__":
    main()
