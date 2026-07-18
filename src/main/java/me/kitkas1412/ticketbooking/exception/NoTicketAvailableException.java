package me.kitkas1412.ticketbooking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT) // 409
public class NoTicketAvailableException extends RuntimeException{
    public NoTicketAvailableException(String message) {
        super(message);
    }
}
