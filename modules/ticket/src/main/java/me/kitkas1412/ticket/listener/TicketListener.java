package me.kitkas1412.ticket.listener;

import me.kitkas1412.common.event.TicketRequestedEvent;
import me.kitkas1412.ticket.cache.TicketInventoryReconciler;
import me.kitkas1412.ticket.entity.Ticket;
import me.kitkas1412.ticket.repository.TicketRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class TicketListener {

    private final TicketRepository ticketRepository;
    private final TicketInventoryReconciler reconciler;

    public TicketListener(TicketRepository ticketRepository, TicketInventoryReconciler reconciler) {
        this.ticketRepository = ticketRepository;
        this.reconciler = reconciler;
    }

    @EventListener
    public void handle(TicketRequestedEvent event) {
        List<Ticket> tickets = IntStream.rangeClosed(1, event.getTotalTickets())
                .mapToObj(seatCode -> Ticket.builder()
                        .eventId(event.getEventId())
                        .seatCode(seatCode)
                        .price(event.getTicketPrice())
                        .build())
                .collect(Collectors.toList());

        ticketRepository.saveAll(tickets);
    }

    @EventListener
    public void handleReconciler(TicketRequestedEvent event){
        reconciler.reconcileAll();
    }
}
