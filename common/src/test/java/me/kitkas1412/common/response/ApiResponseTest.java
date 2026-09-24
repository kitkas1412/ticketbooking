package me.kitkas1412.common.response;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void successWrapsDataWithoutErrorOrMeta() {
        ApiResponse<String> response = ApiResponse.success("payload");

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isEqualTo("payload");
        assertThat(response.error()).isNull();
        assertThat(response.meta()).isNull();
    }

    @Test
    void successWithMetaKeepsMeta() {
        Map<String, Object> meta = Map.of("total", 10);

        ApiResponse<String> response = ApiResponse.success("payload", meta);

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isEqualTo("payload");
        assertThat(response.meta()).isEqualTo(meta);
    }

    @Test
    void errorWrapsErrorDetailWithoutData() {
        ErrorDetail detail = new ErrorDetail(404, "Not Found", "Không tìm thấy");

        ApiResponse<Void> response = ApiResponse.error(detail);

        assertThat(response.success()).isFalse();
        assertThat(response.data()).isNull();
        assertThat(response.error()).isEqualTo(detail);
        assertThat(response.meta()).isNull();
    }
}
