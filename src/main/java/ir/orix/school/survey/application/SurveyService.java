package ir.orix.school.survey.application;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ir.orix.school.audit.application.AuditService;
import ir.orix.school.classroom.persistence.ClassSectionRepository;
import ir.orix.school.enrollment.domain.Enrollment;
import ir.orix.school.enrollment.persistence.EnrollmentRepository;
import ir.orix.school.organization.persistence.TenantRepository;
import ir.orix.school.people.application.PeopleService;
import ir.orix.school.people.domain.InstructorProfile;
import ir.orix.school.people.persistence.InstructorProfileRepository;
import ir.orix.school.shared.web.ApiException;
import ir.orix.school.survey.domain.*;
import ir.orix.school.survey.persistence.*;

@Service
public class SurveyService {
    private final TenantRepository tenants;
    private final SurveyRepository surveys;
    private final SurveyResponseRepository responses;
    private final SurveyAnswerRepository answers;
    private final InstructorProfileRepository instructors;
    private final ClassSectionRepository sections;
    private final EnrollmentRepository enrollments;
    private final PeopleService people;
    private final AuditService audit;
    private final Clock clock;

    public SurveyService(TenantRepository tenants, SurveyRepository surveys, SurveyResponseRepository responses,
            SurveyAnswerRepository answers, InstructorProfileRepository instructors, ClassSectionRepository sections,
            EnrollmentRepository enrollments, PeopleService people, AuditService audit, Clock clock) {
        this.tenants = tenants; this.surveys = surveys; this.responses = responses; this.answers = answers;
        this.instructors = instructors; this.sections = sections; this.enrollments = enrollments; this.people = people;
        this.audit = audit; this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<SurveyView> list(Long tenantId) {
        return surveys.findAllByTenantIdOrderByCreatedAtDesc(tenantId).stream().map(this::view).toList();
    }

    @Transactional
    public SurveyView create(Long tenantId, Long actorId, Command command) {
        InstructorProfile instructor = instructors.findByIdAndTenantId(command.instructorId(), tenantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "INSTRUCTOR_NOT_FOUND", "Instructor was not found."));
        var section = sections.findByIdAndTenantId(command.classSectionId(), tenantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CLASS_SECTION_NOT_FOUND", "Class was not found."));
        Survey survey = new Survey(tenants.findById(tenantId).orElseThrow(), instructor, section, command.title(), command.audience(), command.opensAt(), command.closesAt());
        int seq = 1;
        for (String prompt : command.questions()) survey.addQuestion(prompt, seq++);
        survey = surveys.save(survey);
        audit.record(tenantId, actorId, "SURVEY_CREATED", "SURVEY", survey.getId(), Map.of());
        return view(survey);
    }

    @Transactional(readOnly = true)
    public List<SurveyView> available(Long tenantId, Long studentId) {
        Instant now = Instant.now(clock);
        List<Long> sectionIds = enrollments.findAllByTenantIdAndStudentIdOrderByCreatedAtDesc(tenantId, studentId).stream()
                .filter(item -> item.getStatus() == Enrollment.Status.ACTIVE).map(item -> item.getClassSection().getId()).toList();
        return surveys.findAllByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .filter(survey -> sectionIds.contains(survey.getClassSection().getId()))
                .filter(survey -> !now.isBefore(survey.getOpensAt()) && now.isBefore(survey.getClosesAt()))
                .map(this::view).toList();
    }

    @Transactional
    public void submit(Long tenantId, Long membershipId, Long surveyId, Long studentId, List<AnswerCommand> items) {
        PeopleService.MyProfiles profiles = people.myProfiles(tenantId, membershipId);
        boolean self = profiles.student() != null && profiles.student().id().equals(studentId);
        if (!self) people.requireChildAcademicAccess(tenantId, membershipId, studentId);
        Survey survey = surveys.findByIdAndTenantId(surveyId, tenantId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SURVEY_NOT_FOUND", "Survey was not found."));
        Enrollment enrollment = enrollments.findAllByTenantIdAndStudentIdOrderByCreatedAtDesc(tenantId, studentId).stream()
                .filter(item -> item.getClassSection().getId().equals(survey.getClassSection().getId()) && item.getStatus() == Enrollment.Status.ACTIVE)
                .findFirst().orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "SURVEY_NOT_ELIGIBLE", "Student is not in this class."));
        if (responses.existsBySurveyIdAndEnrollmentId(surveyId, enrollment.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "SURVEY_ALREADY_SUBMITTED", "A response already exists.");
        }
        SurveyResponse response = responses.save(new SurveyResponse(survey.getTenant(), survey, enrollment, Instant.now(clock)));
        Map<Long, SurveyQuestion> questions = survey.getQuestions().stream().collect(java.util.stream.Collectors.toMap(SurveyQuestion::getId, q -> q));
        for (AnswerCommand item : items) {
            SurveyQuestion question = questions.get(item.questionId());
            if (question == null) continue;
            if (item.score() < 1 || item.score() > 5) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_SCORE", "Score must be 1-5.");
            answers.save(new SurveyAnswer(survey.getTenant(), response, question, item.score(), item.comment()));
        }
    }

