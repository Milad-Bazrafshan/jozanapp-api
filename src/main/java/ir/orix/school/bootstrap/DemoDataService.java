package ir.orix.school.bootstrap;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ir.orix.school.academic.application.AcademicService;
import ir.orix.school.academic.domain.AcademicPeriod;
import ir.orix.school.academic.domain.AcademicProgram;
import ir.orix.school.assessment.application.AssessmentService;
import ir.orix.school.assessment.domain.Assessment;
import ir.orix.school.assessment.domain.Question;
import ir.orix.school.attendance.application.AttendanceService;
import ir.orix.school.attendance.domain.AttendanceRecord;
import ir.orix.school.classroom.application.ClassroomService;
import ir.orix.school.classroom.domain.ClassInstructor;
import ir.orix.school.counseling.application.CounselingService;
import ir.orix.school.card.application.StudentCardService;
import ir.orix.school.classroom.domain.ClassSection;
import ir.orix.school.enrollment.application.EnrollmentService;
import ir.orix.school.finance.application.FinanceService;
import ir.orix.school.finance.application.WalletService;
import ir.orix.school.finance.domain.Charge;
import ir.orix.school.finance.domain.Payment;
import ir.orix.school.finance.persistence.ChargeRepository;
import ir.orix.school.organization.domain.AccessGrant;
import ir.orix.school.organization.domain.AccessRole;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.organization.domain.TenantMembership;
import ir.orix.school.organization.persistence.AccessGrantRepository;
import ir.orix.school.organization.persistence.AccessRoleRepository;
import ir.orix.school.organization.persistence.BranchRepository;
import ir.orix.school.organization.persistence.TenantMembershipRepository;
import ir.orix.school.organization.persistence.TenantRepository;
import ir.orix.school.people.application.PeopleService;
import ir.orix.school.people.domain.StudentGuardian;
import ir.orix.school.people.persistence.StudentProfileRepository;
import ir.orix.school.scheduling.application.SchedulingService;
import ir.orix.school.scheduling.domain.AcademicLocation;
import ir.orix.school.scheduling.domain.ScheduleRule;

@Service
public class DemoDataService {
    private static final Logger log = LoggerFactory.getLogger(DemoDataService.class);

    private final BootstrapProperties bootstrap;
    private final TenantRepository tenants;
    private final TenantMembershipRepository memberships;
    private final BranchRepository branches;
    private final StudentProfileRepository students;
    private final PeopleService people;
    private final AcademicService academic;
    private final ClassroomService classroom;
    private final EnrollmentService enrollment;
    private final SchedulingService scheduling;
    private final AttendanceService attendance;
    private final AssessmentService assessments;
    private final FinanceService finance;
    private final WalletService wallets;
    private final ChargeRepository charges;
    private final AccessRoleRepository roles;
    private final AccessGrantRepository grants;
    private final CounselingService counseling;
    private final StudentCardService studentCards;
    private final Clock clock;

    public DemoDataService(BootstrapProperties bootstrap, TenantRepository tenants,
            TenantMembershipRepository memberships, BranchRepository branches, StudentProfileRepository students,
            PeopleService people, AcademicService academic, ClassroomService classroom, EnrollmentService enrollment,
            SchedulingService scheduling, AttendanceService attendance, AssessmentService assessments,
            FinanceService finance, WalletService wallets, ChargeRepository charges, AccessRoleRepository roles,
            AccessGrantRepository grants, CounselingService counseling, StudentCardService studentCards, Clock clock) {
        this.bootstrap = bootstrap;
        this.tenants = tenants;
        this.memberships = memberships;
        this.branches = branches;
        this.students = students;
        this.people = people;
        this.academic = academic;
        this.classroom = classroom;
        this.enrollment = enrollment;
        this.scheduling = scheduling;
        this.attendance = attendance;
        this.assessments = assessments;
        this.finance = finance;
        this.wallets = wallets;
        this.charges = charges;
        this.roles = roles;
        this.grants = grants;
        this.counseling = counseling;
        this.studentCards = studentCards;
        this.clock = clock;
    }

