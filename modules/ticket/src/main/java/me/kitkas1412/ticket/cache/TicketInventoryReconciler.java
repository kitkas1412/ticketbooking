package me.kitkas1412.ticket.cache;

import me.kitkas1412.cache.TicketInventoryKey;
import me.kitkas1412.ticket.entity.Ticket;
import me.kitkas1412.ticket.repository.TicketRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TicketInventoryReconciler {
    private final TicketRepository ticketRepository;
    private final StringRedisTemplate redisTemplate;

    public TicketInventoryReconciler(TicketRepository ticketRepository, StringRedisTemplate redisTemplate) {
        this.ticketRepository = ticketRepository;
        this.redisTemplate = redisTemplate;
    }

    @Scheduled(fixedRate = 300000)
    public void reconcileAll(){
        List<Ticket> tickets = ticketRepository.findAll();

        for (Ticket ticket : tickets){
            Long count = ticketRepository.countByEventAndStatus(ticket.getEventId(), Ticket.TicketStatus.AVAILABLE);
            String key = TicketInventoryKey.availableTickets(ticket.getId());

            redisTemplate.opsForValue().set(key, String.valueOf(count));
        }
    }
}
