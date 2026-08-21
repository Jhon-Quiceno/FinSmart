package com.smartfinance.backend.servicios.service.notification;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.smartfinance.backend.common.config.AsyncConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * {@link PushNotificationSender} adapter that delivers notifications through Expo's push
 * service (see {@code https://docs.expo.dev/push-notifications/sending-notifications/}).
 *
 * <p>A fresh {@link RestClient} is built per call the same way {@code AiChatClient} builds one
 * per provider — the injected {@link RestClient.Builder} bean is cloned with Expo's fixed base
 * URL, never mutated, so its configured connect/read timeouts survive every {@code clone()} and
 * a test can substitute a {@code MockRestServiceServer}-bound builder without this class ever
 * overwriting the mock request factory.
 *
 * <p>{@link #send} catches every exception broadly, same reasoning as
 * {@link EmailNotificationSender#send}: this method runs on the push {@code @Async} executor,
 * where an uncaught exception is only ever logged by Spring's default async exception handler
 * and never surfaces to a caller — an unreachable/misconfigured/expired push channel must never
 * break the in-app notification that was already created.
 */
@Component
public class ExpoPushAdapter implements PushNotificationSender {

    private static final Logger log = LoggerFactory.getLogger(ExpoPushAdapter.class);
    private static final String EXPO_PUSH_BASE_URL = "https://exp.host";
    private static final String EXPO_PUSH_PATH = "/--/api/v2/push/send";

    private final RestClient.Builder restClientBuilder;

    public ExpoPushAdapter(RestClient.Builder restClientBuilder) {
        this.restClientBuilder = restClientBuilder;
    }

    @Async(AsyncConfig.PUSH_EXECUTOR)
    @Override
    public void send(PushRecipient recipient, String title, String body) {
        try {
            restClientBuilder.clone()
                    .baseUrl(EXPO_PUSH_BASE_URL)
                    .build()
                    .post()
                    .uri(EXPO_PUSH_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ExpoPushMessage(recipient.expoPushToken(), title, body))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("failed_to_send_push_notification userId={}", recipient.userId(), ex);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record ExpoPushMessage(String to, String title, String body) {
    }
}
