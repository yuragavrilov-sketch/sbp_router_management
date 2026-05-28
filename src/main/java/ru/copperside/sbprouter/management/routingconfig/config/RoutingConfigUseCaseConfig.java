package ru.copperside.sbprouter.management.routingconfig.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.copperside.sbprouter.management.routingconfig.application.UpstreamService;
import ru.copperside.sbprouter.management.routingconfig.application.port.out.UpstreamRepository;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class RoutingConfigUseCaseConfig {

    @Bean
    @ConditionalOnBean(UpstreamRepository.class)
    UpstreamService upstreamService(UpstreamRepository repository, Clock clock) {
        return new UpstreamService(repository, clock);
    }
}
