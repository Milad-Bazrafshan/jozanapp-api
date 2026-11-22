package ir.orix.school.assessment.application;
import org.springframework.http.HttpStatus; import org.springframework.stereotype.Component; import org.springframework.transaction.annotation.Transactional; import ir.orix.school.assessment.domain.*; import ir.orix.school.assessment.persistence.*; import ir.orix.school.shared.web.ApiException;
@Component public class GradingResourceAccessGuard {
 private final StudentResponseRepository responses; private final AssessmentResultRepository results; private final AssessmentAccessGuard assessments;
 public GradingResourceAccessGuard(StudentResponseRepository responses,AssessmentResultRepository results,AssessmentAccessGuard assessments){this.responses=responses;this.results=results;this.assessments=assessments;}
 @Transactional(readOnly=true) public void response(Long tenantId,Long membershipId,Long accountId,Long responseId){StudentResponse value=responses.findByIdAndTenantId(responseId,tenantId).orElseThrow(()->missing("RESPONSE_NOT_FOUND"));assessments.require(tenantId,membershipId,accountId,value.getAttempt().getParticipant().getAssessment().getId(),"GRADING_WRITE");}
 @Transactional(readOnly=true) public void result(Long tenantId,Long membershipId,Long accountId,Long resultId){AssessmentResult value=results.findByIdAndTenantId(resultId,tenantId).orElseThrow(()->missing("ASSESSMENT_RESULT_NOT_FOUND"));assessments.require(tenantId,membershipId,accountId,value.getParticipant().getAssessment().getId(),"RESULT_PUBLISH");}
 private ApiException missing(String code){return new ApiException(HttpStatus.NOT_FOUND,code,"The requested grading item was not found.");}
}
