package com.smartfinance.backend.integraciones.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramLinkCodeStoreTest {

    private static final Pattern CODE_CHARSET = Pattern.compile("^[A-HJ-NP-Z2-9]{8}$");

    private final MutableClock clock = new MutableClock(Instant.parse("2026-07-19T10:00:00Z"));
    private final TelegramLinkCodeStore store = new TelegramLinkCodeStore(clock);

    @Test
    void issueThenConsumeReturnsTheUserIdTheCodeWasIssuedFor() {
        String code = store.issue(1L);

        Optional<Long> result = store.consume(code);

        assertThat(result).contains(1L);
    }

    @Test
    void consumeIsOneTimeUseAndReturnsEmptyOnSecondCall() {
        String code = store.issue(1L);
        store.consume(code);

        Optional<Long> secondAttempt = store.consume(code);

        assertThat(secondAttempt).isEmpty();
    }

    @Test
    void consumeReturnsEmptyForACodeThatNeverExisted() {
        Optional<Long> result = store.consume("NOEXIST1");

        assertThat(result).isEmpty();
    }

    @Test
    void consumeReturnsEmptyOnceTheTtlHasElapsed() {
        String code = store.issue(1L);
        clock.advanceSeconds(TelegramLinkCodeStore.TTL_SECONDS + 1);

        Optional<Long> result = store.consume(code);

        assertThat(result).isEmpty();
    }

    @Test
    void consumeStillWorksJustBeforeTheTtlElapses() {
        String code = store.issue(1L);
        clock.advanceSeconds(TelegramLinkCodeStore.TTL_SECONDS - 1);

        Optional<Long> result = store.consume(code);

        assertThat(result).contains(1L);
    }

    @Test
    void issueGeneratesAnEightCharacterUppercaseAlphanumericCodeExcludingAmbiguousCharacters() {
        String code = store.issue(1L);

        assertThat(code).hasSize(8);
        assertThat(CODE_CHARSET.matcher(code).matches()).isTrue();
    }

    /** Mutable {@link Clock} used to advance time deterministically without sleeping the test thread. */
    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advanceSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
