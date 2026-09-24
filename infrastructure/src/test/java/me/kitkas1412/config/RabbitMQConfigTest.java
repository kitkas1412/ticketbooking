package me.kitkas1412.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitMQConfigTest {

    private final RabbitMQConfig config = new RabbitMQConfig();

    @Test
    void ticketBuyQueueIsDurable() {
        Queue queue = config.ticketBuyQueue();

        assertThat(queue.getName()).isEqualTo(RabbitMQConfig.TICKET_BUY_QUEUE);
        assertThat(queue.isDurable()).isTrue();
    }

    @Test
    void ticketExchangeIsDurableAndNotAutoDeleted() {
        DirectExchange exchange = config.ticketExchange();

        assertThat(exchange.getName()).isEqualTo(RabbitMQConfig.TICKET_EXCHANGE);
        assertThat(exchange.isDurable()).isTrue();
        assertThat(exchange.isAutoDelete()).isFalse();
    }

    @Test
    void bindingRoutesBuyRequestsFromExchangeToQueue() {
        Binding binding = config.ticketBuyBinding(config.ticketBuyQueue(), config.ticketExchange());

        assertThat(binding.getDestination()).isEqualTo(RabbitMQConfig.TICKET_BUY_QUEUE);
        assertThat(binding.getExchange()).isEqualTo(RabbitMQConfig.TICKET_EXCHANGE);
        assertThat(binding.getRoutingKey()).isEqualTo(RabbitMQConfig.TICKET_BUY_ROUTING_KEY);
    }
}
