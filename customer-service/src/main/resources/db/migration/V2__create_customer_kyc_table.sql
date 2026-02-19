CREATE TABLE IF NOT EXISTS customer_kyc (
    id                  BIGSERIAL    PRIMARY KEY,
    customer_id         BIGINT       NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    document_type       VARCHAR(50),
    document_number     VARCHAR(100),
    issue_date          DATE,
    expiry_date         DATE,
    issuing_country     VARCHAR(100),
    issuing_authority   VARCHAR(200),
    verification_status VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    verified_at         TIMESTAMPTZ,
    verified_by         VARCHAR(100),
    rejection_reason    TEXT,
    notes               TEXT,
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);