    @Transactional
    public void initialize(DemoDataProperties config) {
        if (!config.enabled()) return;
        String slug = (config.tenantSlug() == null || config.tenantSlug().isBlank()) ? bootstrap.tenantSlug() : config.tenantSlug();
        Tenant tenant = tenants.findBySlugIgnoreCase(slug).orElse(null);
        if (tenant == null) {
            log.info("Demo data skipped; tenant '{}' was not found", slug);
            return;
        }
        String marker = (config.markerStudentCode() == null || config.markerStudentCode().isBlank())
                ? "DEMO-STU-001" : config.markerStudentCode();
        if (students.existsByTenantIdAndStudentCodeIgnoreCase(tenant.getId(), marker)) {
            log.info("Demo data already present for tenant '{}'", slug);
            TenantMembership admin = memberships.findByTenantIdAndUserAccountEmailIgnoreCase(tenant.getId(), bootstrap.adminEmail())
                    .orElseThrow(() -> new IllegalStateException("Bootstrap admin membership was not found for demo data seed"));
            seedFinance(tenant, admin);
            grantInstructorRole(tenant);
            ensureCounselorDemo(tenant, admin, config.password() == null || config.password().isBlank() ? "change-me-now" : config.password());
            ensureDemoStudentCards(tenant, admin);
            tenant.updatePlatformSettings(true, true, "DEMO", "GENERIC_LINK", "https://meet.example.local/room/", null);
            return;
        }
        String password = config.password() == null || config.password().isBlank() ? "change-me-now" : config.password();
        if (password.length() < 10) {
            throw new IllegalStateException("DEMO_DATA_PASSWORD must contain at least 10 characters");
        }
        TenantMembership admin = memberships.findByTenantIdAndUserAccountEmailIgnoreCase(tenant.getId(), bootstrap.adminEmail())
                .orElseThrow(() -> new IllegalStateException("Bootstrap admin membership was not found for demo data seed"));
        seed(tenant, admin, marker, password);
        log.info("Demo data seeded for tenant '{}' (students, guardians, classes, schedule, attendance, quiz, finance)", slug);
    }

