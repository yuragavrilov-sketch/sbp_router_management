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
import ru.copperside.sbprouter.management.routingconfig.domain.Upstream;
import ru.copperside.sbprouter.management.support.PostgresTestSupport;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@DirtiesContext
class PostgresUpstreamRepositoryTest extends PostgresTestSupport {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void savesAndReadsBack() {
        PostgresUpstreamRepository repository = new PostgresUpstreamRepository(jdbc);
        Instant now = Instant.parse("2026-05-28T09:00:00Z");
        Upstream upstream = new Upstream(UUID.randomUUID(), "infosrv", "http://infosrv/api", 30000, 2, 500,
                ConfigStatus.DRAFT, false, 1, now, now);

        repository.save(upstream);

        assertThat(repository.findById(upstream.id())).isPresent()
                .get().extracting(Upstream::name).isEqualTo("infosrv");
        assertThat(repository.findAll()).hasSize(1);
    }

    @Test
    void saveIsUpsert() {
        PostgresUpstreamRepository repository = new PostgresUpstreamRepository(jdbc);
        Instant now = Instant.parse("2026-05-28T09:00:00Z");
        UUID id = UUID.randomUUID();
        repository.save(new Upstream(id, "stub", "http://a", null, null, null, ConfigStatus.DRAFT, false, 1, now, now));
        repository.save(new Upstream(id, "stub", "http://b", null, null, null, ConfigStatus.DRAFT, false, 1, now, now));

        assertThat(repository.findAll()).hasSize(1);
        assertThat(repository.findById(id)).get().extracting(Upstream::url).isEqualTo("http://b");
    }
}
