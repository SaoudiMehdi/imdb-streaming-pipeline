package work;

import api.ImdbApiClient;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.News;

import java.util.ArrayList;

public class NewsGetter {

    private static final Logger log = LoggerFactory.getLogger(NewsGetter.class);
    private static final String BASE_URL = "https://imdb8.p.rapidapi.com/actors/get-all-news?nconst=";

    public ArrayList<News> getNews(String actorId) throws UnirestException {
        ArrayList<News> newsList = new ArrayList<>();

        if (ImdbApiClient.API_KEY.isEmpty()) {
            new ApiKeyRotator().switchAPI();
        }
        if (ImdbApiClient.API_KEY.isEmpty()) {
            log.error("No valid API key available.");
            return newsList;
        }

        String url = BASE_URL + actorId;
        HttpResponse<JsonNode> response = Unirest.get(url)
                .header("x-rapidapi-host", ImdbApiClient.API_HOST)
                .header("x-rapidapi-key", ImdbApiClient.API_KEY)
                .asJson();

        if (response.getBody().getArray().getJSONObject(0).has("message")) {
            new ApiKeyRotator().switchAPI();
            response = Unirest.get(url)
                    .header("x-rapidapi-host", ImdbApiClient.API_HOST)
                    .header("x-rapidapi-key", ImdbApiClient.API_KEY)
                    .asJson();
        }

        fillInNews(response, newsList);
        return newsList;
    }

    private void fillInNews(HttpResponse<JsonNode> response, ArrayList<News> newsList) {
        try {
            JSONArray data = response.getBody().getArray().getJSONObject(0).getJSONArray("data");
            int limit = Math.min(9, data.length());
            for (int i = 0; i < limit; i++) {
                JSONObject item = data.getJSONObject(i);
                News news = new News(
                        item.getString("body"),
                        item.getString("head"),
                        item.getString("id"),
                        item.getString("link"),
                        item.getString("publishTime"),
                        null
                );
                newsList.add(news);
            }
        } catch (Exception e) {
            log.error("Failed to parse news response: {}", e.getMessage());
        }
    }
}