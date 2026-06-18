package ru.copperside.sbprouter.management.traffic.application;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class FaultParserTest {

    private final FaultParser parser = new FaultParser();

    private byte[] bytes(String xml) {
        return xml.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void faultWithFaultstring() {
        byte[] xml = bytes("""
                <Document><GCSvc><fault>\
                <faultcode>9999</faultcode>\
                <faultstring>DBMS error: ORA-1555</faultstring>\
                </fault></GCSvc></Document>""");
        FaultParser.Result r = parser.parse(xml);
        assertThat(r.hasFault()).isTrue();
        assertThat(r.faultString()).isEqualTo("DBMS error: ORA-1555");
    }

    @Test
    void faultWithoutFaultstring() {
        byte[] xml = bytes("<Envelope><Body><fault><faultcode>100</faultcode></fault></Body></Envelope>");
        FaultParser.Result r = parser.parse(xml);
        assertThat(r.hasFault()).isTrue();
        assertThat(r.faultString()).isNull();
    }

    @Test
    void noFaultInNormalResponse() {
        byte[] xml = bytes("<Document><GCSvc><AnsAuthPay><rc>0</rc></AnsAuthPay></GCSvc></Document>");
        FaultParser.Result r = parser.parse(xml);
        assertThat(r.hasFault()).isFalse();
        assertThat(r.faultString()).isNull();
    }

    @Test
    void nullInputReturnsFalse() {
        FaultParser.Result r = parser.parse(null);
        assertThat(r.hasFault()).isFalse();
        assertThat(r.faultString()).isNull();
    }

    @Test
    void emptyInputReturnsFalse() {
        FaultParser.Result r = parser.parse(new byte[0]);
        assertThat(r.hasFault()).isFalse();
        assertThat(r.faultString()).isNull();
    }

    @Test
    void unparsableXmlReturnsFalse() {
        byte[] xml = bytes("not xml at all <<<");
        FaultParser.Result r = parser.parse(xml);
        assertThat(r.hasFault()).isFalse();
        assertThat(r.faultString()).isNull();
    }

    @Test
    void faultCaseInsensitiveLocalName() {
        // namespace-agnostic: local-name matching is case-insensitive per spec but elements like <Fault> differ
        // Our implementation uses equalsIgnoreCase so both lowercase and mixed case match
        byte[] xml = bytes("<env><Fault><faultstring>err</faultstring></Fault></env>");
        FaultParser.Result r = parser.parse(xml);
        assertThat(r.hasFault()).isTrue();
        assertThat(r.faultString()).isEqualTo("err");
    }
}
