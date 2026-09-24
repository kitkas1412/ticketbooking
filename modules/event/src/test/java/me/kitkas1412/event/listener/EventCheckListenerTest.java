package me.kitkas1412.event.listener;

import me.kitkas1412.common.event.EventCheckRequiredEvent;
import me.kitkas1412.common.exception.ResourceNotFoundException;
import me.kitkas1412.event.repository.EventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventCheckListenerTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventCheckListener listener;

    @Test
    void marksEventAsExistingWhenFound() {
        UUID eventId = UUID.randomUUID();
        when(eventRepository.existsById(eventId)).thenReturn(true);
        EventCheckRequiredEvent event = new EventCheckRequiredEvent(this, eventId);

        listener.onEventCheckRequired(event);

        assertThat(event.isEventExists()).isTrue();
        assertThat(event.getException()).isNull();
    }

    @Test
    void marksEventAsMissingWhenNotFound() {
        UUID eventId = UUID.randomUUID();
        when(eventRepository.existsById(eventId)).thenReturn(false);
        EventCheckRequiredEvent event = new EventCheckRequiredEvent(this, eventId);

        listener.onEventCheckRequired(event);

        assertThat(event.isEventExists()).isFalse();
        assertThat(event.getException()).isNull();
    }

    @Test
    void storesNotFoundExceptionWhenRepositoryFails() {
        UUID eventId = UUID.randomUUID();
        when(eventRepository.existsById(eventId)).thenThrow(new IllegalStateException("db down"));
        EventCheckRequiredEvent event = new EventCheckRequiredEvent(this, eventId);

        listener.onEventCheckRequired(event);

        assertThat(event.isEventExists()).isFalse();
        assertThat(event.getException())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Không tìm thấy Event!");
    }
}
