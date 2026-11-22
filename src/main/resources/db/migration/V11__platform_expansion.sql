ALTER TABLE tenant
    ADD COLUMN IF NOT EXISTS course_selection_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS virtual_class_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS sms_provider VARCHAR(40) NOT NULL DEFAULT 'DEMO',
    ADD COLUMN IF NOT EXISTS meeting_provider VARCHAR(40) NOT NULL DEFAULT 'GENERIC_LINK',
    ADD COLUMN IF NOT EXISTS meeting_base_url VARCHAR(400),
    ADD COLUMN IF NOT EXISTS meeting_secret VARCHAR(400);

ALTER TABLE class_session ADD COLUMN IF NOT EXISTS meeting_id VARCHAR(120);

ALTER TABLE payment
    ADD COLUMN IF NOT EXISTS refunded_payment_id UUID REFERENCES payment(id),
    ADD COLUMN IF NOT EXISTS refund_reason VARCHAR(500);

CREATE TABLE stored_file (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    original_name VARCHAR(240) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    size_bytes BIGINT NOT NULL,
    storage_key VARCHAR(400) NOT NULL,
    owner_membership_id UUID,
    purpose VARCHAR(60) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_stored_file_tenant_id UNIQUE (tenant_id, id)
);

CREATE TABLE outbox_event (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    event_type VARCHAR(80) NOT NULL,
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id UUID NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(500),
    occurred_at TIMESTAMPTZ NOT NULL,
    processed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX ix_outbox_status ON outbox_event (status, created_at);

CREATE TABLE tenant_feature (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    code VARCHAR(80) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    settings TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_tenant_feature UNIQUE (tenant_id, code)
);

CREATE TABLE announcement (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    title VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    audience_type VARCHAR(40) NOT NULL,
    audience_ref UUID,
    channel VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    scheduled_at TIMESTAMPTZ,
    sent_at TIMESTAMPTZ,
    created_by UUID REFERENCES user_account(id),
    class_section_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_announcement_tenant_id UNIQUE (tenant_id, id)
);

CREATE TABLE in_app_notification (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    membership_id UUID NOT NULL,
    announcement_id UUID REFERENCES announcement(id),
    title VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_notification_tenant_id UNIQUE (tenant_id, id)
);
CREATE INDEX ix_notification_membership ON in_app_notification (tenant_id, membership_id, created_at DESC);

CREATE TABLE sms_delivery (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    announcement_id UUID REFERENCES announcement(id),
    membership_id UUID,
    phone VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    provider_message_id VARCHAR(120),
    error_message VARCHAR(500),
    sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE report_card_run (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    academic_period_id UUID NOT NULL,
    class_section_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    status VARCHAR(30) NOT NULL,
    include_rank BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMPTZ,
    created_by UUID REFERENCES user_account(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_report_card_run_tenant_id UNIQUE (tenant_id, id)
);

CREATE TABLE report_card (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    run_id UUID NOT NULL,
    student_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    attendance_percent NUMERIC(6, 2),
    absence_count INTEGER,
    continuous_score NUMERIC(8, 2),
    rank_in_class INTEGER,
    overall_score NUMERIC(8, 2),
    verification_token VARCHAR(80) NOT NULL,
    correction_reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_report_card_run_student UNIQUE (run_id, student_id),
    CONSTRAINT uq_report_card_token UNIQUE (verification_token),
    CONSTRAINT uq_report_card_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_report_card_run FOREIGN KEY (run_id) REFERENCES report_card_run(id)
);

CREATE TABLE report_card_line (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    report_card_id UUID NOT NULL REFERENCES report_card(id) ON DELETE CASCADE,
    line_type VARCHAR(40) NOT NULL,
    title VARCHAR(200) NOT NULL,
    score NUMERIC(8, 2),
    max_score NUMERIC(8, 2),
    note VARCHAR(400),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE report_card_history (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    report_card_id UUID NOT NULL REFERENCES report_card(id),
    previous_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    reason VARCHAR(500),
    changed_by UUID REFERENCES user_account(id),
    changed_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE ledger_account (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    parent_id UUID,
    code VARCHAR(40) NOT NULL,
    name VARCHAR(200) NOT NULL,
    account_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_ledger_account_code UNIQUE (tenant_id, code),
    CONSTRAINT uq_ledger_account_tenant_id UNIQUE (tenant_id, id)
);

CREATE TABLE voucher_type (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    code VARCHAR(40) NOT NULL,
    name VARCHAR(120) NOT NULL,
    auto_post BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_voucher_type_code UNIQUE (tenant_id, code)
);

CREATE TABLE fiscal_period (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    name VARCHAR(120) NOT NULL,
    starts_on DATE NOT NULL,
    ends_on DATE NOT NULL,
    locked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE journal_voucher (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    voucher_type_id UUID NOT NULL REFERENCES voucher_type(id),
    code VARCHAR(60) NOT NULL,
    voucher_date DATE NOT NULL,
    description VARCHAR(500),
    status VARCHAR(30) NOT NULL,
    source_type VARCHAR(40),
    source_id UUID,
    posted_at TIMESTAMPTZ,
    created_by UUID REFERENCES user_account(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_journal_voucher_code UNIQUE (tenant_id, code),
    CONSTRAINT uq_journal_voucher_tenant_id UNIQUE (tenant_id, id)
);

CREATE TABLE journal_voucher_line (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    voucher_id UUID NOT NULL REFERENCES journal_voucher(id) ON DELETE CASCADE,
    account_id UUID NOT NULL REFERENCES ledger_account(id),
    line_no INTEGER NOT NULL,
    description VARCHAR(400),
    debit NUMERIC(14, 2) NOT NULL DEFAULT 0,
    credit NUMERIC(14, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_voucher_line_dc CHECK ((debit > 0 AND credit = 0) OR (credit > 0 AND debit = 0)),
    CONSTRAINT uq_voucher_line UNIQUE (voucher_id, line_no)
);

CREATE TABLE process_definition (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    code VARCHAR(60) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    starter_role VARCHAR(40) NOT NULL,
    form_schema TEXT,
    output_kind VARCHAR(40) NOT NULL DEFAULT 'NONE',
    output_title VARCHAR(200),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_process_def_code UNIQUE (tenant_id, code)
);

CREATE TABLE process_step (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    definition_id UUID NOT NULL REFERENCES process_definition(id) ON DELETE CASCADE,
    sequence_number INTEGER NOT NULL,
    name VARCHAR(160) NOT NULL,
    approver_role VARCHAR(40) NOT NULL,
    sla_hours INTEGER,
    allow_reject BOOLEAN NOT NULL DEFAULT TRUE,
    allow_return BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_process_step UNIQUE (definition_id, sequence_number)
);

CREATE TABLE process_instance (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    definition_id UUID NOT NULL REFERENCES process_definition(id),
    starter_membership_id UUID NOT NULL,
    student_id UUID,
    status VARCHAR(30) NOT NULL,
    form_data TEXT,
    current_step INTEGER NOT NULL DEFAULT 1,
    certificate_body TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_process_instance_tenant_id UNIQUE (tenant_id, id)
);

CREATE TABLE process_task (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    instance_id UUID NOT NULL REFERENCES process_instance(id) ON DELETE CASCADE,
    step_id UUID NOT NULL REFERENCES process_step(id),
    status VARCHAR(30) NOT NULL,
    comment VARCHAR(500),
    acted_by UUID REFERENCES user_account(id),
    due_at TIMESTAMPTZ,
    acted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE process_history (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    instance_id UUID NOT NULL REFERENCES process_instance(id),
    action VARCHAR(40) NOT NULL,
    comment VARCHAR(500),
    acted_by UUID REFERENCES user_account(id),
    acted_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE chat_room (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    student_id UUID NOT NULL,
    guardian_membership_id UUID NOT NULL,
    instructor_membership_id UUID NOT NULL,
    class_section_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_chat_room UNIQUE (tenant_id, guardian_membership_id, instructor_membership_id, student_id),
    CONSTRAINT uq_chat_room_tenant_id UNIQUE (tenant_id, id)
);

CREATE TABLE chat_message (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    room_id UUID NOT NULL REFERENCES chat_room(id) ON DELETE CASCADE,
    sender_membership_id UUID NOT NULL,
    body TEXT NOT NULL,
    file_id UUID REFERENCES stored_file(id),
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX ix_chat_message_room ON chat_message (room_id, created_at);

CREATE TABLE survey (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    instructor_id UUID NOT NULL,
    class_section_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    audience VARCHAR(40) NOT NULL,
    anonymous BOOLEAN NOT NULL DEFAULT TRUE,
    opens_at TIMESTAMPTZ NOT NULL,
    closes_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_survey_tenant_id UNIQUE (tenant_id, id)
);

CREATE TABLE survey_question (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    survey_id UUID NOT NULL REFERENCES survey(id) ON DELETE CASCADE,
    prompt VARCHAR(400) NOT NULL,
    sequence_number INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE survey_response (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    survey_id UUID NOT NULL REFERENCES survey(id) ON DELETE CASCADE,
    enrollment_id UUID NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_survey_response UNIQUE (survey_id, enrollment_id)
);

CREATE TABLE survey_answer (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    response_id UUID NOT NULL REFERENCES survey_response(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES survey_question(id),
    score INTEGER NOT NULL,
    comment VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE student_card (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    student_id UUID NOT NULL,
    class_section_id UUID,
    valid_until DATE NOT NULL,
    verification_token VARCHAR(80) NOT NULL,
    photo_file_id UUID REFERENCES stored_file(id),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_student_card_token UNIQUE (verification_token),
    CONSTRAINT uq_student_card_tenant_id UNIQUE (tenant_id, id)
);

CREATE TABLE meeting_room (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    class_session_id UUID NOT NULL,
    provider VARCHAR(40) NOT NULL,
    external_id VARCHAR(160),
    instructor_join_url VARCHAR(500) NOT NULL,
    student_join_url VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_meeting_session UNIQUE (class_session_id)
);
