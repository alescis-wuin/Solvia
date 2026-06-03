package fr.seynax.solvia.backend.api.asset;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import fr.seynax.solvia.domain.model.AssetType;

public record AssetCreateRequest(
        @NotBlank String name,
        @NotNull AssetType type,
        @NotBlank String currencyCode,
        String symbol
) {
}
