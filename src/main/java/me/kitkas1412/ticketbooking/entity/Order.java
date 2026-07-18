package me.kitkas1412.ticketbooking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name =  "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_id")
    private UUID id;

    @Column(name = "idempotency_key", unique = true, nullable = false)
    private String idempotencyKey;

    @OneToOne
    @JoinColumn(name = "ticket_id", unique = true, nullable = false)
    private Ticket ticket;
}
