package com.example.ibato.interfaces;

import android.content.Context;

import com.example.ibato.models.Model;

import java.io.Serializable;

public interface IHomeActivity extends Serializable {

    void deleteData(String key, String image, Context context);

}
