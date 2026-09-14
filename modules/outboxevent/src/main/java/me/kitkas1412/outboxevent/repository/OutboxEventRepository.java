package me.kitkas1412.outboxevent.repository;

import me.kitkas1412.outboxevent.entity.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Truy vấn outbox theo UUID và lấy từng trang message chưa được đánh dấu đã gửi.
 */
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    // Lấy bản ghi chưa gửi từ cũ đến mới; Pageable giới hạn kích thước mỗi đợt.
    List<OutboxEvent> findByPublishedAtIsNullOrderByCreatedAtAsc(Pageable pageable);
}
