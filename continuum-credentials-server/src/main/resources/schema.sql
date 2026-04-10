-- credential_types must be created before credentials (FK dependency)
CREATE TABLE IF NOT EXISTS credential_types (
    credential_type_id UUID         PRIMARY KEY,
    type               VARCHAR(50)  NOT NULL UNIQUE,
    "schema"           JSONB        NOT NULL DEFAULT '{}',
    ui_schema          JSONB        NOT NULL DEFAULT '{}',
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    entity_version     BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS credentials (
    credential_id    UUID PRIMARY KEY,
    user_id          VARCHAR(255)  NOT NULL,
    name             VARCHAR(255)  NOT NULL,
    type             VARCHAR(50)   NOT NULL REFERENCES credential_types(type),
    data             JSONB         NOT NULL DEFAULT '{}',
    description      VARCHAR(1000),
    created_by       VARCHAR(255)  NOT NULL,
    updated_by       VARCHAR(255)  NOT NULL,
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_accessed_at TIMESTAMP,
    entity_version   BIGINT        NOT NULL DEFAULT 0
);

-- Unique constraint: credential name must be unique per user
CREATE UNIQUE INDEX IF NOT EXISTS idx_credentials_user_name
    ON credentials (user_id, name);

-- Index for listing credentials by user
CREATE INDEX IF NOT EXISTS idx_credentials_user_id
    ON credentials (user_id);

-- Index for filtering credentials by user and type
CREATE INDEX IF NOT EXISTS idx_credentials_user_type
    ON credentials (user_id, type);
