package ir.orix.school.assessment.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ir.orix.school.assessment.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ir.orix.school.assessment.persistence.AssessmentAttemptEventRepository;
import ir.orix.school.assessment.persistence.AssessmentAttemptRepository;
import ir.orix.school.assessment.persistence.AssessmentParticipantRepository;
import ir.orix.school.assessment.persistence.AssessmentQuestionRepository;
import ir.orix.school.assessment.persistence.AssessmentResultRepository;
import ir.orix.school.assessment.persistence.GradeHistoryRepository;
import ir.orix.school.assessment.persistence.StudentResponseRepository;
import ir.orix.school.audit.application.AuditService;
import ir.orix.school.finance.application.WalletService;
import ir.orix.school.identity.domain.UserAccount;
import ir.orix.school.identity.persistence.UserAccountRepository;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.organization.domain.TenantMembership;
import ir.orix.school.organization.persistence.TenantMembershipRepository;
import ir.orix.school.organization.persistence.TenantRepository;
import ir.orix.school.people.domain.StudentProfile;
import ir.orix.school.people.persistence.StudentProfileRepository;
import ir.orix.school.shared.security.CurrentUser;
import ir.orix.school.shared.web.ApiException;

@Service
public class AssessmentExecutionService {

    private static final BigDecimal BALANCE_BASE = new BigDecimal("5000");
    private static final BigDecimal BALANCE_PER_PERCENT_POINT = new BigDecimal("10");

    private final TenantRepository tenants;
    private final TenantMembershipRepository memberships;
    private final StudentProfileRepository students;
    private final UserAccountRepository accounts;
    private final AssessmentAccessGuard access;
    private final AssessmentParticipantRepository participants;
    private final AssessmentQuestionRepository questions;
    private final AssessmentAttemptRepository attempts;
    private final StudentResponseRepository responses;
    private final AssessmentResultRepository results;
    private final GradeHistoryRepository histories;
    private final AssessmentAttemptEventRepository events;
    private final AuditService audit;
    private final Clock clock;
    private final ObjectMapper json = new ObjectMapper();

