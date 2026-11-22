ALTER TABLE chat_room
    ADD COLUMN IF NOT EXISTS room_type VARCHAR(40) NOT NULL DEFAULT 'INSTRUCTOR_GUARDIAN',
    ADD COLUMN IF NOT EXISTS counselor_membership_id UUID REFERENCES tenant_membership(id),
    ADD COLUMN IF NOT EXISTS student_membership_id UUID REFERENCES tenant_membership(id);

ALTER TABLE chat_room ALTER COLUMN guardian_membership_id DROP NOT NULL;
ALTER TABLE chat_room ALTER COLUMN instructor_membership_id DROP NOT NULL;
ALTER TABLE chat_room ALTER COLUMN class_section_id DROP NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_chat_room_counselor
    ON chat_room (tenant_id, student_id, counselor_membership_id)
    WHERE room_type = 'COUNSELOR_STUDENT';
