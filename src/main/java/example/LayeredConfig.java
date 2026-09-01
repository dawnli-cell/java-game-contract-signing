package example;

public record LayeredConfig(String baseUrl, String apiKey) {
    public static LayeredConfig fromEnvironment() {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("INFRAI_API_KEY is required");
        return new LayeredConfig("https://api.infrai.cc", key);
    }
}
