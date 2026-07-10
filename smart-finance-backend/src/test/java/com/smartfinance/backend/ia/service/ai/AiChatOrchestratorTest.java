package com.smartfinance.backend.ia.service.ai;

import com.smartfinance.backend.ia.exception.AiProviderNotConfiguredException;
import com.smartfinance.backend.ia.exception.AiProviderUnavailableException;
import com.smartfinance.backend.ia.exception.AiProvidersExhaustedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiChatOrchestratorTest {

    @Mock
    private AiProviderRegistry registry;

    @Mock
    private AiChatClient aiChatClient;

    @InjectMocks
    private AiChatOrchestrator orchestrator;

    private static final ResolvedAiProvider NVIDIA = new ResolvedAiProvider("nvidia", "https://integrate.api.nvidia.com/v1", "sk-nvidia", "meta/llama-3.1-70b-instruct");
    private static final ResolvedAiProvider OPENCODE = new ResolvedAiProvider("opencode", "https://opencode.ai/zen/v1", "sk-opencode", "big-pickle");

    @Test
    void completeShouldReturnSecondProvidersResultWhenFirstProviderFails() {
        when(registry.enabledInPriorityOrder()).thenReturn(List.of(NVIDIA, OPENCODE));
        when(aiChatClient.complete(eq(NVIDIA), anyList())).thenThrow(new AiProviderUnavailableException("nvidia"));
        when(aiChatClient.complete(eq(OPENCODE), anyList()))
                .thenReturn(new ChatCompletionResult("Hola desde opencode", null, null, null, null));

        ChatCompletionResult result = orchestrator.complete(List.of(ChatMessage.user("hola")));

        Assertions.assertEquals("Hola desde opencode", result.content());
        Assertions.assertEquals("opencode", result.providerName());
        Assertions.assertEquals("big-pickle", result.model());
    }

    @Test
    void completeShouldThrowAiProvidersExhaustedExceptionWhenEveryConfiguredProviderFails() {
        when(registry.enabledInPriorityOrder()).thenReturn(List.of(NVIDIA, OPENCODE));
        when(aiChatClient.complete(eq(NVIDIA), anyList())).thenThrow(new AiProviderUnavailableException("nvidia"));
        when(aiChatClient.complete(eq(OPENCODE), anyList())).thenThrow(new AiProviderUnavailableException("opencode"));

        List<ChatMessage> messages = List.of(ChatMessage.user("hola"));
        Assertions.assertThrows(AiProvidersExhaustedException.class, () -> orchestrator.complete(messages));
    }

    @Test
    void completeShouldThrowAiProviderNotConfiguredExceptionAndNeverCallClientWhenNoProviderIsEnabled() {
        when(registry.enabledInPriorityOrder()).thenReturn(List.of());

        List<ChatMessage> messages = List.of(ChatMessage.user("hola"));
        Assertions.assertThrows(AiProviderNotConfiguredException.class, () -> orchestrator.complete(messages));
        verifyNoInteractions(aiChatClient);
    }
}
