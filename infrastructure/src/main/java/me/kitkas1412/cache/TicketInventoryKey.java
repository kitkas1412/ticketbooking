package me.kitkas1412.cache;

import java.util.UUID;

public final class TicketInventoryKey {
    private TicketInventoryKey(){}

    public static String availableTickets(UUID eventId){
        return "event:" + eventId + ":tickets_available";
    }

    public static String idempotencyKey(String idempotencyKey){
        return "idempotencyKey:" + idempotencyKey;
    }
}
