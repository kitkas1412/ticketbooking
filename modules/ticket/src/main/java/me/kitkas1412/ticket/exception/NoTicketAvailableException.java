package me.kitkas1412.ticket.exception;

public class NoTicketAvailableException extends RuntimeException{
    public NoTicketAvailableException(String message) {
        super(message);
    }
}
