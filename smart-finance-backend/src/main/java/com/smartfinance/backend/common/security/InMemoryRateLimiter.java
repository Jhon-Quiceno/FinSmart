package com.smartfinance.backend.common.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory, per-key, fixed-window rate limiter — greenfield choice for this sprint (see
 * {@code docs/sprints/sprint1.md}, architecture decision 4: "en memoria, bucket por IP+usuario,
 * no distribuido"), deliberately not backed by Bucket4j/Redis since a single-instance deployment
 * does not need cross-node coordination yet.
 *
 * <p>Not a Spring bean on purpose: {@link RateLimitFilter} constructs its own instance directly
 * so the filter's constructor only depends on primitive {@code @Value} properties, keeping it
 * trivially constructible in {@code @WebMvcTest} slices that don't configure a full application
 * context (see {@code RateLimitFilter}'s class Javadoc).
 *
 * <p>Each key's window is reset lazily on the next {@link #tryConsume} call once it has expired,
 * rather than through a background sweep — this keeps the map bounded by the number of distinct
 * active keys (IPs/users) rather than growing forever, without needing a scheduled cleanup task.
 */
public class InMemoryRateLimiter {

    private final Clock clock;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public InMemoryRateLimiter() {
        this(Clock.systemUTC());
    }

    /** Package-visible constructor used by tests to control time deterministically. */
    InMemoryRateLimiter(Clock clock) {
        this.clock = clock;
    }

    /**
     * @return {@code true} if the request for {@code key} is allowed (and is now counted against
     *         its window), {@code false} if {@code key} has already reached {@code maxRequests}
     *         within the current {@code window}
     */
    public boolean tryConsume(String key, int maxRequests, Duration window) {
        Instant now = clock.instant();
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(now));

        synchronized (bucket) {
            if (Duration.between(bucket.windowStart, now).compareTo(window) >= 0) {
                bucket.windowStart = now;
                bucket.count.set(0);
            }

            if (bucket.count.get() >= maxRequests) {
                return false;
            }

            bucket.count.incrementAndGet();
            return true;
        }
    }

    private static final class Bucket {
        private volatile Instant windowStart;
        private final AtomicInteger count = new AtomicInteger(0);

        private Bucket(Instant windowStart) {
            this.windowStart = windowStart;
        }
    }
}
