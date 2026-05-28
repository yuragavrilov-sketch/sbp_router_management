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
import ru.copperside.sbprouter.management.routingconfig.domain.ExtractionRule;
import ru.copperside.sbprouter.management.routingconfig.domain.FieldBinding;
import ru.copperside.sbprouter.management.support.PostgresTestSupport;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@DirtiesContext
class PostgresExtractionRuleRepositoryTest extends PostgresTestSupport {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void savesAndReadsBindings() {
        PostgresExtractionRuleRepository repository = new PostgresExtractionRuleRepository(jdbc);
        Instant now = Instant.parse("2026-05-28T09:00:00Z");
        ExtractionRule rule = new ExtractionRule(UUID.randomUUID(), "ReqAuthPay",
                List.of(new FieldBinding("terminalName", "PayProfile", "Tran.TermName", null)),
                List.of(new FieldBinding("amount", null, null, "/Document/GCSvc/Amount")),
                ConfigStatus.DRAFT, false, 1, now, now);

        repository.save(rule);

        ExtractionRule loaded = repository.findById(rule.id()).orElseThrow();
        assertThat(loaded.routingFields()).containsExactly(
                new FieldBinding("terminalName", "PayProfile", "Tran.TermName", null));
        assertThat(loaded.extraFields().get(0).path()).isEqualTo("/Document/GCSvc/Amount");
    }
}
