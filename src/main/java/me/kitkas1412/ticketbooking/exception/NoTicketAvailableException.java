package me.kitkas1412.ticketbooking.exception;

public class NoTicketAvailableException extends RuntimeException{
    public NoTicketAvailableException(String message) {
        super(message);
    }
}
