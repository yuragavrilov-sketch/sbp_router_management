package ru.copperside.sbprouter.management.traffic.adapter.in.messaging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import ru.copperside.sbprouter.management.traffic.application.TrafficIngestService;
import ru.copperside.sbprouter.management.traffic.domain.TrafficEvent;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "traffic.kafka", name = "enabled", havingValue = "true")
public class TrafficKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(TrafficKafkaConsumer.class);

    /**
     * Column lengths mirrored from V3__traffic_transactions.sql.
     * If these change in the migration, update here too.
     * A heavier alternative to this guard is a dead-letter queue (DLQ).
     */
    static final int MAX_CORRELATION_ID_LEN = 128;
    static final int MAX_TX_ID_LEN          = 128;

    private final TrafficIngestService ingestService;
    private final TrafficEventMapper mapper;

    public TrafficKafkaConsumer(TrafficIngestService ingestService, TrafficEventMapper mapper) {
        this.ingestService = ingestService;
        this.mapper = mapper;
    }

    @KafkaListener(
            topics = "${traffic.kafka.topic}",
            groupId = "${traffic.kafka.group-id}",
            containerFactory = "trafficKafkaListenerContainerFactory")
    public void onMessage(ConsumerRecord<String, byte[]> record, Acknowledgment ack) {
        Map<String, String> headers = new HashMap<>();
        for (Header h : record.headers()) {
            headers.put(h.key(), h.value() == null ? null : new String(h.value(), StandardCharsets.UTF_8));
        }

        TrafficEvent event = mapper.map(record.key(), headers, record.value());

        // Poison-message guard: if any bounded field exceeds its column length, the DB insert
        // would throw DataIntegrityViolationException and with manual-ack the same message would
        // be redelivered forever. We log a warning and ack (drop) the message instead.
        // A heavier alternative is routing it to a dead-letter topic (DLQ).
        String correlationKey = event.correlationKey();
        if (correlationKey == null || correlationKey.isBlank()) {
            log.warn("traffic: dropping poison message — correlationKey is null/blank (record offset={})",
                    record.offset());
            ack.acknowledge();
            return;
        }
        if (correlationKey.length() > MAX_CORRELATION_ID_LEN) {
            log.warn("traffic: dropping poison message — correlationKey length {} exceeds limit {} " +
                            "(key prefix='{}', record offset={})",
                    correlationKey.length(), MAX_CORRELATION_ID_LEN,
                    correlationKey.substring(0, Math.min(correlationKey.length(), 64)),
                    record.offset());
            ack.acknowledge();
            return;
        }
        String txId = event.txId();
        if (txId != null && txId.length() > MAX_TX_ID_LEN) {
            log.warn("traffic: dropping poison message — txId length {} exceeds limit {} " +
                            "(txId prefix='{}', correlationKey='{}', record offset={})",
                    txId.length(), MAX_TX_ID_LEN,
                    txId.substring(0, Math.min(txId.length(), 64)),
                    correlationKey, record.offset());
            ack.acknowledge();
            return;
        }

        ingestService.record(event);
        ack.acknowledge();
    }
}
