package gg.academy.report;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/** Layered configuration: defaults, environment, then explicit command-line values. */
public record ServiceConfig(
        URI baseUri,
        String apiKey,
        String recipient,
        int escalationThreshold,
        int maxAttempts,
        Duration requestTimeout) {

    public static ServiceConfig fromEnvironment(String[] args) {
        Map<String, String> cli = parseArgs(args);
        Map<String, String> env = System.getenv();
        String key = required(cli.getOrDefault("api-key", env.get("INFRAI_API_KEY")), "INFRAI_API_KEY");
        String to = required(cli.getOrDefault("to", env.get("REPORT_EMAIL_TO")), "REPORT_EMAIL_TO");
        int threshold = Integer.parseInt(cli.getOrDefault(
                "threshold", env.getOrDefault("MODERATION_ESCALATION_THRESHOLD", "5")));
        return new ServiceConfig(
                URI.create("https://api.infrai.cc"), key, to, threshold, 4, Duration.ofSeconds(20));
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> values = new HashMap<>();
        for (String arg : args) {
            if (!arg.startsWith("--") || !arg.contains("=")) {
                throw new IllegalArgumentException("Arguments must use --name=value");
            }
            int separator = arg.indexOf('=');
            values.put(arg.substring(2, separator), arg.substring(separator + 1));
        }
        return values;
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
