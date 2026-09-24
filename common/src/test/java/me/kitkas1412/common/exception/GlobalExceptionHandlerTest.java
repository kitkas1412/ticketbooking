package me.kitkas1412.common.exception;

import me.kitkas1412.common.response.ApiResponse;
import me.kitkas1412.common.response.ErrorDetail;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void resourceNotFoundMapsTo404WithOriginalMessage() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleResourceNotFound(new ResourceNotFoundException("Không tìm thấy event"));

        assertError(response, HttpStatus.NOT_FOUND, "Không tìm thấy event");
    }

    @Test
    void noTicketAvailableMapsTo409WithOriginalMessage() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleNoTicketAvailable(new NoTicketAvailableException("Hết vé!"));

        assertError(response, HttpStatus.CONFLICT, "Hết vé!");
    }

    @Test
    void disabledAndLockedAccountsMapTo403() {
        assertError(handler.handleAccountUnavailable(new DisabledException("disabled")),
                HttpStatus.FORBIDDEN, "Tài khoản đã bị khoá hoặc vô hiệu hoá");
        assertError(handler.handleAccountUnavailable(new LockedException("locked")),
                HttpStatus.FORBIDDEN, "Tài khoản đã bị khoá hoặc vô hiệu hoá");
    }

    @Test
    void badCredentialsMapTo401WithGenericMessage() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleAuthentication(new BadCredentialsException("User not found"));

        // Message không được lộ ra việc email có tồn tại hay không.
        assertError(response, HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng");
    }

    @Test
    void accessDeniedMapsTo403() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleAccessDenied(new AccessDeniedException("denied"));

        assertError(response, HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập tài nguyên này");
    }

    @Test
    void validationErrorsAreJoinedPerField() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "Email không hợp lệ"));
        bindingResult.addError(new FieldError("request", "password", "Mật khẩu không được để trống"));

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(validationException(bindingResult));

        assertError(response, HttpStatus.BAD_REQUEST,
                "email: Email không hợp lệ; password: Mật khẩu không được để trống");
    }

    @Test
    void validationWithoutFieldErrorsFallsBackToGenericMessage() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(validationException(bindingResult));

        assertError(response, HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ");
    }

    @Test
    void unexpectedExceptionMapsTo500WithoutLeakingDetails() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleGenericException(new IllegalStateException("SQL: connection refused"));

        assertError(response, HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error occurred");
    }

    private static MethodArgumentNotValidException validationException(BeanPropertyBindingResult bindingResult)
            throws NoSuchMethodException {
        MethodParameter parameter = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("validationException", BeanPropertyBindingResult.class), 0);
        return new MethodArgumentNotValidException(parameter, bindingResult);
    }

    private static void assertError(ResponseEntity<ApiResponse<Void>> response, HttpStatus status, String detail) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().data()).isNull();
        assertThat(response.getBody().error())
                .isEqualTo(new ErrorDetail(status.value(), status.getReasonPhrase(), detail));
    }
}
