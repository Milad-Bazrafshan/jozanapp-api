CREATE TABLE attendance_sheet (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    class_session_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    finalized_by UUID REFERENCES user_account(id),
    finalized_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_attendance_sheet_session UNIQUE (class_session_id),
    CONSTRAINT uq_attendance_sheet_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_attendance_sheet_session_tenant FOREIGN KEY (tenant_id, class_session_id) REFERENCES class_session(tenant_id, id)
);

CREATE TABLE attendance_record (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    attendance_sheet_id UUID NOT NULL,
    student_id UUID NOT NULL,
    enrollment_id UUID NOT NULL,
    presence_status VARCHAR(30) NOT NULL,
    delivery_mode VARCHAR(30) NOT NULL,
    late_minutes INTEGER,
    note VARCHAR(500),
    marked_by UUID NOT NULL REFERENCES user_account(id),
    marked_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_attendance_student_sheet UNIQUE (attendance_sheet_id, student_id),
    CONSTRAINT uq_attendance_record_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT ck_attendance_late_minutes CHECK (late_minutes IS NULL OR late_minutes >= 0),
    CONSTRAINT fk_attendance_record_sheet_tenant FOREIGN KEY (tenant_id, attendance_sheet_id) REFERENCES attendance_sheet(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_attendance_record_student_tenant FOREIGN KEY (tenant_id, student_id) REFERENCES student_profile(tenant_id, id),
    CONSTRAINT fk_attendance_record_enrollment_tenant FOREIGN KEY (tenant_id, enrollment_id) REFERENCES enrollment(tenant_id, id)
);

CREATE TABLE attendance_record_history (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    attendance_record_id UUID NOT NULL,
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    delivery_mode VARCHAR(30) NOT NULL,
    late_minutes INTEGER,
    note VARCHAR(500),
    change_reason VARCHAR(500),
    changed_by UUID NOT NULL REFERENCES user_account(id),
    changed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_attendance_history_record_tenant FOREIGN KEY (tenant_id, attendance_record_id) REFERENCES attendance_record(tenant_id, id) ON DELETE CASCADE
);

CREATE INDEX ix_attendance_student ON attendance_record (student_id, marked_at);

INSERT INTO permission (code, module, description) VALUES
    ('ATTENDANCE_READ', 'attendance', 'مشاهده حضور و غیاب جلسات'),
    ('ATTENDANCE_WRITE', 'attendance', 'ثبت، نهایی‌سازی و اصلاح حضور و غیاب')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permission (role_id, permission_code)
SELECT ar.id, permission.code FROM access_role ar CROSS JOIN permission
WHERE ar.system_role = TRUE AND ar.code = 'ADMIN' AND permission.code IN ('ATTENDANCE_READ', 'ATTENDANCE_WRITE')
ON CONFLICT DO NOTHING;
