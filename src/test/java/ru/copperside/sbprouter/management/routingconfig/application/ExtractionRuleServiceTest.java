package ru.copperside.sbprouter.management.routingconfig.application;

import org.junit.jupiter.api.Test;
import ru.copperside.sbprouter.management.routingconfig.application.port.out.ExtractionRuleRepository;
import ru.copperside.sbprouter.management.routingconfig.domain.ConfigStatus;
import ru.copperside.sbprouter.management.routingconfig.domain.ExtractionRule;
import ru.copperside.sbprouter.management.routingconfig.domain.FieldBinding;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExtractionRuleServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-28T09:00:00Z"), ZoneOffset.UTC);
    private final InMemoryRepo repository = new InMemoryRepo();
    private final ExtractionRuleService service = new ExtractionRuleService(repository, clock);

    @Test
    void createPersistsDraftWithBindings() {
        ExtractionRule rule = service.create(new SaveExtractionRuleCommand(
                "ReqAuthPay",
                List.of(new FieldBinding("terminalName", "PayProfile", "Tran.TermName", null)),
                List.of(new FieldBinding("amount", null, null, "/Document/GCSvc/Amount"))));

        assertThat(rule.status()).isEqualTo(ConfigStatus.DRAFT);
        assertThat(rule.messageType()).isEqualTo("ReqAuthPay");
        assertThat(rule.routingFields()).hasSize(1);
        assertThat(rule.extraFields()).hasSize(1);
    }

    static class InMemoryRepo implements ExtractionRuleRepository {
        final List<ExtractionRule> saved = new ArrayList<>();
        @Override public ExtractionRule save(ExtractionRule r) {
            saved.removeIf(x -> x.id().equals(r.id())); saved.add(r); return r;
        }
        @Override public Optional<ExtractionRule> findById(UUID id) {
            return saved.stream().filter(x -> x.id().equals(id)).findFirst();
        }
        @Override public List<ExtractionRule> findAll() { return List.copyOf(saved); }
    }
}
