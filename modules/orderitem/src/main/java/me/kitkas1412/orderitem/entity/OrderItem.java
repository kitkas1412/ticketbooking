package me.kitkas1412.orderitem.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import me.kitkas1412.persistence.BaseEntity;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Chi tiết đơn lưu ID đơn, ID vé và giá tại thời điểm mua; mỗi vé chỉ thuộc một chi tiết đơn.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
@Table(name = "order_item")
public class OrderItem extends BaseEntity {

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "ticket_id", unique = true, nullable = false)
    private UUID ticketId;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
}
