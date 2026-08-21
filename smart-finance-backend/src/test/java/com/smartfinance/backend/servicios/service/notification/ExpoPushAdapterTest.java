package com.smartfinance.backend.servicios.service.notification;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ExpoPushAdapterTest {

    private final RestClient.Builder restClientBuilder = RestClient.builder();
    private final MockRestServiceServer mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
    private final ExpoPushAdapter expoPushAdapter = new ExpoPushAdapter(restClientBuilder);

    @Test
    void sendShouldPostRecipientTokenTitleAndBodyToExpoPushEndpoint() {
        mockServer.expect(requestTo("https://exp.host/--/api/v2/push/send"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.to").value("ExponentPushToken[abc123]"))
                .andExpect(jsonPath("$.title").value("Título"))
                .andExpect(jsonPath("$.body").value("Mensaje"))
                .andRespond(withSuccess("""
                        {"data": {"status": "ok"}}
                        """, MediaType.APPLICATION_JSON));

        expoPushAdapter.send(new PushRecipient(1L, "ExponentPushToken[abc123]"), "Título", "Mensaje");

        mockServer.verify();
    }

    @Test
    void sendShouldSwallowServerErrorsWithoutThrowing() {
        mockServer.expect(requestTo("https://exp.host/--/api/v2/push/send"))
                .andRespond(withServerError());

        Assertions.assertDoesNotThrow(() ->
                expoPushAdapter.send(new PushRecipient(1L, "ExponentPushToken[abc123]"), "Título", "Mensaje")
        );
    }
}
