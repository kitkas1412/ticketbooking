package me.kitkas1412.ticketbooking.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
@Table(name = "ticket")
public class Ticket extends BaseEntity{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "seat_code", nullable = false)
    private Integer seatCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private TicketStatus status = TicketStatus.AVAILABLE;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    public enum TicketStatus{
        AVAILABLE,RESERVED,SOLD,CANCELED
    }

}
