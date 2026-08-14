package me.kitkas1412.ticketbooking.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Đọc {@code Authorization: Bearer <token>} và nạp Authentication vào
 * SecurityContext.
 *
 * <p><b>Cố ý không đánh {@code @Component}.</b> Spring Boot tự đăng ký mọi bean
 * kiểu {@code Filter} vào servlet filter chain, nên nếu để nó thành bean thì
 * filter chạy hai lần: một lần ngoài chuỗi của Spring Security (trước cả
 * ExceptionTranslationFilter, nghĩa là lỗi không được xử lý đúng) và một lần
 * trong chuỗi. Cách tránh là khởi tạo trực tiếp trong {@code SecurityConfig}:
 * <pre>
 * http.addFilterBefore(new JwtAuthenticationFilter(jwtService),
 *                      UsernamePasswordAuthenticationFilter.class);
 * </pre>
 *
 * <p><b>Filter dựng Authentication thuần từ claims, không truy DB.</b> Đổi lại
 * tốc độ, việc khoá tài khoản hoặc hạ quyền chỉ có hiệu lực khi token hết hạn
 * (TTL 15 phút). Nếu cần thu hồi tức thì, thay khối dựng principal bên dưới
 * bằng một lượt {@code userDetailsService.loadUserByUsername(...)} — đúng một
 * query mỗi request — hoặc kiểm tra {@code jti} với blacklist trên Redis.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);

        // Không có token, hoặc request đã được xác thực bởi cơ chế khác:
        // đi tiếp và để SecurityFilterChain quyết định cho qua hay chặn.
        if (token == null || SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtService.parseClaims(token);

            @SuppressWarnings("unchecked")
            List<String> roles = claims.get(JwtService.CLAIM_ROLES, List.class);
            String userId = claims.get(JwtService.CLAIM_USER_ID, String.class);

            // Chữ ký hợp lệ nhưng thiếu claim: token do ta ký ở một phiên bản
            // cũ hơn, hoặc bị cắt xén. Không đoán mò, coi như chưa xác thực.
            if (userId == null || roles == null || claims.getSubject() == null) {
                throw new IllegalArgumentException("Token thiếu claim bắt buộc (sub/uid/roles)");
            }

            CustomUserDetails principal = CustomUserDetails.fromClaims(
                    UUID.fromString(userId), claims.getSubject(), roles);

            var authentication = new UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

        } catch (JwtException | IllegalArgumentException e) {
            // Token hỏng/hết hạn/giả mạo: xoá context và đi tiếp với thân phận
            // ẩn danh. Cố ý KHÔNG tự ghi 401 ở đây — để AuthenticationEntryPoint
            // dựng response lỗi thống nhất cho cả trường hợp không gửi token.
            SecurityContextHolder.clearContext();
            log.debug("Bỏ qua JWT không hợp lệ trên {} {}: {}",
                    request.getMethod(), request.getRequestURI(), e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
