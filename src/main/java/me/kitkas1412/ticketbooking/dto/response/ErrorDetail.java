package me.kitkas1412.ticketbooking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Chi tiết lỗi trả về trong ApiResponse")
public record ErrorDetail(

        @Schema(description = "Mã HTTP tương ứng", example = "404")
        int status,

        @Schema(description = "Nhãn ngắn phân loại lỗi", example = "Not Found")
        String title,

        @Schema(description = "Mô tả cụ thể, dùng để hiển thị cho người dùng hoặc ghi log",
                example = "Không tìm thấy Event!")
        String detail
) {
}
