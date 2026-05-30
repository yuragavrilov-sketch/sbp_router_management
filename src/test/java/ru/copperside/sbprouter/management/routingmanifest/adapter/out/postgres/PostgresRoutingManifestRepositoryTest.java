package ru.copperside.sbprouter.management.routingmanifest.adapter.out.postgres;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import ru.copperside.sbprouter.management.routingconfig.adapter.out.postgres.PostgresExtractionRuleRepository;
import ru.copperside.sbprouter.management.routingconfig.adapter.out.postgres.PostgresRoutingFlagRepository;
import ru.copperside.sbprouter.management.routingconfig.adapter.out.postgres.PostgresTerminalRoutingConfigRepository;
import ru.copperside.sbprouter.management.routingconfig.adapter.out.postgres.PostgresTkbPayListRepository;
import ru.copperside.sbprouter.management.routingconfig.adapter.out.postgres.PostgresUpstreamRepository;
import ru.copperside.sbprouter.management.routingconfig.domain.ConfigStatus;
import ru.copperside.sbprouter.management.routingconfig.domain.ExtractionRule;
import ru.copperside.sbprouter.management.routingconfig.domain.FieldBinding;
import ru.copperside.sbprouter.management.routingconfig.domain.RoutingFlag;
import ru.copperside.sbprouter.management.routingconfig.domain.TerminalRoutingConfig;
import ru.copperside.sbprouter.management.routingconfig.domain.TkbPayListEntry;
import ru.copperside.sbprouter.management.routingconfig.domain.Upstream;
import ru.copperside.sbprouter.management.routingmanifest.application.ProspectiveConfigAssembler;
import ru.copperside.sbprouter.management.routingmanifest.application.RoutingManifestCompiler;
import ru.copperside.sbprouter.management.routingmanifest.application.RoutingManifestService;
import ru.copperside.sbprouter.management.routingmanifest.application.port.out.ManifestPublishedNotifier;
import ru.copperside.sbprouter.management.routingmanifest.domain.ProspectiveConfig;
import ru.copperside.sbprouter.management.routingmanifest.domain.RoutingManifest;
import ru.copperside.sbprouter.management.support.PostgresTestSupport;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@DirtiesContext
class PostgresRoutingManifestRepositoryTest extends PostgresTestSupport {

    @Autowired
    JdbcTemplate jdbc;

