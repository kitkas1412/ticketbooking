package me.kitkas1412.ticketbooking.exception;

import me.kitkas1412.ticketbooking.dto.response.ApiResponse;
import me.kitkas1412.ticketbooking.dto.response.ErrorDetail;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoTicketAvailableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoTicketAvailable(NoTicketAvailableException ex) {
        ErrorDetail error = new ErrorDetail(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(error));
    }

    @ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleEventNotFound(EventNotFoundException ex) {
        ErrorDetail error = new ErrorDetail(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(error));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        ErrorDetail error = new ErrorDetail(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(error));
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleOrderNotFound(OrderNotFoundException ex) {
        ErrorDetail error = new ErrorDetail(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(error));
    }

    /**
     * Tài khoản tồn tại và đúng mật khẩu nhưng đang bị khoá/vô hiệu hoá.
     *
     * <p>Trả thông điệp cụ thể là chủ ý: người dùng hợp lệ cần biết vì sao vào
     * không được. Đánh đổi là lộ ra email đó có tồn tại — chấp nhận được vì
     * muốn tới được nhánh này thì đã phải đưa đúng mật khẩu.
     */
    @ExceptionHandler({DisabledException.class, LockedException.class})
    public ResponseEntity<ApiResponse<Void>> handleAccountUnavailable(AuthenticationException ex) {
        ErrorDetail error = new ErrorDetail(
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                "Tài khoản đã bị khoá hoặc vô hiệu hoá"
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(error));
    }

    /**
     * Sai email hoặc sai mật khẩu.
     *
     * <p>Thông điệp cố ý gộp chung hai trường hợp: nói rõ "email không tồn tại"
     * sẽ biến endpoint login thành công cụ dò xem địa chỉ nào đã đăng ký.
     *
     * <p>Nếu thiếu handler này, {@code AuthenticationException} ném từ
     * AuthService sẽ rơi vào handler bắt {@code Exception} bên dưới và trả về
     * 500 cho một lần nhập sai mật khẩu.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException ex) {
        ErrorDetail error = new ErrorDetail(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Email hoặc mật khẩu không đúng"
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(error));
    }

    /**
     * Đã xác thực nhưng không đủ quyền, ném từ trong controller — tức là bởi
     * method security ({@code @PreAuthorize}).
     *
     * <p>Cần handler riêng vì handler bắt {@code Exception} bên dưới sẽ nuốt mất
     * và trả 500. Lỗi phân quyền theo URL không đi qua đây: nó bị
     * {@code ExceptionTranslationFilter} chặn ngay trong filter chain và chuyển
     * cho {@code RestAuthErrorHandler}, nên hai đường phải cho ra cùng một
     * response 403.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        ErrorDetail error = new ErrorDetail(
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                "Bạn không có quyền truy cập tài nguyên này"
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(error));
    }

    /** Body không qua được @Valid — trả 400 kèm lỗi từng field. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));

        ErrorDetail error = new ErrorDetail(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                detail.isEmpty() ? "Dữ liệu không hợp lệ" : detail
        );
        return ResponseEntity.badRequest().body(ApiResponse.error(error));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        ex.printStackTrace();
        ErrorDetail error = new ErrorDetail(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Unexpected error occurred"
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(error));
    }
}
