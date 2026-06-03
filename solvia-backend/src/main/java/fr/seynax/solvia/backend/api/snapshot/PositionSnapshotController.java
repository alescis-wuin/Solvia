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
import fr.seynax.solvia.domain.model.PositionSnapshot;
import fr.seynax.solvia.domain.model.ValuationConfidence;
import fr.seynax.solvia.infrastructure.persistence.position.PositionJdbcRepository;
import fr.seynax.solvia.infrastructure.persistence.position.PositionSnapshotJdbcRepository;

@RestController
public class PositionSnapshotController {

    private final PositionSnapshotJdbcRepository snapshotRepository;
    private final PositionJdbcRepository positionRepository;

    public PositionSnapshotController(PositionSnapshotJdbcRepository snapshotRepository, PositionJdbcRepository positionRepository) {
        this.snapshotRepository = snapshotRepository;
        this.positionRepository = positionRepository;
    }

    @PostMapping("/api/position-snapshots")
    public ResponseEntity<PositionSnapshotResponse> create(@Valid @RequestBody PositionSnapshotCreateRequest request) {
        requirePosition(request.positionId());
        PositionSnapshot snapshot = new PositionSnapshot(
                UUID.randomUUID(),
                request.positionId(),
                request.valueDate(),
                request.quantity(),
                request.marketValue().toMoneyAmount(),
                request.confidence() == null ? ValuationConfidence.OBSERVED : request.confidence(),
                Instant.now()
        );
        snapshotRepository.save(snapshot);
        return ResponseEntity.created(URI.create("/api/positions/" + snapshot.positionId() + "/snapshots"))
                .body(PositionSnapshotResponse.from(snapshot));
    }

    @GetMapping("/api/positions/{positionId}/snapshots")
    public List<PositionSnapshotResponse> findByPositionId(@PathVariable UUID positionId) {
        requirePosition(positionId);
        return snapshotRepository.findByPositionId(positionId).stream()
                .map(PositionSnapshotResponse::from)
                .toList();
    }

    private void requirePosition(UUID positionId) {
        if (positionRepository.findById(positionId).isEmpty()) {
            throw new NotFoundException("Position not found: " + positionId);
        }
    }
}
