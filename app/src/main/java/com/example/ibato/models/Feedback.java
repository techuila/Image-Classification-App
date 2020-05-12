package com.example.ibato.models;

import com.google.firebase.database.Exclude;

public class Feedback {
    private String image;
    private String username;
    private String feedback;
    private String mKey;
    private String userID;
    private String date;
    private float userRating, totalRating;

    public Feedback() {
        //empty constructor needed
    }

    public Feedback(String userID, String image, String username, String feedback, String date, float userRating) {
        this.userID = userID;
        this.image = image;
        this.username = username;
        this.feedback = feedback;
        this.date = date;
        this.userRating = userRating;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public float getUserRating() {
        return userRating;
    }

    public void setUserRating(float userRating) {
        this.userRating = userRating;
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

