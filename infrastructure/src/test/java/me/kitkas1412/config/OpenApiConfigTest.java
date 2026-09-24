package me.kitkas1412.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    @Test
    void declaresBearerSchemeAndReferencesItGlobally() {
        OpenAPI openAPI = new OpenApiConfig().ticketBookingOpenAPI();

        SecurityScheme scheme = openAPI.getComponents().getSecuritySchemes().get(OpenApiConfig.BEARER_SCHEME);
        assertThat(scheme).isNotNull();
        assertThat(scheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(scheme.getScheme()).isEqualTo("bearer");
        assertThat(scheme.getBearerFormat()).isEqualTo("JWT");

        // Tên tham chiếu phải khớp tên khai báo, nếu lệch Swagger UI sẽ không gửi header Authorization.
        assertThat(openAPI.getSecurity())
                .singleElement()
                .satisfies(requirement -> assertThat(requirement).containsKey(OpenApiConfig.BEARER_SCHEME));
    }

    @Test
    void exposesApiInfo() {
        OpenAPI openAPI = new OpenApiConfig().ticketBookingOpenAPI();

        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Ticket Booking API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("v1");
    }
}
