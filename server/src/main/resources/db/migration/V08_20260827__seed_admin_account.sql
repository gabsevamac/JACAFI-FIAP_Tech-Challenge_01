INSERT INTO user_accounts (
    id, username, password_hash, customer_id, active,
    created_at, created_by, updated_at, updated_by,
    deleted_at, deleted_by, version
) VALUES (
    '00000000-0000-0000-0000-000000000001',
    'dev-admin',
    '$2a$10$HCE3ASgilvY3mMMHLobV4uwo9zpBkTH7NfdHTGIZgDtQcK3SMnVhy',
    NULL,
    TRUE,
    CURRENT_TIMESTAMP,
    'flyway',
    CURRENT_TIMESTAMP,
    'flyway',
    NULL,
    NULL,
    0
);

INSERT INTO user_account_roles (user_account_id, role)
VALUES ('00000000-0000-0000-0000-000000000001', 'ADMIN');
