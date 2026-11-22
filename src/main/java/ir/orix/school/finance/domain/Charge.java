package ir.orix.school.finance.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.persistence.*;
import ir.orix.school.classroom.domain.ClassSection;
import ir.orix.school.enrollment.domain.Enrollment;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.people.domain.StudentProfile;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity
@Table(name = "charge", schema = "school")
public class Charge extends AuditedEntity {
    public enum Type { TUITION, ENROLLMENT_FEE, MATERIAL, OTHER }
    public enum Status { OPEN, PARTIALLY_PAID, PAID, VOID }

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tenant_id") private Tenant tenant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "student_id") private StudentProfile student;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "class_section_id") private ClassSection classSection;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "enrollment_id") private Enrollment enrollment;
    @Column(nullable = false, length = 60) private String code;
    @Enumerated(EnumType.STRING) @Column(name = "charge_type", nullable = false, length = 40) private Type type;
    @Column(nullable = false, length = 200) private String title;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal amount;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal discount;
    @Column(name = "due_on", nullable = false) private LocalDate dueOn;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status;
    @Column(length = 500) private String notes;

    protected Charge() {}

    public Charge(Tenant tenant, StudentProfile student, ClassSection classSection, Enrollment enrollment, String code,
            Type type, String title, BigDecimal amount, BigDecimal discount, LocalDate dueOn, String notes) {
        this.tenant = tenant;
        this.student = student;
        this.classSection = classSection;
        this.enrollment = enrollment;
        this.code = code.strip().toUpperCase();
        this.type = type;
        this.title = title.strip();
        this.amount = amount;
        this.discount = discount == null ? BigDecimal.ZERO : discount;
        this.dueOn = dueOn;
        this.notes = blankToNull(notes);
        this.status = Status.OPEN;
        validateAmounts();
    }

    public BigDecimal netAmount() {
        return amount.subtract(discount);
    }

    public void applySettlement(BigDecimal allocated) {
        if (status == Status.VOID) {
            throw new IllegalStateException("CHARGE_VOID");
        }
        BigDecimal settled = allocated == null ? BigDecimal.ZERO : allocated;
        if (settled.signum() < 0) {
            throw new IllegalStateException("NEGATIVE_ALLOCATION");
        }
        if (settled.compareTo(netAmount()) > 0) {
            throw new IllegalStateException("ALLOCATION_EXCEEDS_CHARGE");
        }
        if (settled.signum() == 0) status = Status.OPEN;
        else if (settled.compareTo(netAmount()) < 0) status = Status.PARTIALLY_PAID;
        else status = Status.PAID;
    }

    public void voidCharge() {
        if (status == Status.VOID) throw new IllegalStateException("CHARGE_ALREADY_VOID");
        if (status != Status.OPEN) throw new IllegalStateException("CHARGE_HAS_ALLOCATIONS");
        status = Status.VOID;
    }

    private void validateAmounts() {
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("INVALID_CHARGE_AMOUNT");
        if (discount.signum() < 0 || discount.compareTo(amount) > 0) throw new IllegalArgumentException("INVALID_CHARGE_DISCOUNT");
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.strip();
    }

    public Tenant getTenant() { return tenant; }
    public StudentProfile getStudent() { return student; }
    public ClassSection getClassSection() { return classSection; }
    public Enrollment getEnrollment() { return enrollment; }
    public String getCode() { return code; }
    public Type getType() { return type; }
    public String getTitle() { return title; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getDiscount() { return discount; }
    public LocalDate getDueOn() { return dueOn; }
    public Status getStatus() { return status; }
    public String getNotes() { return notes; }
}