    private void seed(Tenant tenant, TenantMembership admin, String marker, String password) {
        Long tenantId = tenant.getId();
        Long actorId = admin.getUserAccount().getId();
        Long membershipId = admin.getId();
        ZoneId zone = ZoneId.of(tenant.getTimezone());
        LocalDate today = LocalDate.now(clock.withZone(zone));
        Instant now = Instant.now(clock);
        Long branchId = branches.findAllByTenantIdOrderByName(tenantId).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Tenant has no branch to seed classroom data"))
                .getId();

        PeopleService.InstructorView instructor = people.createInstructor(tenantId, actorId, new PeopleService.CreateInstructor(
                "DEMO-INS-001", "استاد", "زبان انگلیسی",
                person("مینا", "رضایی", "0011111111", "09120000011", "instructor@example.com", password)));
        PeopleService.StudentView sara = people.createStudent(tenantId, actorId, new PeopleService.CreateStudent(
                marker, today.minusDays(20),
                person("سارا", "احمدی", "0022222221", "09120000021", "student1@example.com", password)));
        PeopleService.StudentView reza = people.createStudent(tenantId, actorId, new PeopleService.CreateStudent(
                "DEMO-STU-002", today.minusDays(18),
                person("رضا", "کریمی", "0022222222", "09120000022", "student2@example.com", password)));
        PeopleService.StudentView nila = people.createStudent(tenantId, actorId, new PeopleService.CreateStudent(
                "DEMO-STU-003", today.minusDays(16),
                person("نیلا", "محمدی", "0022222223", "09120000023", "student3@example.com", password)));
        PeopleService.GuardianView father = people.createGuardian(tenantId, actorId, new PeopleService.CreateGuardian(
                "کارمند", person("حسین", "احمدی", "0033333331", "09120000031", "guardian@example.com", password)));
        PeopleService.GuardianView mother = people.createGuardian(tenantId, actorId, new PeopleService.CreateGuardian(
                "پزشک", person("مریم", "محمدی", "0033333332", "09120000032", "guardian2@example.com", password)));
        people.linkGuardian(tenantId, actorId, new PeopleService.LinkGuardian(
                sara.id(), father.id(), StudentGuardian.RelationshipType.FATHER, true, true, true, true, true, today.minusDays(20)));
        people.linkGuardian(tenantId, actorId, new PeopleService.LinkGuardian(
                reza.id(), father.id(), StudentGuardian.RelationshipType.FATHER, true, true, true, true, true, today.minusDays(18)));
        people.linkGuardian(tenantId, actorId, new PeopleService.LinkGuardian(
                nila.id(), mother.id(), StudentGuardian.RelationshipType.MOTHER, true, true, true, true, true, today.minusDays(16)));

        AcademicService.PeriodView period = academic.createPeriod(tenantId, actorId, new AcademicService.PeriodCommand(
                "DEMO-1405", "ترم پاییز ۱۴۰۵", AcademicPeriod.Type.SEMESTER, today.minusMonths(1), today.plusMonths(5)));
        AcademicService.ProgramView program = academic.createProgram(tenantId, actorId, new AcademicService.ProgramCommand(
                "ENG-A1", "زبان انگلیسی سطح A1", AcademicProgram.Type.LANGUAGE_LEVEL, "برنامه نمونه برای شروع زبان‌آموزان"));
        AcademicService.CourseView foundation = academic.createCourse(tenantId, actorId, new AcademicService.CourseCommand(
                "ENG-A1-CORE", "زبان پایه", "واژگان، گرامر و مهارت شنیداری سطح مبتدی", 90));
        AcademicService.CourseView conversation = academic.createCourse(tenantId, actorId, new AcademicService.CourseCommand(
                "ENG-A1-CONV", "مکالمه انگلیسی", "تمرین گفتگو برای موقعیت‌های روزمره", 60));
        academic.attachCourse(tenantId, actorId, new AcademicService.AttachCommand(program.id(), foundation.id(), 1, true));
        academic.attachCourse(tenantId, actorId, new AcademicService.AttachCommand(program.id(), conversation.id(), 2, true));

        ClassroomService.OfferingView foundationOffering = classroom.createOffering(tenantId, actorId, new ClassroomService.OfferingCommand(
                period.id(), foundation.id(), branchId, "OFF-A1-F26", today.minusDays(30), today.plusMonths(4)));
        ClassroomService.OfferingView conversationOffering = classroom.createOffering(tenantId, actorId, new ClassroomService.OfferingCommand(
                period.id(), conversation.id(), branchId, "OFF-CONV-F26", today.minusDays(30), today.plusMonths(4)));
        ClassroomService.SectionView foundationClass = classroom.createSection(tenantId, actorId, new ClassroomService.SectionCommand(
                foundationOffering.id(), "CLS-A1-1", "کلاس الف زبان پایه", 20, ClassSection.DeliveryMode.IN_PERSON));
        ClassroomService.SectionView conversationClass = classroom.createSection(tenantId, actorId, new ClassroomService.SectionCommand(
                conversationOffering.id(), "CLS-CONV-1", "کلاس مکالمه الف", 12, ClassSection.DeliveryMode.HYBRID));
        classroom.assignInstructor(tenantId, actorId, new ClassroomService.InstructorCommand(
                foundationClass.id(), instructor.id(), ClassInstructor.Role.PRIMARY_INSTRUCTOR, true, today.minusDays(14), null));
        classroom.assignInstructor(tenantId, actorId, new ClassroomService.InstructorCommand(
                conversationClass.id(), instructor.id(), ClassInstructor.Role.PRIMARY_INSTRUCTOR, true, today.minusDays(14), null));

        LocalDate startedOn = today.minusDays(10);
        for (PeopleService.StudentView student : List.of(sara, reza, nila)) {
            enrollment.enroll(tenantId, actorId, new EnrollmentService.Command(student.id(), foundationClass.id(), startedOn, false, "demo seed"));
        }
        enrollment.enroll(tenantId, actorId, new EnrollmentService.Command(sara.id(), conversationClass.id(), startedOn, false, "demo seed"));
        enrollment.enroll(tenantId, actorId, new EnrollmentService.Command(reza.id(), conversationClass.id(), startedOn, false, "demo seed"));

        SchedulingService.LocationView room101 = scheduling.createLocation(tenantId, actorId, new SchedulingService.LocationCommand(
                branchId, "ROOM-101", "کلاس ۱۰۱", AcademicLocation.Type.CLASSROOM, 20));
        SchedulingService.LocationView room102 = scheduling.createLocation(tenantId, actorId, new SchedulingService.LocationCommand(
                branchId, "ROOM-102", "کلاس ۱۰۲", AcademicLocation.Type.CLASSROOM, 12));
        DayOfWeek foundationDay = today.getDayOfWeek();
        DayOfWeek conversationDay = today.plusDays(2).getDayOfWeek();
        SchedulingService.RuleView foundationRule = scheduling.createRule(tenantId, actorId, new SchedulingService.RuleCommand(
                foundationClass.id(), "SCH-A1-1", ScheduleRule.RecurrenceType.WEEKLY, today, today.plusDays(60),
                foundationDay, LocalTime.of(16, 0), LocalTime.of(17, 30), tenant.getTimezone(), 1, room101.id(), Set.of(instructor.id())));
        SchedulingService.RuleView conversationRule = scheduling.createRule(tenantId, actorId, new SchedulingService.RuleCommand(
                conversationClass.id(), "SCH-CONV-1", ScheduleRule.RecurrenceType.WEEKLY, today, today.plusDays(60),
                conversationDay, LocalTime.of(18, 0), LocalTime.of(19, 0), tenant.getTimezone(), 1, room102.id(), Set.of(instructor.id())));
        List<SchedulingService.SessionView> foundationSessions = scheduling.generate(tenantId, actorId, foundationRule.id(), today.plusDays(60));
        scheduling.generate(tenantId, actorId, conversationRule.id(), today.plusDays(60));

        AssessmentService.QuestionView choice = assessments.createQuestion(tenantId, actorId, new AssessmentService.QuestionCommand(
                foundation.id(), "Q-DEMO-1", Question.Type.SINGLE_CHOICE, "معنی Hello چیست؟",
                List.of("خداحافظ", "سلام", "متشکرم", "ببخشید"), List.of("سلام"), "Hello یعنی سلام.", BigDecimal.valueOf(2)));
        AssessmentService.QuestionView truth = assessments.createQuestion(tenantId, actorId, new AssessmentService.QuestionCommand(
                foundation.id(), "Q-DEMO-2", Question.Type.TRUE_FALSE, "جمله I am a student از نظر گرامری درست است.",
                List.of(), List.of("TRUE"), "جمله درست است.", BigDecimal.valueOf(2)));
        AssessmentService.QuestionView shortAnswer = assessments.createQuestion(tenantId, actorId, new AssessmentService.QuestionCommand(
                foundation.id(), "Q-DEMO-3", Question.Type.SHORT_ANSWER, "معادل انگلیسی «سلام» را بنویسید.",
                List.of(), List.of("hello"), "پاسخ مورد انتظار: hello", BigDecimal.valueOf(2)));
        AssessmentService.AssessmentView quiz = assessments.createAssessment(tenantId, actorId, new AssessmentService.AssessmentCommand(
                foundation.id(), "DEMO-QUIZ-1", "آزمونک آشنایی با زبان پایه", Assessment.Type.QUIZ,
                now.minusSeconds(3600), now.plusSeconds(14L * 24 * 3600), 20, 2, false, 0));
        quiz = assessments.addSection(tenantId, actorId, quiz.id(), new AssessmentService.SectionCommand(
                "سؤالات اصلی", "با دقت پاسخ دهید.", 1));
        Long sectionId = quiz.sections().get(0).id();
        assessments.addQuestion(tenantId, actorId, quiz.id(), new AssessmentService.AssessmentQuestionCommand(
                sectionId, choice.currentVersionId(), BigDecimal.valueOf(2), 1));
        assessments.addQuestion(tenantId, actorId, quiz.id(), new AssessmentService.AssessmentQuestionCommand(
                sectionId, truth.currentVersionId(), BigDecimal.valueOf(2), 2));
        assessments.addQuestion(tenantId, actorId, quiz.id(), new AssessmentService.AssessmentQuestionCommand(
                sectionId, shortAnswer.currentVersionId(), BigDecimal.valueOf(2), 3));

        Long assessmentId = quiz.id();
        Long classId = foundationClass.id();
        runAs(admin, () -> {
            assessments.addTarget(tenantId, membershipId, actorId, assessmentId, classId);
            assessments.publish(tenantId, membershipId, actorId, assessmentId);
            foundationSessions.stream()
                    .filter(session -> !session.sessionDate().isAfter(today))
                    .findFirst()
                    .ifPresent(session -> attendance.save(tenantId, membershipId, actorId, session.id(), new AttendanceService.SaveCommand(
                            true, null, List.of(
                                    new AttendanceService.RecordCommand(sara.id(), AttendanceRecord.PresenceStatus.PRESENT, ClassSection.DeliveryMode.IN_PERSON, null, null),
                                    new AttendanceService.RecordCommand(reza.id(), AttendanceRecord.PresenceStatus.LATE, ClassSection.DeliveryMode.IN_PERSON, 8, "تأخیر کوتاه"),
                                    new AttendanceService.RecordCommand(nila.id(), AttendanceRecord.PresenceStatus.ABSENT, ClassSection.DeliveryMode.IN_PERSON, null, null)
                            ))));
        });
        seedFinance(tenant, admin);
        grantInstructorRole(tenant);
        ensureCounselorDemo(tenant, admin, password);
        ensureDemoStudentCards(tenant, admin);
        tenant.updatePlatformSettings(true, true, "DEMO", "GENERIC_LINK", "https://meet.example.local/room/", null);
    }

