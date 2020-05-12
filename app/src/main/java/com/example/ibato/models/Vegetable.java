package com.example.ibato.models;

public class Vegetable {

    private String name;


    public Vegetable() {
        //empty constructor needed
    }

    public Vegetable(String name) {
        this.name = name;

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
