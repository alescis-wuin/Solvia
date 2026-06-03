package fr.seynax.solvia.desktop.api;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import fr.seynax.solvia.desktop.api.ApiDtos.AccountCreateDto;
import fr.seynax.solvia.desktop.api.ApiDtos.AccountDto;
import fr.seynax.solvia.desktop.api.ApiDtos.AccountSnapshotCreateDto;
import fr.seynax.solvia.desktop.api.ApiDtos.CashFlowCreateDto;
import fr.seynax.solvia.desktop.api.ApiDtos.NetWorthDto;
import fr.seynax.solvia.desktop.api.ApiDtos.PerformanceDto;
import fr.seynax.solvia.desktop.api.ApiDtos.SeriesPointDto;

public final class SolviaApiClient {

    private final URI baseUri;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SolviaApiClient(URI baseUri) {
        this.baseUri = baseUri;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public CompletableFuture<List<AccountDto>> accounts() {
        return get("/api/accounts", new TypeReference<>() {
        });
    }

    public CompletableFuture<AccountDto> createAccount(AccountCreateDto request) {
        return post("/api/accounts", request, AccountDto.class);
    }

    public CompletableFuture<Void> createAccountSnapshot(AccountSnapshotCreateDto request) {
        return post("/api/account-snapshots", request, Object.class).thenApply(ignored -> null);
    }

    public CompletableFuture<Void> createCashFlow(CashFlowCreateDto request) {
        return post("/api/cash-flows", request, Object.class).thenApply(ignored -> null);
    }

    public CompletableFuture<NetWorthDto> netWorth(LocalDate date, String currency) {
        return get("/api/net-worth?date=" + date + "&currency=" + encode(currency), NetWorthDto.class);
    }

    public CompletableFuture<List<SeriesPointDto>> netWorthSeries(
            LocalDate from,
            LocalDate to,
            String bucket,
            String aggregation,
            String currency
    ) {
        String path = "/api/net-worth/series?from=" + from
                + "&to=" + to
                + "&bucket=" + encode(bucket)
                + "&aggregation=" + encode(aggregation)
                + "&currency=" + encode(currency);
        return get(path, new TypeReference<>() {
        });
    }

    public CompletableFuture<PerformanceDto> performance(LocalDate from, LocalDate to, String currency) {
        return get("/api/performance?from=" + from + "&to=" + to + "&currency=" + encode(currency), PerformanceDto.class);
    }

    private <T> CompletableFuture<T> get(String path, Class<T> responseType) {
        HttpRequest request = HttpRequest.newBuilder(resolve(path))
                .GET()
                .header("Accept", "application/json")
                .build();
        return send(request).thenApply(body -> read(body, responseType));
    }

    private <T> CompletableFuture<T> get(String path, TypeReference<T> responseType) {
        HttpRequest request = HttpRequest.newBuilder(resolve(path))
                .GET()
                .header("Accept", "application/json")
                .build();
        return send(request).thenApply(body -> read(body, responseType));
    }

    private <T> CompletableFuture<T> post(String path, Object payload, Class<T> responseType) {
        HttpRequest request = HttpRequest.newBuilder(resolve(path))
                .POST(HttpRequest.BodyPublishers.ofString(write(payload)))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .build();
        return send(request).thenApply(body -> read(body, responseType));
    }

    private CompletableFuture<String> send(HttpRequest request) {
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        throw new IllegalStateException("Backend returned HTTP " + response.statusCode() + ": " + response.body());
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
}
