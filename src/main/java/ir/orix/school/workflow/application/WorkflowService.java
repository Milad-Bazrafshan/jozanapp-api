package ir.orix.school.workflow.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ir.orix.school.audit.application.AuditService;
import ir.orix.school.identity.persistence.UserAccountRepository;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.organization.persistence.TenantRepository;
import ir.orix.school.people.application.PeopleService;
import ir.orix.school.people.domain.StudentProfile;
import ir.orix.school.people.persistence.StudentProfileRepository;
import ir.orix.school.shared.web.ApiException;
import ir.orix.school.shared.web.PrintDocuments;
import ir.orix.school.workflow.domain.ProcessDefinition;
import ir.orix.school.workflow.domain.ProcessInstance;
import ir.orix.school.workflow.domain.ProcessStep;
import ir.orix.school.workflow.domain.ProcessTask;
import ir.orix.school.workflow.persistence.ProcessDefinitionRepository;
import ir.orix.school.workflow.persistence.ProcessInstanceRepository;
import ir.orix.school.workflow.persistence.ProcessTaskRepository;

@Service
public class WorkflowService {
    private final TenantRepository tenants;
    private final ProcessDefinitionRepository definitions;
    private final ProcessInstanceRepository instances;
    private final ProcessTaskRepository tasks;
    private final StudentProfileRepository students;
    private final UserAccountRepository accounts;
    private final PeopleService people;
    private final AuditService audit;
    private final Clock clock;

    public WorkflowService(TenantRepository tenants, ProcessDefinitionRepository definitions, ProcessInstanceRepository instances,
            ProcessTaskRepository tasks, StudentProfileRepository students, UserAccountRepository accounts, PeopleService people,
            AuditService audit, Clock clock) {
        this.tenants = tenants; this.definitions = definitions; this.instances = instances; this.tasks = tasks;
        this.students = students; this.accounts = accounts; this.people = people; this.audit = audit; this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<DefinitionView> definitions(Long tenantId) {
        ensureDefaults(tenantId);
        return definitions.findAllByTenantIdOrderByNameAsc(tenantId).stream().map(this::definitionView).toList();
    }

    @Transactional
    public DefinitionView createDefinition(Long tenantId, Long actorId, DefinitionCommand command) {
        if (definitions.existsByTenantIdAndCodeIgnoreCase(tenantId, command.code())) throw new ApiException(HttpStatus.CONFLICT, "PROCESS_CODE_EXISTS", "Process code exists.");
        Tenant tenant = tenants.findById(tenantId).orElseThrow();
        ProcessDefinition definition = new ProcessDefinition(tenant, command.code(), command.name(), command.description(),
                command.starterRole(), command.formSchema(), command.outputKind(), command.outputTitle());
        int seq = 1;
        for (StepCommand step : command.steps()) definition.addStep(seq++, step.name(), step.approverRole(), step.slaHours());
        definition = definitions.save(definition);
        audit.record(tenantId, actorId, "PROCESS_DEFINED", "PROCESS_DEFINITION", definition.getId(), Map.of("code", definition.getCode()));
        return definitionView(definition);
    }

    @Transactional
    public InstanceView start(Long tenantId, Long membershipId, Long actorId, StartCommand command) {
        ensureDefaults(tenantId);
        ProcessDefinition definition = definitions.findByIdAndTenantId(command.definitionId(), tenantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROCESS_NOT_FOUND", "Process was not found."));
        StudentProfile student = command.studentId() == null ? null : students.findByIdAndTenantId(command.studentId(), tenantId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "STUDENT_NOT_FOUND", "Student was not found."));
        if (student != null && definition.getStarterRole() == ProcessDefinition.StarterRole.GUARDIAN) {
            people.requireChildAcademicAccess(tenantId, membershipId, student.getId());
        }
        ProcessInstance instance = instances.save(new ProcessInstance(tenants.findById(tenantId).orElseThrow(), definition, membershipId, student, command.formData()));
        ProcessStep first = definition.getSteps().stream().min(java.util.Comparator.comparingInt(ProcessStep::getSequenceNumber)).orElse(null);
        if (first != null) {
            Instant due = first.getSlaHours() == null ? null : Instant.now(clock).plusSeconds(first.getSlaHours() * 3600L);
            tasks.save(new ProcessTask(instance.getTenant(), instance, first, due));
        } else {
            complete(instance);
        }
        audit.record(tenantId, actorId, "PROCESS_STARTED", "PROCESS_INSTANCE", instance.getId(), Map.of());
        return instanceView(instance);
    }

    @Transactional(readOnly = true)
    public List<InstanceView> mine(Long tenantId, Long membershipId) {
        return instances.findAllByTenantIdAndStarterMembershipIdOrderByCreatedAtDesc(tenantId, membershipId).stream().map(this::instanceView).toList();
    }

    @Transactional(readOnly = true)
    public List<InstanceView> all(Long tenantId) {
        return instances.findAllByTenantIdOrderByCreatedAtDesc(tenantId).stream().map(this::instanceView).toList();
    }

    @Transactional(readOnly = true)
    public List<TaskView> queue(Long tenantId) {
        return tasks.findAllByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, ProcessTask.Status.PENDING).stream().map(this::taskView).toList();
    }

