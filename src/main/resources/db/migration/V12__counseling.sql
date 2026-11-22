CREATE TABLE counselor_assignment (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    counselor_membership_id UUID NOT NULL REFERENCES tenant_membership(id),
    assignment_type VARCHAR(30) NOT NULL,
    student_id UUID REFERENCES student_profile(id),
    guardian_id UUID REFERENCES guardian_profile(id),
    effective_from DATE NOT NULL,
    effective_to DATE,
    status VARCHAR(30) NOT NULL,
    note VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_counselor_assignment_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_counselor_assignment_student FOREIGN KEY (tenant_id, student_id)
        REFERENCES student_profile(tenant_id, id),
    CONSTRAINT fk_counselor_assignment_guardian FOREIGN KEY (tenant_id, guardian_id)
        REFERENCES guardian_profile(tenant_id, id)
);
CREATE INDEX ix_counselor_assignment_counselor ON counselor_assignment (tenant_id, counselor_membership_id, status);
CREATE INDEX ix_counselor_assignment_student ON counselor_assignment (tenant_id, student_id, status);

CREATE TABLE counselor_supervision (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    senior_membership_id UUID NOT NULL REFERENCES tenant_membership(id),
    counselor_membership_id UUID NOT NULL REFERENCES tenant_membership(id),
    effective_from DATE NOT NULL,
    effective_to DATE,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_counselor_supervision_tenant_id UNIQUE (tenant_id, id)
);
CREATE INDEX ix_counselor_supervision_senior ON counselor_supervision (tenant_id, senior_membership_id, status);

CREATE TABLE student_daily_log (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    student_id UUID NOT NULL,
    log_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    day_summary VARCHAR(2000),
    counselor_feedback VARCHAR(2000),
    reviewed_at TIMESTAMPTZ,
    reviewed_by_membership_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_student_daily_log_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_student_daily_log_day UNIQUE (tenant_id, student_id, log_date),
    CONSTRAINT fk_student_daily_log_student FOREIGN KEY (tenant_id, student_id)
        REFERENCES student_profile(tenant_id, id)
);

CREATE TABLE student_daily_log_entry (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    daily_log_id UUID NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    activity_title VARCHAR(200) NOT NULL,
    subject_name VARCHAR(120),
    duration_minutes INTEGER NOT NULL DEFAULT 0,
    notes VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_student_daily_log_entry_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_student_daily_log_entry_log FOREIGN KEY (tenant_id, daily_log_id)
        REFERENCES student_daily_log(tenant_id, id) ON DELETE CASCADE
);

CREATE TABLE counselor_admin_report (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    counselor_membership_id UUID NOT NULL REFERENCES tenant_membership(id),
    title VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    period_label VARCHAR(120),
    status VARCHAR(30) NOT NULL,
    sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_counselor_admin_report_tenant_id UNIQUE (tenant_id, id)
);
CREATE INDEX ix_counselor_admin_report_sent ON counselor_admin_report (tenant_id, status, sent_at DESC);

CREATE TABLE counselor_senior_report (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    counselor_membership_id UUID NOT NULL REFERENCES tenant_membership(id),
    senior_membership_id UUID NOT NULL REFERENCES tenant_membership(id),
    report_year INTEGER NOT NULL,
    report_month INTEGER NOT NULL,
    title VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    ai_analysis TEXT,
    status VARCHAR(30) NOT NULL,
    finalized_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_counselor_senior_report_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_counselor_senior_report_period UNIQUE (tenant_id, counselor_membership_id, report_year, report_month)
);

CREATE TABLE study_plan (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    student_id UUID NOT NULL,
    counselor_membership_id UUID NOT NULL REFERENCES tenant_membership(id),
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_study_plan_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_study_plan_student FOREIGN KEY (tenant_id, student_id)
        REFERENCES student_profile(tenant_id, id)
);
CREATE INDEX ix_study_plan_student ON study_plan (tenant_id, student_id, status);

CREATE TABLE study_plan_item (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    study_plan_id UUID NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    subject_name VARCHAR(120) NOT NULL,
    activity_title VARCHAR(200) NOT NULL,
    target_minutes INTEGER NOT NULL,
    planned_date DATE,
    notes VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_study_plan_item_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_study_plan_item_plan FOREIGN KEY (tenant_id, study_plan_id)
        REFERENCES study_plan(tenant_id, id) ON DELETE CASCADE
);

CREATE TABLE study_plan_progress (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    study_plan_item_id UUID NOT NULL,
    student_id UUID NOT NULL,
    completed_minutes INTEGER NOT NULL,
    completed_at TIMESTAMPTZ NOT NULL,
    note VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_study_plan_progress_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_study_plan_progress_item UNIQUE (tenant_id, study_plan_item_id),
    CONSTRAINT fk_study_plan_progress_item FOREIGN KEY (tenant_id, study_plan_item_id)
        REFERENCES study_plan_item(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_study_plan_progress_student FOREIGN KEY (tenant_id, student_id)
        REFERENCES student_profile(tenant_id, id)
);

INSERT INTO permission (code, module, description) VALUES
    ('COUNSELING_READ', 'counseling', 'مشاهده مشاوره، گزارش کار و برنامه درسی'),
    ('COUNSELING_WRITE', 'counseling', 'ثبت گزارش کار، برنامه درسی و بازخورد مشاور'),
    ('COUNSELING_ASSIGN', 'counseling', 'تخصیص مشاور به دانش‌آموز یا ولی'),
    ('COUNSELING_ADMIN_REPORT_READ', 'counseling', 'مشاهده گزارش‌های مشاوران برای ادمین'),
    ('COUNSELING_SENIOR_REPORT_READ', 'counseling', 'مشاهده گزارش‌های ماهانه مشاوران برای ارشد')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permission (role_id, permission_code)
SELECT ar.id, p.code
FROM access_role ar
CROSS JOIN permission p
WHERE ar.system_role = TRUE
  AND ar.code = 'ADMIN'
  AND p.code IN (
      'COUNSELING_READ', 'COUNSELING_WRITE', 'COUNSELING_ASSIGN',
      'COUNSELING_ADMIN_REPORT_READ', 'COUNSELING_SENIOR_REPORT_READ'
  )
ON CONFLICT DO NOTHING;
