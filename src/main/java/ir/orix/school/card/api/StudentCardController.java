package ir.orix.school.card.api;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ir.orix.school.card.application.StudentCardService;
import ir.orix.school.people.application.PeopleService;
import ir.orix.school.shared.security.CurrentUser;
import ir.orix.school.shared.web.ApiException;

@RestController
@RequestMapping("/api/v1")
public class StudentCardController {
    private final StudentCardService cards;
    private final PeopleService people;
    public StudentCardController(StudentCardService cards, PeopleService people) { this.cards = cards; this.people = people; }

    @GetMapping("/student-cards")
    @PreAuthorize("hasAuthority('STUDENT_CARD_READ')")
    public List<StudentCardService.CardView> list() { return cards.list(CurrentUser.tenantId()); }

    @PostMapping("/student-cards")
    @PreAuthorize("hasAuthority('STUDENT_CARD_WRITE')")
    public StudentCardService.CardView issue(@Valid @RequestBody IssueRequest request) {
        return cards.issueOne(CurrentUser.tenantId(), CurrentUser.accountId(), request.studentId(), request.classSectionId(), request.validUntil(), request.photoFileId());
    }

    @PostMapping("/student-cards/class/{classSectionId}")
    @PreAuthorize("hasAuthority('STUDENT_CARD_WRITE')")
    public List<StudentCardService.CardView> issueClass(@PathVariable Long classSectionId, @Valid @RequestBody ClassIssueRequest request) {
        return cards.issueClass(CurrentUser.tenantId(), CurrentUser.accountId(), classSectionId, request.validUntil(), request.photoFileId());
    }

    @GetMapping("/student-cards/me")
    public List<StudentCardService.CardView> mine() {
        PeopleService.MyProfiles profiles = people.myProfiles(CurrentUser.tenantId(), CurrentUser.membershipId());
        if (profiles.student() == null) return List.of();
        return cards.forStudent(CurrentUser.tenantId(), profiles.student().id());
    }

    @GetMapping("/guardian/children/{studentId}/cards")
    public List<StudentCardService.CardView> child(@PathVariable Long studentId) {
        people.requireChildAcademicAccess(CurrentUser.tenantId(), CurrentUser.membershipId(), studentId);
        return cards.forStudent(CurrentUser.tenantId(), studentId);
    }

    @GetMapping(value = "/guardian/children/{studentId}/cards/print", produces = MediaType.TEXT_HTML_VALUE)
    public String printChild(@PathVariable Long studentId) {
        people.requireChildAcademicAccess(CurrentUser.tenantId(), CurrentUser.membershipId(), studentId);
        return cards.print(CurrentUser.tenantId(), studentId);
    }

    @GetMapping(value = "/student-cards/me/print", produces = MediaType.TEXT_HTML_VALUE)
    public String printMine() {
        PeopleService.MyProfiles profiles = people.myProfiles(CurrentUser.tenantId(), CurrentUser.membershipId());
        if (profiles.student() == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "STUDENT_NOT_FOUND", "Student profile was not found.");
        }
        return cards.print(CurrentUser.tenantId(), profiles.student().id());
    }

    @GetMapping("/public/student-cards/{token}")
    public StudentCardService.CardView verify(@PathVariable String token) { return cards.verify(token); }

    @GetMapping(value = "/public/student-cards/{token}/print", produces = MediaType.TEXT_HTML_VALUE)
    public String printPublic(@PathVariable String token) { return cards.printPublic(token); }

    public record IssueRequest(@NotNull Long studentId, Long classSectionId, @NotNull LocalDate validUntil, Long photoFileId) {}
    public record ClassIssueRequest(@NotNull LocalDate validUntil, Long photoFileId) {}
}
