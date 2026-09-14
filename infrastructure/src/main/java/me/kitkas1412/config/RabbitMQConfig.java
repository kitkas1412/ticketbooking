package me.kitkas1412.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình queue, direct exchange, binding và JSON message converter cho luồng mua vé.
 */
@Configuration
public class RabbitMQConfig {

    public static final String TICKET_BUY_QUEUE = "ticket.buy.queue";
    public static final String TICKET_EXCHANGE = "ticket.exchange";
    public static final String TICKET_BUY_ROUTING_KEY = "ticket.buy.requested";

    public static final String TICKET_BUY_REQUESTED_EVENT = "TicketBuyRequested";

    // Queue dùng durable=true để vẫn tồn tại sau khi RabbitMQ restart.
    @Bean
    public Queue ticketBuyQueue(){
        return new Queue(TICKET_BUY_QUEUE, true);
    }

    @Bean
    public DirectExchange ticketExchange(){
        return new DirectExchange(TICKET_EXCHANGE, true, false);
    }

    // Binding đưa message có routing key mua vé từ exchange vào queue tương ứng.
    @Bean
    public Binding ticketBuyBinding(Queue ticketBuyQueue, DirectExchange ticketExchange){
        return BindingBuilder.bind(ticketBuyQueue).to(ticketExchange).with(TICKET_BUY_ROUTING_KEY);
    }

    // Serialize và deserialize message JSON khi gửi, nhận qua RabbitMQ.
    @Bean
    public JacksonJsonMessageConverter jsonMessageConverter(){
        return new JacksonJsonMessageConverter();
    }
}
