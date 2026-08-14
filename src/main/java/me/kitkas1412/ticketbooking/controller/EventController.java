package me.kitkas1412.ticketbooking.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import me.kitkas1412.ticketbooking.dto.request.BuyTicketRequest;
import me.kitkas1412.ticketbooking.dto.request.CreateEventRequest;
import me.kitkas1412.ticketbooking.dto.response.ApiResponse;
import me.kitkas1412.ticketbooking.dto.response.BuyTicketAcceptedResponse;
import me.kitkas1412.ticketbooking.dto.response.EventResponse;
import me.kitkas1412.ticketbooking.service.impl.EventServiceImpl;
import me.kitkas1412.ticketbooking.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@Tag(name = "Events", description = "Tạo sự kiện và đặt mua vé")
public class EventController {

    private final EventServiceImpl eventService;
    private final OrderService orderService;

    public EventController(EventServiceImpl eventService, OrderService orderService) {
        this.eventService = eventService;
        this.orderService = orderService;
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
            summary = "Đặt mua vé",
            description = """
                    Xử lý bất đồng bộ: request chỉ giữ chỗ trong tồn kho Redis rồi đẩy message
                    sang RabbitMQ, nên `202` nghĩa là **đã nhận yêu cầu**, chưa phải đã mua xong.
                    Dùng `GET /api/orders/{orderId}` để theo dõi trạng thái thực tế.

                    `idempotencyKey` trong body cho phép gửi lại an toàn khi client timeout:
                    cùng một key sẽ trả về đúng đơn cũ thay vì tạo đơn thứ hai.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "202", description = "Đã nhận yêu cầu, đơn hàng đang được xử lý"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Không tạo được đơn mới (ví dụ yêu cầu trùng đã xử lý xong)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Thiếu token hoặc token hết hạn"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Không tìm thấy sự kiện tương ứng"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "Đã hết vé")
    })
    @PostMapping("/{eventId}/buy")
    public ResponseEntity<ApiResponse<BuyTicketAcceptedResponse>> buyTicket(
            @RequestBody BuyTicketRequest request,
            @Parameter(description = "ID sự kiện muốn mua vé", required = true)
            @PathVariable UUID eventId){
        Optional<BuyTicketAcceptedResponse> response = orderService.buyTicket(request, eventId);
        if (response.isPresent()) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(response.get()));
        }
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null));
    }
}
