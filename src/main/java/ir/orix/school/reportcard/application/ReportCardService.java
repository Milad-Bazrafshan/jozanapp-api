package ir.orix.school.reportcard.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ir.orix.school.academic.domain.AcademicPeriod;
import ir.orix.school.academic.persistence.AcademicPeriodRepository;
import ir.orix.school.assessment.domain.AssessmentResult;
import ir.orix.school.assessment.persistence.AssessmentResultRepository;
import ir.orix.school.attendance.domain.AttendanceRecord;
import ir.orix.school.attendance.persistence.AttendanceRecordRepository;
import ir.orix.school.audit.application.AuditService;
import ir.orix.school.classroom.domain.ClassSection;
import ir.orix.school.classroom.persistence.ClassSectionRepository;
import ir.orix.school.enrollment.domain.Enrollment;
import ir.orix.school.enrollment.persistence.EnrollmentRepository;
import ir.orix.school.identity.persistence.UserAccountRepository;
import ir.orix.school.instructor.application.InstructorPortalService;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.organization.persistence.TenantRepository;
import ir.orix.school.people.domain.StudentProfile;
import ir.orix.school.reportcard.domain.ReportCard;
import ir.orix.school.reportcard.domain.ReportCardLine;
import ir.orix.school.reportcard.domain.ReportCardRun;
import ir.orix.school.reportcard.persistence.ReportCardLineRepository;
import ir.orix.school.reportcard.persistence.ReportCardRepository;
import ir.orix.school.reportcard.persistence.ReportCardRunRepository;
import ir.orix.school.shared.web.ApiException;
import ir.orix.school.shared.web.PrintDocuments;

@Service
public class ReportCardService {
    private final TenantRepository tenants;
    private final AcademicPeriodRepository periods;
    private final ClassSectionRepository sections;
    private final EnrollmentRepository enrollments;
    private final AssessmentResultRepository results;
    private final AttendanceRecordRepository attendance;
    private final ReportCardRunRepository runs;
    private final ReportCardRepository cards;
    private final ReportCardLineRepository lines;
    private final UserAccountRepository accounts;
    private final InstructorPortalService instructorPortal;
    private final AuditService audit;
    private final Clock clock;

