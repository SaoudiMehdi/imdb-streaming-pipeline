package api.movie;

import api.ImdbApiClient;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import org.json.JSONObject;

public class MovieRatingClient {

    private static final String BASE_URL = "https://imdb8.p.rapidapi.com/title/get-ratings?tconst=";
    private final JSONObject data;

    public MovieRatingClient(String movieId) {
        HttpResponse<JsonNode> response = ImdbApiClient.getResponseApi(BASE_URL + movieId);
        data = response.getBody().getObject();
    }

    public double getRating() {
        if (data.getBoolean("canRate"))
            return data.getDouble("rating");
        return 0;
    }
}