    private void seedFinance(Tenant tenant, TenantMembership admin) {
        Long tenantId = tenant.getId();
        if (!charges.existsByTenantIdAndCodeIgnoreCase(tenantId, "CHG-DEMO-001")) {
        Long actorId = admin.getUserAccount().getId();
        ZoneId zone = ZoneId.of(tenant.getTimezone());
        LocalDate today = LocalDate.now(clock.withZone(zone));
        PeopleService.Directory directory = people.directory(tenantId);
        PeopleService.StudentView sara = directory.students().stream().filter(item -> "DEMO-STU-001".equalsIgnoreCase(item.code())).findFirst().orElse(null);
        PeopleService.StudentView reza = directory.students().stream().filter(item -> "DEMO-STU-002".equalsIgnoreCase(item.code())).findFirst().orElse(null);
        PeopleService.StudentView nila = directory.students().stream().filter(item -> "DEMO-STU-003".equalsIgnoreCase(item.code())).findFirst().orElse(null);
        if (sara == null || reza == null || nila == null) {
            log.info("Demo finance skipped; demo students were not found");
        } else {
        Long foundationClassId = classroom.catalog(tenantId).sections().stream()
                .filter(section -> "CLS-A1-1".equalsIgnoreCase(section.code()))
                .map(ClassroomService.SectionView::id)
                .findFirst()
                .orElse(null);
        Long conversationClassId = classroom.catalog(tenantId).sections().stream()
                .filter(section -> "CLS-CONV-1".equalsIgnoreCase(section.code()))
                .map(ClassroomService.SectionView::id)
                .findFirst()
                .orElse(null);
        FinanceService.ChargeView saraFoundation = finance.createCharge(tenantId, actorId, new FinanceService.ChargeCommand(
                sara.id(), foundationClassId, null, Charge.Type.TUITION, "شهریه زبان پایه",
                new BigDecimal("8000000.00"), BigDecimal.ZERO, today.plusDays(20), "قسط اول ترم پاییز", "CHG-DEMO-001"));
        FinanceService.ChargeView saraConversation = finance.createCharge(tenantId, actorId, new FinanceService.ChargeCommand(
                sara.id(), conversationClassId, null, Charge.Type.TUITION, "شهریه مکالمه",
                new BigDecimal("4000000.00"), new BigDecimal("400000.00"), today.plusDays(40), "تخفیف ثبت‌نام همزمان", "CHG-DEMO-002"));
        FinanceService.ChargeView rezaTuition = finance.createCharge(tenantId, actorId, new FinanceService.ChargeCommand(
                reza.id(), foundationClassId, null, Charge.Type.TUITION, "شهریه زبان پایه",
                new BigDecimal("8000000.00"), BigDecimal.ZERO, today.plusDays(20), null, "CHG-DEMO-003"));
        finance.createCharge(tenantId, actorId, new FinanceService.ChargeCommand(
                nila.id(), foundationClassId, null, Charge.Type.TUITION, "شهریه زبان پایه",
                new BigDecimal("8000000.00"), BigDecimal.ZERO, today.minusDays(5), "سررسید گذشته برای گزارش بدهکاران", "CHG-DEMO-004"));
        FinanceService.InvoiceView saraInvoice = finance.createInvoice(tenantId, actorId, new FinanceService.InvoiceCommand(
                sara.id(), today.plusDays(20), "فاکتور ترم پاییز سارا", List.of(saraFoundation.id(), saraConversation.id()), "INV-DEMO-001"));
        finance.issueInvoice(tenantId, actorId, saraInvoice.id());
        FinanceService.InvoiceView rezaInvoice = finance.createInvoice(tenantId, actorId, new FinanceService.InvoiceCommand(
                reza.id(), today.plusDays(20), "فاکتور ترم پاییز رضا", List.of(rezaTuition.id()), "INV-DEMO-002"));
        finance.issueInvoice(tenantId, actorId, rezaInvoice.id());
        finance.recordPayment(tenantId, actorId, new FinanceService.PaymentCommand(
                sara.id(), new BigDecimal("6000000.00"), Payment.Method.CASH, Payment.Provider.INTERNAL, "DEMO-PAY-SARA",
                Instant.now(clock), "پرداخت نقدی میز پذیرش", "REC-DEMO-001",
                List.of(new FinanceService.AllocationCommand(saraFoundation.id(), new BigDecimal("6000000.00")))));
        finance.recordPayment(tenantId, actorId, new FinanceService.PaymentCommand(
                reza.id(), new BigDecimal("8000000.00"), Payment.Method.BANK_TRANSFER, Payment.Provider.INTERNAL, "DEMO-PAY-REZA",
                Instant.now(clock), "واریز کامل شهریه", "REC-DEMO-002",
                List.of(new FinanceService.AllocationCommand(rezaTuition.id(), new BigDecimal("8000000.00")))));
        log.info("Demo finance seeded for tenant '{}'", tenant.getSlug());
        }
        }
        seedWallet(tenant, admin);
        seedOpenWalletCharge(tenant, admin);
    }

