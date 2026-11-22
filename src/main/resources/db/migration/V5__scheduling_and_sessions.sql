CREATE TABLE academic_location (
    id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenant(id), branch_id UUID NOT NULL,
    code VARCHAR(60) NOT NULL, name VARCHAR(160) NOT NULL, location_type VARCHAR(30) NOT NULL,
    capacity INTEGER, status VARCHAR(30) NOT NULL, created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_location_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT uq_location_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT ck_location_capacity CHECK (capacity IS NULL OR capacity > 0),
    CONSTRAINT fk_location_branch_tenant FOREIGN KEY (tenant_id, branch_id) REFERENCES branch(tenant_id, id)
);

CREATE TABLE schedule_rule (
    id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenant(id), class_section_id UUID NOT NULL,
    code VARCHAR(60) NOT NULL, recurrence_type VARCHAR(20) NOT NULL, starts_on DATE NOT NULL,
    ends_on DATE NOT NULL, day_of_week VARCHAR(12), starts_at TIME NOT NULL, ends_at TIME NOT NULL,
    time_zone VARCHAR(60) NOT NULL, interval_weeks INTEGER NOT NULL DEFAULT 1, location_id UUID,
    status VARCHAR(30) NOT NULL, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_schedule_rule_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT uq_schedule_rule_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT ck_schedule_dates CHECK (ends_on >= starts_on),
    CONSTRAINT ck_schedule_times CHECK (ends_at > starts_at),
    CONSTRAINT ck_schedule_interval CHECK (interval_weeks > 0),
    CONSTRAINT fk_schedule_section_tenant FOREIGN KEY (tenant_id, class_section_id) REFERENCES class_section(tenant_id, id),
    CONSTRAINT fk_schedule_location_tenant FOREIGN KEY (tenant_id, location_id) REFERENCES academic_location(tenant_id, id)
);

CREATE TABLE schedule_rule_instructor (
    schedule_rule_id UUID NOT NULL REFERENCES schedule_rule(id) ON DELETE CASCADE,
    instructor_id UUID NOT NULL REFERENCES instructor_profile(id),
    PRIMARY KEY (schedule_rule_id, instructor_id)
);

CREATE TABLE class_session (
    id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenant(id), class_section_id UUID NOT NULL,
    schedule_rule_id UUID, session_date DATE NOT NULL, planned_start_at TIMESTAMPTZ NOT NULL,
    planned_end_at TIMESTAMPTZ NOT NULL, location_id UUID, status VARCHAR(30) NOT NULL,
    manually_overridden BOOLEAN NOT NULL DEFAULT FALSE, change_reason VARCHAR(500), replacement_session_id UUID,
    created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_class_session_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_rule_session_date UNIQUE (schedule_rule_id, session_date),
    CONSTRAINT ck_session_times CHECK (planned_end_at > planned_start_at),
    CONSTRAINT fk_session_section_tenant FOREIGN KEY (tenant_id, class_section_id) REFERENCES class_section(tenant_id, id),
    CONSTRAINT fk_session_rule_tenant FOREIGN KEY (tenant_id, schedule_rule_id) REFERENCES schedule_rule(tenant_id, id),
    CONSTRAINT fk_session_location_tenant FOREIGN KEY (tenant_id, location_id) REFERENCES academic_location(tenant_id, id),
    CONSTRAINT fk_session_replacement_tenant FOREIGN KEY (tenant_id, replacement_session_id) REFERENCES class_session(tenant_id, id)
);

CREATE TABLE class_session_instructor (
    class_session_id UUID NOT NULL REFERENCES class_session(id) ON DELETE CASCADE,
    instructor_id UUID NOT NULL REFERENCES instructor_profile(id),
    PRIMARY KEY (class_session_id, instructor_id)
);

CREATE INDEX ix_session_tenant_start ON class_session (tenant_id, planned_start_at);
CREATE INDEX ix_session_section_start ON class_session (class_section_id, planned_start_at);

INSERT INTO permission (code, module, description) VALUES
    ('SCHEDULE_READ', 'scheduling', 'مشاهده برنامه و جلسات کلاس'),
    ('SCHEDULE_WRITE', 'scheduling', 'تعریف برنامه و مدیریت جلسات کلاس')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permission (role_id, permission_code)
SELECT ar.id, permission.code FROM access_role ar CROSS JOIN permission
WHERE ar.system_role = TRUE AND ar.code = 'ADMIN' AND permission.code IN ('SCHEDULE_READ', 'SCHEDULE_WRITE')
ON CONFLICT DO NOTHING;
