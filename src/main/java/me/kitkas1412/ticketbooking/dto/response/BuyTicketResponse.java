package me.kitkas1412.ticketbooking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import me.kitkas1412.ticketbooking.entity.Ticket;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Thông tin vé đã mua, chỉ trả về khi đơn hàng ở trạng thái CONFIRMED")
public record BuyTicketResponse(

        @Schema(description = "ID vé", example = "3f2504e0-4f89-11d3-9a0c-0305e82c3301")
        UUID ticketId,

        @Schema(description = "Số ghế được cấp", example = "42")
        Integer seatCode,

        @Schema(description = "Trạng thái vé", example = "SOLD")
        Ticket.TicketStatus status,

        @Schema(description = "ID sự kiện", example = "9f1c0a2e-7b4d-4c31-a6b5-2f8e1d3c4a5b")
        UUID eventId,

        @Schema(description = "ID đơn hàng", example = "5c2b1a90-1e2f-4d3c-8b7a-6d5e4f3c2b1a")
        UUID orderId,

        @Schema(description = "Giá vé đã thanh toán", example = "350000")
        BigDecimal price) {
}
