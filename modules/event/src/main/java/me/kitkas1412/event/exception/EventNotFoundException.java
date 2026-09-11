package me.kitkas1412.event.exception;

import org.springframework.web.bind.annotation.ResponseStatus;

public class EventNotFoundException extends RuntimeException {
    public EventNotFoundException(String message) {
        super(message);
    }
}
