package ru.copperside.sbprouter.management.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.copperside.sbprouter.management.config.InternalAdminSecurityProperties;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.UUID;

public class InternalAdminApiKeyFilter extends OncePerRequestFilter {

    private static final String TYPE_BASE = "https://contracts.newpay/errors/";

    private final InternalAdminSecurityProperties properties;
    private final Clock clock;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public InternalAdminApiKeyFilter(InternalAdminSecurityProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !properties.enabled() || !request.getRequestURI().startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (matchesConfiguredKey(request.getHeader(properties.headerName()))) {
            filterChain.doFilter(request, response);
            return;
        }
        writeUnauthorized(response);
    }

    private boolean matchesConfiguredKey(String provided) {
        if (provided == null || provided.isBlank()) {
            return false;
        }
        byte[] expected = properties.apiKey().getBytes(StandardCharsets.UTF_8);
        byte[] given = provided.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, given);
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        ProblemDetail detail = new ProblemDetail(
                TYPE_BASE + "unauthorized",
                "Unauthorized",
                HttpStatus.UNAUTHORIZED.value(),
                "UNAUTHORIZED",
                "Missing or invalid internal admin API key",
                null,
                UUID.randomUUID().toString());
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ProblemEnvelope.of(detail, clock));
    }
}
