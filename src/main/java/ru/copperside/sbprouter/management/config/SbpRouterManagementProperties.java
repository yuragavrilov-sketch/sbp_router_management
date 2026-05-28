package ru.copperside.sbprouter.management.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "sbp-router-management")
public record SbpRouterManagementProperties(
        @NotBlank String serviceName
) {
}
