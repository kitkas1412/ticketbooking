package me.kitkas1412.order.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import me.kitkas1412.persistence.BaseEntity;

import java.util.UUID;

/**
 * Đơn mua vé lưu ID sự kiện, idempotency key và trạng thái xử lý bất đồng bộ.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
@Table(name =  "orders")
public class Order extends BaseEntity {

    @Column(name = "event_id", nullable = false)
    private UUID event_id;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    // Dùng optimistic locking qua @Version để phát hiện các transaction cùng sửa một bản ghi.
    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    public enum OrderStatus{
        PENDING, CONFIRMED, CANCELLED, FAILED,
    }
}
