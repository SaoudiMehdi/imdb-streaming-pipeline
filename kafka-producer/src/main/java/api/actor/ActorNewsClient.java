package api.actor;

import api.ImdbApiClient;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.News;
import util.SimpleDate;

public class ActorNewsClient {

    private static final Logger log = LoggerFactory.getLogger(ActorNewsClient.class);
    private static final String BASE_URL = "https://imdb8.p.rapidapi.com/actors/get-all-news?nconst=";

    private final JSONArray dataArray;
    private final String actorId;
    private int count = 0;

    public ActorNewsClient(String actorId) {
        this.actorId = actorId;
        HttpResponse<JsonNode> response = ImdbApiClient.getResponseApi(BASE_URL + actorId);
        JSONArray array = null;
        if (response != null) {
            try {
                array = (JSONArray) response.getBody().getArray().getJSONObject(0).get("items");
            } catch (Exception e) {
                log.warn("Could not parse news items for actor {}: {}", actorId, e.getMessage());
            }
        }
        this.dataArray = array != null ? array : new JSONArray();
    }

    /**
     * Returns the next valid News item for today, or null when exhausted.
     * Skips malformed entries without recursion.
     */
    public News getNextNews(SimpleDate date) {
        while (count < dataArray.length()) {
            try {
                JSONObject entry = dataArray.getJSONObject(count++);
                String publishTime = entry.getString("publishDateTime");
                publishTime = date.toString() + "T" + publishTime.split("T")[1];
                String body = entry.getString("body");
                String head = entry.getString("head");
                String id   = entry.getString("id").split("/")[3];
                String link = entry.getString("link");
                return new News(body, head, id, link, publishTime, actorId);
            } catch (JSONException | ArrayIndexOutOfBoundsException e) {
                log.warn("Skipping malformed news entry at index {}: {}", count - 1, e.getMessage());
            }
        }
        return null;
    }
}