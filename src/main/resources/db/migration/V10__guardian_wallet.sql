CREATE TABLE guardian_wallet (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    guardian_id UUID NOT NULL,
    balance NUMERIC(14, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_wallet_balance CHECK (balance >= 0),
    CONSTRAINT uq_wallet_guardian UNIQUE (tenant_id, guardian_id),
    CONSTRAINT uq_wallet_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_wallet_guardian_tenant FOREIGN KEY (tenant_id, guardian_id) REFERENCES guardian_profile(tenant_id, id)
);

CREATE TABLE guardian_wallet_txn (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    wallet_id UUID NOT NULL,
    student_id UUID,
    payment_id UUID,
    txn_type VARCHAR(30) NOT NULL,
    amount NUMERIC(14, 2) NOT NULL,
    balance_after NUMERIC(14, 2) NOT NULL,
    notes VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_wallet_txn_amount CHECK (amount > 0),
    CONSTRAINT uq_wallet_txn_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_wallet_txn_wallet_tenant FOREIGN KEY (tenant_id, wallet_id) REFERENCES guardian_wallet(tenant_id, id),
    CONSTRAINT fk_wallet_txn_student_tenant FOREIGN KEY (tenant_id, student_id) REFERENCES student_profile(tenant_id, id),
    CONSTRAINT fk_wallet_txn_payment_tenant FOREIGN KEY (tenant_id, payment_id) REFERENCES payment(tenant_id, id)
);

CREATE INDEX ix_wallet_txn_wallet ON guardian_wallet_txn (wallet_id, created_at DESC);
