package me.kitkas1412.common.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class OutboxEventRequestedEvent extends ApplicationEvent {
    private final String aggregateType;
    private final UUID aggregateId;
    private final String eventType;
    private final String payload; // JSON đã serialize sẵn bởi phía publish

    public OutboxEventRequestedEvent(Object source, String aggregateType, UUID aggregateId,
                                     String eventType, String payload) {
        super(source);
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
    }

}
