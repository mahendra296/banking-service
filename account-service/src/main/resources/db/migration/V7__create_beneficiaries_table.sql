CREATE TABLE IF NOT EXISTS beneficiaries (
    id               BIGSERIAL    PRIMARY KEY,
    customer_id      BIGINT       NOT NULL REFERENCES customers(id),
    beneficiary_name VARCHAR(150) NOT NULL,
    account_number   VARCHAR(20)  NOT NULL,
    bank_name        VARCHAR(150) NOT NULL DEFAULT 'SAME_BANK',
    ifsc_code        VARCHAR(20),
    is_verified      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE(customer_id, account_number)
);
