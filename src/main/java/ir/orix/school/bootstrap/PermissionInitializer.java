package ir.orix.school.bootstrap;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ir.orix.school.organization.domain.AccessRole;
import ir.orix.school.organization.domain.Permission;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.organization.persistence.AccessRoleRepository;
import ir.orix.school.organization.persistence.PermissionRepository;
import ir.orix.school.organization.persistence.TenantRepository;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PermissionInitializer implements ApplicationRunner {
    private static final List<Definition> DEFINITIONS = List.of(
            new Definition("ORGANIZATION_READ", "organization", "مشاهده اطلاعات مؤسسه و شعب"),
            new Definition("BRANCH_WRITE", "organization", "ایجاد و ویرایش شعبه"),
            new Definition("USER_READ", "organization", "مشاهده کاربران و دسترسی‌ها"),
            new Definition("USER_WRITE", "organization", "ایجاد و ویرایش کاربران"),
            new Definition("ROLE_READ", "organization", "مشاهده نقش‌ها و مجوزها"),
            new Definition("ROLE_WRITE", "organization", "ایجاد و ویرایش نقش‌ها"),
            new Definition("AUDIT_READ", "audit", "مشاهده تاریخچه عملیات"),
            new Definition("PEOPLE_READ", "people", "مشاهده اشخاص و پروفایل‌ها"),
            new Definition("PEOPLE_WRITE", "people", "ثبت و ویرایش دانش‌آموز، استاد و ولی"),
            new Definition("ACADEMIC_READ", "academic", "مشاهده ساختار آموزشی"),
            new Definition("ACADEMIC_WRITE", "academic", "تعریف و ویرایش ساختار آموزشی"),
            new Definition("CLASS_READ", "classroom", "مشاهده ارائه درس، کلاس و استادان"),
            new Definition("CLASS_WRITE", "classroom", "ایجاد و ویرایش ارائه درس و کلاس"),
            new Definition("ENROLLMENT_READ", "enrollment", "مشاهده ثبت‌نام‌های دانش‌آموزان"),
            new Definition("ENROLLMENT_WRITE", "enrollment", "ثبت‌نام، انصراف و انتقال دانش‌آموز"),
            new Definition("SCHEDULE_READ", "scheduling", "مشاهده برنامه و جلسات کلاس"),
            new Definition("SCHEDULE_WRITE", "scheduling", "تعریف برنامه و مدیریت جلسات کلاس"),
            new Definition("ATTENDANCE_READ", "attendance", "مشاهده حضور و غیاب جلسات"),
            new Definition("ATTENDANCE_WRITE", "attendance", "ثبت، نهایی‌سازی و اصلاح حضور و غیاب"),
            new Definition("QUESTION_READ", "assessment", "مشاهده بانک سؤال"),
            new Definition("QUESTION_WRITE", "assessment", "ایجاد سؤال و نسخه جدید"),
            new Definition("ASSESSMENT_READ", "assessment", "مشاهده آزمون‌ها"),
            new Definition("ASSESSMENT_WRITE", "assessment", "طراحی آزمون و تعیین مخاطب"),
            new Definition("ASSESSMENT_PUBLISH", "assessment", "اعتبارسنجی و انتشار آزمون"),
            new Definition("GRADING_READ", "assessment", "مشاهده صف تصحیح و نتایج"),
            new Definition("GRADING_WRITE", "assessment", "ثبت و اصلاح نمره تشریحی"),
            new Definition("RESULT_PUBLISH", "assessment", "انتشار نتیجه آزمون"),
            new Definition("FINANCE_READ", "finance", "مشاهده بدهی، فاکتور و پرداخت‌ها"),
            new Definition("FINANCE_WRITE", "finance", "ثبت شهریه، تخفیف و صدور فاکتور"),
            new Definition("PAYMENT_WRITE", "finance", "ثبت پرداخت و تخصیص به بدهی"),
            new Definition("FILE_READ", "files", "مشاهده فایل‌های ذخیره‌شده"),
            new Definition("FILE_WRITE", "files", "بارگذاری فایل"),
            new Definition("NOTIFICATION_READ", "notification", "مشاهده اعلامیه‌ها و صندوق اعلان"),
            new Definition("NOTIFICATION_WRITE", "notification", "ارسال اعلامیه و پیامک"),
            new Definition("REPORT_CARD_READ", "reportcard", "مشاهده کارنامه"),
            new Definition("REPORT_CARD_WRITE", "reportcard", "صدور کارنامه"),
            new Definition("REPORT_CARD_PUBLISH", "reportcard", "انتشار کارنامه"),
            new Definition("LEDGER_READ", "finance", "مشاهده کدینگ، سند و تراز"),
            new Definition("LEDGER_WRITE", "finance", "ثبت سند حسابداری"),
            new Definition("WORKFLOW_READ", "workflow", "مشاهده فرایندها و درخواست‌ها"),
            new Definition("WORKFLOW_WRITE", "workflow", "تعریف فرایند و شروع درخواست"),
            new Definition("WORKFLOW_APPROVE", "workflow", "تأیید یا رد گام فرایند"),
            new Definition("CHAT_READ", "chat", "مشاهده گفتگوها"),
            new Definition("CHAT_WRITE", "chat", "ارسال پیام خصوصی"),
            new Definition("CHAT_MODERATE", "chat", "نظارت بر گفتگوهای خصوصی"),
            new Definition("SURVEY_READ", "survey", "مشاهده نظرسنجی‌ها"),
            new Definition("SURVEY_WRITE", "survey", "ایجاد نظرسنجی اساتید"),
            new Definition("STUDENT_CARD_READ", "card", "مشاهده کارت دانش‌آموزی"),
            new Definition("STUDENT_CARD_WRITE", "card", "صدور کارت دانش‌آموزی"),
            new Definition("MEETING_READ", "meeting", "ورود به کلاس مجازی"),
            new Definition("MEETING_WRITE", "meeting", "تنظیم کلاس مجازی"),
            new Definition("FEATURE_WRITE", "organization", "تنظیم پرچم قابلیت و ارائه‌دهندگان"),
            new Definition("COUNSELING_READ", "counseling", "مشاهده مشاوره، گزارش کار و برنامه درسی"),
            new Definition("COUNSELING_WRITE", "counseling", "ثبت گزارش کار، برنامه درسی و بازخورد مشاور"),
            new Definition("COUNSELING_ASSIGN", "counseling", "تخصیص مشاور به دانش‌آموز یا ولی"),
            new Definition("COUNSELING_ADMIN_REPORT_READ", "counseling", "مشاهده گزارش‌های مشاوران برای ادمین"),
            new Definition("COUNSELING_SENIOR_REPORT_READ", "counseling", "مشاهده گزارش‌های ماهانه مشاوران برای ارشد")
    );
    static final Set<String> COUNSELOR_JUNIOR_PERMISSIONS = Set.of(
            "COUNSELING_READ", "COUNSELING_WRITE", "CHAT_READ", "CHAT_WRITE", "FILE_READ");
    static final Set<String> COUNSELOR_MID_PERMISSIONS = Set.of(
            "COUNSELING_READ", "COUNSELING_WRITE", "CHAT_READ", "CHAT_WRITE", "FILE_READ");
    static final Set<String> COUNSELOR_SENIOR_PERMISSIONS = Set.of(
            "COUNSELING_READ", "COUNSELING_WRITE", "COUNSELING_ADMIN_REPORT_READ", "COUNSELING_SENIOR_REPORT_READ",
            "CHAT_READ", "CHAT_WRITE", "CHAT_MODERATE", "FILE_READ");
    static final Set<String> INSTRUCTOR_PERMISSIONS = Set.of(
            "CLASS_READ", "SCHEDULE_READ", "SCHEDULE_WRITE", "ATTENDANCE_READ", "ATTENDANCE_WRITE",
            "ASSESSMENT_READ", "GRADING_READ", "GRADING_WRITE", "RESULT_PUBLISH",
            "REPORT_CARD_READ", "REPORT_CARD_WRITE", "NOTIFICATION_READ", "NOTIFICATION_WRITE",
            "CHAT_READ", "CHAT_WRITE", "SURVEY_READ", "MEETING_READ", "FILE_READ", "FILE_WRITE");

    private final PermissionRepository permissions;
    private final AccessRoleRepository roles;
    private final TenantRepository tenants;

    public PermissionInitializer(PermissionRepository permissions, AccessRoleRepository roles, TenantRepository tenants) {
        this.permissions = permissions;
        this.roles = roles;
        this.tenants = tenants;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (Definition definition : DEFINITIONS) {
            if (!permissions.existsById(definition.code())) {
                permissions.save(new Permission(definition.code(), definition.module(), definition.description()));
            }
        }
        LinkedHashSet<Permission> all = new LinkedHashSet<>(permissions.findAll());
        roles.findAllBySystemRoleTrue().stream()
                .filter(role -> "ADMIN".equalsIgnoreCase(role.getCode()))
                .forEach(role -> {
                    role.replacePermissions(all);
                    roles.save(role);
                });
        for (Tenant tenant : tenants.findAll()) {
            ensureTenantScopedRoles(tenant);
        }
    }

    /**
     * Ensures INSTRUCTOR and counselor tier roles exist for one tenant. Called from {@link #run} for existing tenants
     * and from {@link BootstrapService} when the first tenant is created (runs after this bean's {@link #run}, so
     * the initial loop often sees no tenants).
     */
    @Transactional
    public void ensureTenantScopedRoles(Tenant tenant) {
        LinkedHashSet<Permission> all = new LinkedHashSet<>(permissions.findAll());
        LinkedHashSet<Permission> instructorPerms = all.stream()
                .filter(permission -> INSTRUCTOR_PERMISSIONS.contains(permission.getCode()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        AccessRole instructor = roles.findAllByTenantIdOrderByName(tenant.getId()).stream()
                .filter(role -> "INSTRUCTOR".equalsIgnoreCase(role.getCode()))
                .findFirst()
                .orElseGet(() -> new AccessRole(tenant, "INSTRUCTOR", "استاد", "دسترسی روزانه کلاس، حضور، تصحیح و پیام", true));
        instructor.replacePermissions(instructorPerms);
        roles.save(instructor);
        ensureCounselorRole(tenant, all, "COUNSELOR_JUNIOR", "مشاور پایین‌رده",
                "ارتباط با دانش‌آموز و گزارش کار", COUNSELOR_JUNIOR_PERMISSIONS);
        ensureCounselorRole(tenant, all, "COUNSELOR_MID", "مشاور میان‌رده",
                "ارتباط با اولیا", COUNSELOR_MID_PERMISSIONS);
        ensureCounselorRole(tenant, all, "COUNSELOR_SENIOR", "مشاور ارشد",
                "نظارت بر مشاوران و گزارش‌های ماهانه", COUNSELOR_SENIOR_PERMISSIONS);
    }

    private void ensureCounselorRole(Tenant tenant, LinkedHashSet<Permission> all, String code, String name,
            String description, Set<String> permissionCodes) {
        LinkedHashSet<Permission> perms = all.stream()
                .filter(permission -> permissionCodes.contains(permission.getCode()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        AccessRole role = roles.findAllByTenantIdOrderByName(tenant.getId()).stream()
                .filter(value -> code.equalsIgnoreCase(value.getCode()))
                .findFirst()
                .orElseGet(() -> new AccessRole(tenant, code, name, description, true));
        role.replacePermissions(perms);
        roles.save(role);
    }

    private record Definition(String code, String module, String description) {}
}
