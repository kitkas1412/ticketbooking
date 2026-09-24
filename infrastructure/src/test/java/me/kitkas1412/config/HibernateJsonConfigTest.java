package me.kitkas1412.config;

import me.kitkas1412.mq.BuyTicketMessage;
import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HibernateJsonConfigTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    void customizeRegistersJackson3FormatMapper() {
        Map<String, Object> properties = new HashMap<>();

        new HibernateJsonConfig(objectMapper).customize(properties);

        assertThat(properties.get(AvailableSettings.JSON_FORMAT_MAPPER))
                .isInstanceOf(HibernateJsonConfig.Jackson3JsonFormatMapper.class);
    }

    @Test
    void formatMapperRoundTripsObjects() {
        HibernateJsonConfig.Jackson3JsonFormatMapper mapper =
                new HibernateJsonConfig.Jackson3JsonFormatMapper(objectMapper);
        BuyTicketMessage message = new BuyTicketMessage(UUID.randomUUID(), UUID.randomUUID());

        String json = mapper.toString(message, BuyTicketMessage.class);
        BuyTicketMessage restored = mapper.fromString(json, BuyTicketMessage.class);

        assertThat(json).contains(message.eventId().toString(), message.orderId().toString());
        assertThat(restored).isEqualTo(message);
    }
}
