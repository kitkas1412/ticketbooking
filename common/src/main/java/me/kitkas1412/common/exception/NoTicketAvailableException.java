package me.kitkas1412.common.exception;

/**
 * Báo hết vé; exception handler trả HTTP 409 cho lỗi này.
 */
public class NoTicketAvailableException extends RuntimeException{
    public NoTicketAvailableException(String message) {
        super(message);
    }
}
