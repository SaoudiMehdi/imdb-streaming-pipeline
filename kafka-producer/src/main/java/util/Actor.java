package util;

import org.json.JSONObject;

public class Actor {

    String id;
    String name;
    String birthDate;
    String birthPlace;
    String gender;

    public Actor(){

    }

    public Actor(String id, String name, String birthDate, String birthPlace, String gender) {
        this.id = id;
        this.name = name;
        this.birthDate = birthDate;
        this.birthPlace = birthPlace;
        this.gender = gender;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getBirthPlace() {
        return birthPlace;
    }

    public void setBirthPlace(String birthPlace) {
        this.birthPlace = birthPlace;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }


    @Override
    public String toString() {
        return "Actor{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", birthDate='" + birthDate + '\'' +
                ", birthPlace='" + birthPlace + '\'' +
                ", gender='" + gender + '\'' +
                '}';
    }

    public JSONObject toJson(){
        JSONObject actorJson = new JSONObject();
        actorJson.put("id", id);
        actorJson.put("name", name);
        actorJson.put("birthDate", birthDate);
        actorJson.put("birthPlace", birthPlace);
        actorJson.put("gender", gender);
        return actorJson;
    }

    public String jsonAsString(){
        JSONObject actorJson = new JSONObject();
        actorJson.put("id", id);
        actorJson.put("name", name);
        actorJson.put("birthDate", birthDate);
        actorJson.put("birthPlace", birthPlace);
        actorJson.put("gender", gender);
        return actorJson.toString();
    }

    public String[] toArray(){
        return new String[]{id, name, birthDate, birthPlace, gender};
    }
}
