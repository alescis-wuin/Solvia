package fr.seynax.solvia.backend.api.position;

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
import fr.seynax.solvia.domain.model.Position;
import fr.seynax.solvia.infrastructure.persistence.account.AccountJdbcRepository;
import fr.seynax.solvia.infrastructure.persistence.asset.AssetJdbcRepository;
import fr.seynax.solvia.infrastructure.persistence.position.PositionJdbcRepository;

@RestController
public class PositionController {

    private final PositionJdbcRepository positionRepository;
    private final AccountJdbcRepository accountRepository;
    private final AssetJdbcRepository assetRepository;

    public PositionController(
            PositionJdbcRepository positionRepository,
            AccountJdbcRepository accountRepository,
            AssetJdbcRepository assetRepository
    ) {
        this.positionRepository = positionRepository;
        this.accountRepository = accountRepository;
        this.assetRepository = assetRepository;
    }

    @GetMapping("/api/accounts/{accountId}/positions")
    public List<PositionResponse> findByAccountId(@PathVariable UUID accountId) {
        requireAccount(accountId);
        return positionRepository.findByAccountId(accountId).stream()
                .map(PositionResponse::from)
                .toList();
    }

    @PostMapping("/api/positions")
    public ResponseEntity<PositionResponse> create(@Valid @RequestBody PositionCreateRequest request) {
        requireAccount(request.accountId());
        requireAsset(request.assetId());

        Position position = Position.create(request.accountId(), request.assetId(), request.quantity());
        positionRepository.save(position);
        return ResponseEntity.created(URI.create("/api/positions/" + position.id()))
                .body(PositionResponse.from(position));
    }

    @GetMapping("/api/positions/{id}")
    public PositionResponse findById(@PathVariable UUID id) {
        return positionRepository.findById(id)
                .map(PositionResponse::from)
                .orElseThrow(() -> new NotFoundException("Position not found: " + id));
    }

    @PutMapping("/api/positions/{id}")
    public PositionResponse update(@PathVariable UUID id, @Valid @RequestBody PositionUpdateRequest request) {
        Position existing = positionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Position not found: " + id));
        Position updated = new Position(
                existing.id(),
                existing.accountId(),
                existing.assetId(),
                request.quantity(),
                request.active(),
                existing.createdAt()
        );
        positionRepository.save(updated);
        return PositionResponse.from(updated);
    }

    @DeleteMapping("/api/positions/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        Position existing = positionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Position not found: " + id));
        positionRepository.save(existing.deactivate());
        return ResponseEntity.noContent().build();
    }

    private void requireAccount(UUID accountId) {
        if (accountRepository.findById(accountId).isEmpty()) {
            throw new NotFoundException("Account not found: " + accountId);
        }
    }

    private void requireAsset(UUID assetId) {
        if (assetRepository.findById(assetId).isEmpty()) {
            throw new NotFoundException("Asset not found: " + assetId);
        }
    }
}
