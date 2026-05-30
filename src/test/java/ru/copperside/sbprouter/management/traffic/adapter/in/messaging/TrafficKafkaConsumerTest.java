package ru.copperside.sbprouter.management.traffic.adapter.in.messaging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;
import ru.copperside.sbprouter.management.traffic.application.TrafficIngestService;
import ru.copperside.sbprouter.management.traffic.application.port.out.TrafficWriteRepository;
import ru.copperside.sbprouter.management.traffic.domain.TrafficTransaction;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Unit tests for poison-message guard in TrafficKafkaConsumer.
 * Verifies that over-long or blank correlationKey causes message to be acked (dropped)
 * without invoking the ingest service — preventing infinite redelivery loops.
 */
class TrafficKafkaConsumerTest {

    private RecordingRepo recordingRepo;
    private TrafficIngestService ingestService;
    private TrafficEventMapper mapper;
    private TrafficKafkaConsumer consumer;
    private RecordingAck ack;

    @BeforeEach
    void setUp() {
        recordingRepo = new RecordingRepo();
        ingestService = new TrafficIngestService(recordingRepo,
                Clock.fixed(Instant.parse("2026-05-29T09:00:05Z"), ZoneOffset.UTC));
        mapper = new TrafficEventMapper();
        consumer = new TrafficKafkaConsumer(ingestService, mapper);
        ack = new RecordingAck();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private ConsumerRecord<String, byte[]> record(String key, String correlationId, String txId) {
        ConsumerRecord<String, byte[]> r = new ConsumerRecord<>("sbp-router-traffic", 0, 0L, key, new byte[0]);
        addHeader(r, "direction", "request");
        if (correlationId != null) addHeader(r, "correlationId", correlationId);
        if (txId != null)          addHeader(r, "txId", txId);
        return r;
    }

    private static void addHeader(ConsumerRecord<?, ?> r, String key, String value) {
        r.headers().add(key, value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static String repeat(char c, int n) {
        return String.valueOf(c).repeat(n);
    }

    // ── TASK 1 tests ─────────────────────────────────────────────────────────

    @Test
    void poisonMessage_correlationIdExceedsLimit_isAckedWithoutIngest() {
        String oversizedId = repeat('X', TrafficKafkaConsumer.MAX_CORRELATION_ID_LEN + 1);
        ConsumerRecord<String, byte[]> r = record(null, oversizedId, null);

        assertThatNoException().isThrownBy(() -> consumer.onMessage(r, ack));

        assertThat(ack.acknowledged).isTrue();
        assertThat(recordingRepo.upserted).isEmpty(); // ingest was not called
    }

    @Test
    void poisonMessage_txIdExceedsLimit_isAckedWithoutIngest() {
        // correlationId is null/blank — correlationKey falls back to txId
        String oversizedTxId = repeat('Y', TrafficKafkaConsumer.MAX_TX_ID_LEN + 1);
        ConsumerRecord<String, byte[]> r = record(null, null, oversizedTxId);

        assertThatNoException().isThrownBy(() -> consumer.onMessage(r, ack));

        assertThat(ack.acknowledged).isTrue();
        assertThat(recordingRepo.upserted).isEmpty();
    }

    @Test
    void poisonMessage_blankCorrelationKeyAndNoTxId_isAckedWithoutIngest() {
        // No correlationId, no txId — correlationKey becomes null
        ConsumerRecord<String, byte[]> r = record(null, null, null);

        assertThatNoException().isThrownBy(() -> consumer.onMessage(r, ack));

        assertThat(ack.acknowledged).isTrue();
        assertThat(recordingRepo.upserted).isEmpty();
    }

    @Test
    void validMessage_withinLimits_isProcessedAndAcked() {
        String validId = repeat('Z', TrafficKafkaConsumer.MAX_CORRELATION_ID_LEN);
        ConsumerRecord<String, byte[]> r = record(validId, validId, "tx-ok");

        assertThatNoException().isThrownBy(() -> consumer.onMessage(r, ack));

        assertThat(ack.acknowledged).isTrue();
        assertThat(recordingRepo.upserted).hasSize(1);
    }

    @Test
    void validMessage_correlationIdAtExactLimit_isProcessed() {
        String exactId = repeat('A', TrafficKafkaConsumer.MAX_CORRELATION_ID_LEN);
        ConsumerRecord<String, byte[]> r = record(exactId, exactId, "tx-exact");

        assertThatNoException().isThrownBy(() -> consumer.onMessage(r, ack));

        assertThat(ack.acknowledged).isTrue();
        assertThat(recordingRepo.upserted).hasSize(1);
    }

    // ── fakes ─────────────────────────────────────────────────────────────────

    static class RecordingRepo implements TrafficWriteRepository {
        final List<TrafficTransaction> upserted = new ArrayList<>();

        @Override
        public void upsert(TrafficTransaction partial) {
            upserted.add(partial);
        }
    }

    static class RecordingAck implements Acknowledgment {
        boolean acknowledged = false;

        @Override
        public void acknowledge() {
            acknowledged = true;
        }
    }
}
