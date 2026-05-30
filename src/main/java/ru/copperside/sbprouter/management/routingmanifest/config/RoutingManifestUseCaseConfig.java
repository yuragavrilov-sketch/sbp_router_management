package ru.copperside.sbprouter.management.routingmanifest.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.copperside.sbprouter.management.routingconfig.application.port.out.ExtractionRuleRepository;
import ru.copperside.sbprouter.management.routingconfig.application.port.out.RoutingFlagRepository;
import ru.copperside.sbprouter.management.routingconfig.application.port.out.TerminalRoutingConfigRepository;
import ru.copperside.sbprouter.management.routingconfig.application.port.out.TkbPayListRepository;
import ru.copperside.sbprouter.management.routingconfig.application.port.out.UpstreamRepository;
import ru.copperside.sbprouter.management.routingmanifest.application.ProspectiveConfigAssembler;
import ru.copperside.sbprouter.management.routingmanifest.application.RoutingManifestCompiler;
import ru.copperside.sbprouter.management.routingmanifest.application.RoutingManifestService;
import ru.copperside.sbprouter.management.routingmanifest.application.port.out.ManifestPublishedNotifier;
import ru.copperside.sbprouter.management.routingmanifest.application.port.out.RoutingManifestRepository;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class RoutingManifestUseCaseConfig {

    @Bean
    ProspectiveConfigAssembler prospectiveConfigAssembler() {
        return new ProspectiveConfigAssembler();
    }

    @Bean
    RoutingManifestCompiler routingManifestCompiler(Clock clock) {
        return new RoutingManifestCompiler(clock);
    }

    @Bean
    @ConditionalOnMissingBean(ManifestPublishedNotifier.class)
    ManifestPublishedNotifier noopManifestPublishedNotifier() {
        return ManifestPublishedNotifier.NOOP;
    }

    @Bean
    @ConditionalOnBean(RoutingManifestRepository.class)
    RoutingManifestService routingManifestService(
            UpstreamRepository upstreams,
            ExtractionRuleRepository extractionRules,
            TerminalRoutingConfigRepository terminalConfig,
            TkbPayListRepository tkbPayList,
            RoutingFlagRepository routingFlags,
            RoutingManifestRepository manifests,
            ProspectiveConfigAssembler assembler,
            RoutingManifestCompiler compiler,
            ManifestPublishedNotifier notifier) {
        return new RoutingManifestService(upstreams, extractionRules, terminalConfig, tkbPayList,
                routingFlags, manifests, assembler, compiler, notifier);
    }
}
