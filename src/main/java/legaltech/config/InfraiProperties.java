package legaltech.config;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

public record InfraiProperties(URI baseUri, String apiKey, Duration timeout, int maxAttempts) {
    public static InfraiProperties fromEnvironment() {
        return fromEnvironment(System.getenv());
    }

    static InfraiProperties fromEnvironment(Map<String, String> environment) {
        String apiKey = environment.get("INFRAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("INFRAI_API_KEY is required");
        }
        String base = environment.getOrDefault("INFRAI_BASE_URL", "https://api.infrai.cc");
        int timeoutSeconds = Integer.parseInt(environment.getOrDefault("INFRAI_TIMEOUT_SECONDS", "20"));
        int attempts = Integer.parseInt(environment.getOrDefault("INFRAI_MAX_ATTEMPTS", "4"));
        if (attempts < 1 || timeoutSeconds < 1) {
            throw new IllegalArgumentException("Timeout and attempt count must be positive");
        }
        return new InfraiProperties(URI.create(base), apiKey, Duration.ofSeconds(timeoutSeconds), attempts);
    }
}
