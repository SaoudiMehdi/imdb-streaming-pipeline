package util;

import org.json.JSONObject;

public class News {
    String body;
    String head;
    String id;
    String id_actor;
    String link;
    String publishTime;

    public News(String body, String head, String id, String link, String publishTime, String id_actor) {
        this.body = body;
        this.head = head;
        this.id = id;
        this.link = link;
        this.publishTime = publishTime;
        this.id_actor = id_actor;
    }

    public News() {
    }

    public String getBody() {
        return body;
    }

    public String getHead() {
        return head;
    }

    public String getId() {
        return id;
    }

    public String getLink() {
        return link;
    }

    public String getPublishTime() {
        return publishTime;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setHead(String head) {
        this.head = head;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public void setPublishTime(String publishTime) {
        this.publishTime = publishTime;
    }

    public String jsonAsString(){
        JSONObject newsJson = new JSONObject();
        newsJson.put("body",body);
        newsJson.put("head",head);
        newsJson.put("id",id);
        newsJson.put("link",link);
        newsJson.put("publishTime",publishTime);
        newsJson.put("id_actor",id_actor);
        return newsJson.toString();
    }

    @Override
    public String toString() {
        return "News{" +
                "id='" + id + '\'' +
                ", head=" + head +
                ", body='" + body + '\'' +
                ", link='" + link + '\'' +
                ", publishTime='" + publishTime + '\'' +
                ", id_actor='" + id_actor + '\'' +
                '}';
    }
}