    public AssessmentExecutionService(
            TenantRepository tenants,
            TenantMembershipRepository memberships,
            StudentProfileRepository students,
            UserAccountRepository accounts,
            AssessmentAccessGuard access,
            AssessmentParticipantRepository participants,
            AssessmentQuestionRepository questions,
            AssessmentAttemptRepository attempts,
            StudentResponseRepository responses,
            AssessmentResultRepository results,
            GradeHistoryRepository histories,
            AssessmentAttemptEventRepository events,
            AuditService audit,
            WalletService wallets,
            Clock clock) {
        this.tenants = tenants;
        this.memberships = memberships;
        this.students = students;
        this.accounts = accounts;
        this.access = access;
        this.participants = participants;
        this.questions = questions;
        this.attempts = attempts;
        this.responses = responses;
        this.results = results;
        this.histories = histories;
        this.events = events;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public AttemptView start(Long tenantId, Long membershipId, Long assessmentId) {
        StudentProfile student = student(tenantId, membershipId);
        AssessmentParticipant participant = participants.findByAssessmentIdAndStudentId(assessmentId, student.getId())
                .orElseThrow(notFound("ASSESSMENT_PARTICIPANT_NOT_FOUND"));
        if (participant.getStatus() != AssessmentParticipant.Status.ELIGIBLE) {
            throw forbidden("ASSESSMENT_NOT_AVAILABLE", "This assessment is not available to the student.");
        }
        Assessment assessment = participant.getAssessment();
        Instant now = Instant.now(clock);
        if (assessment.getStatus() != Assessment.Status.PUBLISHED
                || now.isBefore(assessment.getOpensAt())
                || !now.isBefore(assessment.getClosesAt())) {
            throw conflict("ASSESSMENT_OUTSIDE_WINDOW", "The assessment cannot be started outside its availability window.");
        }
        Optional<AssessmentAttempt> active = attempts.findFirstByParticipantIdAndStatusOrderByStartedAtDesc(
                participant.getId(), AssessmentAttempt.Status.IN_PROGRESS);
        if (active.isPresent()) {
            AssessmentAttempt value = active.get();
            if (!now.isBefore(value.getDeadlineAt())) {
                submitInternal(value, true, now);
            } else {
                return view(value);
            }
        }
        long used = attempts.countByParticipantId(participant.getId());
        if (used >= assessment.getAttemptsAllowed()) {
            throw conflict("ATTEMPT_LIMIT_REACHED", "No assessment attempts remain.");
        }
        Instant deadline = now.plus(java.time.Duration.ofMinutes(assessment.getDurationMinutes()));
        if (deadline.isAfter(assessment.getClosesAt())) {
            deadline = assessment.getClosesAt();
        }
        AssessmentAttempt value = attempts.save(new AssessmentAttempt(tenant(tenantId), participant, (int) used + 1, now, deadline));
        events.save(new AssessmentAttemptEvent(tenant(tenantId), value, "STARTED", now, null));
        return view(value);
    }

    @Transactional
    public AttemptView get(Long tenantId, Long membershipId, Long attemptId) {
        AssessmentAttempt attempt = ownedAttempt(tenantId, membershipId, attemptId);
        Instant now = Instant.now(clock);
        if (attempt.getStatus() == AssessmentAttempt.Status.IN_PROGRESS && !now.isBefore(attempt.getDeadlineAt())) {
            submitInternal(attempt, true, now);
        }
        return view(attempt);
    }

    @Transactional
    public AttemptView save(Long tenantId, Long membershipId, Long attemptId, Long questionId, List<String> answer) {
        AssessmentAttempt attempt = ownedAttempt(tenantId, membershipId, attemptId);
        Instant now = Instant.now(clock);
        if (attempt.getStatus() != AssessmentAttempt.Status.IN_PROGRESS) {
            throw conflict("ATTEMPT_ALREADY_SUBMITTED", "Submitted attempts cannot be changed.");
        }
        if (!now.isBefore(attempt.getDeadlineAt())) {
            submitInternal(attempt, true, now);
            throw conflict("ATTEMPT_DEADLINE_PASSED", "The assessment deadline has passed.");
        }
        AssessmentQuestion question = questions.findById(questionId)
                .filter(q -> q.getAssessment().getId().equals(attempt.getParticipant().getAssessment().getId()))
                .orElseThrow(notFound("ASSESSMENT_QUESTION_NOT_FOUND"));
        String content = write(answer);
        StudentResponse response = responses.findByAttemptIdAndAssessmentQuestionId(attemptId, questionId)
                .orElseGet(() -> new StudentResponse(tenant(tenantId), attempt, question, content, now));
        response.save(content, now);
        responses.save(response);
        events.save(new AssessmentAttemptEvent(tenant(tenantId), attempt, "RESPONSE_SAVED", now,
                "{\"questionId\":\"" + questionId + "\"}"));
        return view(attempt);
    }

    @Transactional
    public AttemptView submit(Long tenantId, Long membershipId, Long attemptId) {
        AssessmentAttempt attempt = ownedAttempt(tenantId, membershipId, attemptId);
        if (attempt.getStatus() == AssessmentAttempt.Status.IN_PROGRESS) {
            submitInternal(attempt, false, Instant.now(clock));
        }
        return view(attempt);
    }

    @Transactional
    public void autoSubmitExpired() {
        Instant now = Instant.now(clock);
        for (AssessmentAttempt attempt : attempts.findAllByStatusAndDeadlineAtLessThanEqual(
                AssessmentAttempt.Status.IN_PROGRESS, now)) {
            submitInternal(attempt, true, now);
        }
    }

    @Transactional(readOnly = true)
    public List<ResultView> grading(Long tenantId, Long assessmentId) {
        access.require(tenantId, CurrentUser.membershipId(), CurrentUser.accountId(), assessmentId, "GRADING_READ");
        return results.findAllByTenantIdAndParticipantAssessmentIdOrderByParticipantStudentNameSnapshotAsc(tenantId, assessmentId)
                .stream()
                .map(this::resultView)
                .toList();
    }

    @Transactional
    public ResultView grade(Long tenantId, Long actorId, Long responseId, BigDecimal score, String feedback, String reason) {
        StudentResponse response = responses.findByIdAndTenantId(responseId, tenantId).orElseThrow(notFound("RESPONSE_NOT_FOUND"));
        AssessmentQuestion question = response.getAssessmentQuestion();
        if (question.getQuestionVersion().getQuestion().getType() != Question.Type.ESSAY) {
            throw conflict("RESPONSE_AUTO_GRADED", "Only essay responses require manual grading.");
        }
        if (score.signum() < 0 || score.compareTo(question.getPoints()) > 0) {
            throw invalid("INVALID_MANUAL_SCORE", "Manual score must be between zero and the question points.");
        }
        AssessmentResult result = results.findByParticipantId(response.getAttempt().getParticipant().getId())
                .orElseThrow(notFound("ASSESSMENT_RESULT_NOT_FOUND"));
        if (!result.getSelectedAttempt().getId().equals(response.getAttempt().getId())) {
            throw conflict("ATTEMPT_NOT_SELECTED", "This response does not belong to the selected result attempt.");
        }
        BigDecimal before = result.getTotalScore();
        Instant now = Instant.now(clock);
        UserAccount actor = account(actorId);
        response.grade(score, feedback, actor, now);
        recalculate(result, response.getAttempt());
        results.save(result);
        Long assessmentId = result.getParticipant().getAssessment().getId();
        maybeAutoPublishIfReady(result, now);
        refreshAssessmentRanking(assessmentId);
        histories.save(new GradeHistory(tenant(tenantId), result, before, result.getTotalScore(), reason, actor, now));
        audit.record(tenantId, actorId, "ASSESSMENT_RESPONSE_GRADED", "ASSESSMENT_RESULT", result.getId(),
                Map.of("responseId", responseId.toString(), "score", score));
        return resultView(results.findByIdAndTenantId(result.getId(), tenantId).orElse(result));
    }

    @Transactional
    public ResultView publishResult(Long tenantId, Long actorId, Long resultId) {
        AssessmentResult result = results.findByIdAndTenantId(resultId, tenantId).orElseThrow(notFound("ASSESSMENT_RESULT_NOT_FOUND"));
        try {
            result.publish(account(actorId), Instant.now(clock));
            results.save(result);
        } catch (IllegalStateException e) {
            throw conflict("RESULT_NOT_READY", "All essay answers must be graded before publishing the result.");
        }
        Long assessmentId = result.getParticipant().getAssessment().getId();
        refreshAssessmentRanking(assessmentId);
        audit.record(tenantId, actorId, "ASSESSMENT_RESULT_PUBLISHED", "ASSESSMENT_RESULT", resultId, Map.of("total", result.getTotalScore()));
        return resultView(result);
    }

    @Transactional(readOnly = true)
    public List<ResultView> myResults(Long tenantId, Long membershipId) {
        return resultsForStudent(tenantId, student(tenantId, membershipId).getId());
    }

    @Transactional(readOnly = true)
    public ResultView myResult(Long tenantId, Long membershipId, Long resultId) {
        StudentProfile student = student(tenantId, membershipId);
        return studentResult(tenantId, student.getId(), resultId);
    }

    /** Guardian path: same visibility rules after child access is checked by the controller. */
    @Transactional(readOnly = true)
    public ResultView studentResult(Long tenantId, Long studentId, Long resultId) {
        students.findByIdAndTenantId(studentId, tenantId).orElseThrow(notFound("STUDENT_NOT_FOUND"));
        return studentResultInternal(tenantId, studentId, resultId);
    }

    private ResultView studentResultInternal(Long tenantId, Long studentId, Long resultId) {
        AssessmentResult result = results.findByIdAndTenantId(resultId, tenantId).orElseThrow(notFound("ASSESSMENT_RESULT_NOT_FOUND"));
        if (!result.getParticipant().getStudent().getId().equals(studentId)) {
            throw forbidden("RESULT_ACCESS_DENIED", "This result belongs to another student.");
        }
        if (result.getStatus() != AssessmentResult.Status.PUBLISHED) {
            throw forbidden("RESULT_NOT_VISIBLE", "This result is not published yet.");
        }
        return resultView(result);
    }

    @Transactional(readOnly = true)
    public List<ResultView> resultsForStudent(Long tenantId, Long studentId) {
        students.findByIdAndTenantId(studentId, tenantId).orElseThrow(notFound("STUDENT_NOT_FOUND"));
        return results.findAllByTenantIdAndParticipantStudentIdAndStatusOrderByCreatedAtDesc(
                        tenantId, studentId, AssessmentResult.Status.PUBLISHED)
                .stream()
                .map(this::resultView)
                .toList();
    }

    private void submitInternal(AssessmentAttempt attempt, boolean automatic, Instant now) {
        if (attempt.getStatus() != AssessmentAttempt.Status.IN_PROGRESS) {
            return;
        }
        List<AssessmentQuestion> items = questions.findAllByAssessmentIdOrderBySectionSequenceNumberAscSequenceNumberAsc(
                attempt.getParticipant().getAssessment().getId());
        Map<Long, StudentResponse> existing = new HashMap<>();
        responses.findAllByAttemptId(attempt.getId()).forEach(r -> existing.put(r.getAssessmentQuestion().getId(), r));
        Tenant tenant = attempt.getParticipant().getStudent().getTenant();
        for (AssessmentQuestion item : items) {
            StudentResponse response = existing.get(item.getId());
            if (response == null) {
                response = new StudentResponse(tenant, attempt, item, "[]", now);
                responses.save(response);
            }
            Question.Type type = item.getQuestionVersion().getQuestion().getType();
            if (type != Question.Type.ESSAY) {
                response.autoScore(correct(item, response) ? item.getPoints() : BigDecimal.ZERO);
            } else if (read(response.getAnswerJson()).isEmpty() && response.getManualScore() == null) {
                response.grade(BigDecimal.ZERO, null, null, now);
            }
        }
        attempt.submit(automatic, now);
        AssessmentResult result = results.findByParticipantId(attempt.getParticipant().getId())
                .orElseGet(() -> new AssessmentResult(tenant, attempt.getParticipant(), attempt));
        recalculate(result, attempt);
        results.save(result);
        Long assessmentId = attempt.getParticipant().getAssessment().getId();
        maybeAutoPublishIfReady(result, now);
        refreshAssessmentRanking(assessmentId);
        events.save(new AssessmentAttemptEvent(tenant, attempt, automatic ? "AUTO_SUBMITTED" : "SUBMITTED", now, null));
    }

    private void maybeAutoPublishIfReady(AssessmentResult result, Instant now) {
        if (result.getStatus() != AssessmentResult.Status.READY) {
            return;
        }
        try {
            result.autoPublish(now);
            results.save(result);
        } catch (IllegalStateException ignored) {
            // still pending grading
        }
    }

    private void refreshAssessmentRanking(Long assessmentId) {
        List<AssessmentResult> list = results.findAllForRanking(assessmentId,
                List.of(AssessmentResult.Status.PUBLISHED));
        int n = list.size();
        if (n == 0) {
            return;
        }
        BigDecimal maxScore = list.get(0).getMaxScore();
        if (maxScore == null || maxScore.signum() <= 0) {
            maxScore = BigDecimal.ONE;
        }
        for (int i = 0; i < n; i++) {
            AssessmentResult r = list.get(i);
            BigDecimal pct = percent(r.getTotalScore(), maxScore);
            BigDecimal balance = BALANCE_BASE.add(pct.multiply(BALANCE_PER_PERCENT_POINT)).setScale(2, RoundingMode.HALF_UP);
            r.applyRanking(balance, i + 1, n);
            results.save(r);
        }
    }

    private static BigDecimal percent(BigDecimal score, BigDecimal max) {
        if (score == null || max == null || max.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return score.multiply(new BigDecimal("100")).divide(max, 2, RoundingMode.HALF_UP);
    }

    private void recalculate(AssessmentResult result, AssessmentAttempt attempt) {
        List<StudentResponse> values = responses.findAllByAttemptId(attempt.getId());
        BigDecimal automatic = values.stream()
                .map(StudentResponse::getAutoScore)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal manual = values.stream()
                .map(StudentResponse::getManualScore)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean pending = values.stream().anyMatch(r ->
                r.getAssessmentQuestion().getQuestionVersion().getQuestion().getType() == Question.Type.ESSAY
                        && !read(r.getAnswerJson()).isEmpty()
                        && r.getManualScore() == null);
        result.calculate(attempt, automatic, manual, pending);
    }

    private boolean correct(AssessmentQuestion item, StudentResponse response) {
        List<String> expected = normalize(read(item.getQuestionVersion().getAnswerKeyJson()));
        List<String> actual = normalize(read(response.getAnswerJson()));
        Question.Type type = item.getQuestionVersion().getQuestion().getType();
        return type == Question.Type.MULTIPLE_CHOICE
                ? new HashSet<>(expected).equals(new HashSet<>(actual))
                : actual.size() == 1 && expected.stream().anyMatch(v -> v.equals(actual.get(0)));
    }

    private List<String> normalize(List<String> values) {
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(v -> !v.isBlank())
                .map(String::toLowerCase)
                .sorted()
                .toList();
    }

    private AssessmentAttempt ownedAttempt(Long tenantId, Long membershipId, Long id) {
        StudentProfile student = student(tenantId, membershipId);
        AssessmentAttempt attempt = attempts.findLocked(id, tenantId).orElseThrow(notFound("ASSESSMENT_ATTEMPT_NOT_FOUND"));
        if (!attempt.getParticipant().getStudent().getId().equals(student.getId())) {
            throw forbidden("ATTEMPT_ACCESS_DENIED", "This attempt belongs to another student.");
        }
        return attempt;
    }

    private StudentProfile student(Long tenantId, Long membershipId) {
        TenantMembership membership = memberships.findByIdAndTenantId(membershipId, tenantId).orElseThrow(notFound("MEMBERSHIP_NOT_FOUND"));
        return students.findByTenantIdAndPersonId(tenantId, membership.getPerson().getId()).orElseThrow(notFound("STUDENT_PROFILE_NOT_FOUND"));
    }

    private AttemptView view(AssessmentAttempt a) {
        List<QuestionView> itemViews = questions
                .findAllByAssessmentIdOrderBySectionSequenceNumberAscSequenceNumberAsc(a.getParticipant().getAssessment().getId())
                .stream()
                .map(q -> new QuestionView(
                        q.getId(),
                        q.getSection().getTitle(),
                        q.getQuestionVersion().getQuestion().getType().name(),
                        q.getQuestionVersion().getPrompt(),
                        read(q.getQuestionVersion().getOptionsJson()),
                        q.getPoints(),
                        q.getSequenceNumber()))
                .toList();
        Map<Long, ResponseView> saved = new HashMap<>();
        responses.findAllByAttemptId(a.getId())
                .forEach(r -> saved.put(
                        r.getAssessmentQuestion().getId(),
                        new ResponseView(r.getId(), r.getAssessmentQuestion().getId(), read(r.getAnswerJson()), r.getSavedAt())));
        Assessment assessment = a.getParticipant().getAssessment();
        Long publishedResultId = results
                .findByParticipantId(a.getParticipant().getId())
                .filter(r -> r.getSelectedAttempt().getId().equals(a.getId()) && r.getStatus() == AssessmentResult.Status.PUBLISHED)
                .map(AssessmentResult::getId)
                .orElse(null);
        return new AttemptView(
                a.getId(),
                assessment.getId(),
                assessment.getTitle(),
                a.getAttemptNumber(),
                a.getStatus().name(),
                a.getStartedAt(),
                a.getDeadlineAt(),
                a.getSubmittedAt(),
                publishedResultId,
                itemViews,
                itemViews.stream().map(q -> saved.get(q.id())).filter(Objects::nonNull).toList());
    }

    private ResultView resultView(AssessmentResult r) {
        Assessment assessment = r.getParticipant().getAssessment();
        List<ResponseGradeView> responseViews = responses.findAllByAttemptId(r.getSelectedAttempt().getId()).stream()
                .map(v -> new ResponseGradeView(
                        v.getId(),
                        v.getAssessmentQuestion().getId(),
                        v.getAssessmentQuestion().getSection().getTitle(),
                        v.getAssessmentQuestion().getQuestionVersion().getPrompt(),
                        v.getAssessmentQuestion().getQuestionVersion().getQuestion().getType().name(),
                        read(v.getAnswerJson()),
                        v.getAssessmentQuestion().getPoints(),
                        v.getAutoScore(),
                        v.getManualScore(),
                        v.getFeedback()))
                .toList();

        Map<String, List<ResponseGradeView>> bySection = responseViews.stream()
                .collect(Collectors.groupingBy(ResponseGradeView::section, Collectors.toCollection(ArrayList::new)));
        List<SectionSummaryView> sectionSummary = new ArrayList<>();
        for (Map.Entry<String, List<ResponseGradeView>> e : bySection.entrySet()) {
            BigDecimal earned = e.getValue().stream()
                    .map(v -> {
                        BigDecimal s = v.manualScore() != null ? v.manualScore() : v.autoScore();
                        return s != null ? s : BigDecimal.ZERO;
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal maxPts = e.getValue().stream().map(ResponseGradeView::points).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal secPct = percent(earned, maxPts.signum() > 0 ? maxPts : BigDecimal.ONE);
            sectionSummary.add(new SectionSummaryView(e.getKey(), earned, maxPts, secPct));
        }
        sectionSummary.sort(Comparator.comparing(SectionSummaryView::section));

        BigDecimal maxScore = r.getMaxScore() != null && r.getMaxScore().signum() > 0 ? r.getMaxScore() : BigDecimal.ONE;
        BigDecimal overallPercent = percent(r.getTotalScore(), maxScore);

        return new ResultView(
                r.getId(),
                assessment.getId(),
                assessment.getTitle(),
                assessment.getType().name(),
                r.getParticipant().getStudentNameSnapshot(),
                r.getParticipant().getStudentCodeSnapshot(),
                r.getStatus().name(),
                r.getAutoScore(),
                r.getManualScore(),
                r.getTotalScore(),
                r.getMaxScore(),
                overallPercent,
                r.getPublishedAt(),
                r.getBalance(),
                r.getRankInAssessment(),
                r.getParticipantCount(),
                sectionSummary,
                responseViews);
    }

    private String write(List<String> value) {
        try {
            return json.writeValueAsString(value == null ? List.of() : value);
        } catch (Exception e) {
            throw invalid("INVALID_ANSWER", "Answer could not be serialized.");
        }
    }

    private List<String> read(String value) {
        try {
            return value == null ? List.of() : json.readValue(value, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private Tenant tenant(Long id) {
        return tenants.findById(id).orElseThrow(notFound("TENANT_NOT_FOUND"));
    }

    private UserAccount account(Long id) {
        return accounts.findById(id).orElseThrow(notFound("ACTOR_NOT_FOUND"));
    }

    private static Supplier<ApiException> notFound(String code) {
        return () -> new ApiException(HttpStatus.NOT_FOUND, code, "The requested assessment item was not found.");
    }

    private static ApiException invalid(String code, String detail) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, detail);
    }

    private static ApiException conflict(String code, String detail) {
        return new ApiException(HttpStatus.CONFLICT, code, detail);
    }

    private static ApiException forbidden(String code, String detail) {
        return new ApiException(HttpStatus.FORBIDDEN, code, detail);
    }

    public record QuestionView(
            Long id, String section, String type, String prompt, List<String> options, BigDecimal points, int sequenceNumber) {}

    public record ResponseView(Long id, Long questionId, List<String> answer, Instant savedAt) {}

    public record AttemptView(
            Long id,
            Long assessmentId,
            String title,
            int attemptNumber,
            String status,
            Instant startedAt,
            Instant deadlineAt,
            Instant submittedAt,
            Long publishedResultId,
            List<QuestionView> questions,
            List<ResponseView> responses) {}

    public record SectionSummaryView(String section, BigDecimal earnedPoints, BigDecimal maxPoints, BigDecimal percent) {}

    public record ResponseGradeView(
            Long id,
            Long questionId,
            String section,
            String prompt,
            String type,
            List<String> answer,
            BigDecimal points,
            BigDecimal autoScore,
            BigDecimal manualScore,
            String feedback) {}

    public record ResultView(
            Long id,
            Long assessmentId,
            String assessmentTitle,
            String assessmentType,
            String studentName,
            String studentCode,
            String status,
            BigDecimal autoScore,
            BigDecimal manualScore,
            BigDecimal totalScore,
            BigDecimal maxScore,
            BigDecimal overallPercent,
            Instant publishedAt,
            BigDecimal balance,
            Integer rankInAssessment,
            Integer participantCount,
            List<SectionSummaryView> sectionSummary,
            List<ResponseGradeView> responses) {}
}
