package ru.copperside.sbprouter.management.routing.application;

import org.junit.jupiter.api.Test;
import ru.copperside.sbprouter.management.routing.application.port.out.RoutingConfigPublisher;
import ru.copperside.sbprouter.management.routing.application.port.out.RoutingConfigRepository;
import ru.copperside.sbprouter.management.routing.domain.RoutingConfig;
import ru.copperside.sbprouter.management.routing.domain.RoutingConfigProblemException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RoutingConfigServiceTest {

    private final RoutingConfigRepository repo = mock(RoutingConfigRepository.class);
    private final RoutingConfigPublisher publisher = mock(RoutingConfigPublisher.class);
    private final RoutingConfigService service = new RoutingConfigService(repo, publisher, new RoutingConfigValidator());

    private static RoutingConfig inbound(String active) {
        return new RoutingConfig(null, active, Map.of("default", new RoutingConfig.Group(List.of("http://a/api"))));
    }

    @Test
    void replaceValidatesAssignsVersionPersistsAndPublishes() {
        when(repo.nextVersion()).thenReturn(7L);
        RoutingConfig saved = service.replace(inbound("default"));
        assertThat(saved.version()).isEqualTo(7L);
        verify(repo).save(argThat(c -> c.version() == 7L));
        verify(publisher).publish(argThat(c -> c.version() == 7L));
    }

    @Test
    void replaceRejectsInvalidWithoutPersisting() {
        assertThatThrownBy(() -> service.replace(inbound("missing")))
                .isInstanceOf(RoutingConfigProblemException.class);
        verify(repo, never()).save(any());
        verify(publisher, never()).publish(any());
    }

    @Test
    void latestDelegates() {
        when(repo.latest()).thenReturn(Optional.of(inbound("default").withVersion(1L)));
        assertThat(service.latest()).isPresent();
    }
}
