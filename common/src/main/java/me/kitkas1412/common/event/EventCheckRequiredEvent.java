package me.kitkas1412.common.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;
import java.util.UUID;

/**
 * Event nội bộ yêu cầu module event kiểm tra một sự kiện có tồn tại hay không.
 * Publisher đọc kết quả hoặc exception trên cùng object sau khi listener chạy xong.
 */
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