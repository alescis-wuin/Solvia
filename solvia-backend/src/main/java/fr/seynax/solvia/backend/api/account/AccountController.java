package fr.seynax.solvia.backend.api.account;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.seynax.solvia.backend.api.common.NotFoundException;
import fr.seynax.solvia.domain.model.Account;
import fr.seynax.solvia.domain.money.CurrencyCode;
import fr.seynax.solvia.infrastructure.persistence.account.AccountJdbcRepository;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountJdbcRepository accountRepository;

    public AccountController(AccountJdbcRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @GetMapping
    public List<AccountResponse> findAll() {
        return accountRepository.findAll().stream()
                .map(AccountResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody AccountCreateRequest request) {
        Account account = Account.create(
                request.name(),
                request.type(),
                request.envelopeType(),
                CurrencyCode.of(request.currencyCode())
        );
        accountRepository.save(account);
        return ResponseEntity.created(URI.create("/api/accounts/" + account.id()))
                .body(AccountResponse.from(account));
    }

    @GetMapping("/{id}")
    public AccountResponse findById(@PathVariable UUID id) {
        return accountRepository.findById(id)
                .map(AccountResponse::from)
                .orElseThrow(() -> new NotFoundException("Account not found: " + id));
    }

    @PutMapping("/{id}")
    public AccountResponse update(@PathVariable UUID id, @Valid @RequestBody AccountUpdateRequest request) {
        Account existing = accountRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Account not found: " + id));
        Account updated = new Account(
                existing.id(),
                request.name(),
                request.type(),
                request.envelopeType(),
                CurrencyCode.of(request.currencyCode()),
                request.active(),
                existing.createdAt()
        );
        accountRepository.save(updated);
        return AccountResponse.from(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        Account existing = accountRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Account not found: " + id));
        accountRepository.save(existing.deactivate());
        return ResponseEntity.noContent().build();
    }
}
