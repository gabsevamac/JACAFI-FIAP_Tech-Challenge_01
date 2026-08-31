INSERT INTO user_accounts (
    id, username, password_hash, customer_id, active,
    created_at, created_by, updated_at, updated_by,
    deleted_at, deleted_by, version
) VALUES (
    '00000000-0000-0000-0000-000000000001',
    'dev-admin',
    '$2a$10$810zYJLvPc510qxCp/2AU.Bo/B3TBqHWaLVDICLQp1E/r7c/2xEIW',
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
