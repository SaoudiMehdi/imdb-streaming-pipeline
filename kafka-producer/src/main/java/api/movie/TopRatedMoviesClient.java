package api.movie;

import api.ImdbApiClient;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.opencsv.CSVWriter;
import org.json.JSONArray;
import util.Movie;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TopRatedMoviesClient {

    private static final String BASE_URL      = "https://imdb8.p.rapidapi.com/title/get-top-rated-movies";
    private static final String CSV_FILE_PATH = "src/main/resources/movie/topRatedMovies.csv";

    private final JSONArray dataArray;
    private int count = 0;

    public TopRatedMoviesClient() {
        HttpResponse<JsonNode> response = ImdbApiClient.getResponseApi(BASE_URL);
        dataArray = response.getBody().getArray();
    }

    public Movie getNextMovie() {
        if (count < dataArray.length()) {
            String id = ((String) dataArray.getJSONObject(count++).get("id")).split("/")[2];
            return new MovieMetadataClient(id).getMovie();
        }
        return null;
    }

    public String getNextMovieId() {
        if (count < dataArray.length()) {
            return ((String) dataArray.getJSONObject(count++).get("id")).split("/")[2];
        }
        return null;
    }

    public void saveTopRatedMovies() {
        try (CSVWriter writer = new CSVWriter(new FileWriter(new File(CSV_FILE_PATH)))) {
            List<String[]> data = new ArrayList<>();
            data.add(new String[]{"Ranking", "Movie_id"});
            String id;
            while ((id = getNextMovieId()) != null) {
                data.add(new String[]{String.valueOf(count), id});
            }
            writer.writeAll(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}