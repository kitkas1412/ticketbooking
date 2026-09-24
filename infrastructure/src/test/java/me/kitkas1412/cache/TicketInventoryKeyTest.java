package me.kitkas1412.cache;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TicketInventoryKeyTest {

    @Test
    void availableTicketsKeyIsScopedByEventId() {
        UUID eventId = UUID.fromString("3f2504e0-4f89-11d3-9a0c-0305e82c3301");

        assertThat(TicketInventoryKey.availableTickets(eventId))
                .isEqualTo("event:3f2504e0-4f89-11d3-9a0c-0305e82c3301:tickets_available");
    }

    @Test
    void idempotencyKeyIsPrefixed() {
        assertThat(TicketInventoryKey.idempotencyKey("abc-123")).isEqualTo("idempotencyKey:abc-123");
    }
}
