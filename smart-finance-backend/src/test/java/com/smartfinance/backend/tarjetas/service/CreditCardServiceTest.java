package com.smartfinance.backend.tarjetas.service;

import com.smartfinance.backend.common.exception.ResourceNotFoundException;
import com.smartfinance.backend.tarjetas.mapper.CreditCardMapper;
import com.smartfinance.backend.tarjetas.model.dto.CreditCardRequest;
import com.smartfinance.backend.tarjetas.model.dto.CreditCardResponse;
import com.smartfinance.backend.tarjetas.model.dto.CreditCardUpdateRequest;
import com.smartfinance.backend.tarjetas.model.entity.CardFranchise;
import com.smartfinance.backend.tarjetas.model.entity.CreditCard;
import com.smartfinance.backend.tarjetas.repository.CreditCardRepository;
import com.smartfinance.backend.usuario.model.entity.User;
import com.smartfinance.backend.usuario.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditCardServiceTest {

    @Mock
    private CreditCardRepository creditCardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CreditCardMapper creditCardMapper;

    @InjectMocks
    private CreditCardService creditCardService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createCardShouldSeedCurrentBalanceToZero() {
        setAuthenticatedUser(1L);
        CreditCardRequest request = new CreditCardRequest(
                "Tarjeta Visa", "Bancolombia", CardFranchise.VISA,
                BigDecimal.valueOf(1_000_000), BigDecimal.valueOf(0.025), 5, 20
        );
        CreditCard mappedCard = new CreditCard();
        CreditCard savedCard = new CreditCard();
        savedCard.setId(10L);
        CreditCardResponse response = new CreditCardResponse(
                10L, "Tarjeta Visa", "Bancolombia", CardFranchise.VISA,
                BigDecimal.valueOf(1_000_000), BigDecimal.valueOf(0.025), 5, 20,
                BigDecimal.ZERO, BigDecimal.valueOf(1_000_000), null, null, null
        );

        when(creditCardMapper.toEntity(request)).thenReturn(mappedCard);
        when(userRepository.getReferenceById(1L)).thenReturn(buildUser(1L));
        when(creditCardRepository.save(mappedCard)).thenReturn(savedCard);
        when(creditCardMapper.toResponse(savedCard)).thenReturn(response);

        CreditCardResponse createdCard = creditCardService.createCard(request);

        Assertions.assertEquals(10L, createdCard.id());
        Assertions.assertEquals(BigDecimal.ZERO, mappedCard.getCurrentBalance());
        Assertions.assertEquals(1L, mappedCard.getUser().getId());
    }

    @Test
    void updateCardShouldNotTouchFranchiseCreditLimitOrBalance() {
        setAuthenticatedUser(1L);
        CreditCard card = buildCard(20L, 1L, CardFranchise.VISA, BigDecimal.valueOf(1_000_000), BigDecimal.valueOf(200_000));
        CreditCardUpdateRequest request = new CreditCardUpdateRequest("Tarjeta Visa Gold", "Davivienda", BigDecimal.valueOf(0.03), 10, 25);

        when(creditCardRepository.findByIdAndUser_Id(20L, 1L)).thenReturn(Optional.of(card));
        when(creditCardRepository.save(card)).thenReturn(card);
        when(creditCardMapper.toResponse(card)).thenReturn(
                new CreditCardResponse(20L, "Tarjeta Visa Gold", "Davivienda", CardFranchise.VISA,
                        BigDecimal.valueOf(1_000_000), BigDecimal.valueOf(0.03), 10, 25,
                        BigDecimal.valueOf(200_000), BigDecimal.valueOf(800_000), null, null, null)
        );

        CreditCardResponse updatedCard = creditCardService.updateCard(20L, request);

        Assertions.assertEquals(BigDecimal.valueOf(200_000), updatedCard.currentBalance());
        Assertions.assertEquals(BigDecimal.valueOf(1_000_000), card.getCreditLimit());
        Assertions.assertEquals(CardFranchise.VISA, card.getFranchise());
    }

    @Test
    void updateCardShouldThrowNotFoundWhenCardBelongsToAnotherUser() {
        setAuthenticatedUser(1L);
        CreditCardUpdateRequest request = new CreditCardUpdateRequest("Otro nombre", null, BigDecimal.valueOf(0.02), 5, 20);
        when(creditCardRepository.findByIdAndUser_Id(99L, 1L)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> creditCardService.updateCard(99L, request));
    }

    @Test
    void deleteCardShouldDeleteWhenOwnedByCurrentUser() {
        setAuthenticatedUser(3L);
        CreditCard card = buildCard(55L, 3L, CardFranchise.MASTERCARD, BigDecimal.valueOf(500_000), BigDecimal.ZERO);
        when(creditCardRepository.findByIdAndUser_Id(55L, 3L)).thenReturn(Optional.of(card));

        creditCardService.deleteCard(55L);

        verify(creditCardRepository).delete(card);
    }

    @Test
    void deleteCardShouldThrowNotFoundWhenOwnedByAnotherUser() {
        setAuthenticatedUser(3L);
        when(creditCardRepository.findByIdAndUser_Id(55L, 3L)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> creditCardService.deleteCard(55L));
    }

    @Test
    void getCardShouldReturnCardWhenOwnedByCurrentUser() {
        setAuthenticatedUser(1L);
        CreditCard card = buildCard(10L, 1L, CardFranchise.AMEX, BigDecimal.valueOf(2_000_000), BigDecimal.valueOf(300_000));
        CreditCardResponse response = new CreditCardResponse(10L, "Tarjeta Amex", "Bancolombia", CardFranchise.AMEX,
                BigDecimal.valueOf(2_000_000), BigDecimal.valueOf(0.025), 5, 20,
                BigDecimal.valueOf(300_000), BigDecimal.valueOf(1_700_000), null, null, null);

        when(creditCardRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(card));
        when(creditCardMapper.toResponse(card)).thenReturn(response);

        CreditCardResponse result = creditCardService.getCard(10L);

        Assertions.assertEquals(response, result);
    }

    @Test
    void getCardShouldThrowNotFoundWhenCardBelongsToAnotherUser() {
        setAuthenticatedUser(1L);
        when(creditCardRepository.findByIdAndUser_Id(99L, 1L)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> creditCardService.getCard(99L));
    }

    @Test
    void getCardsShouldReturnPagedCardsForCurrentUser() {
        setAuthenticatedUser(1L);
        Pageable pageable = PageRequest.of(0, 20);
        CreditCard card = new CreditCard();
        Page<CreditCard> page = new PageImpl<>(List.of(card), pageable, 1);
        CreditCardResponse response = new CreditCardResponse(1L, "Tarjeta", null, CardFranchise.VISA,
                BigDecimal.TEN, BigDecimal.ZERO, 1, 15, BigDecimal.ZERO, BigDecimal.TEN, null, null, null);

        when(creditCardRepository.findAllByUser_Id(1L, pageable)).thenReturn(page);
        when(creditCardMapper.toResponse(card)).thenReturn(response);

        Page<CreditCardResponse> result = creditCardService.getCards(pageable);

        Assertions.assertEquals(1, result.getTotalElements());
    }

    private void setAuthenticatedUser(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null)
        );
    }

    private User buildUser(Long userId) {
        User user = new User();
        user.setId(userId);
        return user;
    }

    private CreditCard buildCard(Long cardId, Long userId, CardFranchise franchise, BigDecimal creditLimit, BigDecimal currentBalance) {
        CreditCard card = new CreditCard();
        card.setId(cardId);
        card.setUser(buildUser(userId));
        card.setName("Tarjeta Visa");
        card.setFranchise(franchise);
        card.setCreditLimit(creditLimit);
        card.setMonthlyRate(BigDecimal.valueOf(0.025));
        card.setCutoffDay(5);
        card.setPaymentDueDay(20);
        card.setCurrentBalance(currentBalance);
        return card;
    }
}
