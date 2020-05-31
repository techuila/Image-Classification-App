package com.example.ibato.home;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ibato.R;
import com.example.ibato.interfaces.IHomeActivity;
import com.example.ibato.interfaces.IMainActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import static com.example.ibato.Utils.Utils.getDatabase;
import static com.example.ibato.Utils.Utils.isNetworkAvailable;
import static com.example.ibato.Utils.Utils.loadImage;

public class DetailActivity extends AppCompatActivity {

    private final String TAG = "DETAILACTIVITY";
    private DatabaseReference mDatabaseRef;
    private FirebaseStorage mStorage;
    private String userID;
    private IHomeActivity mIHomeActivity;
    private ProgressBar progressBar;
    private TextView mTitle, mDescr, mWarning;
    private ImageView mImage, mStatus;
    private Button mDelete;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        initialize();
    }

    private void initialize() {
        /* ====== Firebase Variables ====== */
        userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mStorage = FirebaseStorage.getInstance();
        mDatabaseRef = getDatabase().getReference("uploads").child(userID);
        mDatabaseRef.keepSynced(true);


        /* ====== Android Components ====== */
        mTitle = (TextView) findViewById(R.id.title_text);
        mDescr = (TextView) findViewById(R.id.descr_text);
        mWarning = (TextView) findViewById(R.id.title_text_sub);
        mImage = (ImageView) findViewById(R.id.captured_image);
        mStatus = (ImageView) findViewById(R.id.status_img);
        mDelete = (Button) findViewById(R.id.delete_button);
        progressBar = (ProgressBar) findViewById(R.id.loading);

        /* ====== Diplay Values ====== */
        mTitle.setText(getIntent().getStringExtra("title"));
        mDescr.setText(getIntent().getStringExtra("descr"));
        mWarning.setText(getIntent().getStringExtra("title_sub"));
        mStatus.setImageResource(getIntent().getIntExtra("icon", R.drawable.ic_warning_black_24dp));
        loadImage(this, getIntent().getStringExtra("image"), progressBar, mImage);

        /* ====== Add Listener ====== */
        mDelete.setOnClickListener(v -> {
            deleteData(getIntent().getStringExtra("key"), getIntent().getStringExtra("image"), DetailActivity.this);
        });
    }

    private void deleteData(String key, String image, Context context) {
        final AlertDialog builder = new AlertDialog.Builder(context)
                .setTitle("Confirmation")
                .setMessage("Are you sure you want to permanently delete this data?")
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (isNetworkAvailable(context)) {
                            progressBar.setVisibility(View.VISIBLE);
                            final String modelKey = key;

                            // Gets the image reference from the selected model
                            StorageReference imageRef = mStorage.getReferenceFromUrl(image);
                            // Deletes the image on the firebase storage
                            imageRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    // Deletes the data on the firebase database
                                    mDatabaseRef.child(modelKey).removeValue();
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(context, "Data Successfully Deleted!", Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            // Deletes the data on the firebase database
                                            mDatabaseRef.child(modelKey).removeValue();
                                            progressBar.setVisibility(View.GONE);
                                            Toast.makeText(context, "Data Successfully Deleted!", Toast.LENGTH_SHORT).show();
                                            finish();
//                                            progressBar.setVisibility(View.GONE);
//                                            Toast.makeText(context, "Data failed to delete!", Toast.LENGTH_SHORT).show();
                                        }
                                    });

                            dialog.dismiss();
                        } else {
                            Toast.makeText(context, "You are offline, please try again later.", Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();

        builder.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                builder.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(context.getResources().getColor(R.color.transparent));
                builder.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(context.getResources().getColor(R.color.colorPrimary));

                builder.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(context.getResources().getColor(R.color.transparent));
                builder.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(context.getResources().getColor(R.color.colorPrimary));
            }
        });



        builder.show();
    }
}
