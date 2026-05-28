package ru.copperside.sbprouter.management.routingmanifest.application;

import org.junit.jupiter.api.Test;
import ru.copperside.sbprouter.management.routingconfig.domain.ConfigStatus;
import ru.copperside.sbprouter.management.routingconfig.domain.ExtractionRule;
import ru.copperside.sbprouter.management.routingconfig.domain.FieldBinding;
import ru.copperside.sbprouter.management.routingconfig.domain.RoutingFlag;
import ru.copperside.sbprouter.management.routingconfig.domain.TerminalRoutingConfig;
import ru.copperside.sbprouter.management.routingconfig.domain.TkbPayListEntry;
import ru.copperside.sbprouter.management.routingconfig.domain.Upstream;
import ru.copperside.sbprouter.management.routingmanifest.domain.ProspectiveConfig;
import ru.copperside.sbprouter.management.routingmanifest.domain.RoutingManifest;
import ru.copperside.sbprouter.management.routingmanifest.domain.RoutingManifestProblemException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoutingManifestCompilerTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-28T09:00:00Z"), ZoneOffset.UTC);
    private final RoutingManifestCompiler compiler = new RoutingManifestCompiler(clock);

    private final Instant now = Instant.parse("2026-05-28T09:00:00Z");

    private Upstream upstream(String name) {
        return new Upstream(UUID.randomUUID(), name, "http://" + name + "/api", 30000, 2, 500,
                ConfigStatus.ACTIVE, false, 1, now, now);
    }

    private ExtractionRule rule(String type, String routingFieldName) {
        return new ExtractionRule(UUID.randomUUID(), type,
                List.of(new FieldBinding(routingFieldName, "PayProfile", "Tran.TermName", null)),
                List.of(), ConfigStatus.ACTIVE, false, 1, now, now);
    }

    private TerminalRoutingConfig terminal(String c2b, String b2c) {
        return new TerminalRoutingConfig(UUID.randomUUID(), c2b, b2c, "Pay", ConfigStatus.ACTIVE, false, 1, now, now);
    }

    private ProspectiveConfig validConfig() {
        return new ProspectiveConfig(
                List.of(upstream("infosrv")),
                List.of(rule("ReqAuthPay", "rcvTspId"), rule("ReqNoticePay", "terminalName")),
                terminal("rcvTspId", "terminalName"),
                List.of(new TkbPayListEntry(UUID.randomUUID(), "MB0000700543", ConfigStatus.ACTIVE, false, now, now)),
                List.of(new RoutingFlag(UUID.randomUUID(), "tkb-pay-enabled", "false", ConfigStatus.ACTIVE, false, now, now)));
    }

    @Test
    void compilesValidConfig() {
        RoutingManifest manifest = compiler.compile(1, validConfig());
        assertThat(manifest.version()).isEqualTo(1);
        assertThat(manifest.checksum()).startsWith("sha256:");
        assertThat(manifest.content().upstreams()).containsKey("infosrv");
        assertThat(manifest.content().terminals().tkbPayList()).containsExactly("MB0000700543");
    }

    @Test
    void checksumIsStableAcrossRowOrder() {
        ProspectiveConfig a = validConfig();
        ProspectiveConfig b = new ProspectiveConfig(
                a.upstreams(),
                List.of(a.extractionRules().get(1), a.extractionRules().get(0)),
                a.terminalConfig(), a.tkbPayList(), a.routingFlags());
        assertThat(compiler.compile(1, a).checksum()).isEqualTo(compiler.compile(1, b).checksum());
    }

    @Test
    void rejectsEmptyUpstreams() {
        ProspectiveConfig c = new ProspectiveConfig(List.of(),
                validConfig().extractionRules(), validConfig().terminalConfig(),
                validConfig().tkbPayList(), validConfig().routingFlags());
        assertThatThrownBy(() -> compiler.compile(1, c))
                .isInstanceOf(RoutingManifestProblemException.class)
                .hasMessageContaining("ROUTING_MANIFEST_CONFLICT");
    }

    @Test
    void rejectsUnresolvedRoutingField() {
        ProspectiveConfig c = new ProspectiveConfig(
                validConfig().upstreams(), validConfig().extractionRules(),
                terminal("missingField", "terminalName"),
                validConfig().tkbPayList(), validConfig().routingFlags());
        assertThatThrownBy(() -> compiler.compile(1, c))
                .isInstanceOf(RoutingManifestProblemException.class)
                .satisfies(ex -> assertThat(((RoutingManifestProblemException) ex).diagnostics())
                        .anyMatch(d -> d.code().equals("MANIFEST_ROUTING_FIELD_UNRESOLVED")));
    }

    @Test
    void rejectsInvalidFieldBinding() {
        ExtractionRule bad = new ExtractionRule(UUID.randomUUID(), "ReqAuthPay",
                List.of(new FieldBinding("x", "P", "K", "/both")), List.of(),
                ConfigStatus.ACTIVE, false, 1, now, now);
        ProspectiveConfig c = new ProspectiveConfig(validConfig().upstreams(),
                List.of(bad), validConfig().terminalConfig(),
                validConfig().tkbPayList(), validConfig().routingFlags());
        assertThatThrownBy(() -> compiler.compile(1, c))
                .satisfies(ex -> assertThat(((RoutingManifestProblemException) ex).diagnostics())
                        .anyMatch(d -> d.code().equals("MANIFEST_FIELD_BINDING_INVALID")));
    }

    @Test
    void rejectsMissingTerminalConfig() {
        ProspectiveConfig c = new ProspectiveConfig(validConfig().upstreams(),
                validConfig().extractionRules(), null,
                validConfig().tkbPayList(), validConfig().routingFlags());
        assertThatThrownBy(() -> compiler.compile(1, c))
                .satisfies(ex -> assertThat(((RoutingManifestProblemException) ex).diagnostics())
                        .anyMatch(d -> d.code().equals("MANIFEST_TERMINAL_CONFIG_MISSING")));
    }
}
