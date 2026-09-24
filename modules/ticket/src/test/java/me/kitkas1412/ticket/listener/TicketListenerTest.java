package me.kitkas1412.ticket.listener;

import me.kitkas1412.common.event.TicketRequestedEvent;
import me.kitkas1412.ticket.cache.TicketInventoryReconciler;
import me.kitkas1412.ticket.entity.Ticket;
import me.kitkas1412.ticket.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TicketListenerTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private TicketInventoryReconciler reconciler;

    @InjectMocks
    private TicketListener listener;

    @Test
    @SuppressWarnings("unchecked")
    void handleCreatesOneAvailableTicketPerSeat() {
        UUID eventId = UUID.randomUUID();
        BigDecimal price = new BigDecimal("350000");

        listener.handle(new TicketRequestedEvent(this, eventId, 3, price));

        ArgumentCaptor<List<Ticket>> captor = ArgumentCaptor.forClass(List.class);
        verify(ticketRepository).saveAll(captor.capture());
        List<Ticket> tickets = captor.getValue();
        assertThat(tickets).extracting(Ticket::getSeatCode).containsExactly(1, 2, 3);
        assertThat(tickets).allSatisfy(ticket -> {
            assertThat(ticket.getEventId()).isEqualTo(eventId);
            assertThat(ticket.getPrice()).isEqualByComparingTo(price);
            assertThat(ticket.getStatus()).isEqualTo(Ticket.TicketStatus.AVAILABLE);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void handleWithZeroTicketsSavesEmptyBatch() {
        listener.handle(new TicketRequestedEvent(this, UUID.randomUUID(), 0, BigDecimal.ONE));

        ArgumentCaptor<List<Ticket>> captor = ArgumentCaptor.forClass(List.class);
        verify(ticketRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).isEmpty();
    }

    @Test
    void handleReconcilerDelegatesToReconciler() {
        listener.handleReconciler(new TicketRequestedEvent(this, UUID.randomUUID(), 1, BigDecimal.ONE));

        verify(reconciler).reconcileAll();
    }
}
