package api.movie;

import api.ImdbApiClient;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import org.json.JSONArray;

public class PopularGenresClient {

    private static final String BASE_URL = "https://imdb8.p.rapidapi.com/title/list-popular-genres";

    private final JSONArray dataArray;
    private int count = 0;

    public PopularGenresClient() {
        HttpResponse<JsonNode> response = ImdbApiClient.getResponseApi(BASE_URL);
        dataArray = (JSONArray) response.getBody().getArray().getJSONObject(0).get("genres");
    }

    public String getNextGenre() {
        if (count < dataArray.length()) {
            return (String) dataArray.getJSONObject(count++).get("description");
        }
        return null;
    }
}