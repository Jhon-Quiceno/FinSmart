package com.smartfinance.backend.integraciones.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store for one-time codes used to link a Telegram chat to a user account (see
 * {@code TelegramLinkService#generateLinkCode} and {@code #confirmLink}).
 *
 * <p>Mirrors {@link com.smartfinance.backend.common.security.InMemoryRateLimiter}'s design
 * (fixed-window-free {@code ConcurrentHashMap}, lazy eviction of expired entries on access), but
 * unlike that class it <b>is</b> a Spring bean: the code issued by one HTTP request (authenticated,
 * JWT-protected) must be visible to a later, unrelated HTTP request (the Telegram webhook, a few
 * minutes later) hitting a different filter chain, so the same singleton instance must be shared
 * across requests rather than constructed per-filter.
 *
 * <p>Codes expire after {@link #TTL_SECONDS} (10 minutes) and are single-use: {@link #consume}
 * removes the entry as it reads it, so a leaked or reused code can never be replayed.
 */
@Component
public class TelegramLinkCodeStore {

    /** Time-to-live of an issued code, in seconds. */
    static final long TTL_SECONDS = 600;

    /** Characters allowed in generated codes, excluding visually ambiguous ones (0/O, 1/I). */
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;

    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();

    public TelegramLinkCodeStore() {
        this(Clock.systemUTC());
    }

    /** Package-visible constructor used by tests to control time deterministically. */
    TelegramLinkCodeStore(Clock clock) {
        this.clock = clock;
    }

    /**
     * Generates and stores a new one-time code for {@code userId}, valid for {@link #TTL_SECONDS}.
     *
     * @param userId identifier of the user requesting the link
     * @return the generated code
     */
    public String issue(Long userId) {
        evictExpired();

        String code = generateCode();
        Instant expiresAt = clock.instant().plusSeconds(TTL_SECONDS);
        entries.put(code, new Entry(userId, expiresAt));
        return code;
    }

    /**
     * Consumes {@code code} if it exists and has not expired, removing it so it cannot be reused.
     *
     * @param code the code to consume
     * @return the {@code userId} the code was issued for, or {@link Optional#empty()} when the
     *         code does not exist or has already expired
     */
    public Optional<Long> consume(String code) {
        Entry entry = entries.remove(code);
        if (entry == null || isExpired(entry)) {
            return Optional.empty();
        }
        return Optional.of(entry.userId());
    }

    private void evictExpired() {
        for (Iterator<Map.Entry<String, Entry>> it = entries.entrySet().iterator(); it.hasNext(); ) {
            if (isExpired(it.next().getValue())) {
                it.remove();
            }
        }
    }

    private boolean isExpired(Entry entry) {
        return clock.instant().isAfter(entry.expiresAt());
    }

    private String generateCode() {
        StringBuilder builder = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            builder.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
        }
        return builder.toString();
    }

    private record Entry(Long userId, Instant expiresAt) {
    }
}
