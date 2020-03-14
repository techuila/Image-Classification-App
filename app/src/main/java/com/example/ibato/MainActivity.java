package com.example.ibato;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BlendMode;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import de.hdodenhof.circleimageview.CircleImageView;

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
    private TextView nameText, emailText;
    private View headerView;
    private CircleImageView mProfileImage;
    private BottomNavigationView mBottomBar;

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
        emailText = (TextView) headerView.findViewById(R.id.email_address);
        nameText = (TextView) headerView.findViewById(R.id.account_name);
        mProfileImage = (CircleImageView) headerView.findViewById(R.id.profile_image);

        mBottomBar.bringToFront();

        progressDialog = new ProgressDialog(MainActivity.this);

        mAuth = FirebaseAuth.getInstance();
        userID = mAuth.getCurrentUser().getUid();
        mDatabaseRef = FirebaseDatabase.getInstance().getReference("users" ).child(userID);

        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            String email = user.getEmail();
            emailText.setText(email);

            if (mAuth.getCurrentUser().getPhotoUrl() != null)
                Glide.with(this).load(mAuth.getCurrentUser().getPhotoUrl()).into(mProfileImage);

            if (user.getDisplayName() != null && user.getDisplayName() != "") {
                nameText.setText(user.getDisplayName());
            } else {
                mDatabaseRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            User user = dataSnapshot.getValue(User.class);
                            Log.d(TAG, user.getName());

                            nameText.setText(user.getName());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(MainActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        mBottomBar.setOnNavigationItemSelectedListener(this);

        init();
    }

    private void startCamera2(){
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.camera_container, Camera2Fragment.newInstance(), getString(R.string.fragment_camera2));
        transaction.commit();
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
    public void showProgressDialog(Boolean show) {
        if (show) {
            progressDialog.show();
            progressDialog.setContentView(R.layout.progress_dialog);
            progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        } else {
            progressDialog.dismiss();
        }
    }

    @Override
    public void openDrawer(Boolean isBackPressed) {
        if(navDrawer.isDrawerOpen(GravityCompat.START)) {
            navDrawer.closeDrawer(GravityCompat.START);
        } else if (isBackPressed) {
            finish();
        } else {
            navDrawer.openDrawer(GravityCompat.START);
        }
    }

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
    public void onBackPressed() {
        openDrawer(true);
    }

    @SuppressWarnings("StatementWIthEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_profile: {
                startActivity(new Intent(MainActivity.this, ProfileActivity.class));
                break;
            }

            case R.id.nav_gallery:
            case R.id.nav_history: {
                startActivity(new Intent(MainActivity.this, HistoryActivity.class));
                break;
            }

            case R.id.logout: {
                mAuth.signOut();
                finish();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
            }
        }

        openDrawer(false);
        return true;
    }
}

