package ru.copperside.sbprouter.management.routingconfig.application;

import org.junit.jupiter.api.Test;
import ru.copperside.sbprouter.management.routingconfig.application.port.out.RoutingFlagRepository;
import ru.copperside.sbprouter.management.routingconfig.domain.ConfigStatus;
import ru.copperside.sbprouter.management.routingconfig.domain.RoutingFlag;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RoutingFlagServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-28T09:00:00Z"), ZoneOffset.UTC);
    private final Repo repo = new Repo();
    private final RoutingFlagService service = new RoutingFlagService(repo, clock);

    @Test
    void setCreatesDraftFlag() {
        RoutingFlag flag = service.set("tkb-pay-enabled", "false");
        assertThat(flag.status()).isEqualTo(ConfigStatus.DRAFT);
        assertThat(flag.key()).isEqualTo("tkb-pay-enabled");
        assertThat(flag.value()).isEqualTo("false");
    }

    @Test
    void setReusesExistingDraftByKey() {
        RoutingFlag first = service.set("tkb-pay-enabled", "false");
        RoutingFlag second = service.set("tkb-pay-enabled", "true");
        assertThat(second.id()).isEqualTo(first.id());
        assertThat(second.value()).isEqualTo("true");
    }

    static class Repo implements RoutingFlagRepository {
        final List<RoutingFlag> saved = new ArrayList<>();
        @Override public RoutingFlag save(RoutingFlag f) {
            saved.removeIf(x -> x.id().equals(f.id())); saved.add(f); return f;
        }
        @Override public Optional<RoutingFlag> findWorkingByKey(String key) {
            return saved.stream().filter(x -> x.key().equals(key)
                    && (x.status() == ConfigStatus.DRAFT || x.status() == ConfigStatus.ACTIVE)).findFirst();
        }
        @Override public List<RoutingFlag> findAll() { return List.copyOf(saved); }
    }
}
