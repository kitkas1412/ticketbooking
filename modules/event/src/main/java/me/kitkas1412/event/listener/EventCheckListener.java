package me.kitkas1412.event.listener;

import me.kitkas1412.common.event.EventCheckRequiredEvent;
import me.kitkas1412.common.exception.ResourceNotFoundException;
import me.kitkas1412.event.repository.EventRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listener kiểm tra sự kiện có tồn tại hay không rồi ghi kết quả vào event nhận được.
 */
@Component
public class EventCheckListener {

    private final EventRepository eventRepository;

    public EventCheckListener(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @EventListener
    public void onEventCheckRequired(EventCheckRequiredEvent event) {
        try {
            boolean exists = eventRepository.existsById(event.getEventId());
            event.setEventExists(exists);
        } catch (Exception ex) {
            event.setException(new ResourceNotFoundException("Không tìm thấy Event!"));
        }
    }
}