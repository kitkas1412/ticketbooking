package me.kitkas1412.ticketbooking;

import me.kitkas1412.event.service.EventService;
import me.kitkas1412.order.controller.OrderController;
import me.kitkas1412.order.service.OrderService;
import me.kitkas1412.ticket.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ứng dụng khởi động được với hạ tầng thật và nạp đủ bean của các module nghiệp vụ.
 */
class ApplicationContextIT extends AbstractIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void loadsBeansFromAllBusinessModules() {
        assertThat(context.getBeanNamesForType(EventService.class)).isNotEmpty();
        assertThat(context.getBeanNamesForType(OrderService.class)).isNotEmpty();
        assertThat(context.getBeanNamesForType(OrderController.class)).isNotEmpty();
        assertThat(context.getBeanNamesForType(TicketService.class)).isNotEmpty();
        // Module outboxevent: relay gửi message và consumer xử lý mua vé.
        assertThat(context.containsBean("outboxEventRelay")).isTrue();
        assertThat(context.containsBean("ticketPurchaseConsumer")).isTrue();
    }
}
