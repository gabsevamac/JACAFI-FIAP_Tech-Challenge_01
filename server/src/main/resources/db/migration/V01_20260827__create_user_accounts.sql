CREATE TABLE user_accounts (
    id UUID PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    password_hash VARCHAR(60) NOT NULL,
    customer_id UUID,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    deleted_at TIMESTAMPTZ,
    deleted_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_user_accounts_password_hash_bcrypt
        CHECK (password_hash ~ '^\$2[aby]\$[0-9]{2}\$.{53}$'),
    CONSTRAINT ck_user_accounts_deletion_audit
        CHECK ((deleted_at IS NULL) = (deleted_by IS NULL))
);

CREATE UNIQUE INDEX uk_user_accounts_username ON user_accounts (username);

CREATE TABLE user_account_roles (
    user_account_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_account_id, role),
    CONSTRAINT fk_user_account_roles_account
        FOREIGN KEY (user_account_id) REFERENCES user_accounts (id) ON DELETE CASCADE,
    CONSTRAINT ck_user_account_roles_role
        CHECK (role IN ('ADMIN', 'MANAGER', 'SERVICE_ADVISOR', 'TECHNICIAN', 'CUSTOMER'))
);
