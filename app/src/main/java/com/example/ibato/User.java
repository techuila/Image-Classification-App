package com.example.ibato;

import com.google.firebase.database.Exclude;

public class User {

    private String name;
    private String phone;
    private String address;
    private String userID;

    public User() {
        //empty constructor needed
    }

    public User(String name, String phone, String address) {
        this.name = name;
        this.phone = phone;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Exclude
    public String getKey() {
        return userID;
    }

    @Exclude
    public void setKey(String key) {
        userID = key;
    }
}