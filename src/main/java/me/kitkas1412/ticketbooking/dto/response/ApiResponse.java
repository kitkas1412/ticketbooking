package me.kitkas1412.ticketbooking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Khung bao dùng chung cho mọi response. Thành công thì `data` có giá trị "
        + "và `error` null; thất bại thì ngược lại.")
public record ApiResponse<T>(

        @Schema(description = "true nếu request xử lý thành công", example = "true")
        boolean success,

        @Schema(description = "Dữ liệu trả về, null khi có lỗi")
        T data,

        @Schema(description = "Chi tiết lỗi, null khi thành công")
        ErrorDetail error,

        @Schema(description = "Thông tin phụ trợ (phân trang, tổng số bản ghi...), có thể null")
        Map<String, Object> meta
) {
    public static <T> ApiResponse<T> success(T data){
        return new ApiResponse<>(true, data, null, null);
    }

    public static <T> ApiResponse<T> success(T data, Map<String, Object> meta){
        return new ApiResponse<>(true, data, null, meta);
    }

    public static <T> ApiResponse<T> error(ErrorDetail error){
        return new ApiResponse<>(false, null, error, null);
    }
}