    private void seedOpenWalletCharge(Tenant tenant, TenantMembership admin) {
        Long tenantId = tenant.getId();
        if (charges.existsByTenantIdAndCodeIgnoreCase(tenantId, "CHG-DEMO-005")) {
            return;
        }
        PeopleService.Directory directory = people.directory(tenantId);
        PeopleService.StudentView sara = directory.students().stream()
                .filter(item -> "DEMO-STU-001".equalsIgnoreCase(item.code()))
                .findFirst()
                .orElse(null);
        if (sara == null) return;
        ZoneId zone = ZoneId.of(tenant.getTimezone());
        LocalDate today = LocalDate.now(clock.withZone(zone));
        Long conversationClassId = classroom.catalog(tenantId).sections().stream()
                .filter(section -> "CLS-CONV-1".equalsIgnoreCase(section.code()))
                .map(ClassroomService.SectionView::id)
                .findFirst()
                .orElse(null);
        finance.createCharge(tenantId, admin.getUserAccount().getId(), new FinanceService.ChargeCommand(
                sara.id(), conversationClassId, null, Charge.Type.OTHER, "فوق‌برنامه و کارگاه",
                new BigDecimal("2500000.00"), BigDecimal.ZERO, today.plusDays(12), "قابل پرداخت از کیف پول ولی", "CHG-DEMO-005"));
        log.info("Demo open wallet charge seeded for tenant '{}'", tenant.getSlug());
    }

