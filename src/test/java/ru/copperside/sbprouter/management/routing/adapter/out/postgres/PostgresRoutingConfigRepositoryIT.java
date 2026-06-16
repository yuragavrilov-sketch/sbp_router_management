package ru.copperside.sbprouter.management.routing.adapter.out.postgres;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import ru.copperside.sbprouter.management.routing.domain.RoutingConfig;
import ru.copperside.sbprouter.management.support.PostgresTestSupport;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import(PostgresRoutingConfigRepository.class)
@DirtiesContext
class PostgresRoutingConfigRepositoryIT extends PostgresTestSupport {

    @Autowired
    PostgresRoutingConfigRepository repo;

    private static RoutingConfig cfg(long v, String active) {
        return new RoutingConfig(v, active, Map.of("default", new RoutingConfig.Group(List.of("http://a/api"))));
    }

    @Test
    void savesAndReadsLatestByVersion() {
        assertThat(repo.latest()).isEmpty();
        assertThat(repo.nextVersion()).isEqualTo(1L);

        repo.save(cfg(1L, "default"));
        repo.save(cfg(2L, "default"));

        assertThat(repo.nextVersion()).isEqualTo(3L);
        assertThat(repo.latest()).hasValueSatisfying(c -> {
            assertThat(c.version()).isEqualTo(2L);
            assertThat(c.groups()).containsKey("default");
        });
    }
}
