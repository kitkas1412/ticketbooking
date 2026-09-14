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

/**
 * Nhận sự kiện nội bộ để tạo vé và gọi đối soát tồn kho.
 */
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
        // Đánh số ghế từ 1 đến tổng số vé, cùng giá khởi tạo của sự kiện.
        List<Ticket> tickets = IntStream.rangeClosed(1, event.getTotalTickets())
                .mapToObj(seatCode -> Ticket.builder()
                        .eventId(event.getEventId())
                        .seatCode(seatCode)
                        .price(event.getTicketPrice())
                        .build())
                .collect(Collectors.toList());

        ticketRepository.saveAll(tickets);
    }

    // Hiện phương thức nhận TicketRequestedEvent, khác tín hiệu đối soát phát lúc khởi động.
    @EventListener
    public void handleReconciler(TicketRequestedEvent event){
        reconciler.reconcileAll();
    }
}
