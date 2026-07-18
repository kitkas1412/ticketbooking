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
@Table(name =  "orders")
public class Order extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @OneToOne
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    private enum OrderStatus{
        PENDING, CONFIRMED, CANCELLED, FAILED,
    }
}
