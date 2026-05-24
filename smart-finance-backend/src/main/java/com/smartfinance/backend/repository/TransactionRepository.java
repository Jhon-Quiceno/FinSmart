package com.smartfinance.backend.repository;

import com.smartfinance.backend.model.Transaction;
import com.smartfinance.backend.model.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

	default Page<Transaction> findAllByFilters(
		Long userId,
		TransactionType type,
		Long categoryId,
		Long accountId,
		LocalDate fromDate,
		LocalDate toDate,
		Pageable pageable
	) {
		Specification<Transaction> spec = Specification.where((root, query, cb) ->
			cb.equal(root.get("user").get("id"), userId)
		);

		if (type != null) {
			spec = spec.and((root, query, cb) ->
				cb.equal(root.get("type"), type)
			);
		}

		if (categoryId != null && categoryId > 0) {
			spec = spec.and((root, query, cb) ->
				cb.equal(root.get("category").get("id"), categoryId)
			);
		}

		if (accountId != null && accountId > 0) {
			spec = spec.and((root, query, cb) ->
				cb.equal(root.get("account").get("id"), accountId)
			);
		}

		if (fromDate != null) {
			spec = spec.and((root, query, cb) ->
				cb.greaterThanOrEqualTo(root.get("transactionDate"), fromDate)
			);
		}

		if (toDate != null) {
			spec = spec.and((root, query, cb) ->
				cb.lessThanOrEqualTo(root.get("transactionDate"), toDate)
			);
		}

		return findAll(spec, pageable);
	}
}
