package com.smartfinance.backend.integraciones.service;

import com.smartfinance.backend.integraciones.exception.InvalidLinkCodeException;
import com.smartfinance.backend.integraciones.exception.TelegramAlreadyLinkedException;
import com.smartfinance.backend.integraciones.model.dto.TelegramLinkCodeResponse;
import com.smartfinance.backend.integraciones.model.dto.TelegramLinkStatusResponse;
import com.smartfinance.backend.integraciones.model.entity.TelegramLink;
import com.smartfinance.backend.integraciones.repository.TelegramLinkRepository;
import com.smartfinance.backend.usuario.model.entity.User;
import com.smartfinance.backend.usuario.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramLinkServiceTest {

    @Mock
    private TelegramLinkCodeStore codeStore;

    @Mock
    private TelegramLinkRepository telegramLinkRepository;

    @Mock
    private UserRepository userRepository;

    private TelegramLinkService telegramLinkService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private TelegramLinkService service() {
        return new TelegramLinkService(codeStore, telegramLinkRepository, userRepository);
    }

    @Test
    void generateLinkCodeIssuesACodeForTheCurrentAuthenticatedUser() {
        setAuthenticatedUser(1L);
        when(codeStore.issue(1L)).thenReturn("ABCD2345");
        telegramLinkService = service();

        TelegramLinkCodeResponse response = telegramLinkService.generateLinkCode();

        assertThat(response.code()).isEqualTo("ABCD2345");
        assertThat(response.expiresInSeconds()).isEqualTo(TelegramLinkCodeStore.TTL_SECONDS);
    }

    @Test
    void generateLinkCodeThrowsWhenTheUserAlreadyHasAChatLinkedAndNeverIssuesACode() {
        setAuthenticatedUser(1L);
        when(telegramLinkRepository.existsByUser_Id(1L)).thenReturn(true);
        telegramLinkService = service();

        Assertions.assertThrows(
                TelegramAlreadyLinkedException.class,
                () -> telegramLinkService.generateLinkCode()
        );
        verify(codeStore, org.mockito.Mockito.never()).issue(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void getStatusReturnsLinkedTrueWhenTheUserHasAChatLinked() {
        setAuthenticatedUser(1L);
        when(telegramLinkRepository.existsByUser_Id(1L)).thenReturn(true);
        telegramLinkService = service();

        TelegramLinkStatusResponse status = telegramLinkService.getStatus();

        assertThat(status.linked()).isTrue();
    }

    @Test
    void getStatusReturnsLinkedFalseWhenTheUserHasNoChatLinked() {
        setAuthenticatedUser(1L);
        when(telegramLinkRepository.existsByUser_Id(1L)).thenReturn(false);
        telegramLinkService = service();

        TelegramLinkStatusResponse status = telegramLinkService.getStatus();

        assertThat(status.linked()).isFalse();
    }

    @Test
    void confirmLinkThrowsWhenCodeIsInvalidOrExpired() {
        when(codeStore.consume("BADCODE1")).thenReturn(Optional.empty());
        telegramLinkService = service();

        Assertions.assertThrows(
                InvalidLinkCodeException.class,
                () -> telegramLinkService.confirmLink("BADCODE1", "chat-1")
        );
    }

    @Test
    void confirmLinkInsertsANewLinkWhenTheChatWasNeverLinked() {
        when(codeStore.consume("GOODCODE")).thenReturn(Optional.of(7L));
        when(telegramLinkRepository.findByTelegramChatId("chat-1")).thenReturn(Optional.empty());
        User user = new User();
        user.setId(7L);
        when(userRepository.getReferenceById(7L)).thenReturn(user);
        telegramLinkService = service();

        telegramLinkService.confirmLink("GOODCODE", "chat-1");

        ArgumentCaptor<TelegramLink> captor = ArgumentCaptor.forClass(TelegramLink.class);
        verify(telegramLinkRepository).save(captor.capture());
        assertThat(captor.getValue().getTelegramChatId()).isEqualTo("chat-1");
        assertThat(captor.getValue().getUser().getId()).isEqualTo(7L);
    }

    @Test
    void confirmLinkReassignsAnExistingChatToTheNewUserInsteadOfInsertingANewRow() {
        when(codeStore.consume("GOODCODE")).thenReturn(Optional.of(9L));
        TelegramLink existingLink = new TelegramLink();
        existingLink.setId(3L);
        existingLink.setTelegramChatId("chat-1");
        User previousUser = new User();
        previousUser.setId(1L);
        existingLink.setUser(previousUser);
        when(telegramLinkRepository.findByTelegramChatId("chat-1")).thenReturn(Optional.of(existingLink));
        User newUser = new User();
        newUser.setId(9L);
        when(userRepository.getReferenceById(9L)).thenReturn(newUser);
        telegramLinkService = service();

        telegramLinkService.confirmLink("GOODCODE", "chat-1");

        ArgumentCaptor<TelegramLink> captor = ArgumentCaptor.forClass(TelegramLink.class);
        verify(telegramLinkRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(3L);
        assertThat(captor.getValue().getUser().getId()).isEqualTo(9L);
    }

    private void setAuthenticatedUser(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null)
        );
    }
}
