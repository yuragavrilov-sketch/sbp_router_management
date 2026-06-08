package ru.copperside.sbprouter.management.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

/**
 * Конфигурация межсервисной аутентикации {@code /internal/**}.
 *
 * <p>Модель «один ключ = идентичность caller'а»: {@link #acceptedCallers} — map
 * {@code имя-caller'а -> его ключ}. {@link #apiKey} — legacy общий ключ, оставлен для dual-accept на
 * время миграции и подлежит удалению после cutover.
 */
@Validated
@ConfigurationProperties(prefix = "sbp-router-management.internal-admin")
public record InternalAdminSecurityProperties(
        String apiKey,
        Map<String, String> acceptedCallers,
        @NotBlank String headerName
) {
    public InternalAdminSecurityProperties {
        acceptedCallers = acceptedCallers == null ? Map.of() : Map.copyOf(acceptedCallers);
    }

    public boolean enabled() {
        return hasLegacyKey() || hasAcceptedCaller();
    }

    public boolean hasLegacyKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    private boolean hasAcceptedCaller() {
        return acceptedCallers.values().stream().anyMatch(v -> v != null && !v.isBlank());
    }
}