    private void seedWallet(Tenant tenant, TenantMembership admin) {
        PeopleService.GuardianView father = people.directory(tenant.getId()).guardians().stream()
                .filter(item -> "حسین".equals(item.person().firstName()) && "احمدی".equals(item.person().lastName()))
                .findFirst()
                .orElse(null);
        if (father == null) {
            log.info("Demo wallet skipped; father guardian was not found");
            return;
        }
        wallets.ensureDemoCredit(tenant.getId(), admin.getUserAccount().getId(), father.id(), new BigDecimal("10000000.00"));
        log.info("Demo wallet ready for tenant '{}'", tenant.getSlug());
    }

    private void grantInstructorRole(Tenant tenant) {
        TenantMembership instructorMembership = memberships.findByTenantIdAndUserAccountEmailIgnoreCase(tenant.getId(), "instructor@example.com")
                .orElse(null);
        if (instructorMembership == null) return;
        AccessRole role = roles.findAllByTenantIdOrderByName(tenant.getId()).stream()
                .filter(item -> "INSTRUCTOR".equalsIgnoreCase(item.getCode()))
                .findFirst()
                .orElse(null);
        if (role == null) return;
        boolean granted = grants.findAllByMembershipId(instructorMembership.getId()).stream()
                .anyMatch(item -> "INSTRUCTOR".equalsIgnoreCase(item.getRole().getCode()));
        if (!granted) {
            grants.save(new AccessGrant(tenant, instructorMembership, role, AccessGrant.ScopeType.TENANT, null, instructorMembership.getUserAccount()));
            log.info("Granted INSTRUCTOR role to instructor@example.com");
        }
    }

