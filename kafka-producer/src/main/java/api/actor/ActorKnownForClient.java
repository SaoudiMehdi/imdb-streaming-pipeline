package api.actor;

import api.ImdbApiClient;
import api.movie.MovieRatingClient;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import util.Movie;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ActorKnownForClient {

    private final JSONArray dataArray;
    private static final String BASE_URL              = "https://imdb8.p.rapidapi.com/actors/get-known-for?nconst=";
    private static final String MOST_POPULAR_CB_PATH  = "src/main/resources/actor/mostPopularCelebs.csv";
    private static final String ALL_KNOWN_FOR_PATH    = "src/main/resources/movie_actor/actorKnownFor.csv";

    private int count = 0;
    private final String actorId;

    public ActorKnownForClient(String actorId) {
        this.actorId = actorId;
        HttpResponse<JsonNode> response = ImdbApiClient.getResponseApi(BASE_URL + actorId);
        dataArray = (JSONArray) response.getBody().getArray();
    }

    public Movie getNextMovie() {
        if (count < dataArray.length()) {
            JSONObject movieEntry = dataArray.getJSONObject(count++);
            String id        = movieEntry.getString("id").split("/")[2];
            String titleType = movieEntry.getString("titleType");
            String title     = movieEntry.getString("title");
            double rating    = new MovieRatingClient(id).getRating();
            return new Movie(id, rating, titleType, title);
        }
        return null;
    }

    public String getNextMovieId() {
        while (count < dataArray.length()) {
            JSONObject movieEntry = dataArray.getJSONObject(count++);
            String movieId = movieEntry.getJSONObject("title").getString("id").split("/")[2];
            try {
                String titleType = movieEntry.getJSONObject("title").getString("titleType");
                if (!titleType.toLowerCase().contains("movie")) continue;

                String category = "none";
                if (movieEntry.has("summary") && movieEntry.getJSONObject("summary").has("category")) {
                    category = movieEntry.getJSONObject("summary").getString("category");
                } else if (movieEntry.has("categories")) {
                    JSONArray categories = (JSONArray) movieEntry.get("categories");
                    for (int i = 0; i < categories.length(); i++) {
                        category = (String) categories.get(i);
                        if (category.contains("actor") || category.contains("actress")) break;
                    }
                }

                if (category.contains("actor") || category.contains("actress")) {
                    return movieId;
                }
            } catch (JSONException | ClassCastException e) {
                System.err.println("Skipping movie " + movieId + ": " + e.getMessage());
            }
        }
        return null;
    }

    public static void saveAllKnownFor() {
        try (CSVReader csvReader = new CSVReader(new FileReader(MOST_POPULAR_CB_PATH));
             CSVWriter writer = new CSVWriter(new FileWriter(new File(ALL_KNOWN_FOR_PATH), true))) {

            List<String[]> allData = csvReader.readAll();
            List<String[]> data = new ArrayList<>();
            if (!new File(ALL_KNOWN_FOR_PATH).exists()) {
                data.add(new String[]{"Actor_id", "Movie_id"});
            }

            for (int i = 1; i < allData.size(); i++) {
                String actorId = allData.get(i)[0];
                ActorKnownForClient client = new ActorKnownForClient(actorId);
                String movieId;
                while ((movieId = client.getNextMovieId()) != null) {
                    data.add(new String[]{actorId, movieId});
                }
            }
            writer.writeAll(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}