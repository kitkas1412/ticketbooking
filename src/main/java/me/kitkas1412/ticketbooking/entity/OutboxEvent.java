package me.kitkas1412.ticketbooking.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "outbox_events")
public class OutboxEvent extends BaseEntity{

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType; // 'ORDER' | 'TICKET' | 'EVENT'

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType; // 'OrderConfirmed' | 'TicketSold' ...

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String payload;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

}