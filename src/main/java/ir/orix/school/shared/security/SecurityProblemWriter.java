package ir.orix.school.shared.security;

import java.io.IOException;
import java.util.Map;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import ir.orix.school.shared.config.RequestTraceFilter;

@Component
public class SecurityProblemWriter {
    private final ObjectMapper objectMapper;

    public SecurityProblemWriter(ObjectMapper objectMapper) { this.objectMapper = objectMapper; }

    public void write(HttpServletResponse response, int status, String code, String detail) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "type", "https://school.orix.ir/problems/" + code.toLowerCase().replace('_', '-'),
                "title", status == 401 ? "Unauthorized" : status == 403 ? "Forbidden" : "Invalid request",
                "status", status,
                "detail", detail,
                "code", code,
                "traceId", String.valueOf(MDC.get(RequestTraceFilter.TRACE_KEY))));
    }
}
