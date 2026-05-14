-- Test seed data for H2 in-memory database
-- Uses MERGE INTO to be idempotent across multiple context loads

MERGE INTO role (name) KEY (name) VALUES ('ROLE_ADMIN');
MERGE INTO role (name) KEY (name) VALUES ('ROLE_USER');

MERGE INTO permission (name) KEY (name) VALUES ('ALERT_CREATE');
MERGE INTO permission (name) KEY (name) VALUES ('ALERT_UPDATE');
MERGE INTO permission (name) KEY (name) VALUES ('ALERT_DELETE');
MERGE INTO permission (name) KEY (name) VALUES ('USER_MANAGE');
MERGE INTO permission (name) KEY (name) VALUES ('SYSTEM_CONFIG');

-- ROLE_ADMIN gets all permissions
MERGE INTO role_permissions (role_id, permission_id)
KEY (role_id, permission_id)
SELECT r.id, p.id FROM role r JOIN permission p
ON p.name IN ('ALERT_CREATE', 'ALERT_UPDATE', 'ALERT_DELETE', 'USER_MANAGE', 'SYSTEM_CONFIG')
WHERE r.name = 'ROLE_ADMIN';

-- ROLE_USER gets alert permissions
MERGE INTO role_permissions (role_id, permission_id)
KEY (role_id, permission_id)
SELECT r.id, p.id FROM role r JOIN permission p
ON p.name IN ('ALERT_CREATE', 'ALERT_UPDATE', 'ALERT_DELETE')
WHERE r.name = 'ROLE_USER';
