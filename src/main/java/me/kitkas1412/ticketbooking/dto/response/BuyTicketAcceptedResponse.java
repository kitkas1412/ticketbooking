package me.kitkas1412.ticketbooking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import me.kitkas1412.ticketbooking.entity.Order;

import java.util.UUID;

@Schema(description = "Xác nhận đã nhận yêu cầu mua vé. Chưa phải kết quả cuối cùng — "
        + "poll GET /api/orders/{orderId} để biết đơn có thành công hay không.")
public record BuyTicketAcceptedResponse(

        @Schema(description = "ID đơn hàng, dùng để tra cứu trạng thái",
                example = "3f2504e0-4f89-11d3-9a0c-0305e82c3301")
        UUID orderId,

        @Schema(description = "ID sự kiện", example = "9f1c0a2e-7b4d-4c31-a6b5-2f8e1d3c4a5b")
        UUID eventId,

        @Schema(description = "Trạng thái đơn tại thời điểm trả về", example = "PENDING")
        Order.OrderStatus status) {
}
