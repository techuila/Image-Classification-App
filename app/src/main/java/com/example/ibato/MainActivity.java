package com.example.ibato;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.ibato.camera.Camera2Fragment;
import com.example.ibato.feedback.FeedbackActivity;
import com.example.ibato.history.HistoryActivity;
import com.example.ibato.interfaces.IMainActivity;
import com.example.ibato.tutorial.TutorialActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;

import static com.example.ibato.Utils.Utils.getDatabase;

public class MainActivity extends AppCompatActivity implements
        IMainActivity,
        BottomNavigationView.OnNavigationItemSelectedListener
{

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE = 1234;
    public static String CAMERA_POSITION_FRONT;
    public static String CAMERA_POSITION_BACK;
    public static String MAX_ASPECT_RATIO;
    private DrawerLayout navDrawer;
    private ProgressDialog progressDialog;
    private BottomNavigationView mBottomBar;
    public static FloatingActionButton mMainButton;

    FragmentManager fragmentManager;
    FragmentTransaction fragmentTransaction;

    FirebaseAuth mAuth;
    DatabaseReference mDatabaseRef;

    //widgets

    //vars
    private boolean mPermissions;
    public String mCameraOrientation = "none"; // Front-facing or back-facing
    String userID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBottomBar = (BottomNavigationView) findViewById(R.id.bottom_navigation);
        mMainButton = findViewById(R.id.main_button);

        progressDialog = new ProgressDialog(MainActivity.this);

        mAuth = FirebaseAuth.getInstance();
        userID = mAuth.getCurrentUser().getUid();
        mDatabaseRef = getDatabase().getReference("users" ).child(userID);

        FirebaseUser user = mAuth.getCurrentUser();

//        if (user != null) {
//            String email = user.getEmail();
//            emailText.setText(email);
//
//            if (mAuth.getCurrentUser().getPhotoUrl() != null)
//                Glide.with(this).load(mAuth.getCurrentUser().getPhotoUrl()).into(mProfileImage);
//
//            if (user.getDisplayName() != null && user.getDisplayName() != "") {
//                nameText.setText(user.getDisplayName());
//            } else {
//                mDatabaseRef.addValueEventListener(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                        if (dataSnapshot.exists()) {
//                            User user = dataSnapshot.getValue(User.class);
//                            Log.d(TAG, user.getName());
//
//                            nameText.setText(user.getName());
//                        }
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError databaseError) {
//                        Toast.makeText(MainActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
//                    }
//                });
//            }
//        }

        mBottomBar.setOnNavigationItemSelectedListener(this);
        fragmentManager = getSupportFragmentManager();
        fragmentManager.registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentResumed(FragmentManager fm, Fragment f) {
                super.onFragmentResumed(fm, f);
                Log.v("FragXX6", f.getTag());

                if (f.getTag() == getString(R.string.fragment_camera2)) {
                    mBottomBar.getMenu().getItem(2).setChecked(true);
                    mMainButton.setImageResource(R.drawable.capture2);
                } else if (f.getTag() == "ProfileFragment") {
                    mMainButton.setImageResource(R.drawable.ic_camera);
                    mBottomBar.getMenu().getItem(4).setChecked(true);
                    CAMERA_POSITION_FRONT = null;
                    CAMERA_POSITION_BACK = null;
                } else if (f.getTag() == "AboutFragment") {
                    mBottomBar.getMenu().getItem(3).setChecked(true);
                    mMainButton.setImageResource(R.drawable.ic_camera);
                    CAMERA_POSITION_FRONT = null;
                    CAMERA_POSITION_BACK = null;
                }  else if (f.getTag() == "HistoryFragment") {
                    mBottomBar.getMenu().getItem(0).setChecked(true);
                    mMainButton.setImageResource(R.drawable.ic_camera);
                    CAMERA_POSITION_FRONT = null;
                    CAMERA_POSITION_BACK = null;
                } else if (f.getTag() == "FeedbackFragment") {
                    mBottomBar.getMenu().getItem(1).setChecked(true);
                    mMainButton.setImageResource(R.drawable.ic_camera);
                    CAMERA_POSITION_FRONT = null;
                    CAMERA_POSITION_BACK = null;
                }
            }
        }, true);
        mMainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getFragment().getTag() == getString(R.string.fragment_camera2)) {
                    ((Camera2Fragment) getFragment()).captureTriggered();
                } else {
                    startCamera2();
                }
            }
        });

        init();
    }

    @Override
    public void startCamera2(){
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.camera_container, Camera2Fragment.newInstance(), getString(R.string.fragment_camera2));
        fragmentTransaction.commit();
    }

    private void init(){
        if(mPermissions){
            if(checkCameraHardware(this)){

                // Open the Camera
                startCamera2();
            }
            else{
                showSnackBar("You need a camera to use this application", Snackbar.LENGTH_INDEFINITE);
            }
        }
        else{
            verifyPermissions();
        }
    }

    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    public void verifyPermissions(){
        Log.d(TAG, "verifyPermissions: asking user for permissions.");
        String[] permissions = {
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA};
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                permissions[0] ) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this.getApplicationContext(),
                permissions[1] ) == PackageManager.PERMISSION_GRANTED) {
            mPermissions = true;
            init();
        } else {
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    permissions,
                    REQUEST_CODE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == REQUEST_CODE){
            if(mPermissions){
                init();
            }
            else{
                verifyPermissions();
            }
        }
    }


    private void showSnackBar(final String text, final int length) {
        View view = this.findViewById(android.R.id.content).getRootView();
        Snackbar.make(view, text, length).show();
    }


    @Override
    public String getUserID() {
        return userID;
    }

    @Override
    public Fragment getFragment() {
        return fragmentManager.findFragmentById(R.id.camera_container);
    }

    @Override
    public void showProgressDialog(Boolean show) {
        if (show) {
            progressDialog.show();
            progressDialog.setContentView(R.layout.progress_dialog);
            progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        } else {
            progressDialog.dismiss();
        }
    }

