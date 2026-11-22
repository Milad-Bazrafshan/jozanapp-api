CREATE TABLE charge (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    student_id UUID NOT NULL,
    class_section_id UUID,
    enrollment_id UUID,
    code VARCHAR(60) NOT NULL,
    charge_type VARCHAR(40) NOT NULL,
    title VARCHAR(200) NOT NULL,
    amount NUMERIC(14, 2) NOT NULL,
    discount NUMERIC(14, 2) NOT NULL DEFAULT 0,
    due_on DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    notes VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_charge_amount CHECK (amount > 0),
    CONSTRAINT ck_charge_discount CHECK (discount >= 0 AND discount <= amount),
    CONSTRAINT uq_charge_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT uq_charge_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_charge_student_tenant FOREIGN KEY (tenant_id, student_id) REFERENCES student_profile(tenant_id, id),
    CONSTRAINT fk_charge_section_tenant FOREIGN KEY (tenant_id, class_section_id) REFERENCES class_section(tenant_id, id),
    CONSTRAINT fk_charge_enrollment_tenant FOREIGN KEY (tenant_id, enrollment_id) REFERENCES enrollment(tenant_id, id)
);

CREATE INDEX ix_charge_tenant_student ON charge (tenant_id, student_id, status);
CREATE INDEX ix_charge_tenant_due ON charge (tenant_id, due_on);

CREATE TABLE invoice (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    student_id UUID NOT NULL,
    code VARCHAR(60) NOT NULL,
    due_on DATE NOT NULL,
    issued_on DATE,
    status VARCHAR(30) NOT NULL,
    notes VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_invoice_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT uq_invoice_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_invoice_student_tenant FOREIGN KEY (tenant_id, student_id) REFERENCES student_profile(tenant_id, id)
);

CREATE INDEX ix_invoice_tenant_student ON invoice (tenant_id, student_id, status);

CREATE TABLE invoice_item (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    invoice_id UUID NOT NULL,
    charge_id UUID NOT NULL,
    line_no INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_invoice_item_line CHECK (line_no > 0),
    CONSTRAINT uq_invoice_item_charge UNIQUE (charge_id),
    CONSTRAINT uq_invoice_item_line UNIQUE (invoice_id, line_no),
    CONSTRAINT uq_invoice_item_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_invoice_item_invoice_tenant FOREIGN KEY (tenant_id, invoice_id) REFERENCES invoice(tenant_id, id),
    CONSTRAINT fk_invoice_item_charge_tenant FOREIGN KEY (tenant_id, charge_id) REFERENCES charge(tenant_id, id)
);

CREATE TABLE payment (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    student_id UUID NOT NULL,
    receipt_number VARCHAR(60) NOT NULL,
    amount NUMERIC(14, 2) NOT NULL,
    method VARCHAR(40) NOT NULL,
    provider VARCHAR(40) NOT NULL,
    external_reference VARCHAR(120),
    status VARCHAR(30) NOT NULL,
    paid_at TIMESTAMPTZ NOT NULL,
    notes VARCHAR(500),
    recorded_by UUID REFERENCES user_account(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_payment_amount CHECK (amount > 0),
    CONSTRAINT uq_payment_tenant_receipt UNIQUE (tenant_id, receipt_number),
    CONSTRAINT uq_payment_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_payment_student_tenant FOREIGN KEY (tenant_id, student_id) REFERENCES student_profile(tenant_id, id)
);

CREATE UNIQUE INDEX uq_payment_external_reference
    ON payment (tenant_id, provider, external_reference)
    WHERE external_reference IS NOT NULL;

CREATE INDEX ix_payment_tenant_student ON payment (tenant_id, student_id, status);

CREATE TABLE payment_allocation (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    payment_id UUID NOT NULL,
    charge_id UUID NOT NULL,
    amount NUMERIC(14, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_allocation_amount CHECK (amount > 0),
    CONSTRAINT uq_allocation_payment_charge UNIQUE (payment_id, charge_id),
    CONSTRAINT uq_allocation_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_allocation_payment_tenant FOREIGN KEY (tenant_id, payment_id) REFERENCES payment(tenant_id, id),
    CONSTRAINT fk_allocation_charge_tenant FOREIGN KEY (tenant_id, charge_id) REFERENCES charge(tenant_id, id)
);

CREATE INDEX ix_allocation_charge ON payment_allocation (charge_id);
CREATE INDEX ix_allocation_payment ON payment_allocation (payment_id);
