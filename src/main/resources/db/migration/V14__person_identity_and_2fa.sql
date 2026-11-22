ALTER TABLE school.person
    ADD COLUMN IF NOT EXISTS date_of_birth DATE,
    ADD COLUMN IF NOT EXISTS fathers_name VARCHAR(120),
    ADD COLUMN IF NOT EXISTS place_of_birth VARCHAR(200);

ALTER TABLE school.user_account
    ADD COLUMN IF NOT EXISTS two_factor_sms_enabled BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE school.two_factor_sms_challenge (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES school.user_account(id),
    purpose VARCHAR(30) NOT NULL,
    code_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX ix_two_factor_sms_challenge_account ON school.two_factor_sms_challenge(account_id)
    WHERE consumed = FALSE;
