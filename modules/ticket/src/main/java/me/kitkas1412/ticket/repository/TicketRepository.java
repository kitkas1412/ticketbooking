package me.kitkas1412.ticket.repository;

import me.kitkas1412.ticket.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Truy cập dữ liệu vé và khai báo truy vấn lấy, đếm vé theo sự kiện và trạng thái.
 */
@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    // Derived query dùng Event nhưng field trên entity là eventId; cần sửa tên method cho khớp.
    Optional<Ticket> findFirstByEventAndStatus(UUID eventId, Ticket.TicketStatus ticketStatus);

    Long countByEventAndStatus(UUID eventId, Ticket.TicketStatus status);
}
