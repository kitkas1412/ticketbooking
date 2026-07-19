package me.kitkas1412.ticketbooking.dto.response;

public record ErrorDetail(
        int status,
        String title,
        String detail
) {
}
