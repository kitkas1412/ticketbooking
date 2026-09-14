package me.kitkas1412.event.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * Dữ liệu dự kiến dùng để thay đổi thông tin, thời gian bán và trạng thái sự kiện.
 */
@Schema(description = "Dữ liệu cập nhật sự kiện")
public record UpdateEventRequest(

        @Schema(description = "Tên sự kiện", example = "Đêm nhạc Acoustic 2026")
        String name,

        @Schema(description = "Mô tả chi tiết sự kiện", example = "Đêm nhạc tại Nhà hát Hòa Bình")
        String description,

        @Schema(description = "Thời điểm mở bán", example = "2026-09-01T09:00:00+07:00")
        OffsetDateTime saleStartAt,

        @Schema(description = "Thời điểm đóng bán", example = "2026-09-30T23:59:59+07:00")
        OffsetDateTime saleEndAt,

        @Schema(description = "Trạng thái sự kiện", example = "ON_SALE")
        String status) {
}