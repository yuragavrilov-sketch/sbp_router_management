package ru.copperside.sbprouter.management.fleet.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import ru.copperside.sbprouter.management.fleet.adapter.in.messaging.HeartbeatMapper;
import ru.copperside.sbprouter.management.traffic.config.TrafficProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka wiring for the fleet heartbeat consumer. Gated by the same {@code traffic.kafka.enabled}
 * flag as traffic ingest (Kafka is on/off as a whole for this service); {@code @EnableKafka} is
 * declared once on {@code TrafficKafkaConfig} under the same condition. Reads from the latest
 * offset — heartbeats are frequent, so the fleet populates within one interval without replaying a
 * long history.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "traffic.kafka", name = "enabled", havingValue = "true")
public class FleetKafkaConfig {

    @Bean
    HeartbeatMapper heartbeatMapper() {
        return new HeartbeatMapper(new ObjectMapper());
    }

    @Bean
    ConsumerFactory<String, byte[]> fleetConsumerFactory(TrafficProperties traffic, FleetProperties fleet) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, traffic.kafka().bootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, fleet.groupId());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new ByteArrayDeserializer());
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, byte[]> fleetKafkaListenerContainerFactory(
            ConsumerFactory<String, byte[]> fleetConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, byte[]> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(fleetConsumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }
}
