package me.kitkas1412.ticketbooking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(description = "Dữ liệu tạo sự kiện mới")
public record CreateEventRequest(

        @Schema(description = "Tên sự kiện", example = "Đêm nhạc Acoustic 2026")
        String name,

        @Schema(description = "Mô tả chi tiết sự kiện", example = "Đêm nhạc tại Nhà hát Hòa Bình")
        String description,

        @Schema(description = "Tổng số vé phát hành, cũng là tồn kho khởi tạo trên Redis", example = "1000")
        Integer totalTickets,

        @Schema(description = "Giá một vé", example = "350000")
        BigDecimal ticketPrice,

        @Schema(description = "Thời điểm mở bán", example = "2026-09-01T09:00:00+07:00")
        OffsetDateTime saleStartAt,

        @Schema(description = "Thời điểm đóng bán", example = "2026-09-30T23:59:59+07:00")
        OffsetDateTime saleEndAt) {
}