    private void ensureCounselorDemo(Tenant tenant, TenantMembership admin, String password) {
        PeopleService.StudentView sara = people.directory(tenant.getId()).students().stream()
                .filter(item -> "DEMO-STU-001".equalsIgnoreCase(item.code()))
                .findFirst()
                .orElse(null);
        if (sara == null) {
            log.info("Counselor demo skipped; DEMO-STU-001 not found");
            return;
        }
        ensureCounselorLogin(tenant, admin, password, "counselor@example.com", "دانیال", "مشاور", "0044444441", "DEMO-COUN-JR", "COUNSELOR_JUNIOR");
        ensureCounselorLogin(tenant, admin, password, "counselor.senior@example.com", "شیدا", "ارشد", "0044444442", "DEMO-COUN-SR", "COUNSELOR_SENIOR");
        TenantMembership junior = memberships.findByTenantIdAndUserAccountEmailIgnoreCase(tenant.getId(), "counselor@example.com").orElse(null);
        TenantMembership senior = memberships.findByTenantIdAndUserAccountEmailIgnoreCase(tenant.getId(), "counselor.senior@example.com").orElse(null);
        if (junior == null || senior == null) return;
        runAs(admin, () -> {
            if (counseling.listAssignments(tenant.getId()).stream().noneMatch(item -> sara.id().equals(item.targetId()))) {
                counseling.createAssignment(tenant.getId(), admin.getUserAccount().getId(), new CounselingService.AssignmentCommand(
                        junior.getId(), "STUDENT", sara.id(), null, LocalDate.now(clock), "دمو: مشاور سارا"));
            }
            if (counseling.listSupervisions(tenant.getId()).isEmpty()) {
                counseling.createSupervision(tenant.getId(), admin.getUserAccount().getId(), new CounselingService.SupervisionCommand(
                        senior.getId(), junior.getId(), LocalDate.now(clock)));
            }
        });
        log.info("Counselor demo ready: counselor@example.com / counselor.senior@example.com (password same as DEMO_DATA_PASSWORD)");
    }

