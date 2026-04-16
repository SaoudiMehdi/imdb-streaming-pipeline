package api.actor;

import api.ImdbApiClient;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class ActorBornTodayClient {

    private final JSONArray dataArray;
    private static final String BASE_URL = "https://imdb8.p.rapidapi.com/actors/list-born-today";

    public ActorBornTodayClient(int month, int day) {
        String url = BASE_URL + "?month=" + month + "&day=" + day;
        HttpResponse<JsonNode> response = ImdbApiClient.getResponseApi(url);
        dataArray = (response != null) ? response.getBody().getArray() : new JSONArray();
    }

    public List<String> getAllBornToday() {
        List<String> actorIds = new ArrayList<>();
        for (int i = 0; i < dataArray.length(); i++) {
            actorIds.add(dataArray.getString(i).split("/")[2]);
        }
        return actorIds;
    }
}