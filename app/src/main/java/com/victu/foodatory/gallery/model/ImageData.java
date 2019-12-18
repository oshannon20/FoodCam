package com.victu.foodatory.gallery.model;

public class ImageData {

    int id;
    String name;
    String filePath;
    String uri;
    String date;
    String time;
    boolean isSelected;

    public ImageData(int id, String filePath, String date) {
        this.id = id;
        this.filePath = filePath;
        this.date = date;
    }


    public ImageData(int id, String name, String filePath, String uri, String date, String time) {
        this.id = id;
        this.name = name;
        this.filePath = filePath;
        this.uri = uri;
        this.date = date;
        this.time = time;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }


    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
