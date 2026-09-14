package me.kitkas1412.user.exception;

/**
 * Báo email đã được đăng ký, bao gồm trường hợp hai yêu cầu đăng ký đồng thời.
 */
public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}
