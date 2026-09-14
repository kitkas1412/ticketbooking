package me.kitkas1412.cache;

import java.util.UUID;

/**
 * Tạo Redis key theo cùng một quy ước cho tồn kho và idempotency.
 */
public final class TicketInventoryKey {
    private TicketInventoryKey(){}

    public static String availableTickets(UUID eventId){
        return "event:" + eventId + ":tickets_available";
    }

    public static String idempotencyKey(String idempotencyKey){
        return "idempotencyKey:" + idempotencyKey;
    }
}
