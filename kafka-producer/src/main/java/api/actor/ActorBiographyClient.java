package api.actor;

import api.ImdbApiClient;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import org.json.JSONException;
import org.json.JSONObject;
import util.Actor;

public class ActorBiographyClient {

    private final HttpResponse<JsonNode> response;
    public static final String BASE_URL = "https://imdb8.p.rapidapi.com/actors/get-bio?nconst=";

    public ActorBiographyClient(String actorId) {
        response = ImdbApiClient.getResponseApi(BASE_URL + actorId);
    }

    public Actor getActor() {
        JSONObject actorJson = response.getBody().getObject();
        try {
            String id         = actorJson.getString("id").split("/")[2];
            String birthDate  = actorJson.getString("birthDate");
            String birthPlace = actorJson.getString("birthPlace");
            String name       = actorJson.getString("name");
            String gender     = actorJson.getString("gender");
            return new Actor(id, name, birthDate, birthPlace, gender);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}