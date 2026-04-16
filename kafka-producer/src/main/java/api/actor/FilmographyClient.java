package api.actor;

import api.ImdbApiClient;
import api.movie.MovieRatingClient;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.json.JSONArray;
import org.json.JSONObject;
import util.Movie;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FilmographyClient {

    private final JSONArray dataArray;
    private static final String BASE_URL             = "https://imdb8.p.rapidapi.com/actors/get-all-filmography?nconst=";
    private static final String MOST_POPULAR_CB_PATH = "src/main/resources/actor/mostPopularCelebs.csv";
    private static final String ALL_FILMO_PATH       = "src/main/resources/movie_actor/allFilmography.csv";

    private int count = 0;
    private final String actorId;

    public FilmographyClient(String actorId) {
        this.actorId = actorId;
        HttpResponse<JsonNode> response = ImdbApiClient.getResponseApi(BASE_URL + actorId);
        dataArray = (JSONArray) response.getBody().getArray().getJSONObject(0).get("filmography");
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
        if (count < dataArray.length()) {
            JSONObject movieEntry = dataArray.getJSONObject(count++);
            return movieEntry.getString("id").split("/")[2];
        }
        return null;
    }

    public static void saveAllFilmography() {
        try (CSVReader csvReader = new CSVReader(new FileReader(MOST_POPULAR_CB_PATH));
             CSVWriter writer = new CSVWriter(new FileWriter(new File(ALL_FILMO_PATH), true))) {

            List<String[]> allData = csvReader.readAll();
            List<String[]> data = new ArrayList<>();
            if (!new File(ALL_FILMO_PATH).exists()) {
                data.add(new String[]{"Actor_id", "Movie_id"});
            }

            for (int i = 1; i < allData.size(); i++) {
                String actorId = allData.get(i)[0];
                FilmographyClient client = new FilmographyClient(actorId);
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