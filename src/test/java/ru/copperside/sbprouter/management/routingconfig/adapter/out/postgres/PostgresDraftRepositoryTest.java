package ru.copperside.sbprouter.management.routingconfig.adapter.out.postgres;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import ru.copperside.sbprouter.management.routingconfig.domain.ConfigStatus;
import ru.copperside.sbprouter.management.routingconfig.domain.TkbPayListEntry;
import ru.copperside.sbprouter.management.routingconfig.domain.Upstream;
import ru.copperside.sbprouter.management.support.PostgresTestSupport;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@DirtiesContext
class PostgresDraftRepositoryTest extends PostgresTestSupport {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void discardRemovesDraftsAndClearsRemovalFlags() {
        var upstreams = new PostgresUpstreamRepository(jdbc);
        var tkb = new PostgresTkbPayListRepository(jdbc);
        var drafts = new PostgresDraftRepository(jdbc);
        Instant now = Instant.parse("2026-05-28T09:00:00Z");

        UUID draftId = UUID.randomUUID();
        upstreams.save(new Upstream(draftId, "draft", "http://d", null, null, null, ConfigStatus.DRAFT, false, 1, now, now));
        UUID activeWithRemoval = UUID.randomUUID();
        tkb.save(new TkbPayListEntry(activeWithRemoval, "MB1", ConfigStatus.ACTIVE, true, now, now));

        drafts.discardAll();

        assertThat(upstreams.findById(draftId)).isEmpty();
        assertThat(tkb.findById(activeWithRemoval)).get()
                .extracting(TkbPayListEntry::removal).isEqualTo(false);
    }
}
