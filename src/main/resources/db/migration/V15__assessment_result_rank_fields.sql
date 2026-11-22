-- Denormalized ranking helpers for student-visible assessment results (computed on submit / grade / publish)
ALTER TABLE school.assessment_result
    ADD COLUMN IF NOT EXISTS balance DECIMAL(10, 2) NULL,
    ADD COLUMN IF NOT EXISTS rank_in_assessment INT NULL,
    ADD COLUMN IF NOT EXISTS participant_count INT NULL;
