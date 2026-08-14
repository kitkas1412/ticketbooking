package me.kitkas1412.ticketbooking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import me.kitkas1412.ticketbooking.entity.Event;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Thông tin sự kiện")
public record EventResponse(

        @Schema(description = "ID sự kiện", example = "3f2504e0-4f89-11d3-9a0c-0305e82c3301")
        UUID eventId,

        @Schema(description = "Tên sự kiện", example = "Đêm nhạc Acoustic 2026")
        String name,

        @Schema(description = "Mô tả chi tiết", example = "Đêm nhạc tại Nhà hát Hòa Bình")
        String description,

        @Schema(description = "Tổng số vé phát hành", example = "1000")
        Integer totalTickets,

        @Schema(description = "Thời điểm mở bán", example = "2026-09-01T09:00:00+07:00")
        OffsetDateTime saleStartAt,

        @Schema(description = "Thời điểm đóng bán", example = "2026-09-30T23:59:59+07:00")
        OffsetDateTime saleEndAt,

        @Schema(description = "Trạng thái sự kiện", example = "ON_SALE")
        Event.EventStatus eventStatus) {
}
