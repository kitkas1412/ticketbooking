package me.kitkas1412.ticketbooking.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Phát hành và xác thực access token HS256.
 *
 * <p>Token mang sẵn {@code uid} và {@code roles} để tầng filter dựng được
 * Authentication mà không cần truy DB mỗi request. Đánh đổi: thông tin trong
 * token là ảnh chụp lúc đăng nhập — thu hồi quyền hay khoá tài khoản chỉ có
 * hiệu lực sau khi token hết hạn. Giữ {@code accessTokenTtl} ngắn (15 phút)
 * chính là để giới hạn cửa sổ đó.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    /** Cho phép lệch đồng hồ giữa các instance khi kiểm tra exp/iat. */
    private static final long CLOCK_SKEW_SECONDS = 60;

    static final String CLAIM_USER_ID = "uid";
    static final String CLAIM_ROLES = "roles";

    private final SecretKey signingKey;
    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        // Ném WeakKeyException ngay lúc tạo bean nếu khoá ngắn hơn 256 bit,
        // thay vì để lộ ra khi user đầu tiên đăng nhập.
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
    }

    /** Dùng để trả về {@code expiresIn} cho client, tránh hardcode ở hai nơi. */
    public Duration getAccessTokenTtl() {
        return properties.accessTokenTtl();
    }

    public String generateAccessToken(CustomUserDetails user) {
        Instant now = Instant.now();
        List<String> roles = user.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .toList();

        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(user.getUsername())
                .claim(CLAIM_USER_ID, user.getId().toString())
                .claim(CLAIM_ROLES, roles)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.accessTokenTtl())))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Giải mã và xác minh chữ ký.
     *
     * @throws JwtException nếu token sai chữ ký, hết hạn, sai issuer hoặc méo mó.
     *                      Cố ý để ném ra thay vì trả Optional: caller cần phân
     *                      biệt được token hết hạn với token giả mạo.
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(properties.issuer())
                .clockSkewSeconds(CLOCK_SKEW_SECONDS)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Bản không ném ngoại lệ, dùng cho filter khi chỉ cần biết token có dùng được không. */
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Rejected JWT: {}", e.getMessage());
            return false;
        }
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).get(CLAIM_USER_ID, String.class));
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return parseClaims(token).get(CLAIM_ROLES, List.class);
    }
}
