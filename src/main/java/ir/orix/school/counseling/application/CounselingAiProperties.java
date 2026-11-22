package ir.orix.school.counseling.application;

import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.counseling.ai")
public record CounselingAiProperties(
        boolean enabled,
        String baseUrl,
        String apiKey,
        String model,
        int maxTokens) {
    public CounselingAiProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.openai.com/v1";
        }
        if (model == null || model.isBlank()) {
            model = "gpt-4o-mini";
        }
        if (maxTokens <= 0) {
            maxTokens = 800;
        }
    }
}
