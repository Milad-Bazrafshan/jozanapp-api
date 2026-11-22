ALTER TABLE school.assessment
    ADD COLUMN IF NOT EXISTS price_rial BIGINT NOT NULL DEFAULT 0;

ALTER TABLE school.assessment_participant
    ADD COLUMN IF NOT EXISTS fee_paid_at TIMESTAMPTZ;

ALTER TABLE school.assessment
    ADD CONSTRAINT ck_assessment_price_rial CHECK (price_rial >= 0);