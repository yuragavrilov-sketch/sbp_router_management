package ru.copperside.sbprouter.management.fleet.adapter.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.copperside.sbprouter.management.fleet.domain.RouterInstance;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class HeartbeatMapperTest {

    private final HeartbeatMapper mapper = new HeartbeatMapper(new ObjectMapper());

    @Test
    void parsesFullHeartbeat() {
        String json = """
                {"instanceId":"i-1","startedAt":"2026-06-16T09:00:00Z","timestamp":"2026-06-16T10:00:00Z",
                 "activeGroup":"default","routingConfigVersion":5,"groups":["default","secondary"],
                 "backends":[{"url":"http://a/api","group":"default","banned":false},
                             {"url":"http://b/api","group":"secondary","banned":true}],
                 "metrics":{"activeRequests":2,"requestsTotal":10,"upstreamErrorsTotal":1,
                            "kafkaPublishedTotal":20,"requestCount":10,"avgLatencyMs":40.5,"maxLatencyMs":120.0}}""";

        RouterInstance ri = mapper.map(json.getBytes(StandardCharsets.UTF_8));

        assertThat(ri).isNotNull();
        assertThat(ri.instanceId()).isEqualTo("i-1");
        assertThat(ri.startedAt()).isEqualTo(Instant.parse("2026-06-16T09:00:00Z"));
        assertThat(ri.lastHeartbeat()).isEqualTo(Instant.parse("2026-06-16T10:00:00Z"));
        assertThat(ri.activeGroup()).isEqualTo("default");
        assertThat(ri.routingConfigVersion()).isEqualTo(5L);
        assertThat(ri.groups()).containsExactly("default", "secondary");
        assertThat(ri.backends()).hasSize(2);
        assertThat(ri.backends().get(1).banned()).isTrue();
        assertThat(ri.metrics().activeRequests()).isEqualTo(2);
        assertThat(ri.metrics().avgLatencyMs()).isEqualTo(40.5);
    }

    @Test
    void returnsNullForBadOrIncompletePayloads() {
        assertThat(mapper.map("not json".getBytes(StandardCharsets.UTF_8))).isNull();
        assertThat(mapper.map("{\"activeGroup\":\"x\"}".getBytes(StandardCharsets.UTF_8))).isNull(); // no instanceId
        assertThat(mapper.map(null)).isNull();
        assertThat(mapper.map(new byte[0])).isNull();
    }
}
