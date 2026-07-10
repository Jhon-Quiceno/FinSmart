package com.smartfinance.backend.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Provides the {@link RestClient.Builder} bean used by {@code com.smartfinance.backend.ia.service.ai.AiChatClient}.
 *
 * <p>Spring Boot 4's {@code spring-boot-autoconfigure} module no longer ships a {@code RestClient}
 * autoconfiguration in this project's dependency set ({@code spring-boot-starter-webmvc} alone
 * does not pull one in), so this bean is declared explicitly here instead — this only calls the
 * plain {@link RestClient#builder()} static factory already available via {@code spring-web} (a
 * transitive dependency of {@code spring-boot-starter-webmvc}), adding no new dependency and no
 * new HTTP client library, matching the "RestClient only, no WebFlux/no new HTTP dependency"
 * decision in {@code docs/sprints/sprint5.md}.
 *
 * <p>The 10s connect / 60s read timeouts for AI provider calls are configured once here, on the
 * shared builder, rather than per-call in {@code AiChatClient} — that keeps {@code AiChatClient}
 * free to only ever call {@link RestClient.Builder#clone()} on the builder it receives without
 * ever replacing its request factory, which is what lets tests substitute a
 * {@code MockRestServiceServer}-bound {@link RestClient.Builder} without any of this timeout
 * configuration interfering with request interception.
 *
 * <p>A single shared instance is safe to inject everywhere: every consumer only ever calls
 * {@link RestClient.Builder#clone()} on it before customizing per-call settings (currently just
 * the base URL), never mutating this bean's own state directly.
 */
@Configuration
public class RestClientConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(60);

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder().requestFactory(buildRequestFactory());
    }

    private static ClientHttpRequestFactory buildRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        return factory;
    }
}
