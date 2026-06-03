package fr.seynax.solvia.backend.api.flow;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import fr.seynax.solvia.backend.api.common.NotFoundException;
import fr.seynax.solvia.domain.model.CashFlow;
import fr.seynax.solvia.infrastructure.persistence.account.AccountJdbcRepository;
import fr.seynax.solvia.infrastructure.persistence.flow.CashFlowJdbcRepository;

@RestController
public class CashFlowController {

    private final CashFlowJdbcRepository cashFlowRepository;
    private final AccountJdbcRepository accountRepository;

    public CashFlowController(CashFlowJdbcRepository cashFlowRepository, AccountJdbcRepository accountRepository) {
        this.cashFlowRepository = cashFlowRepository;
        this.accountRepository = accountRepository;
    }

    @PostMapping("/api/cash-flows")
    public ResponseEntity<CashFlowResponse> create(@Valid @RequestBody CashFlowCreateRequest request) {
        requireAccount(request.accountId());
        CashFlow cashFlow = CashFlow.of(
                request.accountId(),
                request.type(),
                request.valueDate(),
                request.amount().toMoneyAmount(),
                request.label()
        );
        cashFlowRepository.save(cashFlow);
        return ResponseEntity.created(URI.create("/api/accounts/" + cashFlow.accountId() + "/cash-flows"))
                .body(CashFlowResponse.from(cashFlow));
    }

    @GetMapping("/api/accounts/{accountId}/cash-flows")
    public List<CashFlowResponse> findByAccountId(@PathVariable UUID accountId) {
        requireAccount(accountId);
        return cashFlowRepository.findByAccountId(accountId).stream()
                .map(CashFlowResponse::from)
                .toList();
    }

    private void requireAccount(UUID accountId) {
        if (accountRepository.findById(accountId).isEmpty()) {
            throw new NotFoundException("Account not found: " + accountId);
        }
    }
}