    public ReportCardService(TenantRepository tenants, AcademicPeriodRepository periods, ClassSectionRepository sections,
            EnrollmentRepository enrollments, AssessmentResultRepository results, AttendanceRecordRepository attendance,
            ReportCardRunRepository runs, ReportCardRepository cards, ReportCardLineRepository lines,
            UserAccountRepository accounts, InstructorPortalService instructorPortal, AuditService audit, Clock clock) {
        this.tenants = tenants; this.periods = periods; this.sections = sections; this.enrollments = enrollments;
        this.results = results; this.attendance = attendance; this.runs = runs; this.cards = cards; this.lines = lines;
        this.accounts = accounts; this.instructorPortal = instructorPortal; this.audit = audit; this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<RunView> list(Long tenantId) {
        return runs.findAllByTenantIdOrderByCreatedAtDesc(tenantId).stream().map(this::runView).toList();
    }

    @Transactional
    public RunView generate(Long tenantId, Long actorId, Long membershipId, Command command, boolean instructorOnly) {
        if (instructorOnly) instructorPortal.requireSection(tenantId, membershipId, command.classSectionId());
        Tenant tenant = tenants.findById(tenantId).orElseThrow();
        AcademicPeriod period = periods.findByIdAndTenantId(command.academicPeriodId(), tenantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ACADEMIC_PERIOD_NOT_FOUND", "Period was not found."));
        ClassSection section = sections.findByIdAndTenantId(command.classSectionId(), tenantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CLASS_SECTION_NOT_FOUND", "Class was not found."));
        ReportCardRun run = runs.save(new ReportCardRun(tenant, period, section, command.title(), command.includeRank(), accounts.findById(actorId).orElseThrow()));
        List<Enrollment> roster = enrollments.findAllByTenantIdAndClassSectionIdOrderByStudentPersonLastNameAsc(tenantId, section.getId())
                .stream().filter(item -> item.getStatus() == Enrollment.Status.ACTIVE).toList();
        List<ReportCard> created = new ArrayList<>();
        for (Enrollment enrollment : roster) {
            ReportCard card = cards.save(new ReportCard(tenant, run, enrollment.getStudent(), UUID.randomUUID().toString().replace("-", "")));
            rebuild(card);
            created.add(card);
        }
        if (command.includeRank()) applyRanks(created);
        audit.record(tenantId, actorId, "REPORT_CARD_RUN_CREATED", "REPORT_CARD_RUN", run.getId(), Map.of("count", created.size()));
        return runView(run);
    }

    @Transactional
    public RunView finalize(Long tenantId, Long actorId, Long id) {
        ReportCardRun run = runs.findByIdAndTenantId(id, tenantId).orElseThrow(() -> notFound());
        run.finalizeRun();
        cards.findAllByRunIdOrderByStudentPersonLastNameAsc(run.getId()).forEach(ReportCard::finalizeCard);
        audit.record(tenantId, actorId, "REPORT_CARD_RUN_FINALIZED", "REPORT_CARD_RUN", id, Map.of());
        return runView(run);
    }

    @Transactional
    public RunView publish(Long tenantId, Long actorId, Long id) {
        ReportCardRun run = runs.findByIdAndTenantId(id, tenantId).orElseThrow(() -> notFound());
        if (run.getStatus() == ReportCardRun.Status.DRAFT) run.finalizeRun();
        run.publish(Instant.now(clock));
        cards.findAllByRunIdOrderByStudentPersonLastNameAsc(run.getId()).forEach(card -> {
            card.finalizeCard();
            card.publish();
        });
        audit.record(tenantId, actorId, "REPORT_CARD_RUN_PUBLISHED", "REPORT_CARD_RUN", id, Map.of());
        return runView(run);
    }

    @Transactional(readOnly = true)
    public List<CardView> cards(Long tenantId, Long runId) {
        runs.findByIdAndTenantId(runId, tenantId).orElseThrow(() -> notFound());
        return cards.findAllByRunIdOrderByStudentPersonLastNameAsc(runId).stream().map(this::cardView).toList();
    }

    @Transactional(readOnly = true)
    public List<CardView> mine(Long tenantId, Long studentId) {
        return cards.findAllByTenantIdAndStudentIdAndStatusOrderByCreatedAtDesc(tenantId, studentId, ReportCard.Status.PUBLISHED)
                .stream().map(this::cardView).toList();
    }

    @Transactional(readOnly = true)
    public CardView get(Long tenantId, Long id) {
        return cardView(cards.findByIdAndTenantId(id, tenantId).orElseThrow(() -> notFound()));
    }

    @Transactional(readOnly = true)
    public CardView verify(String token) {
        return cardView(cards.findByVerificationToken(token).orElseThrow(() -> notFound()));
    }

    @Transactional(readOnly = true)
    public String printHtml(Long tenantId, Long id) {
        ReportCard card = cards.findByIdAndTenantId(id, tenantId).orElseThrow(() -> notFound());
        return print(card);
    }

    @Transactional(readOnly = true)
    public String printPublic(String token) {
        return print(cards.findByVerificationToken(token).orElseThrow(() -> notFound()));
    }

    private void rebuild(ReportCard card) {
        lines.deleteAllByReportCardId(card.getId());
        StudentProfile student = card.getStudent();
        Long tenantId = card.getTenant().getId();
        List<AssessmentResult> published = results.findAllByTenantIdAndParticipantStudentIdAndStatusOrderByCreatedAtDesc(
                tenantId, student.getId(), AssessmentResult.Status.PUBLISHED);
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal max = BigDecimal.ZERO;
        for (AssessmentResult result : published) {
            lines.save(new ReportCardLine(card.getTenant(), card, "ASSESSMENT", result.getParticipant().getAssessment().getTitle(),
                    result.getTotalScore(), result.getMaxScore(), null));
            total = total.add(result.getTotalScore() == null ? BigDecimal.ZERO : result.getTotalScore());
            max = max.add(result.getMaxScore() == null ? BigDecimal.ZERO : result.getMaxScore());
        }
        List<AttendanceRecord> records = attendance.findAllByTenantIdAndStudentIdOrderByMarkedAtDesc(tenantId, student.getId());
        long absences = records.stream().filter(item -> item.getPresenceStatus() == AttendanceRecord.PresenceStatus.ABSENT).count();
        long marked = records.size();
        BigDecimal percent = marked == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(marked - absences).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(marked), 2, RoundingMode.HALF_UP);
        lines.save(new ReportCardLine(card.getTenant(), card, "ATTENDANCE", "حضور", percent, BigDecimal.valueOf(100), absences + " غیبت"));
        BigDecimal overall = max.signum() == 0 ? BigDecimal.ZERO : total.multiply(BigDecimal.valueOf(20)).divide(max, 2, RoundingMode.HALF_UP);
        card.apply(percent, (int) absences, overall, null);
    }

    private void applyRanks(List<ReportCard> created) {
        List<ReportCard> sorted = created.stream()
                .sorted(Comparator.comparing((ReportCard item) -> item.getOverallScore() == null ? BigDecimal.ZERO : item.getOverallScore()).reversed())
                .toList();
        int rank = 1;
        for (ReportCard card : sorted) {
            card.apply(card.getAttendancePercent(), card.getAbsenceCount() == null ? 0 : card.getAbsenceCount(), card.getOverallScore(), rank++);
        }
    }

    private String print(ReportCard card) {
        StringBuilder body = new StringBuilder();
        body.append("<p class='muted'>").append(PrintDocuments.escape(card.getRun().getClassSection().getOffering().getCourse().getTitle()))
                .append(" · ").append(PrintDocuments.escape(card.getRun().getPeriod().getName())).append("</p>");
        body.append("<h1>کارنامه تحصیلی</h1><p>").append(PrintDocuments.escape(card.getStudent().getPerson().getFullName()))
                .append(" — ").append(PrintDocuments.escape(card.getStudent().getStudentCode())).append("</p>");
        body.append("<table><thead><tr><th>مؤلفه</th><th>نمره</th><th>از</th></tr></thead><tbody>");
        for (ReportCardLine line : lines.findAllByReportCardIdOrderByCreatedAtAsc(card.getId())) {
            body.append("<tr><td>").append(PrintDocuments.escape(line.getTitle())).append("</td><td>")
                    .append(PrintDocuments.money(line.getScore())).append("</td><td>")
                    .append(PrintDocuments.money(line.getMaxScore())).append("</td></tr>");
        }
        body.append("</tbody></table><div class='stamp'><div>نمره کل: <b>")
                .append(PrintDocuments.money(card.getOverallScore())).append("</b>");
        if (card.getRankInClass() != null) body.append("<br/>رتبه کلاس: ").append(card.getRankInClass());
        body.append("<br/>مهر آموزشگاه ").append(PrintDocuments.escape(card.getTenant().getName())).append("</div>");
        body.append("<div class='qr'>QR<br/>").append(card.getVerificationToken(), 0, Math.min(8, card.getVerificationToken().length())).append("</div></div>");
        return PrintDocuments.page("کارنامه", body.toString(), true);
    }

    private RunView runView(ReportCardRun run) {
        return new RunView(run.getId(), run.getTitle(), run.getPeriod().getId(), run.getPeriod().getName(),
                run.getClassSection().getId(), run.getClassSection().getName(), run.getStatus().name(), run.isIncludeRank(),
                run.getPublishedAt(), cards.findAllByRunIdOrderByStudentPersonLastNameAsc(run.getId()).size());
    }

    private CardView cardView(ReportCard card) {
        List<LineView> lineViews = lines.findAllByReportCardIdOrderByCreatedAtAsc(card.getId()).stream()
                .map(line -> new LineView(line.getId(), line.getLineType(), line.getTitle(), line.getScore(), line.getMaxScore(), line.getNote()))
                .toList();
        return new CardView(card.getId(), card.getRun().getId(), card.getRun().getTitle(), card.getStudent().getId(),
                card.getStudent().getStudentCode(), card.getStudent().getPerson().getFullName(),
                card.getRun().getClassSection().getName(), card.getRun().getPeriod().getName(), card.getStatus().name(),
                card.getAttendancePercent(), card.getAbsenceCount(), card.getOverallScore(), card.getRankInClass(),
                card.getVerificationToken(), lineViews);
    }

    private ApiException notFound() { return new ApiException(HttpStatus.NOT_FOUND, "REPORT_CARD_NOT_FOUND", "Report card was not found."); }

    public record Command(Long academicPeriodId, Long classSectionId, String title, boolean includeRank) {}
    public record RunView(Long id, String title, Long academicPeriodId, String periodName, Long classSectionId, String className,
            String status, boolean includeRank, Instant publishedAt, int cardCount) {}
    public record LineView(Long id, String lineType, String title, BigDecimal score, BigDecimal maxScore, String note) {}
    public record CardView(Long id, Long runId, String runTitle, Long studentId, String studentCode, String studentName,
            String className, String periodName, String status, BigDecimal attendancePercent, Integer absenceCount,
            BigDecimal overallScore, Integer rankInClass, String verificationToken, List<LineView> lines) {}
}
