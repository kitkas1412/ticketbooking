package me.kitkas1412.ticketbooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Khởi động Spring Boot và bật scheduler cho outbox publisher cùng tác vụ đồng bộ tồn kho.
 */
@SpringBootApplication
@EnableScheduling
public class TicketbookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketbookingApplication.class, args);
    }

}
