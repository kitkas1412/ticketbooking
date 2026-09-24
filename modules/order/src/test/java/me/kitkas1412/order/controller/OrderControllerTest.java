package me.kitkas1412.order.controller;

import me.kitkas1412.common.dto.request.BuyTicketRequest;
import me.kitkas1412.common.dto.response.BuyTicketAcceptedResponse;
import me.kitkas1412.common.response.ApiResponse;
import me.kitkas1412.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController controller;

    @Test
    void acceptedOrderReturns202WithOrderInfo() throws Exception {
        UUID eventId = UUID.randomUUID();
        BuyTicketRequest request = new BuyTicketRequest("key-1");
        BuyTicketAcceptedResponse accepted = new BuyTicketAcceptedResponse(UUID.randomUUID(), eventId, "PENDING");
        when(orderService.buyTicket(request, eventId)).thenReturn(Optional.of(accepted));

        ResponseEntity<ApiResponse<BuyTicketAcceptedResponse>> response = controller.buyTicket(request, eventId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isEqualTo(ApiResponse.success(accepted));
    }

    @Test
    void duplicateRequestReturns200WithoutData() throws Exception {
        UUID eventId = UUID.randomUUID();
        BuyTicketRequest request = new BuyTicketRequest("key-1");
        when(orderService.buyTicket(request, eventId)).thenReturn(Optional.empty());

        ResponseEntity<ApiResponse<BuyTicketAcceptedResponse>> response = controller.buyTicket(request, eventId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().data()).isNull();
    }
}
