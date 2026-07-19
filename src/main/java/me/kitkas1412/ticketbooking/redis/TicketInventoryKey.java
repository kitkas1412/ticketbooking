package me.kitkas1412.ticketbooking.redis;

import java.util.UUID;

public final class TicketInventoryKey {
    private TicketInventoryKey(){}

    public static String availableTickets(UUID eventId){
        return "event:" + eventId + ":tickets_available";
    }
}
