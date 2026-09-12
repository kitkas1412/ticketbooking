package me.kitkas1412.common.cache;

import me.kitkas1412.event.entity.Event;
import me.kitkas1412.event.repository.EventRepository;
import me.kitkas1412.ticket.entity.Ticket;
import me.kitkas1412.ticket.repository.TicketRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TicketInventoryReconciler {
    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    private final StringRedisTemplate redisTemplate;

    public TicketInventoryReconciler(EventRepository eventRepository, TicketRepository ticketRepository, StringRedisTemplate redisTemplate) {
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
        this.redisTemplate = redisTemplate;
    }

    @Scheduled(fixedRate = 300000)
    public void reconcileAll(){
        List<Event> events = eventRepository.findAll();

        for (Event event : events){
            Long count = ticketRepository.countByEventAndStatus(event, Ticket.TicketStatus.AVAILABLE);
            String key = TicketInventoryKey.availableTickets(event.getId());

            redisTemplate.opsForValue().set(key, String.valueOf(count));
        }
    }
}
