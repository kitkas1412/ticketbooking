package me.kitkas1412.event.service;

import me.kitkas1412.event.dto.request.CreateEventRequest;
import me.kitkas1412.event.dto.request.UpdateEventRequest;
import me.kitkas1412.event.dto.response.EventResponse;

import java.util.UUID;

/**
 * Interface quản lý sự kiện; service hiện chưa triển khai phần cập nhật và xóa.
 */
public interface EventService {
    // Tạo sự kiện và phát hành số lượng vé ban đầu.
    EventResponse createEvent(CreateEventRequest eventRequest);

    // Lấy thông tin sự kiện theo UUID hoặc báo lỗi không tìm thấy.
    EventResponse getEventById(UUID eventId);

    EventResponse updateEvent(UpdateEventRequest updateEventRequest);

    void deleteEvent(UUID eventId);
}
