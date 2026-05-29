package ru.copperside.sbprouter.management.traffic.application;

import org.junit.jupiter.api.Test;
import ru.copperside.sbprouter.management.traffic.application.port.out.TrafficRetentionRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TrafficRetentionServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-29T09:00:00Z"), ZoneOffset.UTC);

    @Test
    void purgeUsesCutoffOfNowMinusRetentionDays() {
        AtomicReference<Instant> captured = new AtomicReference<>();
        TrafficRetentionRepository repo = cutoff -> { captured.set(cutoff); return 7; };
        TrafficRetentionService service = new TrafficRetentionService(repo, clock, 30);

        int deleted = service.purge();

        assertThat(deleted).isEqualTo(7);
        assertThat(captured.get()).isEqualTo(Instant.parse("2026-04-29T09:00:00Z"));
    }
}
