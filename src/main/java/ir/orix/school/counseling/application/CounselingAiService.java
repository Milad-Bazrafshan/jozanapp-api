package ir.orix.school.counseling.application;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class CounselingAiService {
    private static final Logger log = LoggerFactory.getLogger(CounselingAiService.class);

    private final CounselingAiProperties properties;
    private final RestClient client;

    public CounselingAiService(CounselingAiProperties properties) {
        this.properties = properties;
        this.client = RestClient.builder().build();
    }

    public Optional<String> analyzeCounselorMonthlyReport(String reportBody) {
        if (!properties.enabled()) {
            return Optional.empty();
        }
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            log.warn("Counseling AI is enabled but COUNSELING_AI_API_KEY is empty");
            return Optional.empty();
        }
        String prompt = """
                You are an educational counseling supervisor. Analyze the following monthly counselor activity summary in Persian.
                Provide: strengths, risks, and 3 concrete recommendations for the senior counselor. Keep it concise (max 12 lines).

                --- report ---
                """ + reportBody;
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", properties.model());
            payload.put("max_tokens", properties.maxTokens());
            payload.put("temperature", 0.3);
            payload.put("messages", List.of(Map.of("role", "user", "content", prompt)));
            ChatResponse response = client.post()
                    .uri(URI.create(properties.baseUrl().replaceAll("/$", "") + "/chat/completions"))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(ChatResponse.class);
            if (response == null || response.choices == null || response.choices.isEmpty()) {
                return Optional.empty();
            }
            String content = response.choices.get(0).message().content();
            return content == null || content.isBlank() ? Optional.empty() : Optional.of(content.strip());
        } catch (Exception ex) {
            log.error("Counseling AI request failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private record ChatResponse(List<Choice> choices) {}
    private record Choice(Message message) {}
    private record Message(String content) {}
}
