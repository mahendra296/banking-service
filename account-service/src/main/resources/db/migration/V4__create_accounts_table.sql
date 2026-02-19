CREATE SEQUENCE IF NOT EXISTS account_number_seq START 1000000001 INCREMENT 1;

CREATE TABLE IF NOT EXISTS accounts (
    id              BIGSERIAL     PRIMARY KEY,
    account_number  VARCHAR(20)   NOT NULL UNIQUE
                    DEFAULT ('ACC-' || NEXTVAL('account_number_seq')::TEXT),
    customer_id     BIGINT        NOT NULL REFERENCES customers(id),
    branch_id       BIGINT        NOT NULL REFERENCES branches(id),
    account_type    VARCHAR(20)   NOT NULL
                    CHECK (account_type IN ('SAVINGS', 'CURRENT', 'FIXED_DEPOSIT')),
    balance         DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    currency        VARCHAR(3)    NOT NULL DEFAULT 'USD',
    interest_rate   DECIMAL(5,2)  NOT NULL DEFAULT 0.00,
    min_balance     DECIMAL(15,2) NOT NULL DEFAULT 500.00,
    overdraft_limit DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    status          VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE'
                    CHECK (status IN ('ACTIVE', 'DORMANT', 'FROZEN', 'CLOSED')),
    closed_at       TIMESTAMPTZ,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
