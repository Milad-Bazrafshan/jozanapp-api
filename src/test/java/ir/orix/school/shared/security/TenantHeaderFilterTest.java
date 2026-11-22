package ir.orix.school.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import java.time.Instant;
import java.util.UUID;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class TenantHeaderFilterTest {
    @AfterEach void clearSecurityContext() { SecurityContextHolder.clearContext(); }

    @Test
    void acceptsTheTenantCarriedByTheToken() throws Exception {
        UUID tenantId = UUID.randomUUID();
        authenticate(tenantId);
        TenantHeaderFilter filter = new TenantHeaderFilter(new SecurityProblemWriter(new ObjectMapper()));
        MockHttpServletRequest request = request(tenantId.toString());
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    void rejectsAHeaderForAnotherTenant() throws Exception {
        authenticate(UUID.randomUUID());
        TenantHeaderFilter filter = new TenantHeaderFilter(new SecurityProblemWriter(new ObjectMapper()));
        MockHttpServletRequest request = request(UUID.randomUUID().toString());
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("TENANT_ACCESS_DENIED");
        verifyNoInteractions(chain);
    }

    private static MockHttpServletRequest request(String tenantId) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/organization/current");
        request.addHeader(TenantHeaderFilter.TENANT_HEADER, tenantId);
        return request;
    }

    private static void authenticate(UUID tenantId) {
        Instant now = Instant.now();
        Jwt jwt = new Jwt("token", now, now.plusSeconds(60),
                java.util.Map.of("alg", "HS256"),
                java.util.Map.of("sub", UUID.randomUUID().toString(), "tenantId", tenantId.toString()));
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, java.util.List.of(new SimpleGrantedAuthority("ORGANIZATION_READ"))));
    }
}
