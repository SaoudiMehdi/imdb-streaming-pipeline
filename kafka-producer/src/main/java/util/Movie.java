package util;

import org.json.JSONObject;

public class Movie {
    String id;
    double rate= 0;
    String title;
    String titletype;
    boolean canRate;
    String releaseDate;
    int runningTimeInMinutes;

    public Movie(){

    }

    public Movie(String id, double rate, String titletype, String title, boolean canRate, String releaseDate, int runningTimeInMinutes) {
        this.id = id;
        this.rate = rate;
        this.title = title;
        this.titletype = titletype;
        this.canRate = canRate;
        this.releaseDate = releaseDate;
        this.runningTimeInMinutes = runningTimeInMinutes;
    }

    public Movie(String id, double rate, String title, String titletype) {
        this.id = id;
        this.rate = rate;
        this.title = title;
        this.titletype = titletype;
    }



    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitletype() {
        return titletype;
    }

    public void setTitletype(String titletype) {
        this.titletype = titletype;
    }

    @Override
    public String toString() {
        return "Movie{" +
                "id='" + id + '\'' +
                ", rate=" + rate +
                ", title='" + title + '\'' +
                ", titletype='" + titletype + '\'' +
                ", canRate='" + canRate + '\'' +
                ", releaseDate='" + releaseDate + '\'' +
                ", runningTimeInMinutes='" + runningTimeInMinutes + '\'' +
                '}';
    }

    public JSONObject toJson(){
        JSONObject movieJson = new JSONObject();
        movieJson.put("id", id);
        movieJson.put("rate", rate);
        movieJson.put("title", title);
        movieJson.put("titletype", titletype);
        return movieJson;
    }

    public String[] toArray(){
        return new String[]{id, String.valueOf(rate), titletype, title, String.valueOf(canRate), releaseDate, String.valueOf(runningTimeInMinutes)};
    }
}
