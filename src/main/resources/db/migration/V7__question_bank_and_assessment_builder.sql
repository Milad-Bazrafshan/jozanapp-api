CREATE TABLE question (
    id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenant(id), course_id UUID,
    code VARCHAR(60) NOT NULL, question_type VARCHAR(30) NOT NULL, status VARCHAR(30) NOT NULL,
    current_version_number INTEGER NOT NULL, created_by UUID REFERENCES user_account(id),
    created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_question_tenant_code UNIQUE (tenant_id, code), CONSTRAINT uq_question_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_question_course_tenant FOREIGN KEY (tenant_id, course_id) REFERENCES course(tenant_id, id)
);

CREATE TABLE question_version (
    id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenant(id), question_id UUID NOT NULL,
    version_number INTEGER NOT NULL, prompt TEXT NOT NULL, options_json TEXT, answer_key_json TEXT,
    explanation TEXT, default_points NUMERIC(10,2) NOT NULL, created_by UUID REFERENCES user_account(id),
    created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_question_version UNIQUE (question_id, version_number), CONSTRAINT uq_question_version_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT ck_question_default_points CHECK (default_points > 0),
    CONSTRAINT fk_question_version_question_tenant FOREIGN KEY (tenant_id, question_id) REFERENCES question(tenant_id, id)
);

CREATE TABLE assessment (
    id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenant(id), course_id UUID,
    code VARCHAR(60) NOT NULL, title VARCHAR(200) NOT NULL, assessment_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL, opens_at TIMESTAMPTZ NOT NULL, closes_at TIMESTAMPTZ NOT NULL,
    duration_minutes INTEGER NOT NULL, attempts_allowed INTEGER NOT NULL DEFAULT 1,
    randomize_questions BOOLEAN NOT NULL DEFAULT FALSE, total_points NUMERIC(10,2) NOT NULL DEFAULT 0,
    created_by UUID REFERENCES user_account(id), published_by UUID REFERENCES user_account(id), published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_assessment_tenant_code UNIQUE (tenant_id, code), CONSTRAINT uq_assessment_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT ck_assessment_window CHECK (closes_at > opens_at), CONSTRAINT ck_assessment_duration CHECK (duration_minutes > 0),
    CONSTRAINT ck_assessment_attempts CHECK (attempts_allowed > 0),
    CONSTRAINT fk_assessment_course_tenant FOREIGN KEY (tenant_id, course_id) REFERENCES course(tenant_id, id)
);

CREATE TABLE assessment_section (
    id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenant(id), assessment_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL, instructions TEXT, sequence_number INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_assessment_section_sequence UNIQUE (assessment_id, sequence_number), CONSTRAINT uq_assessment_section_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_assessment_section_assessment_tenant FOREIGN KEY (tenant_id, assessment_id) REFERENCES assessment(tenant_id, id) ON DELETE CASCADE
);

CREATE TABLE assessment_question (
    id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenant(id), assessment_id UUID NOT NULL,
    section_id UUID NOT NULL, question_version_id UUID NOT NULL, points NUMERIC(10,2) NOT NULL,
    sequence_number INTEGER NOT NULL, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_assessment_question_sequence UNIQUE (section_id, sequence_number), CONSTRAINT uq_assessment_question_version UNIQUE (assessment_id, question_version_id),
    CONSTRAINT ck_assessment_question_points CHECK (points > 0),
    CONSTRAINT fk_assessment_question_assessment_tenant FOREIGN KEY (tenant_id, assessment_id) REFERENCES assessment(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_assessment_question_section_tenant FOREIGN KEY (tenant_id, section_id) REFERENCES assessment_section(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_assessment_question_version_tenant FOREIGN KEY (tenant_id, question_version_id) REFERENCES question_version(tenant_id, id)
);

CREATE TABLE assessment_target (
    id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenant(id), assessment_id UUID NOT NULL,
    class_section_id UUID NOT NULL, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_assessment_target UNIQUE (assessment_id, class_section_id),
    CONSTRAINT fk_assessment_target_assessment_tenant FOREIGN KEY (tenant_id, assessment_id) REFERENCES assessment(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_assessment_target_section_tenant FOREIGN KEY (tenant_id, class_section_id) REFERENCES class_section(tenant_id, id)
);

CREATE TABLE assessment_participant (
    id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenant(id), assessment_id UUID NOT NULL,
    student_id UUID NOT NULL, enrollment_id UUID NOT NULL, student_name_snapshot VARCHAR(250) NOT NULL,
    student_code_snapshot VARCHAR(50) NOT NULL, status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_assessment_participant UNIQUE (assessment_id, student_id), CONSTRAINT uq_assessment_participant_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_participant_assessment_tenant FOREIGN KEY (tenant_id, assessment_id) REFERENCES assessment(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_participant_student_tenant FOREIGN KEY (tenant_id, student_id) REFERENCES student_profile(tenant_id, id),
    CONSTRAINT fk_participant_enrollment_tenant FOREIGN KEY (tenant_id, enrollment_id) REFERENCES enrollment(tenant_id, id)
);

INSERT INTO permission (code, module, description) VALUES
 ('QUESTION_READ','assessment','مشاهده بانک سؤال'), ('QUESTION_WRITE','assessment','ایجاد سؤال و نسخه جدید'),
 ('ASSESSMENT_READ','assessment','مشاهده آزمون‌ها'), ('ASSESSMENT_WRITE','assessment','طراحی آزمون و تعیین مخاطب'),
 ('ASSESSMENT_PUBLISH','assessment','اعتبارسنجی و انتشار آزمون')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permission (role_id, permission_code)
SELECT ar.id, permission.code FROM access_role ar CROSS JOIN permission
WHERE ar.system_role=TRUE AND ar.code='ADMIN' AND permission.code IN ('QUESTION_READ','QUESTION_WRITE','ASSESSMENT_READ','ASSESSMENT_WRITE','ASSESSMENT_PUBLISH')
ON CONFLICT DO NOTHING;
