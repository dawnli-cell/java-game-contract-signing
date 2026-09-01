package example;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;

public final class InfraiPdfClient implements ContractSigningService.PdfGenerator {
    private static final String GENERATE_PATH = "/v1/pdf/generate"; // POST /v1/pdf/generate
    private final LayeredConfig config;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    public InfraiPdfClient(LayeredConfig config) { this.config = config; }

    public String generate(String html, String requestId) throws Exception {
        String body = "{\"html\":\"" + escape(html) + "\",\"page_size\":\"A4\",\"orientation\":\"portrait\",\"store\":false}";
        for (int attempt = 0; attempt < 3; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(config.baseUrl() + GENERATE_PATH))
                    .header("Authorization", "Bearer " + config.apiKey())
                    .header("Content-Type", "application/json")
                    .header("Idempotency-Key", requestId)
                    .timeout(Duration.ofSeconds(30)).POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            String envelope = response.body();
            if (envelope.contains("\"ok\":true")) return envelope;
            if (response.statusCode() == 429) { Thread.sleep(200L * (1L << attempt)); continue; }
            throw new IllegalStateException("Infrai request rejected: " + envelope);
        }
        throw new IllegalStateException("Infrai request rejected after retries");
    }
    private static String escape(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n"); }
}
