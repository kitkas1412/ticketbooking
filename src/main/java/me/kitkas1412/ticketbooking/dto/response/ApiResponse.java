package me.kitkas1412.ticketbooking.dto.response;

import java.util.Map;

public record ApiResponse<T>(
        boolean success,
        T data,
        ErrorDetail error,
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
