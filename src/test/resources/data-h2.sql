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

-- Seed a test user for rule-related tests
MERGE INTO "user" (name, email, password_hash, subscription_plan, enabled, created_at, updated_at)
KEY (email)
VALUES ('Test User', 'test@investalert.com', '$2a$10$dummyhashfortest000000000000000000000000000000000000000', 'FREE', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Seed FII assets for integration tests
MERGE INTO asset (ticker, name, asset_type, updated_at)
KEY (ticker)
VALUES ('XPLG11', 'XP Log FII', 'FII', CURRENT_TIMESTAMP);

MERGE INTO asset (ticker, name, asset_type, updated_at)
KEY (ticker)
VALUES ('HGLG11', 'CSHG Logistica FII', 'FII', CURRENT_TIMESTAMP);

-- Seed indicator values for the FII assets
MERGE INTO asset_indicator_value (asset_id, indicator_type, value)
KEY (asset_id, indicator_type)
SELECT id, 'PRICE', 110.00 FROM asset WHERE ticker = 'XPLG11';

MERGE INTO asset_indicator_value (asset_id, indicator_type, value)
KEY (asset_id, indicator_type)
SELECT id, 'DIVIDEND_YIELD', 8.50 FROM asset WHERE ticker = 'XPLG11';

MERGE INTO asset_indicator_value (asset_id, indicator_type, value)
KEY (asset_id, indicator_type)
SELECT id, 'PVP', 0.95 FROM asset WHERE ticker = 'XPLG11';

MERGE INTO asset_indicator_value (asset_id, indicator_type, value)
KEY (asset_id, indicator_type)
SELECT id, 'PRICE', 170.50 FROM asset WHERE ticker = 'HGLG11';

MERGE INTO asset_indicator_value (asset_id, indicator_type, value)
KEY (asset_id, indicator_type)
SELECT id, 'DIVIDEND_YIELD', 9.20 FROM asset WHERE ticker = 'HGLG11';

MERGE INTO asset_indicator_value (asset_id, indicator_type, value)
KEY (asset_id, indicator_type)
SELECT id, 'PVP', 1.10 FROM asset WHERE ticker = 'HGLG11';

-- Seed pre-existing rules to verify indicator_type preservation after migration
-- These simulate rules that existed before the V4 migration renamed 'indicator_code' to 'indicator_type'
MERGE INTO rule (user_id, ticker, group_id, indicator_type, operator, target_value, active, created_at, updated_at)
KEY (user_id, ticker, indicator_type, operator, target_value)
SELECT u.id, 'XPLG11', NULL, 'PRICE', 'LESS_THAN', 100.00, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM "user" u WHERE u.email = 'test@investalert.com';

MERGE INTO rule (user_id, ticker, group_id, indicator_type, operator, target_value, active, created_at, updated_at)
KEY (user_id, ticker, indicator_type, operator, target_value)
SELECT u.id, 'XPLG11', NULL, 'DIVIDEND_YIELD', 'GREATER_THAN', 7.00, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM "user" u WHERE u.email = 'test@investalert.com';

MERGE INTO rule (user_id, ticker, group_id, indicator_type, operator, target_value, active, created_at, updated_at)
KEY (user_id, ticker, indicator_type, operator, target_value)
SELECT u.id, 'HGLG11', NULL, 'PVP', 'LESS_THAN', 1.20, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM "user" u WHERE u.email = 'test@investalert.com';
