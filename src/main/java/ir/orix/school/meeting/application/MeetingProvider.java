package ir.orix.school.meeting.application;

public interface MeetingProvider {
    String code();
    Created create(String baseUrl, String secret, String sessionName, String sessionId);
    record Created(String externalId, String instructorJoinUrl, String studentJoinUrl) {}
}
