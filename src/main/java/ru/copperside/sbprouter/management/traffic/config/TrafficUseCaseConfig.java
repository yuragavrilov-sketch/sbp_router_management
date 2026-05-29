package ru.copperside.sbprouter.management.traffic.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.copperside.sbprouter.management.traffic.application.TrafficIngestService;
import ru.copperside.sbprouter.management.traffic.application.TrafficQueryService;
import ru.copperside.sbprouter.management.traffic.application.port.out.TrafficQueryRepository;
import ru.copperside.sbprouter.management.traffic.application.port.out.TrafficWriteRepository;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class TrafficUseCaseConfig {

    @Bean
    @ConditionalOnBean(TrafficWriteRepository.class)
    TrafficIngestService trafficIngestService(TrafficWriteRepository repository, Clock clock) {
        return new TrafficIngestService(repository, clock);
    }

    @Bean
    @ConditionalOnBean(TrafficQueryRepository.class)
    TrafficQueryService trafficQueryService(TrafficQueryRepository repository, Clock clock) {
        return new TrafficQueryService(repository, clock);
    }

    @Bean
    @ConditionalOnBean(ru.copperside.sbprouter.management.traffic.application.port.out.TrafficRetentionRepository.class)
    ru.copperside.sbprouter.management.traffic.application.TrafficRetentionService trafficRetentionService(
            ru.copperside.sbprouter.management.traffic.application.port.out.TrafficRetentionRepository repository,
            Clock clock,
            TrafficProperties properties) {
        return new ru.copperside.sbprouter.management.traffic.application.TrafficRetentionService(
                repository, clock, properties.retentionDays());
    }

    @Bean
    @ConditionalOnBean(ru.copperside.sbprouter.management.traffic.application.TrafficRetentionService.class)
    ru.copperside.sbprouter.management.traffic.application.TrafficRetentionJob trafficRetentionJob(
            ru.copperside.sbprouter.management.traffic.application.TrafficRetentionService service) {
        return new ru.copperside.sbprouter.management.traffic.application.TrafficRetentionJob(service);
    }
}
