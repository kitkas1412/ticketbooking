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

    @Bean
    public Queue ticketBuyQueue(){
        return new Queue("ticket.buy.queue", true);
    }

    @Bean
    public DirectExchange ticketExchange(){
        return new DirectExchange("ticket.exchange", true, false);
    }

    @Bean
    public Binding ticketBuyBinding(Queue ticketBuyQueue, DirectExchange ticketExchange){
        return BindingBuilder.bind(ticketBuyQueue).to(ticketExchange).with("ticket.buy.requested");
    }

    @Bean
    public JacksonJsonMessageConverter jsonMessageConverter(){
        return new JacksonJsonMessageConverter();
    }
}
