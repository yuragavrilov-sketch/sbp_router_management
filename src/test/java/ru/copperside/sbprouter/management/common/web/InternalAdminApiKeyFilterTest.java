package ru.copperside.sbprouter.management.common.web;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.copperside.sbprouter.management.config.InternalAdminSecurityProperties;

import java.time.Clock;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class InternalAdminApiKeyFilterTest {

    private final Clock clock = Clock.fixed(java.time.Instant.parse("2026-05-29T09:00:00Z"), ZoneOffset.UTC);

    private OncePerRequestFilter filter(String configuredKey) {
        return new InternalAdminApiKeyFilter(
                new InternalAdminSecurityProperties(configuredKey, "X-Internal-Admin-Key"), clock);
    }

    @Test
    void validKeyPassesThrough() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/internal/v1/sbp-router-management/upstreams");
        req.addHeader("X-Internal-Admin-Key", "secret");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter("secret").doFilter(req, res, chain);

        verify(chain, times(1)).doFilter(req, res);
        assertThat(res.getStatus()).isEqualTo(200);
    }

    @Test
    void missingOrWrongKeyReturns401Problem() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/internal/v1/sbp-router-management/upstreams");
        req.addHeader("X-Internal-Admin-Key", "wrong");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter("secret").doFilter(req, res, chain);

        verify(chain, never()).doFilter(req, res);
        assertThat(res.getStatus()).isEqualTo(401);
        assertThat(res.getContentType()).contains("application/problem+json");
        assertThat(res.getContentAsString()).contains("UNAUTHORIZED");
    }

    @Test
    void disabledWhenKeyBlankLetsEverythingThrough() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/internal/v1/sbp-router-management/upstreams");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter("").doFilter(req, res, chain);

        verify(chain, times(1)).doFilter(req, res);
    }

    @Test
    void nonInternalPathNotFilteredEvenWhenEnabled() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter("secret").doFilter(req, res, chain);

        verify(chain, times(1)).doFilter(req, res);
    }
}
