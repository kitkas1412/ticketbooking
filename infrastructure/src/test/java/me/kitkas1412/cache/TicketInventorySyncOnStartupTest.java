package me.kitkas1412.cache;

import me.kitkas1412.common.event.TicketReconcilerRequestedEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TicketInventorySyncOnStartupTest {

    @Test
    void runPublishesReconcilerRequest() throws Exception {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        TicketInventorySyncOnStartup runner = new TicketInventorySyncOnStartup(publisher);

        runner.run();

        ArgumentCaptor<ApplicationEvent> captor = ArgumentCaptor.forClass(ApplicationEvent.class);
        verify(publisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(TicketReconcilerRequestedEvent.class);
        assertThat(captor.getValue().getSource()).isSameAs(runner);
    }
}
