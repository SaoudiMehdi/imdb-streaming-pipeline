package api.movie;

import api.ImdbApiClient;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import org.json.JSONArray;

public class ComingSoonTvShowsClient {

    private static final String BASE_URL = "https://imdb8.p.rapidapi.com/title/get-coming-soon-tv-shows?currentCountry=US";

    private final JSONArray dataArray;
    private int count = 0;

    public ComingSoonTvShowsClient() {
        HttpResponse<JsonNode> response = ImdbApiClient.getResponseApi(BASE_URL);
        dataArray = response.getBody().getArray();
    }

    public String getNextTvShowId() {
        if (count < dataArray.length()) {
            return ((String) dataArray.get(count++)).split("/")[2];
        }
        return null;
    }
}