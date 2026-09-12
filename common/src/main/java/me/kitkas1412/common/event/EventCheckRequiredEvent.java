// common/src/main/java/me/kitkas1412/common/event/EventCheckRequiredEvent.java
package me.kitkas1412.common.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;
import java.util.UUID;

@Getter
@Setter
public class EventCheckRequiredEvent extends ApplicationEvent {
    private final UUID eventId;
    private volatile boolean eventExists;
    private volatile Exception exception;

    public EventCheckRequiredEvent(Object source, UUID eventId) {
        super(source);
        this.eventId = eventId;
        this.eventExists = false;
    }
}