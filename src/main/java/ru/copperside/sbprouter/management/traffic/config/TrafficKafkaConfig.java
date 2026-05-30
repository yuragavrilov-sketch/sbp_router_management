package ru.copperside.sbprouter.management.traffic.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import ru.copperside.sbprouter.management.traffic.adapter.in.messaging.TrafficEventMapper;

import java.util.HashMap;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "traffic.kafka", name = "enabled", havingValue = "true")
@EnableKafka
public class TrafficKafkaConfig {

    @Bean
    TrafficEventMapper trafficEventMapper() {
        return new TrafficEventMapper();
    }

    @Bean
    ConsumerFactory<String, byte[]> trafficConsumerFactory(TrafficProperties properties) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.kafka().bootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, properties.kafka().groupId());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new ByteArrayDeserializer());
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, byte[]> trafficKafkaListenerContainerFactory(
            ConsumerFactory<String, byte[]> trafficConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, byte[]> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(trafficConsumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }

    @Bean
    ProducerFactory<String, byte[]> manifestEventProducerFactory(TrafficProperties properties) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.kafka().bootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000);
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 10000);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    KafkaTemplate<String, byte[]> manifestEventKafkaTemplate(ProducerFactory<String, byte[]> manifestEventProducerFactory) {
        return new KafkaTemplate<>(manifestEventProducerFactory);
    }
}
