package ru.copperside.sbprouter.management.routing.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RoutingConfigJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void omitsAuthPayWhenNull() throws Exception {
        RoutingConfig c = new RoutingConfig(1L, "default",
                Map.of("default", new RoutingConfig.Group(List.of("http://a/api"))));
        String json = mapper.writeValueAsString(c);
        assertThat(json).doesNotContain("authPay");
    }

    @Test
    void roundTripsAuthPay() throws Exception {
        RoutingConfig c = new RoutingConfig(2L, "default",
                Map.of("default", new RoutingConfig.Group(List.of("http://a/api"))),
                new RoutingConfig.AuthPay(true, List.of("http://authpay/x"), 1500));
        RoutingConfig back = mapper.readValue(mapper.writeValueAsString(c), RoutingConfig.class);
        assertThat(back.authPay().enabled()).isTrue();
        assertThat(back.authPay().backends()).containsExactly("http://authpay/x");
        assertThat(back.authPay().timeoutMs()).isEqualTo(1500);
        assertThat(back.withVersion(9L).authPay()).isEqualTo(c.authPay());
    }
}
