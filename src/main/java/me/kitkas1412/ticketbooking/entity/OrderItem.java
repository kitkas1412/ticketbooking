package me.kitkas1412.ticketbooking.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
@Table(name = "order_item")
public class OrderItem extends BaseEntity{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", unique = true, nullable = false)
    private Ticket ticket;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
}
