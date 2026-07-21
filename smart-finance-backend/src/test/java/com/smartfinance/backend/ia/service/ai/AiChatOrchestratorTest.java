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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    private static final ResolvedAiProvider NVIDIA = new ResolvedAiProvider(
            "nvidia", "https://integrate.api.nvidia.com/v1", "sk-nvidia", "meta/llama-3.1-70b-instruct", "nvidia/nemotron-nano-12b-v2-vl");
    private static final ResolvedAiProvider OPENCODE = new ResolvedAiProvider(
            "opencode", "https://opencode.ai/zen/v1", "sk-opencode", "big-pickle", null);
    /**
     * {@code completeVision} nunca llama a {@link AiChatClient#complete} con {@link #NVIDIA} tal
     * cual: reemplaza {@code model} por {@code visionModel} antes de llamar, precisamente para no
     * repetir el bug real encontrado hoy (una llamada de visión terminó usando el modelo de texto
     * configurado, que no entiende {@code image_url} y rechazó la petición). Este es el provider
     * "swapeado" que realmente cruza a {@link AiChatClient}.
     */
    private static final ResolvedAiProvider NVIDIA_VISION = new ResolvedAiProvider(
            "nvidia", "https://integrate.api.nvidia.com/v1", "sk-nvidia", "nvidia/nemotron-nano-12b-v2-vl", "nvidia/nemotron-nano-12b-v2-vl");

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

    @Test
    void completeVisionShouldOnlyCallNvidiaEvenWhenOtherProvidersAreConfiguredWithHigherPriority() {
        when(registry.enabledInPriorityOrder()).thenReturn(List.of(OPENCODE, NVIDIA));
        when(aiChatClient.complete(eq(NVIDIA_VISION), anyList()))
                .thenReturn(new ChatCompletionResult("{\"isReceipt\":true}", null, null, 10, 5));

        List<ChatMessage> messages = List.of(
                ChatMessage.system("contexto"),
                ChatMessage.userWithImage("Extraé los datos.", "data:image/jpeg;base64,abc")
        );
        ChatCompletionResult result = orchestrator.completeVision(messages);

        Assertions.assertEquals("nvidia", result.providerName());
        Assertions.assertEquals("nvidia/nemotron-nano-12b-v2-vl", result.model());
        verify(aiChatClient, never()).complete(eq(OPENCODE), anyList());
    }

    @Test
    void completeVisionShouldNeverSendNvidiasTextModelForAVisionTurn() {
        when(registry.enabledInPriorityOrder()).thenReturn(List.of(NVIDIA));
        when(aiChatClient.complete(eq(NVIDIA_VISION), anyList()))
                .thenReturn(new ChatCompletionResult("{\"isReceipt\":true}", null, null, 10, 5));

        orchestrator.completeVision(List.of(ChatMessage.userWithImage("Extraé los datos.", "data:image/jpeg;base64,abc")));

        // El bug real encontrado hoy: completeVision terminaba llamando con el ResolvedAiProvider
        // de texto (model = "meta/llama-3.1-70b-instruct"), que NVIDIA rechaza al no entender
        // image_url. Esta aserción falla si esa regresión vuelve a aparecer.
        verify(aiChatClient, never()).complete(eq(NVIDIA), anyList());
    }

    @Test
    void completeVisionShouldThrowAiProviderNotConfiguredExceptionAndNeverCallClientWhenNvidiaIsNotEnabled() {
        when(registry.enabledInPriorityOrder()).thenReturn(List.of(OPENCODE));

        List<ChatMessage> messages = List.of(ChatMessage.userWithImage("Extraé los datos.", "data:image/jpeg;base64,abc"));
        Assertions.assertThrows(AiProviderNotConfiguredException.class, () -> orchestrator.completeVision(messages));
        verifyNoInteractions(aiChatClient);
    }

    @Test
    void completeVisionShouldThrowAiProviderNotConfiguredExceptionWhenNvidiaHasNoVisionModelConfigured() {
        ResolvedAiProvider nvidiaWithoutVision = new ResolvedAiProvider(
                "nvidia", "https://integrate.api.nvidia.com/v1", "sk-nvidia", "meta/llama-3.1-70b-instruct", null);
        when(registry.enabledInPriorityOrder()).thenReturn(List.of(nvidiaWithoutVision));

        List<ChatMessage> messages = List.of(ChatMessage.userWithImage("Extraé los datos.", "data:image/jpeg;base64,abc"));
        Assertions.assertThrows(AiProviderNotConfiguredException.class, () -> orchestrator.completeVision(messages));
        verifyNoInteractions(aiChatClient);
    }

    @Test
    void completeVisionShouldPropagateNvidiaFailureWithoutTryingAnyOtherProvider() {
        when(registry.enabledInPriorityOrder()).thenReturn(List.of(NVIDIA, OPENCODE));
        when(aiChatClient.complete(eq(NVIDIA_VISION), anyList())).thenThrow(new AiProviderUnavailableException("nvidia"));

        List<ChatMessage> messages = List.of(ChatMessage.userWithImage("Extraé los datos.", "data:image/jpeg;base64,abc"));
        Assertions.assertThrows(AiProviderUnavailableException.class, () -> orchestrator.completeVision(messages));
        verify(aiChatClient, never()).complete(eq(OPENCODE), anyList());
    }
}
