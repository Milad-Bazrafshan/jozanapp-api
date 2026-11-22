package ir.orix.school.system.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ir.orix.school.shared.config.RequestTraceFilter;
import ir.orix.school.system.application.SystemHealthService;

@ExtendWith(MockitoExtension.class)
class SystemHealthControllerTest {

    @Mock
    private SystemHealthService healthService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SystemHealthController(healthService))
                .addFilters(new RequestTraceFilter())
                .build();
    }

    @Test
    void returnsBackendHealthAndTraceId() throws Exception {
        when(healthService.getHealth()).thenReturn(new SystemHealthResponse(
                "UP",
                "school-backend-api",
                "0.1.0-SNAPSHOT",
                Instant.parse("2026-09-15T08:30:00Z")));

        mockMvc.perform(get("/api/v1/system/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists(RequestTraceFilter.TRACE_HEADER))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("school-backend-api"));
    }
}
