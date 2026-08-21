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

    /** Bean name of {@link #pushTaskExecutor()}, referenced by {@code @Async("pushTaskExecutor")}. */
    public static final String PUSH_EXECUTOR = "pushTaskExecutor";

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

    /**
     * Dedicated pool for {@code ExpoPushAdapter}, kept separate from {@link #mailTaskExecutor()}
     * so a slow/unreachable Expo push service never queues behind — or starves — outbound email
     * delivery, and vice versa.
     */
    @Bean(name = PUSH_EXECUTOR)
    public Executor pushTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("push-notif-");
        executor.initialize();
        return executor;
    }
}
