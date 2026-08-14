package me.kitkas1412.ticketbooking.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    /**
     * Tên của security scheme. Đây là khoá tra cứu: chuỗi này phải khớp giữa
     * {@code components.securitySchemes} khai báo bên dưới và mọi chỗ tham chiếu
     * tới nó ({@code addSecurityItem} ở đây, hoặc
     * {@code @SecurityRequirement(name = ...)} trên từng endpoint). Lệch một ký
     * tự thì Swagger UI vẫn render nhưng không đính kèm header Authorization.
     */
    public static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI ticketBookingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ticket Booking API")
                        .description("""
                                API đặt vé sự kiện: đăng ký / đăng nhập, xem sự kiện, mua vé bất đồng bộ
                                qua RabbitMQ và tra cứu trạng thái đơn hàng.

                                **Cách xác thực:** gọi `POST /api/auth/login`, copy `data.accessToken` trong
                                response rồi bấm nút **Authorize** ở góc phải trên và dán token vào
                                (không cần gõ tiền tố `Bearer`, Swagger UI tự thêm).
                                """)
                        .version("v1")
                        .contact(new Contact().name("kitkas1412").url("https://github.com/kitkas1412"))
                        .license(new License().name("MIT")))

                // Khai báo scheme một lần ở components, các endpoint chỉ tham chiếu tới tên.
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                // Chỉ là gợi ý hiển thị cho UI, không ảnh hưởng tới việc verify token.
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .description("Access token lấy từ POST /api/auth/login")))

                // Áp dụng mặc định cho toàn bộ endpoint. Các endpoint công khai
                // (login, register, GET events) tự gỡ bằng @SecurityRequirements rỗng
                // để Swagger UI không gửi kèm header thừa.
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
