package ir.orix.school.card.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ir.orix.school.audit.application.AuditService;
import ir.orix.school.card.domain.StudentCard;
import ir.orix.school.card.persistence.StudentCardRepository;
import ir.orix.school.classroom.domain.ClassSection;
import ir.orix.school.classroom.persistence.ClassSectionRepository;
import ir.orix.school.enrollment.domain.Enrollment;
import ir.orix.school.enrollment.persistence.EnrollmentRepository;
import ir.orix.school.files.domain.StoredFile;
import ir.orix.school.files.persistence.StoredFileRepository;
import ir.orix.school.organization.persistence.TenantRepository;
import ir.orix.school.people.domain.StudentProfile;
import ir.orix.school.people.persistence.StudentProfileRepository;
import ir.orix.school.shared.web.ApiException;
import ir.orix.school.shared.web.PrintDocuments;

@Service
public class StudentCardService {
    private final TenantRepository tenants;
    private final StudentCardRepository cards;
    private final StudentProfileRepository students;
    private final ClassSectionRepository sections;
    private final EnrollmentRepository enrollments;
    private final StoredFileRepository files;
    private final AuditService audit;

    public StudentCardService(TenantRepository tenants, StudentCardRepository cards, StudentProfileRepository students,
            ClassSectionRepository sections, EnrollmentRepository enrollments, StoredFileRepository files, AuditService audit) {
        this.tenants = tenants; this.cards = cards; this.students = students; this.sections = sections;
        this.enrollments = enrollments; this.files = files; this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<CardView> list(Long tenantId) { return cards.findAllByTenantIdOrderByCreatedAtDesc(tenantId).stream().map(this::view).toList(); }

    @Transactional
    public List<CardView> issueClass(Long tenantId, Long actorId, Long classSectionId, LocalDate validUntil, Long photoFileId) {
        ClassSection section = sections.findByIdAndTenantId(classSectionId, tenantId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CLASS_SECTION_NOT_FOUND", "Class was not found."));
        StoredFile photo = photoFileId == null ? null : files.findByIdAndTenantId(photoFileId, tenantId).orElse(null);
        List<CardView> created = new java.util.ArrayList<>();
        for (Enrollment enrollment : enrollments.findAllByTenantIdAndClassSectionIdOrderByStudentPersonLastNameAsc(tenantId, classSectionId)) {
            if (enrollment.getStatus() != Enrollment.Status.ACTIVE) continue;
            created.add(issue(tenantId, actorId, enrollment.getStudent(), section, validUntil, photo));
        }
        return created;
    }

    @Transactional
    public CardView issueOne(Long tenantId, Long actorId, Long studentId, Long classSectionId, LocalDate validUntil, Long photoFileId) {
        StudentProfile student = students.findByIdAndTenantId(studentId, tenantId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "STUDENT_NOT_FOUND", "Student was not found."));
        ClassSection section = classSectionId == null ? null : sections.findByIdAndTenantId(classSectionId, tenantId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CLASS_SECTION_NOT_FOUND", "Class was not found."));
        StoredFile photo = photoFileId == null ? null : files.findByIdAndTenantId(photoFileId, tenantId).orElse(null);
        return issue(tenantId, actorId, student, section, validUntil, photo);
    }

    @Transactional(readOnly = true)
    public List<CardView> forStudent(Long tenantId, Long studentId) {
        return cards.findAllByTenantIdAndStudentIdOrderByCreatedAtDesc(tenantId, studentId).stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public CardView verify(String token) {
        return view(cards.findByVerificationToken(token).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CARD_NOT_FOUND", "Card was not found.")));
    }

    @Transactional(readOnly = true)
    public String print(Long tenantId, Long studentId) {
        StudentCard card = cards.findAllByTenantIdAndStudentIdOrderByCreatedAtDesc(tenantId, studentId).stream().findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CARD_NOT_FOUND", "Card was not found."));
        return printHtml(card);
    }

    @Transactional(readOnly = true)
    public String printPublic(String token) {
        return printHtml(cards.findByVerificationToken(token).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CARD_NOT_FOUND", "Card was not found.")));
    }

    private CardView issue(Long tenantId, Long actorId, StudentProfile student, ClassSection section, LocalDate validUntil, StoredFile photo) {
        StudentCard card = cards.save(new StudentCard(tenants.findById(tenantId).orElseThrow(), student, section, validUntil,
                UUID.randomUUID().toString().replace("-", ""), photo));
        audit.record(tenantId, actorId, "STUDENT_CARD_ISSUED", "STUDENT_CARD", card.getId(), Map.of("student", student.getStudentCode()));
        return view(card);
    }

    private String printHtml(StudentCard card) {
        StringBuilder body = new StringBuilder("<h1>کارت دانش‌آموزی</h1>");
        body.append("<p><b>").append(PrintDocuments.escape(card.getStudent().getPerson().getFullName())).append("</b></p>");
        body.append("<p>کد: ").append(PrintDocuments.escape(card.getStudent().getStudentCode())).append("</p>");
        if (card.getClassSection() != null) body.append("<p>کلاس: ").append(PrintDocuments.escape(card.getClassSection().getName())).append("</p>");
        body.append("<p>اعتبار تا ").append(PrintDocuments.day(card.getValidUntil())).append("</p>");
        body.append("<p class='muted'>").append(PrintDocuments.escape(card.getTenant().getName())).append("</p>");
        body.append("<div class='qr'>QR<br/>/verify/student-card/").append(card.getVerificationToken()).append("</div>");
        return PrintDocuments.page("کارت دانش‌آموزی", body.toString(), true);
    }

    private CardView view(StudentCard card) {
        return new CardView(card.getId(), card.getStudent().getId(), card.getStudent().getStudentCode(), card.getStudent().getPerson().getFullName(),
                card.getClassSection() == null ? null : card.getClassSection().getName(), card.getValidUntil(), card.getVerificationToken(),
                card.getPhoto() == null ? null : card.getPhoto().getId(), card.getStatus().name());
    }

    public record CardView(Long id, Long studentId, String studentCode, String studentName, String className, LocalDate validUntil,
            String verificationToken, Long photoFileId, String status) {}
}
