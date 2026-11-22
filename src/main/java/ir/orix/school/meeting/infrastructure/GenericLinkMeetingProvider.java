package ir.orix.school.meeting.infrastructure;

import org.springframework.stereotype.Component;
import ir.orix.school.meeting.application.MeetingProvider;

@Component
public class GenericLinkMeetingProvider implements MeetingProvider {
    @Override public String code() { return "GENERIC_LINK"; }
    @Override public Created create(String baseUrl, String secret, String sessionName, String sessionId) {
        String root = (baseUrl == null || baseUrl.isBlank() ? "https://meet.example.local/room/" : baseUrl.strip());
        if (!root.endsWith("/")) root += "/";
        String instructor = root + sessionId + "?role=moderator";
        String student = root + sessionId + "?role=viewer";
        return new Created(sessionId, instructor, student);
    }
}
