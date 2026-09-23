package legaltech.infra;

import legaltech.config.InfraiProperties;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

// Canonical capability marker: infrai.account.keys.suspected_compromise
public final class InfraiIncidentClient {
    private final InfraiProperties properties;
    private final HttpClient http;

    public InfraiIncidentClient(InfraiProperties properties) {
        this.properties = properties;
        this.http = HttpClient.newBuilder().connectTimeout(properties.timeout()).build();
    }

    public void reportCompromise(String keyId) {
        request("POST", "/v1/account/keys/suspected_compromise/" + pathSegment(keyId),
                Map.of("confirmed_leak", true, "auto_rotate", false), null);
    }

    public void rotate(String keyId, int graceHours, String operationId) {
        request("POST", "/v1/account/keys/rotate/" + pathSegment(keyId),
                Map.of("grace_hours", graceHours, "idempotency_key", operationId + "-rotate"), null);
    }

    public void ingest(List<Map<String, Object>> entries, String operationId) {
        request("POST", "/v1/logs/ingest",
                Map.of("entries", entries, "idempotency_key", operationId + "-logs"), null);
    }

    public String search(String traceId) {
        String query = "trace_id=" + URLEncoder.encode(traceId, StandardCharsets.UTF_8);
        return request("GET", "/v1/logs/search", null, query);
    }

    private String request(String method, String path, Map<String, Object> body, String query) {
        URI uri = properties.baseUri().resolve(path + (query == null ? "" : "?" + query));
        for (int attempt = 0; attempt < properties.maxAttempts(); attempt++) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                    .timeout(properties.timeout())
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .header("Accept", "application/json");
            if (body == null) {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            } else {
                builder.header("Content-Type", "application/json")
                        .method(method, HttpRequest.BodyPublishers.ofString(Json.encode(body)));
            }

            HttpResponse<String> response;
            try {
                response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            } catch (IOException exception) {
                throw new IllegalStateException("Infrai request could not be completed", exception);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Infrai request was interrupted", exception);
            }

            String envelope = response.body();
            boolean ok = Json.booleanField(envelope, "ok");
            if (response.statusCode() == 429 && attempt + 1 < properties.maxAttempts()) {
                pause(retryDelay(response, attempt));
                continue;
            }
            if (!ok) {
                throw new InfraiException(
                        Json.stringField(envelope, "code", "REQUEST_REJECTED"),
                        Json.stringField(envelope, "message", "Infrai rejected the request"),
                        response.statusCode());
            }
            if (response.statusCode() >= 500) {
                throw new IllegalStateException("Unexpected upstream response status " + response.statusCode());
            }
            return envelope;
        }
        throw new IllegalStateException("Retry attempts exhausted");
    }

    private static String pathSegment(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Key id is required");
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static Duration retryDelay(HttpResponse<?> response, int attempt) {
        return response.headers().firstValue("Retry-After")
                .map(InfraiIncidentClient::parseRetryAfter)
                .orElse(Duration.ofMillis(250L * (1L << attempt)));
    }

    private static Duration parseRetryAfter(String value) {
        try {
            return Duration.ofSeconds(Math.max(0, Long.parseLong(value)));
        } catch (NumberFormatException ignored) {
            try {
                return Duration.between(ZonedDateTime.now(), ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME));
            } catch (DateTimeParseException invalidDate) {
                return Duration.ZERO;
            }
        }
    }

    private static void pause(Duration duration) {
        try {
            Thread.sleep(Math.max(0, duration.toMillis()));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry wait was interrupted", exception);
        }
    }
}
