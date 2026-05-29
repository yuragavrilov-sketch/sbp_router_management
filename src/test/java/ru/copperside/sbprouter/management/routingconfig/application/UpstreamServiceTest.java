package ru.copperside.sbprouter.management.routingconfig.application;

import org.junit.jupiter.api.Test;
import ru.copperside.sbprouter.management.routingconfig.application.port.out.UpstreamRepository;
import ru.copperside.sbprouter.management.routingconfig.domain.ConfigStatus;
import ru.copperside.sbprouter.management.routingconfig.domain.RoutingConfigProblemException;
import ru.copperside.sbprouter.management.routingconfig.domain.Upstream;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpstreamServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-28T09:00:00Z"), ZoneOffset.UTC);
    private final InMemoryUpstreamRepository repository = new InMemoryUpstreamRepository();
    private final UpstreamService service = new UpstreamService(repository, clock);

    @Test
    void createPersistsDraft() {
        Upstream created = service.create(new CreateUpstreamCommand(
                "infosrv", "http://infosrv.bank.local/api/gcsvc", 30000, 2, 500));

        assertThat(created.status()).isEqualTo(ConfigStatus.DRAFT);
        assertThat(created.version()).isEqualTo(1);
        assertThat(created.name()).isEqualTo("infosrv");
        assertThat(repository.saved).hasSize(1);
    }

    @Test
    void patchUpdatesExistingDraft() {
        Upstream created = service.create(new CreateUpstreamCommand("stub", "http://localhost/stub", null, null, null));

        Upstream patched = service.patch(created.id(), new PatchUpstreamCommand("http://localhost/stub2", null, null, null));

        assertThat(patched.url()).isEqualTo("http://localhost/stub2");
        assertThat(patched.name()).isEqualTo("stub");
        assertThat(patched.createdAt()).isEqualTo(created.createdAt());
        assertThat(patched.updatedAt()).isAfterOrEqualTo(created.createdAt());
    }

    @Test
    void patchMissingThrowsNotFound() {
        assertThatThrownBy(() -> service.patch(UUID.randomUUID(), new PatchUpstreamCommand("x", null, null, null)))
                .isInstanceOf(RoutingConfigProblemException.class)
                .hasMessageContaining("UPSTREAM_NOT_FOUND");
    }

    @Test
    void markRemovalSetsRemovalFlag() {
        Upstream created = service.create(new CreateUpstreamCommand("stub", "http://localhost/stub", null, null, null));

        Upstream removed = service.markRemoval(created.id());

        assertThat(removed.removal()).isTrue();
        assertThat(removed.id()).isEqualTo(created.id());
        assertThat(removed.name()).isEqualTo("stub");
        assertThat(removed.status()).isEqualTo(ConfigStatus.DRAFT);
    }

    @Test
    void markRemovalMissingThrowsNotFound() {
        assertThatThrownBy(() -> service.markRemoval(UUID.randomUUID()))
                .isInstanceOf(RoutingConfigProblemException.class)
                .hasMessageContaining("UPSTREAM_NOT_FOUND");
    }

    static class InMemoryUpstreamRepository implements UpstreamRepository {
        final List<Upstream> saved = new ArrayList<>();

        @Override
        public Upstream save(Upstream upstream) {
            saved.removeIf(u -> u.id().equals(upstream.id()));
            saved.add(upstream);
            return upstream;
        }

        @Override
        public Optional<Upstream> findById(UUID id) {
            return saved.stream().filter(u -> u.id().equals(id)).findFirst();
        }

        @Override
        public List<Upstream> findAll() {
            return List.copyOf(saved);
        }
    }
}
