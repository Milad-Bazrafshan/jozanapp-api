package ir.orix.school.finance.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.people.domain.StudentProfile;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity
@Table(name = "invoice", schema = "school")
public class Invoice extends AuditedEntity {
    public enum Status { DRAFT, ISSUED, VOID }

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tenant_id") private Tenant tenant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "student_id") private StudentProfile student;
    @Column(nullable = false, length = 60) private String code;
    @Column(name = "due_on", nullable = false) private LocalDate dueOn;
    @Column(name = "issued_on") private LocalDate issuedOn;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status;
    @Column(length = 500) private String notes;
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo")
    private List<InvoiceItem> items = new ArrayList<>();

    protected Invoice() {}

    public Invoice(Tenant tenant, StudentProfile student, String code, LocalDate dueOn, String notes) {
        this.tenant = tenant;
        this.student = student;
        this.code = code.strip().toUpperCase();
        this.dueOn = dueOn;
        this.notes = notes == null || notes.isBlank() ? null : notes.strip();
        this.status = Status.DRAFT;
    }

    public InvoiceItem addCharge(Charge charge) {
        if (status != Status.DRAFT) throw new IllegalStateException("INVOICE_NOT_DRAFT");
        if (!charge.getStudent().getId().equals(student.getId())) throw new IllegalStateException("INVOICE_STUDENT_MISMATCH");
        if (charge.getStatus() == Charge.Status.VOID) throw new IllegalStateException("CHARGE_VOID");
        InvoiceItem item = new InvoiceItem(tenant, this, charge, items.size() + 1);
        items.add(item);
        return item;
    }

    public void issue(LocalDate issuedOn) {
        if (status != Status.DRAFT) throw new IllegalStateException("INVOICE_NOT_DRAFT");
        if (items.isEmpty()) throw new IllegalStateException("INVOICE_EMPTY");
        this.issuedOn = issuedOn;
        this.status = Status.ISSUED;
    }

    public void voidInvoice() {
        if (status == Status.VOID) throw new IllegalStateException("INVOICE_ALREADY_VOID");
        if (status == Status.ISSUED) {
            boolean settled = items.stream().anyMatch(item -> item.getCharge().getStatus() != Charge.Status.OPEN
                    && item.getCharge().getStatus() != Charge.Status.VOID);
            if (settled) throw new IllegalStateException("INVOICE_HAS_ALLOCATIONS");
        }
        this.status = Status.VOID;
    }

    public Tenant getTenant() { return tenant; }
    public StudentProfile getStudent() { return student; }
    public String getCode() { return code; }
    public LocalDate getDueOn() { return dueOn; }
    public LocalDate getIssuedOn() { return issuedOn; }
    public Status getStatus() { return status; }
    public String getNotes() { return notes; }
    public List<InvoiceItem> getItems() { return items; }
}
