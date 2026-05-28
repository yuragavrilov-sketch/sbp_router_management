package ru.copperside.sbprouter.management.routingmanifest.application;

import org.junit.jupiter.api.Test;
import ru.copperside.sbprouter.management.routingconfig.domain.ConfigStatus;
import ru.copperside.sbprouter.management.routingconfig.domain.Upstream;
import ru.copperside.sbprouter.management.routingmanifest.domain.PendingChanges;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProspectiveConfigAssemblerTest {

    private final Instant now = Instant.parse("2026-05-28T09:00:00Z");
    private final ProspectiveConfigAssembler assembler = new ProspectiveConfigAssembler();

    private Upstream upstream(String name, String url, ConfigStatus status, boolean removal) {
        return new Upstream(UUID.randomUUID(), name, url, null, null, null, status, removal, 1, now, now);
    }

    @Test
    void draftOverridesLiveActiveBySameName() {
        Upstream live = upstream("infosrv", "http://old", ConfigStatus.ACTIVE, false);
        Upstream draft = upstream("infosrv", "http://new", ConfigStatus.DRAFT, false);

        List<Upstream> prospective = assembler.resolveUpstreams(List.of(live, draft));

        assertThat(prospective).hasSize(1);
        assertThat(prospective.get(0).url()).isEqualTo("http://new");
    }

    @Test
    void removalDropsLiveActive() {
        Upstream removed = upstream("infosrv", "http://old", ConfigStatus.ACTIVE, true);
        assertThat(assembler.resolveUpstreams(List.of(removed))).isEmpty();
    }

    @Test
    void draftAddsNewKey() {
        Upstream live = upstream("infosrv", "http://a", ConfigStatus.ACTIVE, false);
        Upstream added = upstream("stub", "http://b", ConfigStatus.DRAFT, false);
        assertThat(assembler.resolveUpstreams(List.of(live, added))).hasSize(2);
    }

    @Test
    void pendingChangesCountsAddModifyRemove() {
        Upstream liveKept = upstream("keep", "http://k", ConfigStatus.ACTIVE, false);
        Upstream liveRemoved = upstream("drop", "http://d", ConfigStatus.ACTIVE, true);
        Upstream draftAdded = upstream("new", "http://n", ConfigStatus.DRAFT, false);

        PendingChanges pending = assembler.pendingChanges(
                List.of(liveKept, liveRemoved, draftAdded),
                List.of(), null, List.of(), List.of(), 3, null);

        assertThat(pending.count()).isEqualTo(2);
        assertThat(pending.nextVersion()).isEqualTo(3);
    }
}
