package com.smartfinance.backend.integraciones.service;

import com.smartfinance.backend.common.security.SecurityUtils;
import com.smartfinance.backend.integraciones.exception.InvalidLinkCodeException;
import com.smartfinance.backend.integraciones.model.dto.TelegramLinkCodeResponse;
import com.smartfinance.backend.integraciones.model.entity.TelegramLink;
import com.smartfinance.backend.integraciones.repository.TelegramLinkRepository;
import com.smartfinance.backend.usuario.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lógica de negocio para vincular un chat de Telegram con el usuario actualmente autenticado.
 *
 * <p>El flujo tiene dos pasos, cada uno disparado por un actor distinto: {@link #generateLinkCode}
 * lo llama la app (con JWT, endpoint protegido) y devuelve un código de un solo uso; luego el
 * usuario le envía ese código al bot, y n8n llama a {@link #confirmLink} (endpoint protegido por
 * {@code TelegramWebhookFilter}, sin JWT) para completar el vínculo.
 */
@Service
public class TelegramLinkService {

    private final TelegramLinkCodeStore codeStore;
    private final TelegramLinkRepository telegramLinkRepository;
    private final UserRepository userRepository;

    public TelegramLinkService(
            TelegramLinkCodeStore codeStore,
            TelegramLinkRepository telegramLinkRepository,
            UserRepository userRepository
    ) {
        this.codeStore = codeStore;
        this.telegramLinkRepository = telegramLinkRepository;
        this.userRepository = userRepository;
    }

    /**
     * Genera un código de un solo uso para que el usuario autenticado vincule su cuenta de
     * Telegram.
     *
     * @return el código generado y su tiempo de vida en segundos
     */
    public TelegramLinkCodeResponse generateLinkCode() {
        Long userId = SecurityUtils.getCurrentUserId();
        String code = codeStore.issue(userId);
        return new TelegramLinkCodeResponse(code, TelegramLinkCodeStore.TTL_SECONDS);
    }

    /**
     * Confirma el vínculo entre {@code chatId} y el usuario dueño de {@code code}. Si el chat ya
     * estaba vinculado a otro usuario, ese vínculo se re-asigna (no se crea una fila nueva),
     * porque {@code telegram_chat_id} es único.
     *
     * @param code   código de un solo uso emitido por {@link #generateLinkCode}
     * @param chatId identificador del chat de Telegram a vincular
     * @throws InvalidLinkCodeException si el código no existe, ya fue usado, o expiró
     */
    @Transactional
    public void confirmLink(String code, String chatId) {
        Long userId = codeStore.consume(code)
                .orElseThrow(() -> new InvalidLinkCodeException(
                        "Código inválido o expirado. Genere uno nuevo desde la aplicación."));

        TelegramLink link = telegramLinkRepository.findByTelegramChatId(chatId)
                .orElseGet(() -> {
                    TelegramLink newLink = new TelegramLink();
                    newLink.setTelegramChatId(chatId);
                    return newLink;
                });
        link.setUser(userRepository.getReferenceById(userId));

        telegramLinkRepository.save(link);
    }
}
