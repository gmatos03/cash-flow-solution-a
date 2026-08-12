package com.cashflow.ledger.query.service;

import com.cashflow.ledger.query.cache.BalanceCacheService;
import com.cashflow.ledger.query.entity.AccountEntity;
import com.cashflow.ledger.query.repository.AccountRepository;
import com.cashflow.ledger.query.web.BalanceResponse;
import org.springframework.stereotype.Service;

import java.util.Optional;

/** Backs {@code GET /queries/accounts/{accountId}/balance} (Appendix F.3). */
@Service
public class BalanceQueryService {

    private final AccountRepository accountRepository;
    private final BalanceCacheService balanceCacheService;

    public BalanceQueryService(AccountRepository accountRepository, BalanceCacheService balanceCacheService) {
        this.accountRepository = accountRepository;
        this.balanceCacheService = balanceCacheService;
    }

    public BalanceResponse getBalance(String accountId) {
        Optional<BalanceResponse> cached = balanceCacheService.get(accountId);
        if (cached.isPresent()) {
            return cached.get().withSource("cache");
        }

        AccountEntity account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        BalanceResponse response = new BalanceResponse(
                account.getAccountId(),
                account.getCurrentBalance(),
                account.getCurrency(),
                account.getUpdatedAt(),
                "db"
        );
        balanceCacheService.put(accountId, response);
        return response;
    }
}