    private final Instant now = Instant.parse("2026-05-28T09:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);

    @Test
    void publishPersistsManifestAndPromotesDrafts() {
        var upstreams = new PostgresUpstreamRepository(jdbc);
        var extraction = new PostgresExtractionRuleRepository(jdbc);
        var terminal = new PostgresTerminalRoutingConfigRepository(jdbc);
        var tkb = new PostgresTkbPayListRepository(jdbc);
        var flags = new PostgresRoutingFlagRepository(jdbc);
        var manifests = new PostgresRoutingManifestRepository(jdbc);

        Upstream draftUpstream = new Upstream(UUID.randomUUID(), "infosrv", "http://infosrv/api",
                30000, 2, 500, ConfigStatus.DRAFT, false, 1, now, now);
        upstreams.save(draftUpstream);
        ExtractionRule rule = new ExtractionRule(UUID.randomUUID(), "ReqAuthPay",
                List.of(new FieldBinding("rcvTspId", "PayProfile", "RcvTSPId", null)), List.of(),
                ConfigStatus.DRAFT, false, 1, now, now);
        extraction.save(rule);
        ExtractionRule rule2 = new ExtractionRule(UUID.randomUUID(), "ReqNoticePay",
                List.of(new FieldBinding("terminalName", "AdditionInfo", "Tran.TermName", null)), List.of(),
                ConfigStatus.DRAFT, false, 1, now, now);
        extraction.save(rule2);
        TerminalRoutingConfig term = new TerminalRoutingConfig(UUID.randomUUID(), "rcvTspId", "terminalName",
                "Pay", ConfigStatus.DRAFT, false, 1, now, now);
        terminal.save(term);
        tkb.save(new TkbPayListEntry(UUID.randomUUID(), "MB0000700543", ConfigStatus.DRAFT, false, now, now));
        flags.save(new RoutingFlag(UUID.randomUUID(), "tkb-pay-enabled", "false", ConfigStatus.DRAFT, false, now, now));

        ProspectiveConfig prospective = new ProspectiveConfigAssembler().assemble(
                upstreams.findAll(), extraction.findAll(), terminal.findWorking().stream().toList(),
                tkb.findAll(), flags.findAll());
        RoutingManifest manifest = new RoutingManifestCompiler(clock).compile(manifests.nextVersion(), prospective);

        manifests.publish(manifest, prospective);

        assertThat(manifests.findLatest()).isPresent()
                .get().extracting(RoutingManifest::version).isEqualTo(1);
        assertThat(upstreams.findById(draftUpstream.id())).get()
                .extracting(Upstream::status).isEqualTo(ConfigStatus.ACTIVE);
        assertThat(manifests.latestVersion()).contains(1);
        assertThat(manifests.find(manifest.id())).isPresent();
    }

    @Test
    void publishArchivesOldActiveAndActivatesNewDraftWithSameName() {
        var upstreams = new PostgresUpstreamRepository(jdbc);
        var extraction = new PostgresExtractionRuleRepository(jdbc);
        var terminal = new PostgresTerminalRoutingConfigRepository(jdbc);
        var tkb = new PostgresTkbPayListRepository(jdbc);
        var flags = new PostgresRoutingFlagRepository(jdbc);
        var manifests = new PostgresRoutingManifestRepository(jdbc);

        // Seed an existing ACTIVE upstream with the same name that a draft override will replace.
        UUID oldActiveId = UUID.randomUUID();
        Upstream existingActive = new Upstream(oldActiveId, "infosrv", "http://infosrv-old/api",
                10000, null, null, ConfigStatus.ACTIVE, false, 1, now, now);
        upstreams.save(existingActive);

        // Draft override for the same upstream name with new URL.
        UUID draftId = UUID.randomUUID();
        Upstream draftUpstream = new Upstream(draftId, "infosrv", "http://infosrv-new/api",
                30000, 2, 500, ConfigStatus.DRAFT, false, 2, now, now);
        upstreams.save(draftUpstream);

        ExtractionRule rule = new ExtractionRule(UUID.randomUUID(), "ReqAuthPay",
                List.of(new FieldBinding("rcvTspId", "PayProfile", "RcvTSPId", null)), List.of(),
                ConfigStatus.DRAFT, false, 1, now, now);
        extraction.save(rule);
        TerminalRoutingConfig term = new TerminalRoutingConfig(UUID.randomUUID(), "rcvTspId", "rcvTspId",
                "Pay", ConfigStatus.DRAFT, false, 1, now, now);
        terminal.save(term);

        ProspectiveConfig prospective = new ProspectiveConfigAssembler().assemble(
                upstreams.findAll(), extraction.findAll(), terminal.findWorking().stream().toList(),
                tkb.findAll(), flags.findAll());
        RoutingManifest manifest = new RoutingManifestCompiler(clock).compile(manifests.nextVersion(), prospective);

        manifests.publish(manifest, prospective);

        // The old ACTIVE row must be ARCHIVED; the draft becomes ACTIVE.
        assertThat(upstreams.findById(oldActiveId)).get()
                .extracting(Upstream::status).isEqualTo(ConfigStatus.ARCHIVED);
        assertThat(upstreams.findById(draftId)).get()
                .extracting(Upstream::status).isEqualTo(ConfigStatus.ACTIVE);
    }

    @Test
    void republishSameChecksumCreatesNewVersion() {
        var upstreams = new PostgresUpstreamRepository(jdbc);
        var extraction = new PostgresExtractionRuleRepository(jdbc);
        var terminal = new PostgresTerminalRoutingConfigRepository(jdbc);
        var tkb = new PostgresTkbPayListRepository(jdbc);
        var flags = new PostgresRoutingFlagRepository(jdbc);
        var manifests = new PostgresRoutingManifestRepository(jdbc);

        upstreams.save(new Upstream(UUID.randomUUID(), "infosrv", "http://infosrv/api",
                30000, null, null, ConfigStatus.DRAFT, false, 1, now, now));
        extraction.save(new ExtractionRule(UUID.randomUUID(), "ReqAuthPay",
                List.of(new FieldBinding("rcvTspId", "PayProfile", "RcvTSPId", null)), List.of(),
                ConfigStatus.DRAFT, false, 1, now, now));
        terminal.save(new TerminalRoutingConfig(UUID.randomUUID(), "rcvTspId", "rcvTspId",
                "Pay", ConfigStatus.DRAFT, false, 1, now, now));

        ProspectiveConfig prospective = new ProspectiveConfigAssembler().assemble(
                upstreams.findAll(), extraction.findAll(), terminal.findWorking().stream().toList(),
                tkb.findAll(), flags.findAll());
        RoutingManifest first = new RoutingManifestCompiler(clock).compile(manifests.nextVersion(), prospective);
        manifests.publish(first, prospective);

        // A second manifest with the SAME prospective config → identical checksum, next version.
        RoutingManifest duplicate = new RoutingManifestCompiler(clock).compile(manifests.nextVersion(), prospective);

        // After dropping the checksum unique constraint, this no longer throws.
        manifests.publish(duplicate, prospective);

        assertThat(manifests.findLatest()).isPresent()
                .get().extracting(RoutingManifest::version).isEqualTo(2);
        assertThat(duplicate.checksum()).isEqualTo(first.checksum());
        Integer sameChecksumCount = jdbc.queryForObject(
                "select count(*) from routing_manifests where checksum = ?", Integer.class, first.checksum());
        assertThat(sameChecksumCount).isEqualTo(2);
    }

    @Test
    void serviceEmitsEventOnlyOnNewVersionAndStaysIdempotent() {
        var upstreams = new PostgresUpstreamRepository(jdbc);
        var extraction = new PostgresExtractionRuleRepository(jdbc);
        var terminal = new PostgresTerminalRoutingConfigRepository(jdbc);
        var tkb = new PostgresTkbPayListRepository(jdbc);
        var flags = new PostgresRoutingFlagRepository(jdbc);
        var manifests = new PostgresRoutingManifestRepository(jdbc);

        upstreams.save(new Upstream(UUID.randomUUID(), "infosrv", "http://infosrv/api",
                30000, null, null, ConfigStatus.DRAFT, false, 1, now, now));
        extraction.save(new ExtractionRule(UUID.randomUUID(), "ReqAuthPay",
                List.of(new FieldBinding("rcvTspId", "PayProfile", "RcvTSPId", null)), List.of(),
                ConfigStatus.DRAFT, false, 1, now, now));
        terminal.save(new TerminalRoutingConfig(UUID.randomUUID(), "rcvTspId", "rcvTspId",
                "Pay", ConfigStatus.DRAFT, false, 1, now, now));

        var notified = new ArrayList<RoutingManifest>();
        ManifestPublishedNotifier notifier = notified::add;
        RoutingManifestService service = new RoutingManifestService(
                upstreams, extraction, terminal, tkb, flags, manifests,
                new ProspectiveConfigAssembler(), new RoutingManifestCompiler(clock), notifier);

        RoutingManifest v1 = service.publish();
        RoutingManifest again = service.publish(); // no config change

        assertThat(again.version()).isEqualTo(v1.version());
        assertThat(jdbc.queryForObject("select count(*) from routing_manifests", Integer.class)).isEqualTo(1);
        assertThat(notified).hasSize(1); // emitted only for the genuinely new publish
        assertThat(notified.get(0).version()).isEqualTo(v1.version());
    }
}
