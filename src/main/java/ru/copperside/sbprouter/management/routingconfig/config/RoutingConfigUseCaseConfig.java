package ru.copperside.sbprouter.management.routingconfig.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.copperside.sbprouter.management.routingconfig.application.ExtractionRuleService;
import ru.copperside.sbprouter.management.routingconfig.application.TerminalRoutingConfigService;
import ru.copperside.sbprouter.management.routingconfig.application.TkbPayListService;
import ru.copperside.sbprouter.management.routingconfig.application.UpstreamService;
import ru.copperside.sbprouter.management.routingconfig.application.port.out.ExtractionRuleRepository;
import ru.copperside.sbprouter.management.routingconfig.application.port.out.TerminalRoutingConfigRepository;
import ru.copperside.sbprouter.management.routingconfig.application.port.out.TkbPayListRepository;
import ru.copperside.sbprouter.management.routingconfig.application.port.out.UpstreamRepository;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class RoutingConfigUseCaseConfig {

    @Bean
    @ConditionalOnBean(UpstreamRepository.class)
    UpstreamService upstreamService(UpstreamRepository repository, Clock clock) {
        return new UpstreamService(repository, clock);
    }

    @Bean
    @ConditionalOnBean(ExtractionRuleRepository.class)
    ExtractionRuleService extractionRuleService(ExtractionRuleRepository repository, Clock clock) {
        return new ExtractionRuleService(repository, clock);
    }

    @Bean
    @ConditionalOnBean(TerminalRoutingConfigRepository.class)
    TerminalRoutingConfigService terminalRoutingConfigService(TerminalRoutingConfigRepository repository, Clock clock) {
        return new TerminalRoutingConfigService(repository, clock);
    }

    @Bean
    @ConditionalOnBean(TkbPayListRepository.class)
    TkbPayListService tkbPayListService(TkbPayListRepository repository, Clock clock) {
        return new TkbPayListService(repository, clock);
    }
}
