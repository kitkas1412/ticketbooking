package me.kitkas1412.common.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class TicketRequestedEvent extends ApplicationEvent {

    private final UUID eventId;
    private final int totalTickets;
    private final BigDecimal ticketPrice;


    public TicketRequestedEvent(Object source, UUID eventId, int totalTickets, BigDecimal ticketPrice) {
        super(source);
        this.eventId = eventId;
        this.totalTickets = totalTickets;
        this.ticketPrice = ticketPrice;
    }
}
