package ru.copperside.sbprouter.management.routingconfig.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FieldBindingTest {

    @Test
    void parentKeyBindingIsValid() {
        assertThat(new FieldBinding("terminalName", "PayProfile", "Tran.TermName", null).isValid()).isTrue();
    }

    @Test
    void pathBindingIsValid() {
        assertThat(new FieldBinding("amount", null, null, "/Document/GCSvc/Amount").isValid()).isTrue();
    }

    @Test
    void bothFormsIsInvalid() {
        assertThat(new FieldBinding("x", "P", "K", "/path").isValid()).isFalse();
    }

    @Test
    void neitherFormIsInvalid() {
        assertThat(new FieldBinding("x", null, null, null).isValid()).isFalse();
    }
}
