package me.kitkas1412.persistence;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BaseEntityTest {

    private static final class TestEntity extends BaseEntity {
    }

    @Test
    void onCreateSetsCreatedAndUpdatedToSameInstant() {
        TestEntity entity = new TestEntity();

        entity.onCreate();

        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isEqualTo(entity.getCreatedAt());
    }

    @Test
    void onUpdateRefreshesOnlyUpdatedAt() {
        TestEntity entity = new TestEntity();
        OffsetDateTime createdAt = OffsetDateTime.now().minusDays(1);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(createdAt);

        entity.onUpdate();

        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getUpdatedAt()).isAfter(createdAt);
    }
}
