package me.kitkas1412.common.exception;

/**
 * Báo không tìm thấy tài nguyên; exception handler trả HTTP 404 cho lỗi này.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
