CREATE SEQUENCE IF NOT EXISTS customer_code_seq START 1 INCREMENT 1;

CREATE TABLE IF NOT EXISTS customers (
    id            BIGSERIAL    PRIMARY KEY,
    customer_code VARCHAR(20)  NOT NULL UNIQUE DEFAULT ('CUST-' || LPAD(NEXTVAL('customer_code_seq')::TEXT, 5, '0')),
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    phone         VARCHAR(20),
    date_of_birth DATE,
    address       TEXT,
    id_type       VARCHAR(20),
    id_number     VARCHAR(100) UNIQUE,
    kyc_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    branch_id     INTEGER,
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);
