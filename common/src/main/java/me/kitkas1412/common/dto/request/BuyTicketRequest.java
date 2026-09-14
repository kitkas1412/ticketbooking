package me.kitkas1412.common.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request đặt một vé; idempotency key dùng để nhận diện request gửi lại.
 * Service hiện trả Optional.empty() nếu key đã tồn tại trong Redis.
 */
@Schema(description = "Yêu cầu mua vé")
public record BuyTicketRequest(

        @Schema(description = """
                Khoá chống trùng do client sinh (nên dùng UUID). Gửi lại cùng một khoá
                sẽ trả về đúng đơn đã tạo thay vì tạo thêm đơn mới — cần thiết khi client
                timeout rồi thử lại.
                """,
                example = "9f1c0a2e-7b4d-4c31-a6b5-2f8e1d3c4a5b")
        String idempotencyKey) {
}
