-- =====================================================================
-- V2: Seed the default RBAC catalogue.
-- Two system roles are created: USER (assigned to every new account) and
-- ADMIN (full access). Permissions follow the `resource:action` convention
-- enforced by the admin routes. ADMIN additionally bypasses individual
-- permission checks in code, but is granted every permission explicitly so
-- the catalogue remains the single source of truth.
-- =====================================================================

-- --- Roles -----------------------------------------------------------
INSERT INTO roles (id, name, description, is_system) VALUES
    (gen_random_uuid(), 'USER',  'Default role granted to every registered account.', TRUE),
    (gen_random_uuid(), 'ADMIN', 'Full administrative access to the identity service.', TRUE);

-- --- Permissions -----------------------------------------------------
INSERT INTO permissions (id, name, description) VALUES
    (gen_random_uuid(), 'users:read',   'View user accounts and details.'),
    (gen_random_uuid(), 'users:write',  'Suspend or reactivate user accounts.'),
    (gen_random_uuid(), 'users:delete', 'Soft-delete user accounts.'),
    (gen_random_uuid(), 'roles:read',   'List roles.'),
    (gen_random_uuid(), 'roles:assign', 'Assign or revoke roles on users.'),
    (gen_random_uuid(), 'audit:read',   'Read the security audit log.');

-- --- Grant every permission to ADMIN ---------------------------------
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN';
