CREATE SEQUENCE IF NOT EXISTS transfer_ref_seq START 1 INCREMENT 1;

CREATE TABLE IF NOT EXISTS transfers (
    id              BIGSERIAL     PRIMARY KEY,
    transfer_ref    VARCHAR(30)   NOT NULL UNIQUE
                    DEFAULT ('TRF-' || TO_CHAR(NOW(), 'YYYYMMDD') || '-' || LPAD(NEXTVAL('transfer_ref_seq')::TEXT, 8, '0')),
    from_account_id BIGINT        NOT NULL REFERENCES accounts(id),
    to_account_id   BIGINT        NOT NULL REFERENCES accounts(id),
    amount          DECIMAL(15,2) NOT NULL CHECK (amount > 0),
    fee             DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    status          VARCHAR(20)   NOT NULL DEFAULT 'COMPLETED'
                    CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'REVERSED')),
    description     TEXT,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_diff_accounts CHECK (from_account_id != to_account_id)
);
