ALTER TABLE assessment_question ADD CONSTRAINT uq_assessment_question_tenant_id UNIQUE (tenant_id, id);

CREATE TABLE assessment_attempt (
    id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenant(id), participant_id UUID NOT NULL,
    attempt_number INTEGER NOT NULL, status VARCHAR(30) NOT NULL, started_at TIMESTAMPTZ NOT NULL,
    deadline_at TIMESTAMPTZ NOT NULL, submitted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_attempt_number UNIQUE (participant_id, attempt_number),
    CONSTRAINT uq_assessment_attempt_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT ck_attempt_number CHECK (attempt_number > 0),
    CONSTRAINT fk_attempt_participant_tenant FOREIGN KEY (tenant_id, participant_id) REFERENCES assessment_participant(tenant_id, id)
);

CREATE INDEX ix_attempt_expiration ON assessment_attempt(status, deadline_at);

CREATE TABLE student_response (
    id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenant(id), attempt_id UUID NOT NULL,
    assessment_question_id UUID NOT NULL, answer_json TEXT NOT NULL, saved_at TIMESTAMPTZ NOT NULL,
    auto_score NUMERIC(10,2), manual_score NUMERIC(10,2), feedback TEXT,
    graded_by UUID REFERENCES user_account(id), graded_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_response_question UNIQUE (attempt_id, assessment_question_id),
    CONSTRAINT uq_student_response_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT ck_response_scores CHECK ((auto_score IS NULL OR auto_score >= 0) AND (manual_score IS NULL OR manual_score >= 0)),
    CONSTRAINT fk_response_attempt_tenant FOREIGN KEY (tenant_id, attempt_id) REFERENCES assessment_attempt(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_response_question_tenant FOREIGN KEY (tenant_id, assessment_question_id) REFERENCES assessment_question(tenant_id, id)
);

CREATE TABLE assessment_result (
    id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenant(id), participant_id UUID NOT NULL,
    selected_attempt_id UUID NOT NULL, status VARCHAR(30) NOT NULL,
    auto_score NUMERIC(10,2) NOT NULL, manual_score NUMERIC(10,2) NOT NULL,
    total_score NUMERIC(10,2) NOT NULL, max_score NUMERIC(10,2) NOT NULL,
    published_by UUID REFERENCES user_account(id), published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_result_participant UNIQUE (participant_id),
    CONSTRAINT uq_assessment_result_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT ck_result_scores CHECK (auto_score >= 0 AND manual_score >= 0 AND total_score >= 0 AND max_score >= 0),
    CONSTRAINT fk_result_participant_tenant FOREIGN KEY (tenant_id, participant_id) REFERENCES assessment_participant(tenant_id, id),
    CONSTRAINT fk_result_attempt_tenant FOREIGN KEY (tenant_id, selected_attempt_id) REFERENCES assessment_attempt(tenant_id, id)
);

CREATE TABLE grade_history (
    id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenant(id), result_id UUID NOT NULL,
    previous_total NUMERIC(10,2) NOT NULL, new_total NUMERIC(10,2) NOT NULL,
    reason VARCHAR(500) NOT NULL, changed_by UUID NOT NULL REFERENCES user_account(id), changed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_grade_history_result_tenant FOREIGN KEY (tenant_id, result_id) REFERENCES assessment_result(tenant_id, id)
);

CREATE TABLE assessment_attempt_event (
    id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenant(id), attempt_id UUID NOT NULL,
    event_type VARCHAR(40) NOT NULL, occurred_at TIMESTAMPTZ NOT NULL, metadata TEXT,
    CONSTRAINT fk_attempt_event_tenant FOREIGN KEY (tenant_id, attempt_id) REFERENCES assessment_attempt(tenant_id, id) ON DELETE CASCADE
);

INSERT INTO permission (code, module, description) VALUES
 ('GRADING_READ','assessment','مشاهده صف تصحیح و نتایج'),
 ('GRADING_WRITE','assessment','ثبت و اصلاح نمره تشریحی'),
 ('RESULT_PUBLISH','assessment','انتشار نتیجه آزمون')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permission (role_id, permission_code)
SELECT ar.id, permission.code FROM access_role ar CROSS JOIN permission
WHERE ar.system_role=TRUE AND ar.code='ADMIN' AND permission.code IN ('GRADING_READ','GRADING_WRITE','RESULT_PUBLISH')
ON CONFLICT DO NOTHING;
