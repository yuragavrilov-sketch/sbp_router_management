package ru.copperside.sbprouter.management.routing.application;

import org.junit.jupiter.api.Test;
import ru.copperside.sbprouter.management.routing.domain.RoutingConfig;
import ru.copperside.sbprouter.management.routing.domain.RoutingConfig.AuthPay;
import ru.copperside.sbprouter.management.routing.domain.RoutingConfigProblemException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoutingConfigValidatorTest {

    private final RoutingConfigValidator validator = new RoutingConfigValidator();

    private static RoutingConfig cfg(String active, Map<String, List<String>> groups) {
        Map<String, RoutingConfig.Group> g = new java.util.LinkedHashMap<>();
        groups.forEach((k, v) -> g.put(k, new RoutingConfig.Group(v)));
        return new RoutingConfig(null, active, g);
    }

    @Test
    void acceptsValidConfig() {
        assertThatCode(() -> validator.validate(cfg("default", Map.of("default", List.of("http://a/api")))))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingActiveGroup() {
        assertThatThrownBy(() -> validator.validate(cfg("nope", Map.of("default", List.of("http://a/api")))))
                .isInstanceOf(RoutingConfigProblemException.class);
        assertThatThrownBy(() -> validator.validate(cfg(" ", Map.of("default", List.of("http://a/api")))))
                .isInstanceOf(RoutingConfigProblemException.class);
    }

    @Test
    void rejectsEmptyGroupsOrBackendsOrBlankUrl() {
        assertThatThrownBy(() -> validator.validate(cfg("default", Map.of())))
                .isInstanceOf(RoutingConfigProblemException.class);
        assertThatThrownBy(() -> validator.validate(cfg("default", Map.of("default", List.of()))))
                .isInstanceOf(RoutingConfigProblemException.class);
        assertThatThrownBy(() -> validator.validate(cfg("default", Map.of("default", List.of(" ")))))
                .isInstanceOf(RoutingConfigProblemException.class);
    }

    private static RoutingConfig withAuthPay(RoutingConfig.AuthPay ap) {
        Map<String, RoutingConfig.Group> g = new java.util.LinkedHashMap<>();
        g.put("default", new RoutingConfig.Group(List.of("http://a/api")));
        return new RoutingConfig(null, "default", g, ap);
    }

    @Test
    void acceptsDisabledOrAbsentAuthPay() {
        assertThatCode(() -> validator.validate(withAuthPay(null))).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(withAuthPay(new RoutingConfig.AuthPay(false, List.of(), null))))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsEnabledAuthPayWithBackends() {
        assertThatCode(() -> validator.validate(
                withAuthPay(new RoutingConfig.AuthPay(true, List.of("http://authpay/x"), 1500))))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsEnabledAuthPayWithoutBackends() {
        assertThatThrownBy(() -> validator.validate(withAuthPay(new RoutingConfig.AuthPay(true, List.of(), null))))
                .isInstanceOf(RoutingConfigProblemException.class);
        assertThatThrownBy(() -> validator.validate(withAuthPay(new RoutingConfig.AuthPay(true, List.of(" "), null))))
                .isInstanceOf(RoutingConfigProblemException.class);
    }

    @Test
    void rejectsNonPositiveAuthPayTimeout() {
        assertThatThrownBy(() -> validator.validate(
                withAuthPay(new RoutingConfig.AuthPay(true, List.of("http://authpay/x"), 0))))
                .isInstanceOf(RoutingConfigProblemException.class);
    }
}
