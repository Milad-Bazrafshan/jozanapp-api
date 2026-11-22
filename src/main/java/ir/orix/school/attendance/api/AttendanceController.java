package ir.orix.school.attendance.api;

import java.util.*; import jakarta.validation.Valid; import jakarta.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*;
import ir.orix.school.attendance.application.AttendanceService; import ir.orix.school.attendance.domain.AttendanceRecord; import ir.orix.school.classroom.domain.ClassSection; import ir.orix.school.shared.security.CurrentUser;

@RestController @RequestMapping("/api/v1/attendance")
public class AttendanceController {
 private final AttendanceService service; public AttendanceController(AttendanceService service){this.service=service;}
 @GetMapping @PreAuthorize("hasAuthority('ATTENDANCE_READ')") public List<AttendanceService.SummaryView> list(){return service.list(CurrentUser.tenantId(),CurrentUser.membershipId());}
 @GetMapping("/sessions/{id}") @PreAuthorize("hasAuthority('ATTENDANCE_READ')") public AttendanceService.SheetView sheet(@PathVariable Long id){return service.sheet(CurrentUser.tenantId(),CurrentUser.membershipId(),id);}
 @PutMapping("/sessions/{id}") @PreAuthorize("hasAuthority('ATTENDANCE_WRITE')") public AttendanceService.SheetView save(@PathVariable Long id,@Valid @RequestBody SaveRequest request){return service.save(CurrentUser.tenantId(),CurrentUser.membershipId(),CurrentUser.accountId(),id,new AttendanceService.SaveCommand(request.finalizeSheet(),request.correctionReason(),request.records().stream().map(r->new AttendanceService.RecordCommand(r.studentId(),r.status(),r.deliveryMode(),r.lateMinutes(),r.note())).toList()));}
 @GetMapping("/me") public List<AttendanceService.MyAttendanceView> mine(){return service.mine(CurrentUser.tenantId(),CurrentUser.membershipId());}
 public record RecordRequest(@NotNull Long studentId,@NotNull AttendanceRecord.PresenceStatus status,@NotNull ClassSection.DeliveryMode deliveryMode,@PositiveOrZero Integer lateMinutes,@Size(max=500) String note){}
 public record SaveRequest(boolean finalizeSheet,@Size(max=500) String correctionReason,@NotNull List<@Valid RecordRequest> records){}
}
