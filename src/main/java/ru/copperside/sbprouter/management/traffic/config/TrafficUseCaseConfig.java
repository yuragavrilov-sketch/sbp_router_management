package ru.copperside.sbprouter.management.traffic.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.copperside.sbprouter.management.traffic.application.TrafficIngestService;
import ru.copperside.sbprouter.management.traffic.application.port.out.TrafficWriteRepository;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class TrafficUseCaseConfig {

    @Bean
    @ConditionalOnBean(TrafficWriteRepository.class)
    TrafficIngestService trafficIngestService(TrafficWriteRepository repository, Clock clock) {
        return new TrafficIngestService(repository, clock);
    }
}
