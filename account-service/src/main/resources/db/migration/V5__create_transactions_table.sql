CREATE SEQUENCE IF NOT EXISTS transaction_ref_seq START 1 INCREMENT 1;

CREATE TABLE IF NOT EXISTS transactions (
    id               BIGSERIAL     PRIMARY KEY,
    transaction_ref  VARCHAR(30)   NOT NULL UNIQUE
                     DEFAULT ('TXN-' || TO_CHAR(NOW(), 'YYYYMMDD') || '-' || LPAD(NEXTVAL('transaction_ref_seq')::TEXT, 8, '0')),
    account_id       BIGINT        NOT NULL REFERENCES accounts(id),
    transaction_type VARCHAR(20)   NOT NULL
                     CHECK (transaction_type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER_IN', 'TRANSFER_OUT', 'INTEREST', 'FEE', 'REVERSAL')),
    amount           DECIMAL(15,2) NOT NULL CHECK (amount > 0),
    balance_before   DECIMAL(15,2) NOT NULL,
    balance_after    DECIMAL(15,2) NOT NULL,
    description      TEXT,
    related_txn_id   BIGINT        REFERENCES transactions(id),
    performed_by     BIGINT,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
