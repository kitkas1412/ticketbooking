package me.kitkas1412.ticketbooking;

import me.kitkas1412.cache.TicketInventoryKey;
import me.kitkas1412.event.dto.request.CreateEventRequest;
import me.kitkas1412.event.service.EventService;
import me.kitkas1412.order.entity.Order;
import me.kitkas1412.order.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Nền chung cho integration test: khởi động toàn bộ ứng dụng với container thật.
 *
 * <p>Profile {@code it} để không nạp {@code application-local.yaml} trên máy dev.
 * Các biến môi trường bắt buộc trong application.yaml được gán giá trị giả; kết nối
 * thật do {@link TestcontainersConfiguration} cung cấp.
 *
 * <p>Các test dùng chung database nên mỗi test tự tạo sự kiện riêng và chỉ kiểm tra
 * dữ liệu của sự kiện đó. Mọi cấu hình context (kể cả MockMvc) đặt ở đây để các test
 * class dùng chung một context và một bộ container.
 */
@SpringBootTest(properties = {
        "POSTGRES_DB=ticketbooking",
        "POSTGRES_USER=test",
        "POSTGRES_PASS=test",
        // Khoá chỉ dùng cho test (base64 của 64 byte), không dùng ở môi trường thật.
        "JWT_SECRET=aW50ZWdyYXRpb24tdGVzdC1vbmx5LXNlY3JldC1ub3QtZm9yLXByb2R1Y3Rpb24tdXNlLTY0LWJ5dGVzLWxvbmc=",
        "spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("it")
public abstract class AbstractIntegrationTest {

    @Autowired
    protected EventService eventService;

    @Autowired
    protected OrderRepository orderRepository;

    @Autowired
    protected StringRedisTemplate redisTemplate;

    /** Tạo sự kiện mới cùng {@code totalTickets} vé AVAILABLE và bộ đếm tồn kho Redis. */
    protected UUID createEvent(int totalTickets) {
        OffsetDateTime now = OffsetDateTime.now();
        return eventService.createEvent(new CreateEventRequest(
                "IT event " + UUID.randomUUID(),
                "Integration test",
                totalTickets,
                new BigDecimal("350000.00"),
                now.minusDays(1),
                now.plusDays(1))).eventId();
    }

    protected long availableInRedis(UUID eventId) {
        return Long.parseLong(redisTemplate.opsForValue().get(TicketInventoryKey.availableTickets(eventId)));
    }

    protected List<Order> ordersOf(UUID eventId) {
        return orderRepository.findAll().stream()
                .filter(order -> eventId.equals(order.getEvent_id()))
                .toList();
    }
}
