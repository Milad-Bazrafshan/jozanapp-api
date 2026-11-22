package ir.orix.school.scheduling.api;

import java.time.*;
import java.util.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ir.orix.school.scheduling.application.SchedulingService;
import ir.orix.school.scheduling.domain.*;
import ir.orix.school.shared.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/scheduling")
public class SchedulingController {
    private final SchedulingService service;

    public SchedulingController(SchedulingService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@scopeAuthorizer.hasTenantPermission('SCHEDULE_READ')")
    public SchedulingService.Catalog catalog() {
        return service.catalog(CurrentUser.tenantId());
    }

    @GetMapping("/me")
    public List<SchedulingService.SessionView> mine() {
        return service.mine(CurrentUser.tenantId(), CurrentUser.membershipId());
    }

    @PostMapping("/locations")
    @PreAuthorize("@scopeAuthorizer.hasTenantPermission('SCHEDULE_WRITE')")
    public SchedulingService.LocationView location(@Valid @RequestBody LocationRequest r) {
        return service.createLocation(CurrentUser.tenantId(), CurrentUser.accountId(), new SchedulingService.LocationCommand(r.branchId(), r.code(), r.name(), r.type(), r.capacity()));
    }

    @PostMapping("/rules")
    @PreAuthorize("@scopeAuthorizer.hasTenantPermission('SCHEDULE_WRITE')")
    public SchedulingService.RuleView rule(@Valid @RequestBody RuleRequest r) {
        return service.createRule(CurrentUser.tenantId(), CurrentUser.accountId(), new SchedulingService.RuleCommand(r.classSectionId(), r.code(), r.recurrenceType(), r.startsOn(), r.endsOn(), r.dayOfWeek(), r.startsAt(), r.endsAt(), r.timeZone(), r.intervalWeeks(), r.locationId(), r.instructorIds()));
    }

    @PostMapping("/rules/{id}/generate")
    @PreAuthorize("@scopeAuthorizer.hasTenantPermission('SCHEDULE_WRITE')")
    public List<SchedulingService.SessionView> generate(@PathVariable Long id, @Valid @RequestBody GenerateRequest r) {
        return service.generate(CurrentUser.tenantId(), CurrentUser.accountId(), id, r.throughDate());
    }

    @PostMapping("/sessions/{id}/cancel")
    @PreAuthorize("@scopeAuthorizer.hasTenantPermission('SCHEDULE_WRITE')")
    public SchedulingService.SessionView cancel(@PathVariable Long id, @Valid @RequestBody ReasonRequest r) {
        return service.cancel(CurrentUser.tenantId(), CurrentUser.accountId(), id, r.reason());
    }

    @PostMapping("/sessions/{id}/reschedule")
    @PreAuthorize("@scopeAuthorizer.hasTenantPermission('SCHEDULE_WRITE')")
    public SchedulingService.SessionView reschedule(@PathVariable Long id, @Valid @RequestBody RescheduleRequest r) {
        return service.reschedule(CurrentUser.tenantId(), CurrentUser.accountId(), id, new SchedulingService.RescheduleCommand(r.sessionDate(), r.startsAt(), r.endsAt(), r.timeZone(), r.reason()));
    }

    public record LocationRequest(@NotNull Long branchId, @NotBlank @Size(max = 60) String code,
                                  @NotBlank @Size(max = 160) String name, @NotNull AcademicLocation.Type type,
                                  @Positive Integer capacity) {
    }

    public record RuleRequest(@NotNull Long classSectionId, @NotBlank @Size(max = 60) String code,
                              @NotNull ScheduleRule.RecurrenceType recurrenceType, @NotNull LocalDate startsOn,
                              @NotNull LocalDate endsOn, DayOfWeek dayOfWeek, @NotNull LocalTime startsAt,
                              @NotNull LocalTime endsAt, @NotBlank @Size(max = 60) String timeZone,
                              @Min(1) int intervalWeeks, Long locationId, @NotEmpty Set<Long> instructorIds) {
    }

    public record GenerateRequest(LocalDate throughDate) {
    }

    public record ReasonRequest(@NotBlank @Size(max = 500) String reason) {
    }

    public record RescheduleRequest(@NotNull LocalDate sessionDate, @NotNull LocalTime startsAt,
                                    @NotNull LocalTime endsAt, @NotBlank @Size(max = 60) String timeZone,
                                    @NotBlank @Size(max = 500) String reason) {
    }
}
