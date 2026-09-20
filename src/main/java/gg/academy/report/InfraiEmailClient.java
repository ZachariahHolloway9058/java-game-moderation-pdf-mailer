package gg.academy.report;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Small reusable module for infrai.email.send over plain REST. */
public final class InfraiEmailClient {
    private static final Pattern OK = Pattern.compile("\\\"ok\\\"\\s*:\\s*(true|false)");
    private static final Pattern MESSAGE_ID = Pattern.compile("\\\"message_id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern ERROR_CODE = Pattern.compile("\\\"code\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern ERROR_MESSAGE = Pattern.compile("\\\"(?:message|hint)\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    private final HttpClient http;
    private final ServiceConfig config;

    private InfraiEmailClient(HttpClient http, ServiceConfig config) {
        this.http = http;
        this.config = config;
    }

    public static InfraiEmailClient create(ServiceConfig config) {
        return new InfraiEmailClient(HttpClient.newBuilder()
                .connectTimeout(config.requestTimeout()).build(), config);
    }

    public String send(String to, String subject, String html, String idempotencyKey) throws Exception {
        String body = "{\"to\":\"" + jsonEscape(to) + "\",\"subject\":\"" + jsonEscape(subject)
                + "\",\"html\":\"" + jsonEscape(html) + "\"}";
        for (int attempt = 1; attempt <= config.maxAttempts(); attempt++) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(config.baseUri().resolve("/v1/email/send"))
                    .timeout(config.requestTimeout())
                    .header("Authorization", "Bearer " + config.apiKey())
                    .header("Content-Type", "application/json")
                    .header("Idempotency-Key", idempotencyKey)
                    .method("POST", HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            Envelope envelope = decodeEnvelope(response.body(), response.statusCode());
            if (response.statusCode() == 429) {
                if (attempt == config.maxAttempts()) {
                    throw new InfraiException(envelope.code(), envelope.message(), response.statusCode());
                }
                sleepBeforeRetry(response, attempt);
                continue;
            }
            if (response.statusCode() >= 500) {
                if (attempt == config.maxAttempts()) throw new InfraiException("HTTP_ERROR", "request rejected", response.statusCode());
                sleepBeforeRetry(response, attempt);
                continue;
            }
            if (!envelope.ok()) throw new InfraiException(envelope.code(), envelope.message(), response.statusCode());
            Matcher message = MESSAGE_ID.matcher(response.body());
            if (!message.find()) throw new InfraiException("INVALID_RESPONSE", "message_id is missing", response.statusCode());
            return message.group(1);
        }
        throw new IllegalStateException("retry loop exhausted");
    }

    private Envelope decodeEnvelope(String json, int status) {
        Matcher ok = OK.matcher(json);
        if (!ok.find()) throw new InfraiException("INVALID_RESPONSE", "response envelope is missing", status);
        if (Boolean.parseBoolean(ok.group(1))) return new Envelope(true, "", "");
        return new Envelope(false, matchOr(ERROR_CODE, json, "REQUEST_REJECTED"),
                matchOr(ERROR_MESSAGE, json, "request rejected"));
    }

    private void sleepBeforeRetry(HttpResponse<?> response, int attempt) throws InterruptedException {
        long fallback = Math.min(8, 1L << (attempt - 1));
        long seconds = response.headers().firstValue("Retry-After")
                .map(value -> parseRetryAfter(value, fallback)).orElse(fallback);
        Thread.sleep(Duration.ofSeconds(seconds).toMillis());
    }

    private long parseRetryAfter(String value, long fallback) {
        try { return Math.max(0, Long.parseLong(value)); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private static String matchOr(Pattern pattern, String input, String fallback) {
        Matcher matcher = pattern.matcher(input);
        return matcher.find() ? matcher.group(1) : fallback;
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    private record Envelope(boolean ok, String code, String message) {}
}

final class InfraiException extends RuntimeException {
    private final String code;
    private final int status;

    InfraiException(String code, String message, int status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    String code() { return code; }
    int status() { return status; }
}
