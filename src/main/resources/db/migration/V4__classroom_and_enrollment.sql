CREATE TABLE course_offering (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    academic_period_id UUID NOT NULL,
    course_id UUID NOT NULL,
    branch_id UUID NOT NULL,
    code VARCHAR(60) NOT NULL,
    enrollment_starts_on DATE,
    enrollment_ends_on DATE,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_offering_enrollment_dates CHECK (enrollment_ends_on IS NULL OR enrollment_starts_on IS NULL OR enrollment_ends_on >= enrollment_starts_on),
    CONSTRAINT uq_offering_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT uq_offering_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_offering_period_tenant FOREIGN KEY (tenant_id, academic_period_id) REFERENCES academic_period(tenant_id, id),
    CONSTRAINT fk_offering_course_tenant FOREIGN KEY (tenant_id, course_id) REFERENCES course(tenant_id, id),
    CONSTRAINT fk_offering_branch_tenant FOREIGN KEY (tenant_id, branch_id) REFERENCES branch(tenant_id, id)
);

CREATE TABLE class_section (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    offering_id UUID NOT NULL,
    code VARCHAR(60) NOT NULL,
    name VARCHAR(200) NOT NULL,
    capacity INTEGER NOT NULL,
    delivery_mode VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_class_section_capacity CHECK (capacity > 0),
    CONSTRAINT uq_class_section_offering_code UNIQUE (offering_id, code),
    CONSTRAINT uq_class_section_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_class_section_offering_tenant FOREIGN KEY (tenant_id, offering_id) REFERENCES course_offering(tenant_id, id)
);

CREATE TABLE class_instructor (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    class_section_id UUID NOT NULL,
    instructor_id UUID NOT NULL,
    instructor_role VARCHAR(40) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    effective_from DATE NOT NULL,
    effective_to DATE,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_class_instructor_dates CHECK (effective_to IS NULL OR effective_to >= effective_from),
    CONSTRAINT uq_class_instructor_assignment UNIQUE (class_section_id, instructor_id, instructor_role, effective_from),
    CONSTRAINT fk_class_instructor_section_tenant FOREIGN KEY (tenant_id, class_section_id) REFERENCES class_section(tenant_id, id),
    CONSTRAINT fk_class_instructor_profile_tenant FOREIGN KEY (tenant_id, instructor_id) REFERENCES instructor_profile(tenant_id, id)
);

CREATE UNIQUE INDEX uq_primary_instructor_active
    ON class_instructor (class_section_id)
    WHERE is_primary = TRUE AND effective_to IS NULL AND status = 'ACTIVE';

CREATE TABLE enrollment (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    class_section_id UUID NOT NULL,
    student_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    source VARCHAR(30) NOT NULL,
    started_on DATE NOT NULL,
    ended_on DATE,
    enrolled_by UUID REFERENCES user_account(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_enrollment_dates CHECK (ended_on IS NULL OR ended_on >= started_on),
    CONSTRAINT uq_enrollment_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_enrollment_section_tenant FOREIGN KEY (tenant_id, class_section_id) REFERENCES class_section(tenant_id, id),
    CONSTRAINT fk_enrollment_student_tenant FOREIGN KEY (tenant_id, student_id) REFERENCES student_profile(tenant_id, id)
);

CREATE UNIQUE INDEX uq_current_student_class_enrollment
    ON enrollment (student_id, class_section_id)
    WHERE status IN ('ACTIVE', 'WAITLISTED', 'SUSPENDED');

CREATE INDEX ix_enrollment_section_status ON enrollment (class_section_id, status);
CREATE INDEX ix_enrollment_student_status ON enrollment (student_id, status);

CREATE TABLE enrollment_status_history (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    enrollment_id UUID NOT NULL,
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    reason VARCHAR(500),
    changed_by UUID REFERENCES user_account(id),
    changed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_enrollment_history_tenant FOREIGN KEY (tenant_id, enrollment_id) REFERENCES enrollment(tenant_id, id) ON DELETE CASCADE
);

INSERT INTO permission (code, module, description) VALUES
    ('CLASS_READ', 'classroom', 'مشاهده ارائه درس، کلاس و استادان'),
    ('CLASS_WRITE', 'classroom', 'ایجاد و ویرایش ارائه درس و کلاس'),
    ('ENROLLMENT_READ', 'enrollment', 'مشاهده ثبت‌نام‌های دانش‌آموزان'),
    ('ENROLLMENT_WRITE', 'enrollment', 'ثبت‌نام، انصراف و انتقال دانش‌آموز')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permission (role_id, permission_code)
SELECT ar.id, permission.code
FROM access_role ar
CROSS JOIN permission
WHERE ar.system_role = TRUE
  AND ar.code = 'ADMIN'
  AND permission.code IN ('CLASS_READ', 'CLASS_WRITE', 'ENROLLMENT_READ', 'ENROLLMENT_WRITE')
ON CONFLICT DO NOTHING;