    @Transactional
    public InstanceView act(Long tenantId, Long actorId, Long taskId, ProcessTask.Status decision, String comment) {
        ProcessTask task = tasks.findByIdAndTenantId(taskId, tenantId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND", "Task was not found."));
        if (task.getStatus() != ProcessTask.Status.PENDING) throw new ApiException(HttpStatus.CONFLICT, "TASK_NOT_PENDING", "Task is not pending.");
        task.complete(decision, comment, accounts.findById(actorId).orElseThrow(), Instant.now(clock));
        ProcessInstance instance = task.getInstance();
        if (decision == ProcessTask.Status.REJECTED) {
            instance.close(ProcessInstance.Status.REJECTED, null);
        } else if (decision == ProcessTask.Status.RETURNED) {
            instance.close(ProcessInstance.Status.RETURNED, null);
        } else {
            int next = task.getStep().getSequenceNumber() + 1;
            ProcessStep following = instance.getDefinition().getSteps().stream().filter(step -> step.getSequenceNumber() == next).findFirst().orElse(null);
            if (following == null) complete(instance);
            else {
                instance.advance(next);
                Instant due = following.getSlaHours() == null ? null : Instant.now(clock).plusSeconds(following.getSlaHours() * 3600L);
                tasks.save(new ProcessTask(instance.getTenant(), instance, following, due));
            }
        }
        audit.record(tenantId, actorId, "PROCESS_TASK_" + decision.name(), "PROCESS_INSTANCE", instance.getId(), Map.of());
        return instanceView(instance);
    }

    @Transactional(readOnly = true)
    public String printCertificate(Long tenantId, Long membershipId, Long instanceId, boolean staff) {
        ProcessInstance instance = instances.findByIdAndTenantId(instanceId, tenantId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROCESS_NOT_FOUND", "Request was not found."));
        if (!staff && !instance.getStarterMembershipId().equals(membershipId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "REQUEST_ACCESS_DENIED", "This request is not yours.");
        }
        if (instance.getStatus() != ProcessInstance.Status.APPROVED || instance.getCertificateBody() == null) {
            throw new ApiException(HttpStatus.CONFLICT, "CERTIFICATE_NOT_READY", "Certificate is not ready.");
        }
        return PrintDocuments.page(instance.getDefinition().getOutputTitle() == null ? "گواهی" : instance.getDefinition().getOutputTitle(),
                instance.getCertificateBody(), true);
    }

    @Transactional
    public void ensureDefaults(Long tenantId) {
        if (definitions.existsByTenantIdAndCodeIgnoreCase(tenantId, "ENROLLMENT-CERT")) return;
        Tenant tenant = tenants.findById(tenantId).orElseThrow();
        ProcessDefinition cert = new ProcessDefinition(tenant, "ENROLLMENT-CERT", "گواهی اشتغال به تحصیل",
                "صدور گواهی اشتغال به تحصیل پس از تأیید آموزشگاه", ProcessDefinition.StarterRole.GUARDIAN,
                "{\"fields\":[{\"name\":\"reason\",\"label\":\"علت درخواست\",\"type\":\"text\"}]}",
                ProcessDefinition.OutputKind.CERTIFICATE, "گواهی اشتغال به تحصیل");
        cert.addStep(1, "تأیید آموزشگاه", "STAFF", 48);
        definitions.save(cert);
        ProcessDefinition leave = new ProcessDefinition(tenant, "LEAVE-REQUEST", "مرخصی / غیبت موجه",
                "درخواست ولی برای ثبت غیبت موجه", ProcessDefinition.StarterRole.GUARDIAN,
                "{\"fields\":[{\"name\":\"days\",\"label\":\"تعداد روز\",\"type\":\"number\"},{\"name\":\"note\",\"label\":\"توضیح\",\"type\":\"text\"}]}",
                ProcessDefinition.OutputKind.NONE, null);
        leave.addStep(1, "تأیید آموزشگاه", "STAFF", 24);
        definitions.save(leave);
    }

    private void complete(ProcessInstance instance) {
        String body = null;
        if (instance.getDefinition().getOutputKind() == ProcessDefinition.OutputKind.CERTIFICATE) {
            String student = instance.getStudent() == null ? "—" : instance.getStudent().getPerson().getFullName();
            String code = instance.getStudent() == null ? "" : instance.getStudent().getStudentCode();
            body = "<h1>" + PrintDocuments.escape(instance.getDefinition().getOutputTitle()) + "</h1>"
                    + "<p>گواهی می‌شود که <b>" + PrintDocuments.escape(student) + "</b> (" + PrintDocuments.escape(code)
                    + ") در " + PrintDocuments.escape(instance.getTenant().getName()) + " اشتغال به تحصیل دارد.</p>"
                    + "<p class='muted'>تاریخ صدور: " + LocalDate.now(clock) + "</p><div class='stamp'><div>مهر آموزشگاه</div></div>";
        }
        instance.close(ProcessInstance.Status.APPROVED, body);
    }

    private DefinitionView definitionView(ProcessDefinition definition) {
        return new DefinitionView(definition.getId(), definition.getCode(), definition.getName(), definition.getDescription(),
                definition.getStarterRole().name(), definition.getFormSchema(), definition.getOutputKind().name(),
                definition.getOutputTitle(), definition.getStatus().name(),
                definition.getSteps().stream().map(step -> new StepView(step.getId(), step.getSequenceNumber(), step.getName(), step.getApproverRole(), step.getSlaHours())).toList());
    }

    private InstanceView instanceView(ProcessInstance instance) {
        return new InstanceView(instance.getId(), instance.getDefinition().getId(), instance.getDefinition().getName(),
                instance.getDefinition().getOutputKind().name(), instance.getStatus().name(), instance.getFormData(),
                instance.getStudent() == null ? null : instance.getStudent().getId(),
                instance.getStudent() == null ? null : instance.getStudent().getPerson().getFullName(),
                instance.getCurrentStep(), instance.getCertificateBody() != null, instance.getCreatedAt());
    }

    private TaskView taskView(ProcessTask task) {
        ProcessInstance instance = task.getInstance();
        return new TaskView(task.getId(), instance.getId(), instance.getDefinition().getName(), task.getStep().getName(),
                task.getStatus().name(), instance.getStudent() == null ? null : instance.getStudent().getPerson().getFullName(),
                task.getDueAt(), instance.getCreatedAt());
    }

    public record StepCommand(String name, String approverRole, Integer slaHours) {}
    public record DefinitionCommand(String code, String name, String description, ProcessDefinition.StarterRole starterRole,
            String formSchema, ProcessDefinition.OutputKind outputKind, String outputTitle, List<StepCommand> steps) {}
    public record StartCommand(Long definitionId, Long studentId, String formData) {}
    public record StepView(Long id, int sequenceNumber, String name, String approverRole, Integer slaHours) {}
    public record DefinitionView(Long id, String code, String name, String description, String starterRole, String formSchema,
            String outputKind, String outputTitle, String status, List<StepView> steps) {}
    public record InstanceView(Long id, Long definitionId, String processName, String outputKind, String status, String formData,
            Long studentId, String studentName, int currentStep, boolean certificateReady, Instant createdAt) {}
    public record TaskView(Long id, Long instanceId, String processName, String stepName, String status, String studentName,
            Instant dueAt, Instant createdAt) {}
}
