package me.kitkas1412.ticketbooking.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String TICKET_BUY_QUEUE = "ticket.buy.queue";
    public static final String TICKET_EXCHANGE = "ticket.exchange";
    public static final String TICKET_BUY_ROUTING_KEY = "ticket.buy.requested";

    @Bean
    public Queue ticketBuyQueue(){
        return new Queue(TICKET_BUY_QUEUE, true);
    }

    @Bean
    public DirectExchange ticketExchange(){
        return new DirectExchange(TICKET_EXCHANGE, true, false);
    }

    @Bean
    public Binding ticketBuyBinding(Queue ticketBuyQueue, DirectExchange ticketExchange){
        return BindingBuilder.bind(ticketBuyQueue).to(ticketExchange).with(TICKET_BUY_ROUTING_KEY);
    }

    @Bean
    public JacksonJsonMessageConverter jsonMessageConverter(){
        return new JacksonJsonMessageConverter();
    }
}
