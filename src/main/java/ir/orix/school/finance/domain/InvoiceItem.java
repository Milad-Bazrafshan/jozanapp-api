package ir.orix.school.finance.domain;

import jakarta.persistence.*;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity
@Table(name = "invoice_item", schema = "school")
public class InvoiceItem extends AuditedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tenant_id") private Tenant tenant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "invoice_id") private Invoice invoice;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "charge_id") private Charge charge;
    @Column(name = "line_no", nullable = false) private int lineNo;

    protected InvoiceItem() {}

    InvoiceItem(Tenant tenant, Invoice invoice, Charge charge, int lineNo) {
        this.tenant = tenant;
        this.invoice = invoice;
        this.charge = charge;
        this.lineNo = lineNo;
    }

    public Invoice getInvoice() { return invoice; }
    public Charge getCharge() { return charge; }
    public int getLineNo() { return lineNo; }
}
