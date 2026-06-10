package com.smartfinance.backend.service;

import com.smartfinance.backend.dto.transaction.TransactionRequest;
import com.smartfinance.backend.dto.transaction.TransactionResponse;
import com.smartfinance.backend.mapper.TransactionMapper;
import com.smartfinance.backend.model.Account;
import com.smartfinance.backend.model.Category;
import com.smartfinance.backend.model.CategoryType;
import com.smartfinance.backend.model.PaymentMethodType;
import com.smartfinance.backend.model.Transaction;
import com.smartfinance.backend.model.TransactionType;
import com.smartfinance.backend.model.User;
import com.smartfinance.backend.repository.AccountRepository;
import com.smartfinance.backend.repository.CategoryRepository;
import com.smartfinance.backend.repository.TransactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private TransactionService transactionService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createTransactionShouldSaveWithCurrentUserCategoryAndAccount() {
        setAuthenticatedUser(1L);
        TransactionRequest request = new TransactionRequest(
                5L,
                4L,
                TransactionType.EXPENSE,
                new BigDecimal("180.50"),
                "Supermercado",
                LocalDate.now(),
                PaymentMethodType.DEBIT_CARD,
                null,
                null,
                null,
                "Compra semanal"
        );
        Transaction mappedTransaction = new Transaction();
        mappedTransaction.setType(request.type());
        Category category = buildCategory(4L, CategoryType.EXPENSE, buildUser(1L), false);
        Account account = buildAccount(5L, buildUser(1L));
        Transaction savedTransaction = new Transaction(
                12L,
                buildUser(1L),
                account,
                category,
                TransactionType.EXPENSE,
                new BigDecimal("180.50"),
                "Supermercado",
                request.transactionDate(),
                PaymentMethodType.DEBIT_CARD,
                null,
                null,
                null,
                "Compra semanal",
                Instant.now(),
                Instant.now()
        );
        TransactionResponse response = new TransactionResponse(
                12L,
                5L,
                "Efectivo",
                4L,
                "Supermercado Cat",
                TransactionType.EXPENSE,
                new BigDecimal("180.50"),
                "Supermercado",
                request.transactionDate(),
                PaymentMethodType.DEBIT_CARD,
                null,
                null,
                null,
                "Compra semanal",
                savedTransaction.getCreatedAt(),
                savedTransaction.getUpdatedAt()
        );

        when(transactionMapper.toEntity(request)).thenReturn(mappedTransaction);
        when(categoryRepository.findAccessibleByIdAndUserId(4L, 1L)).thenReturn(Optional.of(category));
        when(accountRepository.findByIdAndUser_Id(5L, 1L)).thenReturn(Optional.of(account));
        when(transactionRepository.save(mappedTransaction)).thenReturn(savedTransaction);
        when(transactionMapper.toResponse(savedTransaction)).thenReturn(response);

        TransactionResponse createdTransaction = transactionService.createTransaction(request);

        Assertions.assertEquals(12L, createdTransaction.id());
        Assertions.assertEquals(1L, mappedTransaction.getUser().getId());
        Assertions.assertEquals(4L, mappedTransaction.getCategory().getId());
        Assertions.assertEquals(5L, mappedTransaction.getAccount().getId());
    }

    @Test
    void createTransactionShouldThrowWhenCategoryBelongsToAnotherUser() {
        setAuthenticatedUser(1L);
        TransactionRequest request = new TransactionRequest(
                null,
                7L,
                TransactionType.EXPENSE,
                new BigDecimal("55.00"),
                "Taxi",
                LocalDate.now(),
                PaymentMethodType.CASH,
                null,
                null,
                null,
                null
        );
        when(transactionMapper.toEntity(request)).thenReturn(new Transaction());
        when(categoryRepository.findAccessibleByIdAndUserId(7L, 1L)).thenReturn(Optional.empty());
        when(categoryRepository.existsById(7L)).thenReturn(true);

        Assertions.assertThrows(AccessDeniedException.class, () -> transactionService.createTransaction(request));
    }

    @Test
    void updateTransactionShouldThrowWhenUserIsNotOwner() {
        setAuthenticatedUser(1L);
        TransactionRequest request = new TransactionRequest(
                null,
                null,
                TransactionType.EXPENSE,
                new BigDecimal("20.00"),
                "Pasaje",
                LocalDate.now(),
                PaymentMethodType.CASH,
                null,
                null,
                null,
                null
        );
        Transaction transaction = new Transaction();
        transaction.setId(88L);
        transaction.setUser(buildUser(2L));
        when(transactionRepository.findById(88L)).thenReturn(Optional.of(transaction));

        Assertions.assertThrows(AccessDeniedException.class, () -> transactionService.updateTransaction(88L, request));
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

    private Category buildCategory(Long categoryId, CategoryType type, User user, boolean isSystem) {
        Category category = new Category();
        category.setId(categoryId);
        category.setType(type);
        category.setUser(user);
        category.setSystem(isSystem);
        return category;
    }

    private Account buildAccount(Long accountId, User user) {
        Account account = new Account();
        account.setId(accountId);
        account.setName("Efectivo");
        account.setUser(user);
        return account;
    }
}
