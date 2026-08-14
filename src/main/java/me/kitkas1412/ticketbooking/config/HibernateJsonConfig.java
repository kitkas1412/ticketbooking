package me.kitkas1412.ticketbooking.config;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.type.format.AbstractJsonFormatMapper;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * Teaches Hibernate how to (de)serialize {@code @JdbcTypeCode(SqlTypes.JSON)}
 * columns — currently {@code OutboxEvent.payload}.
 *
 * <p>Hibernate auto-detects a JSON {@code FormatMapper} only for Jackson 2
 * ({@code com.fasterxml.jackson}) or a JSON-B implementation. Spring Boot 4
 * ships Jackson 3 ({@code tools.jackson}), which that detection does not
 * recognise, so without this every insert into {@code outbox_events} fails at
 * flush time with "Could not find a FormatMapper for the JSON format" — taking
 * down the entire purchase path.
 *
 * <p>Extending {@link AbstractJsonFormatMapper} rather than implementing
 * {@code FormatMapper} directly matters: it short-circuits {@code String}-typed
 * properties so an already-serialized payload is written through verbatim
 * instead of being JSON-encoded a second time.
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
