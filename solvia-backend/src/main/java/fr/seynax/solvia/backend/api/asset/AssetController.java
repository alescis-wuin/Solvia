package fr.seynax.solvia.backend.api.asset;

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
import fr.seynax.solvia.domain.model.Asset;
import fr.seynax.solvia.domain.money.CurrencyCode;
import fr.seynax.solvia.infrastructure.persistence.asset.AssetJdbcRepository;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetJdbcRepository assetRepository;

    public AssetController(AssetJdbcRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    @GetMapping
    public List<AssetResponse> findAll() {
        return assetRepository.findAll().stream()
                .map(AssetResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<AssetResponse> create(@Valid @RequestBody AssetCreateRequest request) {
        Asset asset = Asset.create(
                request.name(),
                request.type(),
                CurrencyCode.of(request.currencyCode()),
                request.symbol()
        );
        assetRepository.save(asset);
        return ResponseEntity.created(URI.create("/api/assets/" + asset.id()))
                .body(AssetResponse.from(asset));
    }

    @GetMapping("/{id}")
    public AssetResponse findById(@PathVariable UUID id) {
        return assetRepository.findById(id)
                .map(AssetResponse::from)
                .orElseThrow(() -> new NotFoundException("Asset not found: " + id));
    }

    @PutMapping("/{id}")
    public AssetResponse update(@PathVariable UUID id, @Valid @RequestBody AssetUpdateRequest request) {
        Asset existing = assetRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Asset not found: " + id));
        Asset updated = new Asset(
                existing.id(),
                request.name(),
                request.type(),
                CurrencyCode.of(request.currencyCode()),
                request.symbol(),
                request.active(),
                existing.createdAt()
        );
        assetRepository.save(updated);
        return AssetResponse.from(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        Asset existing = assetRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Asset not found: " + id));
        Asset deactivated = new Asset(
                existing.id(),
                existing.name(),
                existing.type(),
                existing.currency(),
                existing.symbol(),
                false,
                existing.createdAt()
        );
        assetRepository.save(deactivated);
        return ResponseEntity.noContent().build();
    }
}
