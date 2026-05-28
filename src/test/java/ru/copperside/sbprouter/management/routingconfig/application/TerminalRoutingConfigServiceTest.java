package ru.copperside.sbprouter.management.routingconfig.application;

import org.junit.jupiter.api.Test;
import ru.copperside.sbprouter.management.routingconfig.application.port.out.TerminalRoutingConfigRepository;
import ru.copperside.sbprouter.management.routingconfig.domain.ConfigStatus;
import ru.copperside.sbprouter.management.routingconfig.domain.TerminalRoutingConfig;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TerminalRoutingConfigServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-28T09:00:00Z"), ZoneOffset.UTC);

    @Test
    void saveCreatesDraftWhenNoWorkingRow() {
        var holder = new Object() { TerminalRoutingConfig stored; };
        TerminalRoutingConfigRepository repo = new TerminalRoutingConfigRepository() {
            @Override public TerminalRoutingConfig save(TerminalRoutingConfig c) { holder.stored = c; return c; }
            @Override public Optional<TerminalRoutingConfig> findWorking() { return Optional.ofNullable(holder.stored); }
        };
        TerminalRoutingConfigService service = new TerminalRoutingConfigService(repo, clock);

        TerminalRoutingConfig saved = service.save(new SaveTerminalConfigCommand("rcvTspId", "terminalName", "Pay"));

        assertThat(saved.status()).isEqualTo(ConfigStatus.DRAFT);
        assertThat(saved.c2bFieldName()).isEqualTo("rcvTspId");
        assertThat(saved.b2cFieldName()).isEqualTo("terminalName");
        assertThat(saved.tkbPayPrefix()).isEqualTo("Pay");
    }
}
