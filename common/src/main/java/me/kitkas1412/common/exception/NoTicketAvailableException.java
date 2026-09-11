package me.kitkas1412.common.exception;

public class NoTicketAvailableException extends RuntimeException{
    public NoTicketAvailableException(String message) {
        super(message);
    }
}
