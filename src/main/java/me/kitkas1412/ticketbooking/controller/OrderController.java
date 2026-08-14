package me.kitkas1412.ticketbooking.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import me.kitkas1412.ticketbooking.dto.response.ApiResponse;
import me.kitkas1412.ticketbooking.dto.response.BuyTicketAcceptedResponse;
import me.kitkas1412.ticketbooking.dto.response.BuyTicketResponse;
import me.kitkas1412.ticketbooking.dto.response.ErrorDetail;
import me.kitkas1412.ticketbooking.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Tra cứu trạng thái đơn mua vé")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(
            summary = "Xem trạng thái đơn hàng",
            description = """
                    Endpoint để client poll sau khi `POST /api/events/{eventId}/buy` trả 202.
                    Trạng thái chuyển từ PENDING sang CONFIRMED, CANCELLED hoặc FAILED khi
                    consumer xử lý xong message trong RabbitMQ.

                    Kiểu của `data` phụ thuộc trạng thái: đơn CONFIRMED trả về thông tin vé
                    (`BuyTicketResponse`), các trạng thái còn lại chỉ trả về trạng thái đơn
                    (`BuyTicketAcceptedResponse`).
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Trả về trạng thái hiện tại của đơn",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OrderStatusApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Thiếu token hoặc token hết hạn"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Không tìm thấy đơn hàng")
    })
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<?>> getOrderStatus(
            @Parameter(description = "ID đơn hàng nhận được từ response khi đặt vé", required = true)
            @PathVariable UUID orderId) {
        Object response = orderService.getOrderStatus(orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Kiểu chỉ tồn tại để sinh tài liệu, không dùng lúc chạy.
     *
     * <p>{@code getOrderStatus} khai báo trả {@code ApiResponse<?>} vì {@code data}
     * có hai hình dạng tuỳ trạng thái đơn; springdoc gặp wildcard thì chỉ sinh
     * được {@code data: object} — đúng nhưng vô dụng với người đọc. Bản sao này
     * giữ nguyên lớp bao {@code ApiResponse} và nói rõ hai kiểu có thể xuất hiện.
     *
     * <p>Đánh đổi: nếu {@code ApiResponse} đổi cấu trúc, phải sửa cả ở đây —
     * trình biên dịch không bắt được sự lệch pha này.
     */
    @Schema(name = "ApiResponseOrderStatus",
            description = "Trạng thái đơn hàng, bọc trong khung ApiResponse chung")
    private record OrderStatusApiResponse(

            @Schema(example = "true")
            boolean success,

            @Schema(description = "BuyTicketResponse khi đơn đã CONFIRMED, "
                    + "BuyTicketAcceptedResponse ở các trạng thái còn lại",
                    oneOf = {BuyTicketResponse.class, BuyTicketAcceptedResponse.class})
            Object data,

            ErrorDetail error,

            Map<String, Object> meta) {
    }
}
