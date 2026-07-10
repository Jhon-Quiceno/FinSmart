package com.smartfinance.backend.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Enables {@code @Async} method execution and provides a small, dedicated thread pool for
 * sending notification emails (see
 * {@code com.smartfinance.backend.servicios.service.notification.EmailNotificationSender}), so a slow or
 * unreachable SMTP server never blocks the request thread that triggered a notification.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /** Bean name of {@link #mailTaskExecutor()}, referenced by {@code @Async("mailTaskExecutor")}. */
    public static final String MAIL_EXECUTOR = "mailTaskExecutor";

    @Bean(name = MAIL_EXECUTOR)
    public Executor mailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("mail-notif-");
        executor.initialize();
        return executor;
    }
}
