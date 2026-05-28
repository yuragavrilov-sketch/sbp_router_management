package ru.copperside.sbprouter.management.routingconfig.application;

import org.junit.jupiter.api.Test;
import ru.copperside.sbprouter.management.routingconfig.application.port.out.TkbPayListRepository;
import ru.copperside.sbprouter.management.routingconfig.domain.ConfigStatus;
import ru.copperside.sbprouter.management.routingconfig.domain.RoutingConfigProblemException;
import ru.copperside.sbprouter.management.routingconfig.domain.TkbPayListEntry;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TkbPayListServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-28T09:00:00Z"), ZoneOffset.UTC);
    private final Repo repo = new Repo();
    private final TkbPayListService service = new TkbPayListService(repo, clock);

    @Test
    void addCreatesDraftEntry() {
        TkbPayListEntry entry = service.add("MB0000700543");
        assertThat(entry.status()).isEqualTo(ConfigStatus.DRAFT);
        assertThat(entry.rcvTspId()).isEqualTo("MB0000700543");
    }

    @Test
    void markRemovalSetsRemovalFlag() {
        TkbPayListEntry entry = service.add("MB0000700543");
        TkbPayListEntry removed = service.markRemoval(entry.id());
        assertThat(removed.removal()).isTrue();
    }

    @Test
    void markRemovalMissingThrows() {
        assertThatThrownBy(() -> service.markRemoval(UUID.randomUUID()))
                .isInstanceOf(RoutingConfigProblemException.class)
                .hasMessageContaining("TKB_PAY_ENTRY_NOT_FOUND");
    }

    static class Repo implements TkbPayListRepository {
        final List<TkbPayListEntry> saved = new ArrayList<>();
        @Override public TkbPayListEntry save(TkbPayListEntry e) {
            saved.removeIf(x -> x.id().equals(e.id())); saved.add(e); return e;
        }
        @Override public Optional<TkbPayListEntry> findById(UUID id) {
            return saved.stream().filter(x -> x.id().equals(id)).findFirst();
        }
        @Override public List<TkbPayListEntry> findAll() { return List.copyOf(saved); }
    }
}