    @Transactional(readOnly = true)
    public Report report(Long tenantId, Long surveyId) {
        Survey survey = surveys.findByIdAndTenantId(surveyId, tenantId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SURVEY_NOT_FOUND", "Survey was not found."));
        List<SurveyAnswer> values = answers.findAllByResponseSurveyId(surveyId);
        List<QuestionAvg> avgs = new ArrayList<>();
        for (SurveyQuestion question : survey.getQuestions()) {
            List<Integer> scores = values.stream().filter(item -> item.getQuestion().getId().equals(question.getId())).map(SurveyAnswer::getScore).toList();
            double avg = scores.isEmpty() ? 0 : scores.stream().mapToInt(Integer::intValue).average().orElse(0);
            avgs.add(new QuestionAvg(question.getId(), question.getPrompt(), avg, scores.size()));
        }
        double overall = avgs.stream().mapToDouble(QuestionAvg::average).average().orElse(0);
        return new Report(view(survey), overall, responses.findAllBySurveyId(surveyId).size(), avgs);
    }

    @Transactional(readOnly = true)
    public Report instructorSummary(Long tenantId, Long instructorId) {
        List<Survey> items = surveys.findAllByTenantIdAndInstructorIdOrderByCreatedAtDesc(tenantId, instructorId);
        double overall = items.stream().map(item -> report(tenantId, item.getId()).overallAverage()).mapToDouble(Double::doubleValue).average().orElse(0);
        return new Report(items.isEmpty() ? null : view(items.get(0)), overall, items.size(), List.of());
    }

    private SurveyView view(Survey survey) {
        return new SurveyView(survey.getId(), survey.getTitle(), survey.getInstructor().getId(), survey.getInstructor().getPerson().getFullName(),
                survey.getClassSection().getId(), survey.getClassSection().getName(), survey.getAudience().name(), survey.getOpensAt(),
                survey.getClosesAt(), survey.getStatus().name(),
                survey.getQuestions().stream().map(q -> new QuestionView(q.getId(), q.getPrompt(), q.getSequenceNumber())).toList());
    }

    public record Command(Long instructorId, Long classSectionId, String title, Survey.Audience audience, Instant opensAt, Instant closesAt, List<String> questions) {}
    public record AnswerCommand(Long questionId, int score, String comment) {}
    public record QuestionView(Long id, String prompt, int sequenceNumber) {}
    public record SurveyView(Long id, String title, Long instructorId, String instructorName, Long classSectionId, String className,
            String audience, Instant opensAt, Instant closesAt, String status, List<QuestionView> questions) {}
    public record QuestionAvg(Long questionId, String prompt, double average, int count) {}
    public record Report(SurveyView survey, double overallAverage, int responseCount, List<QuestionAvg> questions) {}
}
