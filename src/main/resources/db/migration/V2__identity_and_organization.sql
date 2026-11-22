CREATE TABLE tenant (
    id UUID PRIMARY KEY,
    slug VARCHAR(80) NOT NULL,
    name VARCHAR(200) NOT NULL,
    status VARCHAR(30) NOT NULL,
    timezone VARCHAR(80) NOT NULL DEFAULT 'Asia/Tehran',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_tenant_slug UNIQUE (slug),
    CONSTRAINT ck_tenant_slug_lowercase CHECK (slug = lower(slug))
);

CREATE TABLE educational_institution (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    name VARCHAR(200) NOT NULL,
    institution_type VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_institution_tenant UNIQUE (tenant_id),
    CONSTRAINT uq_institution_tenant_id UNIQUE (tenant_id, id)
);

CREATE TABLE branch (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    institution_id UUID NOT NULL REFERENCES educational_institution(id),
    code VARCHAR(40) NOT NULL,
    name VARCHAR(200) NOT NULL,
    status VARCHAR(30) NOT NULL,
    phone VARCHAR(30),
    address VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_branch_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT uq_branch_tenant_id UNIQUE (tenant_id, id)
);

CREATE TABLE user_account (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    password_changed_at TIMESTAMPTZ NOT NULL,
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_user_account_email UNIQUE (email),
    CONSTRAINT ck_user_account_email_lowercase CHECK (email = lower(email))
);

CREATE TABLE person (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    first_name VARCHAR(120) NOT NULL,
    last_name VARCHAR(120) NOT NULL,
    national_id VARCHAR(30),
    mobile VARCHAR(30),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_person_tenant_id UNIQUE (tenant_id, id)
);

CREATE UNIQUE INDEX uq_person_tenant_national_id
    ON person (tenant_id, national_id)
    WHERE national_id IS NOT NULL;

CREATE TABLE tenant_membership (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    user_account_id UUID NOT NULL REFERENCES user_account(id),
    person_id UUID NOT NULL REFERENCES person(id),
    status VARCHAR(30) NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_membership_tenant_user UNIQUE (tenant_id, user_account_id),
    CONSTRAINT uq_membership_tenant_person UNIQUE (tenant_id, person_id)
);

CREATE TABLE permission (
    code VARCHAR(100) PRIMARY KEY,
    module VARCHAR(60) NOT NULL,
    description VARCHAR(250) NOT NULL
);

CREATE TABLE access_role (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    code VARCHAR(60) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    system_role BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_role_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT uq_role_tenant_id UNIQUE (tenant_id, id)
);

CREATE TABLE role_permission (
    role_id UUID NOT NULL REFERENCES access_role(id) ON DELETE CASCADE,
    permission_code VARCHAR(100) NOT NULL REFERENCES permission(code),
    PRIMARY KEY (role_id, permission_code)
);

CREATE TABLE access_grant (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    membership_id UUID NOT NULL REFERENCES tenant_membership(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES access_role(id),
    scope_type VARCHAR(30) NOT NULL,
    scope_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID REFERENCES user_account(id),
    CONSTRAINT ck_access_grant_scope CHECK (
        (scope_type = 'TENANT' AND scope_id IS NULL)
        OR (scope_type = 'BRANCH' AND scope_id IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uq_access_grant
    ON access_grant (membership_id, role_id, scope_type, COALESCE(scope_id, '00000000-0000-0000-0000-000000000000'::uuid));

CREATE TABLE refresh_token (
    id UUID PRIMARY KEY,
    token_hash CHAR(64) NOT NULL,
    user_account_id UUID NOT NULL REFERENCES user_account(id),
    membership_id UUID NOT NULL REFERENCES tenant_membership(id),
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    replaced_by_id UUID REFERENCES refresh_token(id),
    CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash)
);

CREATE INDEX ix_refresh_token_membership ON refresh_token (membership_id, expires_at);

CREATE TABLE password_reset_token (
    id UUID PRIMARY KEY,
    token_hash CHAR(64) NOT NULL,
    user_account_id UUID NOT NULL REFERENCES user_account(id),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    CONSTRAINT uq_password_reset_token_hash UNIQUE (token_hash)
);

CREATE TABLE external_identity (
    id UUID PRIMARY KEY,
    user_account_id UUID NOT NULL REFERENCES user_account(id),
    provider VARCHAR(60) NOT NULL,
    external_user_id VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_external_identity_provider_user UNIQUE (provider, external_user_id),
    CONSTRAINT uq_external_identity_account_provider UNIQUE (user_account_id, provider)
);

CREATE TABLE audit_log (
    id UUID PRIMARY KEY,
    tenant_id UUID REFERENCES tenant(id),
    actor_user_id UUID REFERENCES user_account(id),
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id UUID,
    trace_id VARCHAR(128),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_audit_log_tenant_occurred_at ON audit_log (tenant_id, occurred_at DESC);

INSERT INTO permission (code, module, description) VALUES
    ('ORGANIZATION_READ', 'organization', 'مشاهده اطلاعات مؤسسه و شعب'),
    ('BRANCH_WRITE', 'organization', 'ایجاد و ویرایش شعبه'),
    ('USER_READ', 'organization', 'مشاهده کاربران و دسترسی‌ها'),
    ('USER_WRITE', 'organization', 'ایجاد و ویرایش کاربران'),
    ('ROLE_READ', 'organization', 'مشاهده نقش‌ها و مجوزها'),
    ('ROLE_WRITE', 'organization', 'ایجاد و ویرایش نقش‌ها'),
    ('AUDIT_READ', 'audit', 'مشاهده تاریخچه عملیات')
ON CONFLICT (code) DO NOTHING;
