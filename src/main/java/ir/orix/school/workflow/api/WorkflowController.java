package ir.orix.school.workflow.api;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ir.orix.school.shared.security.CurrentUser;
import ir.orix.school.workflow.application.WorkflowService;
import ir.orix.school.workflow.domain.ProcessDefinition;
import ir.orix.school.workflow.domain.ProcessTask;

@RestController
@RequestMapping("/api/v1")
public class WorkflowController {
    private final WorkflowService service;
    public WorkflowController(WorkflowService service) { this.service = service; }

    @GetMapping("/workflows")
    public List<WorkflowService.DefinitionView> definitions() { return service.definitions(CurrentUser.tenantId()); }

    @PostMapping("/workflows")
    @PreAuthorize("hasAuthority('WORKFLOW_WRITE')")
    public WorkflowService.DefinitionView create(@Valid @RequestBody DefinitionRequest request) {
        return service.createDefinition(CurrentUser.tenantId(), CurrentUser.accountId(), request.toCommand());
    }

    @PostMapping("/requests")
    public WorkflowService.InstanceView start(@Valid @RequestBody StartRequest request) {
        return service.start(CurrentUser.tenantId(), CurrentUser.membershipId(), CurrentUser.accountId(), request.toCommand());
    }

    @GetMapping("/requests")
    public List<WorkflowService.InstanceView> mine() { return service.mine(CurrentUser.tenantId(), CurrentUser.membershipId()); }

    @GetMapping("/requests/all")
    @PreAuthorize("hasAuthority('WORKFLOW_READ')")
    public List<WorkflowService.InstanceView> all() { return service.all(CurrentUser.tenantId()); }

    @GetMapping("/requests/queue")
    @PreAuthorize("hasAuthority('WORKFLOW_APPROVE')")
    public List<WorkflowService.TaskView> queue() { return service.queue(CurrentUser.tenantId()); }

    @PostMapping("/requests/tasks/{id}")
    @PreAuthorize("hasAuthority('WORKFLOW_APPROVE')")
    public WorkflowService.InstanceView act(@PathVariable Long id, @Valid @RequestBody ActRequest request) {
        return service.act(CurrentUser.tenantId(), CurrentUser.accountId(), id, request.decision(), request.comment());
    }

    @GetMapping(value = "/requests/{id}/print", produces = MediaType.TEXT_HTML_VALUE)
    public String print(@PathVariable Long id) {
        return service.printCertificate(CurrentUser.tenantId(), CurrentUser.membershipId(), id, false);
    }

    public record StepRequest(@NotBlank String name, @NotBlank String approverRole, Integer slaHours) {}
    public record DefinitionRequest(@NotBlank String code, @NotBlank String name, String description,
            @NotNull ProcessDefinition.StarterRole starterRole, String formSchema, ProcessDefinition.OutputKind outputKind,
            String outputTitle, @NotNull List<@Valid StepRequest> steps) {
        WorkflowService.DefinitionCommand toCommand() {
            return new WorkflowService.DefinitionCommand(code, name, description, starterRole, formSchema,
                    outputKind == null ? ProcessDefinition.OutputKind.NONE : outputKind, outputTitle,
                    steps.stream().map(step -> new WorkflowService.StepCommand(step.name(), step.approverRole(), step.slaHours())).toList());
        }
    }
    public record StartRequest(@NotNull Long definitionId, Long studentId, String formData) {
        WorkflowService.StartCommand toCommand() { return new WorkflowService.StartCommand(definitionId, studentId, formData); }
    }
    public record ActRequest(@NotNull ProcessTask.Status decision, String comment) {}
}
