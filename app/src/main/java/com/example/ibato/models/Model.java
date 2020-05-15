package com.example.ibato.models;

import com.google.firebase.database.Exclude;

public class Model {

    private String image;
    private String title;
    private String desc;
    private String mKey;
    private String userID;
    private String isEdible;

    public Model() {
        //empty constructor needed
    }

    public Model(String userID, String image, String title, String desc, String isEdible) {
        this.userID = userID;
        this.image = image;
        this.title = title;
        this.desc = desc;
        this.isEdible = isEdible;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getIsEdible() {
        return isEdible;
    }

    public void setIsEdible(String isEdible) {
        this.isEdible = isEdible;
    }


    @Exclude
    public String getKey() {
        return mKey;
    }

    @Exclude
    public void setKey(String key) {
        mKey = key;
    }
}