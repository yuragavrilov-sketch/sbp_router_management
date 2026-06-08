package ru.copperside.sbprouter.management.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.copperside.sbprouter.management.config.InternalAdminSecurityProperties;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;

public class InternalAdminApiKeyFilter extends OncePerRequestFilter {

    private static final String TYPE_BASE = "https://contracts.newpay/errors/";

    /** MDC-ключ для идентичности вызывающего сервиса (виден в логах). */
    static final String CALLER_MDC_KEY = "caller";
    /** Имя для совпадения по legacy общему ключу (dual-accept на время миграции). */
    static final String LEGACY_CALLER = "legacy-shared-key";

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
        String caller = resolveCaller(request.getHeader(properties.headerName()));
        if (caller == null) {
            writeUnauthorized(response);
            return;
        }
        MDC.put(CALLER_MDC_KEY, caller);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CALLER_MDC_KEY);
        }
    }

    /**
     * Возвращает имя совпавшего caller'а (или {@link #LEGACY_CALLER} для legacy-ключа), либо
     * {@code null}, если ключ отсутствует/не совпал. Перебираем все ключи без раннего выхода, чтобы
     * не давать timing-сигнал о номере совпавшего ключа.
     */
    private String resolveCaller(String provided) {
        if (provided == null || provided.isBlank()) {
            return null;
        }
        byte[] given = provided.getBytes(StandardCharsets.UTF_8);
        String match = null;
        for (Map.Entry<String, String> entry : properties.acceptedCallers().entrySet()) {
            if (keyMatches(entry.getValue(), given)) {
                match = entry.getKey();
            }
        }
        if (properties.hasLegacyKey() && keyMatches(properties.apiKey(), given) && match == null) {
            match = LEGACY_CALLER;
        }
        return match;
    }

    private boolean keyMatches(String configured, byte[] given) {
        if (configured == null || configured.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(configured.getBytes(StandardCharsets.UTF_8), given);
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
