package api.movie;

import api.ImdbApiClient;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.json.JSONException;
import org.json.JSONObject;
import util.Movie;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MovieMetadataClient {

    private static final String BASE_URL          = "https://imdb8.p.rapidapi.com/title/get-meta-data?ids=";
    private static final String ALL_KNOWN_FOR_PATH  = "src/main/resources/movie_actor/actorKnownFor.csv";
    private static final String ALL_KNOWN_FOR_PATH2 = "src/main/resources/movie_actor/actorKnownFor_v2.csv";
    private static final String TOP_RATED_PATH      = "src/main/resources/movie/topRatedMovies.csv";
    private static final String TOP_RATED_PATH2     = "src/main/resources/movie/topRatedMovies_v2.csv";
    private static final String MOVIES_PATH         = "src/main/resources/movie/knownMovies.csv";
    private static final String MOST_POPULAR_PATH   = "src/main/resources/actor/mostPopularCelebs.csv";
    private static final String MOST_POPULAR_PATH2  = "src/main/resources/actor/mostPopularCelebs2.csv";

    private JSONObject dataJson;
    private final String movieId;

    public MovieMetadataClient(String movieId) {
        this.movieId = movieId;
        HttpResponse<JsonNode> response = ImdbApiClient.getResponseApi(BASE_URL + movieId);
        dataJson = response.getBody().getObject();
    }

    public Movie getMovie() {
        dataJson = (JSONObject) dataJson.get(movieId);
        try {
            String titleType = dataJson.getJSONObject("title").getString("titleType");
            if (!titleType.toLowerCase().contains("movie")) {
                System.out.println(movieId + " not a movie: " + titleType);
                return null;
            }
            String title     = dataJson.getJSONObject("title").getString("title");
            boolean canRate  = dataJson.getJSONObject("ratings").getBoolean("canRate");
            double rating    = canRate ? dataJson.getJSONObject("ratings").getDouble("rating") : 0;
            String releaseDate         = (String) dataJson.get("releaseDate");
            int runningTimeInMinutes   = dataJson.getJSONObject("title").getInt("runningTimeInMinutes");
            return new Movie(movieId, rating, titleType, title, canRate, releaseDate, runningTimeInMinutes);
        } catch (JSONException | ClassCastException e) {
            System.err.println("Failed to parse metadata for " + movieId + ": " + e.getMessage());
        }
        return null;
    }

    public static void saveAllMovies() {
        Map<String, Integer> allMovies = new HashMap<>();
        try {
            collectMovieIds(ALL_KNOWN_FOR_PATH, 1, allMovies);
            collectMovieIds(TOP_RATED_PATH, 1, allMovies);
            System.out.println("Total unique movies: " + allMovies.size());

            try (CSVWriter writer = new CSVWriter(new FileWriter(new File(MOVIES_PATH), true))) {
                List<String[]> data = new ArrayList<>();
                data.add(new String[]{"id", "rate", "title", "canRate", "releaseDate", "runningTimeInMinutes"});
                for (String id : allMovies.keySet()) {
                    Movie movie = new MovieMetadataClient(id).getMovie();
                    if (movie != null) data.add(movie.toArray());
                }
                writer.writeAll(data);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void deleteMoviesNotInList() {
        Map<String, Integer> allMovies = new HashMap<>();
        try {
            collectMovieIds(MOVIES_PATH, 0, allMovies);

            filterAndWrite(TOP_RATED_PATH, TOP_RATED_PATH2, allMovies, 1);
            filterAndWrite(ALL_KNOWN_FOR_PATH, ALL_KNOWN_FOR_PATH2, allMovies, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void deleteCelebsWithoutMovie() {
        Map<String, Integer> allActors = new HashMap<>();
        try {
            collectMovieIds(ALL_KNOWN_FOR_PATH, 0, allActors);

            try (CSVReader csvReader = new CSVReader(new FileReader(MOST_POPULAR_PATH));
                 CSVWriter writer = new CSVWriter(new FileWriter(new File(MOST_POPULAR_PATH2), true))) {
                List<String[]> allData = csvReader.readAll();
                List<String[]> filtered = new ArrayList<>();
                filtered.add(allData.get(0));
                for (int i = 1; i < allData.size(); i++) {
                    if (allActors.containsKey(allData.get(i)[0])) filtered.add(allData.get(i));
                }
                writer.writeAll(filtered);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void collectMovieIds(String path, int colIndex, Map<String, Integer> target) throws IOException {
        try (CSVReader reader = new CSVReader(new FileReader(path))) {
            List<String[]> rows = reader.readAll();
            for (int i = 1; i < rows.size(); i++) target.put(rows.get(i)[colIndex], 1);
        }
    }

    private static void filterAndWrite(String srcPath, String dstPath, Map<String, Integer> keep, int colIndex) throws IOException {
        try (CSVReader reader = new CSVReader(new FileReader(srcPath));
             CSVWriter writer = new CSVWriter(new FileWriter(new File(dstPath), true))) {
            List<String[]> rows = reader.readAll();
            List<String[]> filtered = new ArrayList<>();
            filtered.add(rows.get(0));
            for (int i = 1; i < rows.size(); i++) {
                if (keep.containsKey(rows.get(i)[colIndex])) filtered.add(rows.get(i));
            }
            writer.writeAll(filtered);
        }
    }
}