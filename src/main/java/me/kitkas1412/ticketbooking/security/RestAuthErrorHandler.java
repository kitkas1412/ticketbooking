package me.kitkas1412.ticketbooking.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.kitkas1412.ticketbooking.dto.response.ApiResponse;
import me.kitkas1412.ticketbooking.dto.response.ErrorDetail;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Trả lỗi 401/403 theo đúng format {@link ApiResponse} mà
 * {@code GlobalExceptionHandler} đang dùng.
 *
 * <p>Cần class này vì lỗi xác thực xảy ra trong filter chain, tức là trước khi
 * request tới được DispatcherServlet — {@code @RestControllerAdvice} không bắt
 * được. Không có nó, client sẽ nhận về trang lỗi mặc định của servlet container
 * với cấu trúc khác hẳn phần còn lại của API.
 *
 * <p>Gộp hai interface vào một class vì chúng chỉ khác nhau ở HTTP status và
 * thông điệp; tách đôi sẽ nhân bản phần ghi JSON.
 */
@Component
public class RestAuthErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAuthErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Chưa xác thực: thiếu token, token hỏng hoặc hết hạn.
     *
     * <p>Tách riêng trường hợp hết hạn — kèm header {@code X-Token-Expired} —
     * để client biết nên đăng nhập lại thay vì hiển thị lỗi cho người dùng.
     * Việc token đã hết hạn không phải thông tin nhạy cảm: ai giữ token cũng tự
     * đọc được trường {@code exp} trong đó.
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        Object reason = request.getAttribute(JwtAuthenticationFilter.TOKEN_ERROR_ATTRIBUTE);

        if (JwtAuthenticationFilter.TOKEN_EXPIRED.equals(reason)) {
            response.setHeader("X-Token-Expired", "true");
            write(response, HttpStatus.UNAUTHORIZED,
                    "Access token đã hết hạn, vui lòng đăng nhập lại");
            return;
        }

        if (JwtAuthenticationFilter.TOKEN_INVALID.equals(reason)) {
            write(response, HttpStatus.UNAUTHORIZED, "Access token không hợp lệ");
            return;
        }

        write(response, HttpStatus.UNAUTHORIZED,
                "Yêu cầu cần access token hợp lệ trong header Authorization: Bearer <token>");
    }

    /** Đã xác thực nhưng không đủ quyền. */
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        write(response, HttpStatus.FORBIDDEN,
                "Bạn không có quyền truy cập tài nguyên này");
    }

    private void write(HttpServletResponse response, HttpStatus status, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorDetail error = new ErrorDetail(status.value(), status.getReasonPhrase(), detail);
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(error));
    }
}
