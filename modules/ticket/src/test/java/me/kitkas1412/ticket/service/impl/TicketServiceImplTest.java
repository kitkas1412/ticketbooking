package me.kitkas1412.ticket.service.impl;

import me.kitkas1412.common.exception.ResourceNotFoundException;
import me.kitkas1412.ticket.entity.Ticket;
import me.kitkas1412.ticket.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private TicketServiceImpl ticketService;

    @Test
    void reserveTicketMarksFirstAvailableTicketAsSold() {
        UUID eventId = UUID.randomUUID();
        Ticket ticket = Ticket.builder().eventId(eventId).seatCode(1).price(BigDecimal.TEN).build();
        when(ticketRepository.findFirstByEventIdAndStatus(eventId, Ticket.TicketStatus.AVAILABLE))
                .thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        Ticket reserved = ticketService.reserveTicket(eventId);

        assertThat(reserved).isSameAs(ticket);
        assertThat(reserved.getStatus()).isEqualTo(Ticket.TicketStatus.SOLD);
        verify(ticketRepository).save(ticket);
    }

    @Test
    void reserveTicketThrowsWhenNoTicketIsAvailable() {
        UUID eventId = UUID.randomUUID();
        when(ticketRepository.findFirstByEventIdAndStatus(eventId, Ticket.TicketStatus.AVAILABLE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.reserveTicket(eventId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Không tìm thấy Ticket");
        verify(ticketRepository, never()).save(any());
    }
}
