-- H2-compatible schema for integration tests

CREATE TABLE IF NOT EXISTS "user" (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    name              VARCHAR(255) NOT NULL,
    email             VARCHAR(255) NOT NULL,
    password_hash     VARCHAR(255) NOT NULL,
    subscription_plan VARCHAR(20)  NOT NULL DEFAULT 'FREE',
    enabled           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP(6) NOT NULL,
    updated_at        TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS asset (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    ticker          VARCHAR(20)    NOT NULL,
    name            VARCHAR(255)   NOT NULL,
    current_price   DECIMAL(19,4)  NOT NULL,
    dividend_yield  DECIMAL(19,4)  NOT NULL,
    p_vp            DECIMAL(19,4)  NOT NULL,
    updated_at      TIMESTAMP(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_asset_ticker UNIQUE (ticker)
);

CREATE TABLE IF NOT EXISTS rule_group (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    ticker      VARCHAR(20)  NOT NULL,
    name        VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_rule_group_user FOREIGN KEY (user_id) REFERENCES "user" (id)
);

CREATE TABLE IF NOT EXISTS rule (
    id            BIGINT         NOT NULL AUTO_INCREMENT,
    user_id       BIGINT         NOT NULL,
    ticker        VARCHAR(20)    NOT NULL,
    group_id      BIGINT         NULL,
    field         VARCHAR(30)    NOT NULL,
    operator      VARCHAR(30)    NOT NULL,
    target_value  DECIMAL(19,4)  NOT NULL,
    active        BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP(6)   NOT NULL,
    updated_at    TIMESTAMP(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_rule_user  FOREIGN KEY (user_id)  REFERENCES "user" (id),
    CONSTRAINT fk_rule_asset FOREIGN KEY (ticker)    REFERENCES asset (ticker),
    CONSTRAINT fk_rule_group FOREIGN KEY (group_id)  REFERENCES rule_group (id)
);

CREATE TABLE IF NOT EXISTS alert (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    rule_id     BIGINT       NULL,
    group_id    BIGINT       NULL,
    ticker      VARCHAR(20)  NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    details     TEXT         NULL,
    created_at  TIMESTAMP(6) NOT NULL,
    sent_at     TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_alert_user  FOREIGN KEY (user_id)  REFERENCES "user" (id),
    CONSTRAINT fk_alert_rule  FOREIGN KEY (rule_id)   REFERENCES rule (id),
    CONSTRAINT fk_alert_group FOREIGN KEY (group_id)  REFERENCES rule_group (id)
);

CREATE TABLE IF NOT EXISTS role (
    id   BIGINT      NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_role_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS permission (
    id   BIGINT       NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_permission_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES "user" (id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES role (id)
);

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role       FOREIGN KEY (role_id)       REFERENCES role (id),
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permission (id)
);

CREATE TABLE IF NOT EXISTS refresh_token (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    token      VARCHAR(512) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_token_token UNIQUE (token)
);
