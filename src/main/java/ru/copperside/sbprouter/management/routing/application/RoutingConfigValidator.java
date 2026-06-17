package ru.copperside.sbprouter.management.routing.application;

import org.springframework.stereotype.Component;
import ru.copperside.sbprouter.management.routing.domain.RoutingConfig;
import ru.copperside.sbprouter.management.routing.domain.RoutingConfigProblemException;

import java.util.List;
import java.util.Map;

@Component
public class RoutingConfigValidator {

    private static final String CODE = "ROUTING_CONFIG_INVALID";

    public void validate(RoutingConfig config) {
        Map<String, RoutingConfig.Group> groups = config.groups();
        if (groups == null || groups.isEmpty()) {
            throw new RoutingConfigProblemException(CODE, "at least one group is required");
        }
        groups.forEach((name, group) -> {
            List<String> backends = group == null ? null : group.backends();
            if (backends == null || backends.isEmpty()) {
                throw new RoutingConfigProblemException(CODE, "group '" + name + "' must have at least one backend");
            }
            for (String url : backends) {
                if (url == null || url.isBlank()) {
                    throw new RoutingConfigProblemException(CODE, "group '" + name + "' has a blank backend url");
                }
            }
        });
        String active = config.activeGroup();
        if (active == null || active.isBlank() || !groups.containsKey(active)) {
            throw new RoutingConfigProblemException(CODE, "activeGroup '" + active + "' must be one of " + groups.keySet());
        }
        RoutingConfig.AuthPay authPay = config.authPay();
        if (authPay != null && authPay.enabled()) {
            List<String> backends = authPay.backends();
            if (backends == null || backends.isEmpty()) {
                throw new RoutingConfigProblemException(CODE, "authPay.backends must have at least one backend when enabled");
            }
            for (String url : backends) {
                if (url == null || url.isBlank()) {
                    throw new RoutingConfigProblemException(CODE, "authPay.backends has a blank url");
                }
            }
            if (authPay.timeoutMs() != null && authPay.timeoutMs() <= 0) {
                throw new RoutingConfigProblemException(CODE, "authPay.timeoutMs must be positive");
            }
            if (authPay.sbpOperations() != null) {
                for (String op : authPay.sbpOperations()) {
                    if (op == null || op.isBlank()) {
                        throw new RoutingConfigProblemException(CODE, "authPay.sbpOperations has a blank value");
                    }
                }
            }
        }
    }
}
