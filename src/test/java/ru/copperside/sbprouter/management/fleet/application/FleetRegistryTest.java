package ru.copperside.sbprouter.management.fleet.application;

import org.junit.jupiter.api.Test;
import ru.copperside.sbprouter.management.fleet.domain.RouterInstance;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FleetRegistryTest {

    private static RouterInstance ri(String id, Instant heartbeat) {
        return new RouterInstance(id, Instant.parse("2026-06-16T09:00:00Z"), heartbeat, "default",
                0L, List.of("default"), List.of(), new RouterInstance.RouterMetrics(0, 0, 0, 0, 0, 0, 0));
    }

    @Test
    void keepsNewestHeartbeatPerInstance() {
        FleetRegistry r = new FleetRegistry();
        r.record(ri("a", Instant.parse("2026-06-16T10:00:00Z")));
        r.record(ri("a", Instant.parse("2026-06-16T10:00:30Z")));
        r.record(ri("a", Instant.parse("2026-06-16T10:00:15Z"))); // out of order, older — ignored

        assertThat(r.all()).hasSize(1);
        assertThat(r.all().get(0).lastHeartbeat()).isEqualTo(Instant.parse("2026-06-16T10:00:30Z"));
    }

    @Test
    void purgeRemovesInstancesOlderThanCutoff() {
        FleetRegistry r = new FleetRegistry();
        r.record(ri("a", Instant.parse("2026-06-16T10:00:00Z")));
        r.record(ri("b", Instant.parse("2026-06-16T10:05:00Z")));

        r.purgeOlderThan(Instant.parse("2026-06-16T10:02:00Z"));

        assertThat(r.all()).extracting(RouterInstance::instanceId).containsExactly("b");
    }

    @Test
    void ignoresNullAndMissingId() {
        FleetRegistry r = new FleetRegistry();
        r.record(null);
        r.record(ri(null, Instant.parse("2026-06-16T10:00:00Z")));
        assertThat(r.all()).isEmpty();
    }
}
