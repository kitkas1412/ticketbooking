package me.kitkas1412.event.repository;

import me.kitkas1412.event.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Truy cập dữ liệu sự kiện bằng UUID thông qua các thao tác JPA dùng chung.
 */
@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

}
