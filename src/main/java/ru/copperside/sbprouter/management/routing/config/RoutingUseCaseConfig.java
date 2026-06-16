package ru.copperside.sbprouter.management.routing.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.copperside.sbprouter.management.routing.application.RoutingConfigService;
import ru.copperside.sbprouter.management.routing.application.RoutingConfigValidator;
import ru.copperside.sbprouter.management.routing.application.port.out.RoutingConfigPublisher;
import ru.copperside.sbprouter.management.routing.application.port.out.RoutingConfigRepository;

@Configuration(proxyBeanMethods = false)
public class RoutingUseCaseConfig {

    @Bean
    RoutingConfigService routingConfigService(RoutingConfigRepository repo,
                                              ObjectProvider<RoutingConfigPublisher> publisher,
                                              RoutingConfigValidator validator) {
        RoutingConfigPublisher p = publisher.getIfAvailable(() -> config -> { /* Kafka off: persist only */ });
        return new RoutingConfigService(repo, p, validator);
    }
}
