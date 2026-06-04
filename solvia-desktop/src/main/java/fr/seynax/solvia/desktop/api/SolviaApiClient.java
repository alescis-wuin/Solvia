package fr.seynax.solvia.desktop.api;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import fr.seynax.solvia.desktop.api.ApiDtos.AccountCreateDto;
import fr.seynax.solvia.desktop.api.ApiDtos.AccountDto;
import fr.seynax.solvia.desktop.api.ApiDtos.AccountSnapshotCreateDto;
import fr.seynax.solvia.desktop.api.ApiDtos.AssetCreateDto;
import fr.seynax.solvia.desktop.api.ApiDtos.AssetDto;
import fr.seynax.solvia.desktop.api.ApiDtos.AssetUpdateDto;
import fr.seynax.solvia.desktop.api.ApiDtos.CashFlowCreateDto;
import fr.seynax.solvia.desktop.api.ApiDtos.NetWorthDto;
import fr.seynax.solvia.desktop.api.ApiDtos.PerformanceDto;
import fr.seynax.solvia.desktop.api.ApiDtos.PositionCreateDto;
import fr.seynax.solvia.desktop.api.ApiDtos.PositionDto;
import fr.seynax.solvia.desktop.api.ApiDtos.PositionSnapshotCreateDto;
import fr.seynax.solvia.desktop.api.ApiDtos.PositionSnapshotDto;
import fr.seynax.solvia.desktop.api.ApiDtos.PositionUpdateDto;
import fr.seynax.solvia.desktop.api.ApiDtos.ReadinessDto;
import fr.seynax.solvia.desktop.api.ApiDtos.SeriesPointDto;

