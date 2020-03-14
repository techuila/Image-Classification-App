package com.example.ibato;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.PersistableBundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCanceledListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity implements View.OnClickListener  {

    private final String TAG = "ProfileActivity";
    private final int TAKE_IMAGE_CODE = 10001;

    private Button mBackButton, mChangePassword, mSaveChanges;
    private TextInputEditText mName, mEmail, mPhone, mAddress;
    private ProgressDialog progressDialog;
    private CircleImageView mProfileImage;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initialize();
    }

    private void initialize() {
        /* Components */
        mBackButton = (Button) findViewById(R.id.back_button);
        mSaveChanges = (Button) findViewById(R.id.save_changes);
        mChangePassword = (Button) findViewById(R.id.change_password);
        mProfileImage = (CircleImageView) findViewById(R.id.profile_image);
        mName = (TextInputEditText) findViewById(R.id.account_name_input);
        mEmail = (TextInputEditText) findViewById(R.id.email_input);
        mPhone = (TextInputEditText) findViewById(R.id.phone_input);
        mAddress = (TextInputEditText) findViewById(R.id.address_input);
        progressDialog = new ProgressDialog(ProfileActivity.this);

        /* Firebase */
        mAuth = FirebaseAuth.getInstance();
        mDatabaseRef = FirebaseDatabase.getInstance().getReference("users").child(mAuth.getCurrentUser().getUid());

        /* Fill form from firebase authentication */
        mName.setText(mAuth.getCurrentUser().getDisplayName());
        mEmail.setText(mAuth.getCurrentUser().getEmail());
        if (mAuth.getCurrentUser().getPhotoUrl() != null)
            Glide.with(this).load(mAuth.getCurrentUser().getPhotoUrl()).into(mProfileImage);


        /* Fill form from firebase database */
        mDatabaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    User user = dataSnapshot.getValue(User.class);
                    mPhone.setText(user.getPhone());
                    mAddress.setText(user.getAddress());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ProfileActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        /* Action Listeners */
        mBackButton.setOnClickListener(this);
        mChangePassword.setOnClickListener(this);
        mSaveChanges.setOnClickListener(this);
        mProfileImage.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back_button: {
                startActivity(new Intent(ProfileActivity.this, MainActivity.class));
                break;
            }

            case R.id.change_password: {
                View mContent = ProfileActivity.this.getLayoutInflater().inflate(R.layout.dialog_change_password, null);
                final TextInputEditText mCurrentPassword = mContent.findViewById(R.id.current_password_input);
                final TextInputEditText mPassword = mContent.findViewById(R.id.password_input);
                final TextInputEditText mConfirmPassword = mContent.findViewById(R.id.confirm_password_input);

                final AlertDialog aDialog = new AlertDialog.Builder(ProfileActivity.this, R.style.AlertDialogTheme)
                        .setTitle("Change Password")
                        .setView(mContent)
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Save", null)
                        .create();

                aDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        aDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(getResources().getColor(R.color.transparent));
                        aDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.colorPrimary));

                        aDialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(getResources().getColor(R.color.transparent));
                        aDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.colorPrimary));

                        Button button = ((AlertDialog) aDialog).getButton(AlertDialog.BUTTON_POSITIVE);
                        button.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Log.d(TAG, mAuth.getCurrentUser().getEmail());
                                Log.d(TAG, mCurrentPassword.getText().toString());
                                AuthCredential credential = EmailAuthProvider
                                        .getCredential(mAuth.getCurrentUser().getEmail(), mCurrentPassword.getText().toString());

                                mAuth.getCurrentUser().reauthenticate(credential)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                Log.d(TAG, "User re-authenticated.");

                                                if (mPassword.getText().toString().isEmpty()) {
                                                    mPassword.setError("Password is required");
                                                    mPassword.requestFocus();
                                                    return;
                                                } else if (mPassword.getText().toString().length() < 6) {
                                                    mPassword.setError("Minimum length of password should be 6");
                                                    mPassword.requestFocus();
                                                    return;
                                                }

                                                /* Compares the password and confirm password placed by the user */
                                                if (mPassword.getText().toString().equals(mConfirmPassword.getText().toString())) {
                                                    /* Update Password */
                                                    mAuth.getCurrentUser().updatePassword(mPassword.getText().toString())
                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    aDialog.dismiss();
                                                                    Toast.makeText(ProfileActivity.this, "Successfully Changed Password!", Toast.LENGTH_SHORT).show();
                                                                }
                                                            })
                                                            .addOnFailureListener(new OnFailureListener() {
                                                                @Override
                                                                public void onFailure(@NonNull Exception e) {
                                                                    Toast.makeText(ProfileActivity.this, "Change Password Failed!", Toast.LENGTH_SHORT).show();
                                                                }
                                                            });
                                                } else {
                                                    mConfirmPassword.setError("Password doesn't match!");
                                                    mConfirmPassword.requestFocus();
                                                }

                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                mCurrentPassword.setError("Password is incorrect!");
                                                mCurrentPassword.requestFocus();
                                            }
                                        });

                            }
                        });

                    }
                });

                aDialog.show();
                break;
            }

            case R.id.profile_image: {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, TAKE_IMAGE_CODE);

                break;
            }

            case R.id.save_changes: {
                showProgressDialog(true);

                mAuth.getCurrentUser().updateEmail(mEmail.getText().toString())
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(mName.getText().toString())
                                        .build();
                                mAuth.getCurrentUser().updateProfile(profileUpdates);

                                User user = new User(mName.getText().toString(), mPhone.getText().toString(), mAddress.getText().toString());
                                mDatabaseRef.setValue(user);

                                Toast.makeText(ProfileActivity.this, "Profile Successfully Updated!", Toast.LENGTH_SHORT).show();
                                showProgressDialog(false);
                            }
                        }
                    }).addOnCanceledListener(new OnCanceledListener() {
                        @Override
                        public void onCanceled() {
                            Toast.makeText(ProfileActivity.this, "Profile Update Failed!", Toast.LENGTH_SHORT).show();
                            showProgressDialog(false);
                        }
                    });

                break;
            }
        }
    }

    private void showProgressDialog(Boolean show) {
        if (show) {
            progressDialog.show();
            progressDialog.setContentView(R.layout.progress_dialog);
            progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        } else {
            progressDialog.dismiss();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TAKE_IMAGE_CODE && resultCode == RESULT_OK) {
            switch (resultCode) {
                case RESULT_OK:
                    handleUpload(data.getData());
            }
        }
    }

    private void handleUpload(Uri uri) {
        showProgressDialog(true);
        final StorageReference reference = FirebaseStorage.getInstance().getReference("profileImages").child(mAuth.getCurrentUser().getUid()).child(uri.getLastPathSegment());

        reference.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(final UploadTask.TaskSnapshot taskSnapshot) {
                reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        showProgressDialog(false);
                        Glide.with(ProfileActivity.this).load(uri).into(mProfileImage);
                        Log.d("tag", "onSuccess: Uploaded Image URl is " + uri.toString());
                        setUserProfileUrl(uri);
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(ProfileActivity.this, "Update Failed!", Toast.LENGTH_SHORT).show();
                showProgressDialog(false);
            }
        });
    }

    private void setUserProfileUrl(Uri uri) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setPhotoUri(uri)
                .build();

        mAuth.getCurrentUser().updateProfile(profileUpdates)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Toast.makeText(ProfileActivity.this, "Successfully Updated Profile Image!", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(ProfileActivity.this, "Profile image update failed!", Toast.LENGTH_SHORT).show();
                }
            });

    }
}
