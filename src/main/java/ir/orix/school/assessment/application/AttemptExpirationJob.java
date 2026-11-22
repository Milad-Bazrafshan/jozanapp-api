package ir.orix.school.assessment.application;
import org.springframework.scheduling.annotation.Scheduled; import org.springframework.stereotype.Component;
@Component public class AttemptExpirationJob { private final AssessmentExecutionService service; public AttemptExpirationJob(AssessmentExecutionService service){this.service=service;} @Scheduled(fixedDelayString="${app.assessment.expiration-scan-delay:PT1M}") public void submitExpired(){service.autoSubmitExpired();} }
