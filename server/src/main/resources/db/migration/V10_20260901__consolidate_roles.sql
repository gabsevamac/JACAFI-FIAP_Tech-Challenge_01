ALTER TABLE user_account_roles
    DROP CONSTRAINT ck_user_account_roles_role;

INSERT INTO user_account_roles (user_account_id, role)
SELECT DISTINCT user_account_id, 'EMPLOYEE'
FROM user_account_roles
WHERE role <> 'CUSTOMER'
ON CONFLICT (user_account_id, role) DO NOTHING;

DELETE FROM user_account_roles
WHERE role NOT IN ('EMPLOYEE', 'CUSTOMER');

ALTER TABLE user_account_roles
    ADD CONSTRAINT ck_user_account_roles_role
        CHECK (role IN ('EMPLOYEE', 'CUSTOMER'));
