package me.kitkas1412.cache;

import me.kitkas1412.common.event.TicketReconcilerRequestedEvent;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publish event yêu cầu đồng bộ lại tồn kho sau khi Spring khởi động.
 * Listener cần đăng ký đúng event type TicketReconcilerRequestedEvent để nhận yêu cầu này.
 */
@Component
public class TicketInventorySyncOnStartup implements CommandLineRunner {
    private final ApplicationEventPublisher eventPublisher;

    public TicketInventorySyncOnStartup(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void run(String... args) throws Exception {
        eventPublisher.publishEvent(new TicketReconcilerRequestedEvent(this));
    }
}
