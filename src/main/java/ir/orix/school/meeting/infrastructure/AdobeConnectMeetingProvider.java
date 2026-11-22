package ir.orix.school.meeting.infrastructure;

import org.springframework.stereotype.Component;
import ir.orix.school.meeting.application.MeetingProvider;

@Component
public class AdobeConnectMeetingProvider implements MeetingProvider {
    @Override public String code() { return "ADOBE_CONNECT"; }
    @Override public Created create(String baseUrl, String secret, String sessionName, String sessionId) {
        String root = (baseUrl == null || baseUrl.isBlank() ? "https://connect.example.local/" : baseUrl.strip());
        if (!root.endsWith("/")) root += "/";
        return new Created(sessionId, root + sessionId + "/host", root + sessionId);
    }
}
