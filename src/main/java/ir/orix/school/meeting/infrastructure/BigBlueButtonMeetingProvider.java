package ir.orix.school.meeting.infrastructure;

import org.springframework.stereotype.Component;
import ir.orix.school.meeting.application.MeetingProvider;

@Component
public class BigBlueButtonMeetingProvider implements MeetingProvider {
    @Override public String code() { return "BIGBLUEBUTTON"; }
    @Override public Created create(String baseUrl, String secret, String sessionName, String sessionId) {
        String root = (baseUrl == null || baseUrl.isBlank() ? "https://bbb.example.local/bigbluebutton/" : baseUrl.strip());
        if (!root.endsWith("/")) root += "/";
        String meeting = sessionId;
        return new Created(meeting, root + "join?meetingID=" + meeting + "&role=MODERATOR", root + "join?meetingID=" + meeting + "&role=VIEWER");
    }
}
