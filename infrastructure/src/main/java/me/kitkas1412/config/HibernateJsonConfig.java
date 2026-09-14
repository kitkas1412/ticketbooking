package me.kitkas1412.config;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.type.format.AbstractJsonFormatMapper;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * Cấu hình Hibernate đọc và ghi các cột JSON bằng ObjectMapper của Jackson 3.
 *
 * <p>Hibernate tự tìm JSON FormatMapper cho Jackson 2 hoặc JSON-B;
 * ứng dụng dùng Jackson 3 nên cần cấu hình FormatMapper cho OutboxEvent.payload.
 *
 * <p>AbstractJsonFormatMapper giữ nguyên giá trị của field kiểu String,
 * tránh serialize lần nữa khi payload đã là chuỗi JSON.
 */
@Configuration
public class HibernateJsonConfig implements HibernatePropertiesCustomizer {

    private final ObjectMapper objectMapper;

    public HibernateJsonConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(AvailableSettings.JSON_FORMAT_MAPPER, new Jackson3JsonFormatMapper(objectMapper));
    }

    static final class Jackson3JsonFormatMapper extends AbstractJsonFormatMapper {

        private final ObjectMapper objectMapper;

        Jackson3JsonFormatMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        protected <T> T fromString(CharSequence charSequence, Type type) {
            return objectMapper.readValue(charSequence.toString(), objectMapper.constructType(type));
        }

        @Override
        protected <T> String toString(T value, Type type) {
            return objectMapper.writerFor(objectMapper.constructType(type)).writeValueAsString(value);
        }
    }
}
