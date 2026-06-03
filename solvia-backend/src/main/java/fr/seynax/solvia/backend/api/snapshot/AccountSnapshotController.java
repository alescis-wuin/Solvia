package fr.seynax.solvia.backend.api.snapshot;

import java.net.URI;
import java.time.Instant;
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
import fr.seynax.solvia.domain.model.AccountBalanceSnapshot;
import fr.seynax.solvia.domain.model.ValuationConfidence;
import fr.seynax.solvia.infrastructure.persistence.account.AccountJdbcRepository;
import fr.seynax.solvia.infrastructure.persistence.account.AccountSnapshotJdbcRepository;

@RestController
public class AccountSnapshotController {

    private final AccountSnapshotJdbcRepository snapshotRepository;
    private final AccountJdbcRepository accountRepository;

    public AccountSnapshotController(AccountSnapshotJdbcRepository snapshotRepository, AccountJdbcRepository accountRepository) {
        this.snapshotRepository = snapshotRepository;
        this.accountRepository = accountRepository;
    }

    @PostMapping("/api/account-snapshots")
    public ResponseEntity<AccountSnapshotResponse> create(@Valid @RequestBody AccountSnapshotCreateRequest request) {
        requireAccount(request.accountId());
        AccountBalanceSnapshot snapshot = new AccountBalanceSnapshot(
                UUID.randomUUID(),
                request.accountId(),
                request.valueDate(),
                request.balance().toMoneyAmount(),
                request.confidence() == null ? ValuationConfidence.OBSERVED : request.confidence(),
                request.note(),
                Instant.now()
        );
        snapshotRepository.save(snapshot);
        return ResponseEntity.created(URI.create("/api/accounts/" + snapshot.accountId() + "/snapshots"))
                .body(AccountSnapshotResponse.from(snapshot));
    }

    @GetMapping("/api/accounts/{accountId}/snapshots")
    public List<AccountSnapshotResponse> findByAccountId(@PathVariable UUID accountId) {
        requireAccount(accountId);
        return snapshotRepository.findByAccountId(accountId).stream()
                .map(AccountSnapshotResponse::from)
                .toList();
    }

    private void requireAccount(UUID accountId) {
        if (accountRepository.findById(accountId).isEmpty()) {
            throw new NotFoundException("Account not found: " + accountId);
        }
    }
}
