package api.movie;

import api.ImdbApiClient;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import org.json.JSONArray;
import util.Movie;

public class SimilarMoviesClient {

    private static final String BASE_URL = "https://imdb8.p.rapidapi.com/title/get-more-like-this?tconst=";

    private final JSONArray dataArray;
    private int count = 0;

    public SimilarMoviesClient(String movieId) {
        HttpResponse<JsonNode> response = ImdbApiClient.getResponseApi(BASE_URL + movieId);
        dataArray = response.getBody().getArray();
    }

    public Movie getNextMovie() {
        if (count < dataArray.length()) {
            String id = ((String) dataArray.get(count++)).split("/")[2];
            return new MovieMetadataClient(id).getMovie();
        }
        return null;
    }
}