    private void ensureDemoStudentCards(Tenant tenant, TenantMembership admin) {
        Long tenantId = tenant.getId();
        PeopleService.StudentView sara = people.directory(tenantId).students().stream()
                .filter(item -> "DEMO-STU-001".equalsIgnoreCase(item.code()))
                .findFirst()
                .orElse(null);
        if (sara == null) return;
        Long classId = classroom.catalog(tenantId).sections().stream()
                .filter(section -> "CLS-A1-1".equalsIgnoreCase(section.code()))
                .map(ClassroomService.SectionView::id)
                .findFirst()
                .orElse(null);
        ZoneId zone = ZoneId.of(tenant.getTimezone());
        LocalDate validUntil = LocalDate.now(clock.withZone(zone)).plusYears(1);
        runAs(admin, () -> {
            if (!studentCards.forStudent(tenantId, sara.id()).isEmpty()) return;
            studentCards.issueOne(tenantId, admin.getUserAccount().getId(), sara.id(), classId, validUntil, null);
            log.info("Demo student card issued for {}", sara.code());
        });
    }

    private void ensureCounselorLogin(Tenant tenant, TenantMembership admin, String password, String email, String first, String last,
            String nationalId, String code, String roleCode) {
        if (memberships.findByTenantIdAndUserAccountEmailIgnoreCase(tenant.getId(), email).isPresent()) {
            grantRoleByEmail(tenant, email, roleCode);
            return;
        }
        people.createInstructor(tenant.getId(), admin.getUserAccount().getId(), new PeopleService.CreateInstructor(
                code, "مشاور", "مشاوره تحصیلی", person(first, last, nationalId, "09120000041", email, password)));
        grantRoleByEmail(tenant, email, roleCode);
    }

    private void grantRoleByEmail(Tenant tenant, String email, String roleCode) {
        TenantMembership membership = memberships.findByTenantIdAndUserAccountEmailIgnoreCase(tenant.getId(), email).orElse(null);
        if (membership == null) return;
        AccessRole role = roles.findAllByTenantIdOrderByName(tenant.getId()).stream()
                .filter(item -> roleCode.equalsIgnoreCase(item.getCode()))
                .findFirst()
                .orElse(null);
        if (role == null) return;
        boolean granted = grants.findAllByMembershipId(membership.getId()).stream()
                .anyMatch(item -> roleCode.equalsIgnoreCase(item.getRole().getCode()));
        if (!granted) {
            grants.save(new AccessGrant(tenant, membership, role, AccessGrant.ScopeType.TENANT, null, membership.getUserAccount()));
        }
    }

    private static PeopleService.PersonInput person(String first, String last, String nationalId, String mobile, String email, String password) {
        return new PeopleService.PersonInput(first, last, nationalId, mobile, email, password);
    }

    private void runAs(TenantMembership membership, Runnable action) {
        Jwt jwt = Jwt.withTokenValue("demo-seed")
                .header("alg", "none")
                .subject(membership.getUserAccount().getId().toString())
                .claim("tenantId", membership.getTenant().getId().toString())
                .claim("membershipId", membership.getId().toString())
                .issuedAt(Instant.now(clock))
                .expiresAt(Instant.now(clock).plusSeconds(600))
                .build();
        SecurityContext previous = SecurityContextHolder.getContext();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        SecurityContextHolder.setContext(context);
        try {
            action.run();
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }
}
