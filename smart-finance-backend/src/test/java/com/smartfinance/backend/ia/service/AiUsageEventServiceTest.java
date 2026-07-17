package com.smartfinance.backend.ia.service;

import com.smartfinance.backend.ia.model.dto.AiUsageEventSummaryResponse;
import com.smartfinance.backend.ia.model.entity.AiUsageEvent;
import com.smartfinance.backend.ia.model.entity.AiUsageEventType;
import com.smartfinance.backend.ia.repository.AiUsageEventRepository;
import com.smartfinance.backend.usuario.model.entity.User;
import com.smartfinance.backend.usuario.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiUsageEventServiceTest {

    @Mock
    private AiUsageEventRepository usageEventRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AiUsageEventService service;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordShouldPersistUsageEventWithGivenFields() {
        User user = new User();
        user.setId(1L);
        when(userRepository.getReferenceById(1L)).thenReturn(user);

        service.record(1L, "groq", AiUsageEventType.CHAT, 120, BigDecimal.valueOf(0.002));

        ArgumentCaptor<AiUsageEvent> captor = ArgumentCaptor.forClass(AiUsageEvent.class);
        verify(usageEventRepository).save(captor.capture());
        AiUsageEvent saved = captor.getValue();
        Assertions.assertEquals(user, saved.getUser());
        Assertions.assertEquals("groq", saved.getProvider());
        Assertions.assertEquals(AiUsageEventType.CHAT, saved.getEventType());
        Assertions.assertEquals(120, saved.getTokensUsed());
        Assertions.assertEquals(BigDecimal.valueOf(0.002), saved.getCostEstimate());
    }

    @Test
    void recordShouldClampNegativeTokensToZero() {
        when(userRepository.getReferenceById(1L)).thenReturn(new User());

        service.record(1L, "groq", AiUsageEventType.CATEGORIZE, -5, null);

        ArgumentCaptor<AiUsageEvent> captor = ArgumentCaptor.forClass(AiUsageEvent.class);
        verify(usageEventRepository).save(captor.capture());
        Assertions.assertEquals(0, captor.getValue().getTokensUsed());
    }

    @Test
    void recordShouldNotThrowWhenPersistenceFails() {
        when(userRepository.getReferenceById(anyLong())).thenReturn(new User());
        doThrow(new RuntimeException("db down")).when(usageEventRepository).save(any(AiUsageEvent.class));

        Assertions.assertDoesNotThrow(() ->
                service.record(1L, "groq", AiUsageEventType.CHAT, 50, null)
        );
    }

    @Test
    void recordShouldNotThrowWhenUserLookupFails() {
        when(userRepository.getReferenceById(anyLong())).thenThrow(new RuntimeException("user not found"));

        Assertions.assertDoesNotThrow(() ->
                service.record(1L, "groq", AiUsageEventType.CHAT, 50, null)
        );
    }

    @Test
    void getUsageSummaryShouldAggregateTokensAndEventsByType() {
        YearMonth period = YearMonth.of(2026, 7);
        when(usageEventRepository.aggregateByEventType(eq(1L), any(), any())).thenReturn(List.of(
                aggregate(AiUsageEventType.CHAT, 3, 300),
                aggregate(AiUsageEventType.CATEGORIZE, 2, 20)
        ));

        AiUsageEventSummaryResponse summary = service.getUsageSummary(1L, period);

        Assertions.assertEquals(period, summary.period());
        Assertions.assertEquals(320, summary.totalTokens());
        Assertions.assertEquals(5, summary.totalEvents());
        Assertions.assertEquals(3L, summary.eventsByType().get(AiUsageEventType.CHAT));
        Assertions.assertEquals(2L, summary.eventsByType().get(AiUsageEventType.CATEGORIZE));
    }

    @Test
    void getUsageSummaryShouldReturnZeroedSummaryWhenNoEventsExist() {
        YearMonth period = YearMonth.of(2026, 7);
        when(usageEventRepository.aggregateByEventType(eq(1L), any(), any())).thenReturn(List.of());

        AiUsageEventSummaryResponse summary = service.getUsageSummary(1L, period);

        Assertions.assertEquals(0, summary.totalTokens());
        Assertions.assertEquals(0, summary.totalEvents());
        Assertions.assertTrue(summary.eventsByType().isEmpty());
    }

    @Test
    void getUsageSummaryWithoutUserIdResolvesCurrentAuthenticatedUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null)
        );
        YearMonth period = YearMonth.of(2026, 7);
        when(usageEventRepository.aggregateByEventType(eq(1L), any(), any())).thenReturn(List.of(
                aggregate(AiUsageEventType.CHAT, 1, 100)
        ));

        AiUsageEventSummaryResponse summary = service.getUsageSummary(period);

        Assertions.assertEquals(100, summary.totalTokens());
        Assertions.assertEquals(1, summary.totalEvents());
    }

    private static AiUsageEventRepository.AiUsageEventTypeAggregate aggregate(AiUsageEventType type, long count, long tokens) {
        return new AiUsageEventRepository.AiUsageEventTypeAggregate() {
            @Override
            public AiUsageEventType getEventType() {
                return type;
            }

            @Override
            public long getEventCount() {
                return count;
            }

            @Override
            public long getTotalTokens() {
                return tokens;
            }
        };
    }
}
