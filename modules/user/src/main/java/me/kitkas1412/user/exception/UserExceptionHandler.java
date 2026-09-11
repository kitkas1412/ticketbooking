package me.kitkas1412.user.exception;

import me.kitkas1412.common.response.ApiResponse;
import me.kitkas1412.common.response.ErrorDetail;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class UserExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        ErrorDetail error = new ErrorDetail(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(error));
    }
}
