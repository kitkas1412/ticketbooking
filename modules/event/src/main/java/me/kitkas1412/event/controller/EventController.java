package me.kitkas1412.event.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import me.kitkas1412.common.response.ApiResponse;
import me.kitkas1412.event.dto.request.CreateEventRequest;
import me.kitkas1412.event.dto.response.EventResponse;
import me.kitkas1412.event.service.impl.EventServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Cung cấp API tạo và xem sự kiện; phần nghiệp vụ do EventServiceImpl xử lý.
 */
@RestController
@RequestMapping("/api/events")
@Tag(name = "Events", description = "Tạo sự kiện và đặt mua vé")
public class EventController {

    private final EventServiceImpl eventService;

    public EventController(EventServiceImpl eventService) {
        this.eventService = eventService;
    }

    @Operation(
            summary = "Tạo sự kiện mới",
            description = "Chỉ ADMIN gọi được. Số vé khai báo ở đây cũng là tồn kho khởi tạo trên Redis.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "Tạo sự kiện thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Thiếu token hoặc token hết hạn"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Token hợp lệ nhưng không phải ADMIN")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(@RequestBody CreateEventRequest request){
        EventResponse response = eventService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(
            summary = "Lấy thông tin sự kiện",
            description = "Lấy chi tiết một sự kiện theo ID")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "202", description = "Lấy sự kiện thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Sự kiện không tồn tại")
    })
    @GetMapping("/{eventId}")
    public ResponseEntity<ApiResponse<EventResponse>> getEvent(@PathVariable UUID eventId){
        EventResponse response = eventService.getEventById(eventId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(response));
    }
}
