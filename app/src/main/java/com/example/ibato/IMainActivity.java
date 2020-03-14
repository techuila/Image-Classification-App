package com.example.ibato;


import android.graphics.drawable.Drawable;

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

    void openDrawer(Boolean isBackPressed);

    void showProgressDialog(Boolean show);
}
