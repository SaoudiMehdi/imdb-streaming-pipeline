package api;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.AppConfig;

import java.util.concurrent.TimeUnit;

public class ImdbApiClient {

    private static final Logger log = LoggerFactory.getLogger(ImdbApiClient.class);

    public static final String API_HOST = AppConfig.get("api.host", "imdb8.p.rapidapi.com");
    private static final int MAX_RETRIES = AppConfig.getInt("api.retry.max", 3);
    private static final int RETRY_SLEEP_SECONDS = AppConfig.getInt("api.retry.sleep.seconds", 2);

    // Loaded from env vars — never hardcode keys in source
    public static String[] API_KEYS = loadApiKeys();
    public static String API_KEY = API_KEYS.length > 0 ? API_KEYS[0] : "";
    public static int index_api = 0;

    private static String[] loadApiKeys() {
        String raw = System.getenv("IMDB_API_KEYS");
        if (raw != null && !raw.isEmpty()) {
            return raw.split(",");
        }
        String single = System.getenv("IMDB_API_KEY");
        if (single != null && !single.isEmpty()) {
            return new String[]{single};
        }
        log.warn("No API keys found. Set IMDB_API_KEYS (comma-separated) or IMDB_API_KEY env var.");
        return new String[0];
    }

    private static boolean isRateLimited(HttpResponse<JsonNode> response) {
        try {
            return response == null
                || (response.getBody().getArray().get(0) instanceof JSONObject
                    && response.getBody().getArray().getJSONObject(0).has("message"));
        } catch (Exception e) {
            return true;
        }
    }

    public static synchronized HttpResponse<JsonNode> getResponseApi(String url) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                HttpResponse<JsonNode> response = Unirest.get(url)
                        .header("x-rapidapi-host", API_HOST)
                        .header("x-rapidapi-key", API_KEY)
                        .asJson();

                if (!isRateLimited(response)) {
                    return response;
                }

                log.warn("Rate limited on key index {}. Switching API key.", index_api);
                HttpResponse<JsonNode> switched = switchApiKey(url);
                if (switched != null) return switched;

            } catch (UnirestException e) {
                log.error("HTTP error on attempt {}: {}", attempt + 1, e.getMessage());
            }

            try {
                TimeUnit.SECONDS.sleep(RETRY_SLEEP_SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        log.error("All {} retries exhausted for URL: {}", MAX_RETRIES, url);
        return null;
    }

    public static HttpResponse<JsonNode> switchApiKey(String url) {
        if (API_KEYS.length == 0) return null;
        for (int i = (index_api + 1) % API_KEYS.length; i != index_api; i = (i + 1) % API_KEYS.length) {
            try {
                HttpResponse<JsonNode> response = Unirest.get(url)
                        .header("x-rapidapi-host", API_HOST)
                        .header("x-rapidapi-key", API_KEYS[i])
                        .asJson();
                if (!isRateLimited(response)) {
                    API_KEY = API_KEYS[i];
                    index_api = i;
                    log.info("Switched to API key index {}", i);
                    return response;
                }
            } catch (UnirestException e) {
                log.warn("Key index {} also failed: {}", i, e.getMessage());
            }
        }
        return null;
    }
}