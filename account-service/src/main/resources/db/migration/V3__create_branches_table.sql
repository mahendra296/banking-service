CREATE TABLE IF NOT EXISTS branches (
    id          BIGSERIAL    PRIMARY KEY,
    branch_code VARCHAR(10)  NOT NULL UNIQUE,
    branch_name VARCHAR(150) NOT NULL,
    city        VARCHAR(100) NOT NULL,
    state       VARCHAR(100),
    phone       VARCHAR(20),
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
