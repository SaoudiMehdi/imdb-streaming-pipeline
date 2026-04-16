package api.actor;

import api.ImdbApiClient;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.opencsv.CSVWriter;
import org.json.JSONArray;
import util.Actor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PopularCelebsClient {

    private final JSONArray dataArray;
    private static final String BASE_URL       = "https://imdb8.p.rapidapi.com/actors/list-most-popular-celebs";
    private static final String CSV_FILE_PATH  = "src/main/resources/actor/mostPopularCelebs.csv";

    private int count = 0;

    public PopularCelebsClient() {
        HttpResponse<JsonNode> response = ImdbApiClient.getResponseApi(BASE_URL);
        dataArray = response.getBody().getArray();
    }

    public Actor getNextActor() {
        while (count < dataArray.length()) {
            String actorId = ((String) dataArray.get(count++)).split("/")[2];
            Actor actor = new ActorBiographyClient(actorId).getActor();
            if (actor != null) return actor;
        }
        return null;
    }

    public void savePopularCelebs() {
        try (CSVWriter writer = new CSVWriter(new FileWriter(new File(CSV_FILE_PATH)))) {
            List<String[]> data = new ArrayList<>();
            data.add(new String[]{"id", "name", "birthDate", "birthPlace", "gender"});
            Actor actor;
            while ((actor = getNextActor()) != null) {
                data.add(actor.toArray());
            }
            writer.writeAll(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}