public final class SolviaApiClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(4);

    private volatile URI baseUri;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SolviaApiClient(URI baseUri) {
        this.baseUri = normalizeBaseUri(baseUri);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public URI baseUri() {
        return baseUri;
    }

    public void updateBaseUri(URI baseUri) {
        this.baseUri = normalizeBaseUri(baseUri);
    }

    public CompletableFuture<BackendStatusSnapshot> readiness() {
        URI currentBaseUri = baseUri;
        return get("/api/readiness", ReadinessDto.class)
                .thenApply(readiness -> BackendStatusSnapshot.from(currentBaseUri.toString(), readiness))
                .exceptionally(error -> BackendStatusSnapshot.unavailable(currentBaseUri.toString(), userMessage(error)));
    }

    public CompletableFuture<List<AccountDto>> accounts() {
        return get("/api/accounts", new TypeReference<List<AccountDto>>() {
        });
    }

    public CompletableFuture<AccountDto> createAccount(AccountCreateDto request) {
        return post("/api/accounts", request, AccountDto.class);
    }

    public CompletableFuture<List<AssetDto>> assets() {
        return get("/api/assets", new TypeReference<List<AssetDto>>() {
        });
    }

    public CompletableFuture<AssetDto> createAsset(AssetCreateDto request) {
        return post("/api/assets", request, AssetDto.class);
    }

    public CompletableFuture<AssetDto> updateAsset(UUID assetId, AssetUpdateDto request) {
        return put("/api/assets/" + assetId, request, AssetDto.class);
    }

    public CompletableFuture<Void> deactivateAsset(UUID assetId) {
        return delete("/api/assets/" + assetId);
    }

    public CompletableFuture<List<PositionDto>> positions(UUID accountId) {
        return get("/api/accounts/" + accountId + "/positions", new TypeReference<List<PositionDto>>() {
        });
    }

    public CompletableFuture<PositionDto> createPosition(PositionCreateDto request) {
        return post("/api/positions", request, PositionDto.class);
    }

    public CompletableFuture<PositionDto> updatePosition(UUID positionId, PositionUpdateDto request) {
        return put("/api/positions/" + positionId, request, PositionDto.class);
    }

    public CompletableFuture<Void> deactivatePosition(UUID positionId) {
        return delete("/api/positions/" + positionId);
    }

    public CompletableFuture<Void> createAccountSnapshot(AccountSnapshotCreateDto request) {
        return post("/api/account-snapshots", request, Object.class).thenApply(ignored -> null);
    }

    public CompletableFuture<Void> createPositionSnapshot(PositionSnapshotCreateDto request) {
        return post("/api/position-snapshots", request, Object.class).thenApply(ignored -> null);
    }

    public CompletableFuture<List<PositionSnapshotDto>> positionSnapshots(UUID positionId) {
        return get("/api/positions/" + positionId + "/snapshots", new TypeReference<List<PositionSnapshotDto>>() {
        });
    }

    public CompletableFuture<Void> createCashFlow(CashFlowCreateDto request) {
        return post("/api/cash-flows", request, Object.class).thenApply(ignored -> null);
    }

    public CompletableFuture<NetWorthDto> netWorth(LocalDate date, String currency) {
        return get("/api/net-worth?date=" + date + "&currency=" + encode(currency), NetWorthDto.class);
    }

    public CompletableFuture<List<SeriesPointDto>> netWorthSeries(LocalDate from, LocalDate to, String bucket, String aggregation, String currency) {
        String path = "/api/net-worth/series?from=" + from
                + "&to=" + to
                + "&bucket=" + encode(bucket)
                + "&aggregation=" + encode(aggregation)
                + "&currency=" + encode(currency);
        return get(path, new TypeReference<List<SeriesPointDto>>() {
        });
    }

    public CompletableFuture<PerformanceDto> performance(LocalDate from, LocalDate to, String currency) {
        return get("/api/performance?from=" + from + "&to=" + to + "&currency=" + encode(currency), PerformanceDto.class);
    }

    public String userMessage(Throwable throwable) {
        Throwable root = rootCause(throwable);
        if (root instanceof SolviaApiException apiException) {
            return apiException.userMessage();
        }
        if (root instanceof HttpTimeoutException) {
            return "Le backend ne répond pas dans le délai attendu.";
        }
        if (root instanceof ConnectException) {
            return "Connexion impossible au backend local. Vérifier qu’il est démarré.";
        }
        String message = root.getMessage();
        if (message == null || message.isBlank()) {
            return "Erreur de communication avec le backend.";
        }
        return message;
    }

    private <T> CompletableFuture<T> get(String path, Class<T> responseType) {
        HttpRequest request = request(path).GET().build();
        return send(request).thenApply(body -> read(body, responseType));
    }

    private <T> CompletableFuture<T> get(String path, TypeReference<T> responseType) {
        HttpRequest request = request(path).GET().build();
        return send(request).thenApply(body -> read(body, responseType));
    }

    private <T> CompletableFuture<T> post(String path, Object payload, Class<T> responseType) {
        HttpRequest request = request(path)
                .POST(HttpRequest.BodyPublishers.ofString(write(payload)))
                .header("Content-Type", "application/json")
                .build();
        return send(request).thenApply(body -> read(body, responseType));
    }

    private <T> CompletableFuture<T> put(String path, Object payload, Class<T> responseType) {
        HttpRequest request = request(path)
                .PUT(HttpRequest.BodyPublishers.ofString(write(payload)))
                .header("Content-Type", "application/json")
                .build();
        return send(request).thenApply(body -> read(body, responseType));
    }

    private CompletableFuture<Void> delete(String path) {
        HttpRequest request = request(path).DELETE().build();
        return send(request).thenApply(ignored -> null);
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(resolve(path))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json");
    }

    private CompletableFuture<String> send(HttpRequest request) {
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        throw SolviaApiException.from(response.statusCode(), response.body(), objectMapper);
                    }
                    return response.body();
                });
    }

    private URI resolve(String path) {
        return baseUri.resolve(path);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String write(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to serialize request", exception);
        }
    }

    private <T> T read(String body, Class<T> responseType) {
        try {
            return objectMapper.readValue(body, responseType);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read backend response", exception);
        }
    }

    private <T> T read(String body, TypeReference<T> responseType) {
        try {
            return objectMapper.readValue(body, responseType);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read backend response", exception);
        }
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static URI normalizeBaseUri(URI uri) {
        if (uri == null || uri.getScheme() == null || uri.getHost() == null) {
            throw new IllegalArgumentException("Backend URL must be an absolute HTTP URL");
        }
        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Backend URL must use HTTP or HTTPS");
        }
        return uri;
    }
}
