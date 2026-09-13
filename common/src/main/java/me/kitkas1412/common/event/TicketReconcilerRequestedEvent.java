package me.kitkas1412.common.event;

import org.springframework.context.ApplicationEvent;

public class TicketReconcilerRequestedEvent extends ApplicationEvent {
    public TicketReconcilerRequestedEvent(Object source) {
        super(source);
    }
}
