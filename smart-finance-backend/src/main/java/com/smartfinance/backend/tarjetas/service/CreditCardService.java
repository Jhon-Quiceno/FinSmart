package com.smartfinance.backend.tarjetas.service;

import com.smartfinance.backend.common.exception.ResourceNotFoundException;
import com.smartfinance.backend.common.security.SecurityUtils;
import com.smartfinance.backend.tarjetas.mapper.CreditCardMapper;
import com.smartfinance.backend.tarjetas.model.dto.CreditCardRequest;
import com.smartfinance.backend.tarjetas.model.dto.CreditCardResponse;
import com.smartfinance.backend.tarjetas.model.dto.CreditCardUpdateRequest;
import com.smartfinance.backend.tarjetas.model.entity.CreditCard;
import com.smartfinance.backend.tarjetas.repository.CreditCardRepository;
import com.smartfinance.backend.usuario.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Business logic for managing the current user's {@link CreditCard} records.
 *
 * <p>Every operation resolves the caller via {@link SecurityUtils#getCurrentUserId()} and
 * scopes reads/writes strictly to that user. Mutations on a card owned by another user raise
 * {@link ResourceNotFoundException} (HTTP 404) rather than a 403, so card existence is never
 * leaked to a non-owner — mirrors {@code DebtService}.
 *
 * <p>{@link #updateCard} deliberately cannot change {@code franchise}, {@code creditLimit} or
 * {@code currentBalance} — see {@link CreditCardUpdateRequest}'s Javadoc. The only place
 * {@code currentBalance} changes after creation is the ledger-driven atomic updates added in
 * Fase B.2.
 */
@Service
public class CreditCardService {

    private final CreditCardRepository creditCardRepository;
    private final UserRepository userRepository;
    private final CreditCardMapper creditCardMapper;

    public CreditCardService(
            CreditCardRepository creditCardRepository,
            UserRepository userRepository,
            CreditCardMapper creditCardMapper
    ) {
        this.creditCardRepository = creditCardRepository;
        this.userRepository = userRepository;
        this.creditCardMapper = creditCardMapper;
    }

    @Transactional(readOnly = true)
    public Page<CreditCardResponse> getCards(Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId();
        return creditCardRepository.findAllByUser_Id(userId, pageable)
                .map(creditCardMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CreditCardResponse getCard(Long cardId) {
        Long userId = SecurityUtils.getCurrentUserId();
        CreditCard card = findOwnedCard(cardId, userId);
        return creditCardMapper.toResponse(card);
    }

    @Transactional
    public CreditCardResponse createCard(CreditCardRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        CreditCard card = creditCardMapper.toEntity(request);
        card.setUser(userRepository.getReferenceById(userId));
        card.setCurrentBalance(BigDecimal.ZERO);

        CreditCard savedCard = creditCardRepository.save(card);
        return creditCardMapper.toResponse(savedCard);
    }

    @Transactional
    public CreditCardResponse updateCard(Long cardId, CreditCardUpdateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        CreditCard card = findOwnedCard(cardId, userId);

        creditCardMapper.updateEntityFromRequest(request, card);

        CreditCard updatedCard = creditCardRepository.save(card);
        return creditCardMapper.toResponse(updatedCard);
    }

    @Transactional
    public void deleteCard(Long cardId) {
        Long userId = SecurityUtils.getCurrentUserId();
        CreditCard card = findOwnedCard(cardId, userId);
        creditCardRepository.delete(card);
    }

    private CreditCard findOwnedCard(Long cardId, Long userId) {
        return creditCardRepository.findByIdAndUser_Id(cardId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarjeta no encontrada"));
    }
}
