package me.kitkas1412.ticketbooking.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
@Table(name = "event")
public class Event extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "total_tickets", nullable = false)
    private Integer totalTickets;

    @Column(name = "sale_start_at", nullable = false)
    private OffsetDateTime saleStartAt;

    @Column(name = "sale_end_at", nullable = false)
    private OffsetDateTime saleEndAt;

    @Column(name = "status", nullable = false)
    @Builder.Default
    private EventStatus status = EventStatus.DRAFT;

    @Version
    @Column(name = "version")
    private Integer version;

    public enum EventStatus {
        DRAFT, ON_SALE, SOLD_OUT, CLOSED
    }
}
