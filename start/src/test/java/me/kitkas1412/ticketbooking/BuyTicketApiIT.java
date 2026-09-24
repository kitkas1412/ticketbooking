package me.kitkas1412.ticketbooking;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API mua vé qua HTTP: xác thực JWT, mã trạng thái và body trả về.
 */
class BuyTicketApiIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String accessToken;

    @BeforeEach
    void registerUser() throws Exception {
        String body = """
                {"email": "%s@it.test", "password": "MatKhau123", "fullName": "Integration Test"}
                """.formatted(UUID.randomUUID());
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        accessToken = JsonPath.read(response, "$.data.accessToken");
    }

    @Test
    void rejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(post("/api/orders/{eventId}/buy", createEvent(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buyBody(UUID.randomUUID().toString())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void acceptsPurchaseAndTreatsRetryAsDuplicate() throws Exception {
        UUID eventId = createEvent(2);
        String idempotencyKey = UUID.randomUUID().toString();

        buy(eventId, idempotencyKey)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.eventId").value(eventId.toString()))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        buy(eventId, idempotencyKey)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void returnsConflictWhenSoldOut() throws Exception {
        UUID eventId = createEvent(1);
        buy(eventId, UUID.randomUUID().toString()).andExpect(status().isAccepted());

        buy(eventId, UUID.randomUUID().toString())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void returnsNotFoundForUnknownEvent() throws Exception {
        buy(UUID.randomUUID(), UUID.randomUUID().toString())
                .andExpect(status().isNotFound());
    }

    private ResultActions buy(UUID eventId, String idempotencyKey) throws Exception {
        return mockMvc.perform(post("/api/orders/{eventId}/buy", eventId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(buyBody(idempotencyKey)));
    }

    private static String buyBody(String idempotencyKey) {
        return """
                {"idempotencyKey": "%s"}
                """.formatted(idempotencyKey);
    }
}
