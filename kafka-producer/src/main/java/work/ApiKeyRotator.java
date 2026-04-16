package work;

import api.ImdbApiClient;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiKeyRotator {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyRotator.class);
    private static final String TEST_URL = "https://imdb8.p.rapidapi.com/actors/get-all-news?nconst=nm0001667";

    public void switchAPI() {
        String[] keys = ImdbApiClient.API_KEYS;
        for (int i = 0; i < keys.length; i++) {
            try {
                if (tryKey(i)) return;
            } catch (UnirestException e) {
                log.warn("Key index {} failed: {}", i, e.getMessage());
            }
        }
        log.error("No valid API key found.");
    }

    private synchronized boolean tryKey(int i) throws UnirestException {
        HttpResponse<JsonNode> response = Unirest.get(TEST_URL)
                .header("x-rapidapi-host", ImdbApiClient.API_HOST)
                .header("x-rapidapi-key", ImdbApiClient.API_KEYS[i])
                .asJson();
        if (!response.getBody().getArray().getJSONObject(0).has("message")) {
            ImdbApiClient.API_KEY = ImdbApiClient.API_KEYS[i];
            ImdbApiClient.index_api = i;
            log.info("Valid API key found at index {}", i);
            return true;
        }
        return false;
    }
}