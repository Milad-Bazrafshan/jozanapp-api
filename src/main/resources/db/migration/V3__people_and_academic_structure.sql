CREATE TABLE student_profile (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    person_id UUID NOT NULL,
    student_code VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    admission_date DATE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_student_tenant_code UNIQUE (tenant_id, student_code),
    CONSTRAINT uq_student_person UNIQUE (tenant_id, person_id),
    CONSTRAINT uq_student_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_student_person_tenant FOREIGN KEY (tenant_id, person_id) REFERENCES person(tenant_id, id)
);

CREATE TABLE instructor_profile (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    person_id UUID NOT NULL,
    instructor_code VARCHAR(50) NOT NULL,
    title VARCHAR(120),
    specialization VARCHAR(300),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_instructor_tenant_code UNIQUE (tenant_id, instructor_code),
    CONSTRAINT uq_instructor_person UNIQUE (tenant_id, person_id),
    CONSTRAINT uq_instructor_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_instructor_person_tenant FOREIGN KEY (tenant_id, person_id) REFERENCES person(tenant_id, id)
);

CREATE TABLE guardian_profile (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    person_id UUID NOT NULL,
    occupation VARCHAR(150),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_guardian_person UNIQUE (tenant_id, person_id),
    CONSTRAINT uq_guardian_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_guardian_person_tenant FOREIGN KEY (tenant_id, person_id) REFERENCES person(tenant_id, id)
);

CREATE TABLE student_guardian (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    student_id UUID NOT NULL,
    guardian_id UUID NOT NULL,
    relationship_type VARCHAR(40) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    has_academic_access BOOLEAN NOT NULL DEFAULT TRUE,
    has_financial_access BOOLEAN NOT NULL DEFAULT FALSE,
    has_pickup_permission BOOLEAN NOT NULL DEFAULT FALSE,
    receives_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    effective_from DATE NOT NULL,
    effective_to DATE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_student_guardian_dates CHECK (effective_to IS NULL OR effective_to >= effective_from),
    CONSTRAINT uq_student_guardian UNIQUE (student_id, guardian_id, relationship_type, effective_from),
    CONSTRAINT fk_student_guardian_student_tenant FOREIGN KEY (tenant_id, student_id) REFERENCES student_profile(tenant_id, id),
    CONSTRAINT fk_student_guardian_guardian_tenant FOREIGN KEY (tenant_id, guardian_id) REFERENCES guardian_profile(tenant_id, id)
);

CREATE UNIQUE INDEX uq_primary_guardian_active
    ON student_guardian (student_id)
    WHERE is_primary = TRUE AND effective_to IS NULL;

CREATE TABLE academic_period (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    period_type VARCHAR(30) NOT NULL,
    starts_on DATE NOT NULL,
    ends_on DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_academic_period_dates CHECK (ends_on >= starts_on),
    CONSTRAINT uq_academic_period_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT uq_academic_period_tenant_id UNIQUE (tenant_id, id)
);

CREATE TABLE academic_program (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(180) NOT NULL,
    program_type VARCHAR(40) NOT NULL,
    description VARCHAR(700),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_academic_program_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT uq_academic_program_tenant_id UNIQUE (tenant_id, id)
);

CREATE TABLE course (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    code VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    duration_minutes INTEGER,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_course_duration CHECK (duration_minutes IS NULL OR duration_minutes > 0),
    CONSTRAINT uq_course_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT uq_course_tenant_id UNIQUE (tenant_id, id)
);

CREATE TABLE program_course (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    program_id UUID NOT NULL,
    course_id UUID NOT NULL,
    sequence_number INTEGER,
    required BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_program_course_sequence CHECK (sequence_number IS NULL OR sequence_number > 0),
    CONSTRAINT uq_program_course UNIQUE (program_id, course_id),
    CONSTRAINT fk_program_course_program_tenant FOREIGN KEY (tenant_id, program_id) REFERENCES academic_program(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_program_course_course_tenant FOREIGN KEY (tenant_id, course_id) REFERENCES course(tenant_id, id)
);

INSERT INTO permission (code, module, description) VALUES
    ('PEOPLE_READ', 'people', 'مشاهده اشخاص و پروفایل‌ها'),
    ('PEOPLE_WRITE', 'people', 'ثبت و ویرایش دانش‌آموز، استاد و ولی'),
    ('ACADEMIC_READ', 'academic', 'مشاهده ساختار آموزشی'),
    ('ACADEMIC_WRITE', 'academic', 'تعریف و ویرایش ساختار آموزشی')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permission (role_id, permission_code)
SELECT ar.id, permission.code
FROM access_role ar
CROSS JOIN permission
WHERE ar.system_role = TRUE
  AND ar.code = 'ADMIN'
  AND permission.code IN ('PEOPLE_READ', 'PEOPLE_WRITE', 'ACADEMIC_READ', 'ACADEMIC_WRITE')
ON CONFLICT DO NOTHING;
