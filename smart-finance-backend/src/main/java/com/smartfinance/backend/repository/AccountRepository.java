package com.smartfinance.backend.repository;

import com.smartfinance.backend.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByIdAndUser_Id(Long accountId, Long userId);
}
