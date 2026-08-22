package me.kitkas1412.ticketbooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TicketbookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketbookingApplication.class, args);
    }

}
