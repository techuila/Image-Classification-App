package com.example.ibato.models;

public class Vegetable {

    private String name, descr;
    private Boolean isLimit;


    public Vegetable() {
        //empty constructor needed
    }

    public Vegetable(String name, Boolean isLimit, String descr) {
        this.name = name;
        this.isLimit = isLimit;
        this.descr = descr;

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getIsLimit() {
        return isLimit;
    }

    public void setIsLimit(Boolean isLimit) {
        this.isLimit = isLimit;
    }

    public String getDescr() {
        return descr;
    }

    public void setDescr(String descr) {
        this.descr = descr;
    }

}