//    @Override
//    public void openDrawer(Boolean isBackPressed) {
//        if(navDrawer.isDrawerOpen(GravityCompat.START)) {
//            navDrawer.closeDrawer(GravityCompat.START);
//        } else if (isBackPressed) {
//            finish();
//        } else {
//            navDrawer.openDrawer(GravityCompat.START);
//        }
//    }

    @Override
    public void setCameraBackFacing() {
        Log.d(TAG, "setCameraBackFacing: setting camera to back facing.");
        mCameraOrientation = CAMERA_POSITION_BACK;
    }

    @Override
    public void setCameraFrontFacing() {
        Log.d(TAG, "setCameraFrontFacing: setting camera to front facing.");
        mCameraOrientation = CAMERA_POSITION_FRONT;
    }

    @Override
    public void setFrontCameraId(String cameraId){
        CAMERA_POSITION_FRONT = cameraId;
    }


    @Override
    public void setBackCameraId(String cameraId){
        CAMERA_POSITION_BACK = cameraId;
    }

    @Override
    public boolean isCameraFrontFacing() {
        if(mCameraOrientation.equals(CAMERA_POSITION_FRONT)){
            return true;
        }
        return false;
    }

    @Override
    public boolean isCameraBackFacing() {
        if(mCameraOrientation.equals(CAMERA_POSITION_BACK)){
            return true;
        }
        return false;
    }

    @Override
    public String getBackCameraId(){
        return CAMERA_POSITION_BACK;
    }

    @Override
    public String getFrontCameraId(){
        return CAMERA_POSITION_FRONT;
    }

    @Override
    public void hideStatusBar() {

        View decorView = getWindow().getDecorView();
        // Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }

    @Override
    public void showStatusBar() {

        View decorView = getWindow().getDecorView();
        // Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }

    @Override
    public void showTutorial() {
        Intent intent = new Intent(MainActivity.this, TutorialActivity.class);
        intent.putExtra("IS_INFO_CLICKED", Boolean.TRUE);
        startActivity(intent);
    }

//    @Override
//    public void onBackPressed() {
//        FeedbackActivity myFragment = (FeedbackActivity)getSupportFragmentManager().findFragmentByTag("FeedbackFragment");
//        if (myFragment != null && myFragment.isVisible()) {
//
//        } else {
//            super.onBackPressed();
//        }
//    }

    @SuppressWarnings("StatementWIthEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        try {
            fragmentTransaction = fragmentManager.beginTransaction();

            switch (item.getItemId()) {
                case R.id.nav_home: {
                    openFragment(item, FeedbackActivity.class, "FeedbackFragment");
                    break;
                }

                case R.id.nav_aboutus: {
                    openFragment(item, AboutActivity.class,"AboutFragment");
                    break;
                }

                case R.id.nav_profile: {
                    openFragment(item, ProfileActivity.class,"ProfileFragment");
                    break;
                }

                case R.id.nav_gallery: {
                    openFragment(item, HistoryActivity.class,"HistoryFragment");
                    break;
                }

                case R.id.logout: {
                    item.setChecked(true);
                    mAuth.signOut();
                    finish();
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                }
            }

//        openDrawer(false);
        } catch (IllegalAccessException ex) {
            Log.d(TAG, "IllegalAccessException", ex);
        } catch (InstantiationException ex) {
            Log.d(TAG, "InstantiationException", ex);
        }

        return true;
    }

    private void openFragment(MenuItem item, Class<? extends Fragment> fragment, String tag) throws IllegalAccessException, InstantiationException {
        item.setChecked(true);

        fragmentTransaction.replace(R.id.camera_container, fragment.newInstance(), tag);
        fragmentTransaction.commit();
    }
}

