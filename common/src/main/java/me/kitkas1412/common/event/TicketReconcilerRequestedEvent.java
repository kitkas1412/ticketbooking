package me.kitkas1412.common.event;

import org.springframework.context.ApplicationEvent;

/**
 * Event yêu cầu đồng bộ lại tồn kho, được publish khi ứng dụng khởi động.
 */
public class TicketReconcilerRequestedEvent extends ApplicationEvent {
    public TicketReconcilerRequestedEvent(Object source) {
        super(source);
    }
}
