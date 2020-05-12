package com.example.ibato.interfaces;


import android.graphics.drawable.Drawable;

import androidx.fragment.app.Fragment;

/**
 * Created by User on 6/5/2018.
 */

public interface IMainActivity {

    String getUserID();

    void setCameraFrontFacing();

    void setCameraBackFacing();

    boolean isCameraFrontFacing();

    boolean isCameraBackFacing();

    void setFrontCameraId(String cameraId);

    void setBackCameraId(String cameraId);

    String getFrontCameraId();

    String getBackCameraId();

    void hideStatusBar();

    void showStatusBar();

//    void openDrawer(Boolean isBackPressed);

    Fragment getFragment();

    void startCamera2();

    void showProgressDialog(Boolean show);

    void showTutorial();
}
