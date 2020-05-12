package com.example.ibato;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.ibato.interfaces.IMainActivity;
import com.example.ibato.models.User;
import com.google.android.gms.tasks.OnCanceledListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.BaseTransientBottomBar;
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

import de.hdodenhof.circleimageview.CircleImageView;

import static com.example.ibato.Utils.Utils.getDatabase;

public class ProfileActivity extends Fragment implements
        View.OnClickListener,
        PopupMenu.OnMenuItemClickListener {

    public static ProfileActivity newInstance(){
        return new ProfileActivity();
    }

    private IMainActivity mIMainActivity;
    private final String TAG = "ProfileActivity";
    private final int TAKE_IMAGE_CODE = 10001;

    private Button mBackButton, mChangePassword, mSaveChanges, mOptionMenu;
    private TextInputEditText mName, mEmail, mPhone, mAddress;
    private ProgressDialog progressDialog;
    private CircleImageView mProfileImage;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabaseRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_profile, container, false);

        return view;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        initialize(view);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try{
            mIMainActivity = (IMainActivity) ProfileActivity.this.getActivity();
        }catch (ClassCastException e){
            Log.e(TAG, "onAttach: ClassCastException: " + e.getMessage() );
        }
    }

//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_profile);
//
//
//    }

    private void initialize(final View view) {
        /* Components */
//        mBackButton = (Button) view.findViewById(R.id.back_button);
        mOptionMenu = (Button) view.findViewById(R.id.menu_button);
        mSaveChanges = (Button) view.findViewById(R.id.save_changes);
        mChangePassword = (Button) view.findViewById(R.id.change_password);
        mProfileImage = (CircleImageView) view.findViewById(R.id.profile_image);
        mName = (TextInputEditText) view.findViewById(R.id.account_name_input);
        mEmail = (TextInputEditText) view.findViewById(R.id.email_input);
        mPhone = (TextInputEditText) view.findViewById(R.id.phone_input);
        mAddress = (TextInputEditText) view.findViewById(R.id.address_input);

        /* Firebase */
        mAuth = FirebaseAuth.getInstance();
        mDatabaseRef = getDatabase().getReference("users").child(mAuth.getCurrentUser().getUid());

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
                Toast.makeText(view.getContext(), databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        /* Action Listeners */
//        mBackButton.setOnClickListener(this);
        mChangePassword.setOnClickListener(this);
        mSaveChanges.setOnClickListener(this);
        mProfileImage.setOnClickListener(this);
        mOptionMenu.setOnClickListener(this);
    }

    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
//            case R.id.back_button: {
//                startActivity(new Intent(ProfileActivity.this, MainActivity.class));
//                break;
//            }

            case R.id.change_password: {
                View mContent = ProfileActivity.this.getLayoutInflater().inflate(R.layout.dialog_change_password, null);
                final TextInputEditText mCurrentPassword = mContent.findViewById(R.id.current_password_input);
                final TextInputEditText mPassword = mContent.findViewById(R.id.password_input);
                final TextInputEditText mConfirmPassword = mContent.findViewById(R.id.confirm_password_input);

                final AlertDialog aDialog = new AlertDialog.Builder(v.getContext(), R.style.AlertDialogTheme)
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
                                                                    Toast.makeText(ProfileActivity.this.getActivity(), "Successfully Changed Password!", Toast.LENGTH_SHORT).show();
                                                                }
                                                            })
                                                            .addOnFailureListener(new OnFailureListener() {
                                                                @Override
                                                                public void onFailure(@NonNull Exception e) {
                                                                    Toast.makeText(ProfileActivity.this.getActivity(), "Change Password Failed!", Toast.LENGTH_SHORT).show();
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
                if (mPhone.length() != 0 && mPhone.length() < 11) {
                    mPhone.setError("Length of phone number should be 11");
                    mPhone.requestFocus();
                    return;
                }

                mIMainActivity.showProgressDialog(true);


                mAuth.getCurrentUser().updateEmail(mEmail.getText().toString())
                    .addOnCompleteListener((task) -> {
                            if (task.isSuccessful()) {
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(mName.getText().toString())
                                        .build();
                                mAuth.getCurrentUser().updateProfile(profileUpdates);

                                User user = new User(mName.getText().toString(), mPhone.getText().toString(), mAddress.getText().toString(), null);
                                mDatabaseRef.setValue(user);

                                Toast.makeText(v.getContext(), "Profile Successfully Updated!", Toast.LENGTH_SHORT).show();
                                mIMainActivity.showProgressDialog(false);
                            }

                    }).addOnCanceledListener(new OnCanceledListener() {
                        @Override
                        public void onCanceled() {
                            Toast.makeText(v.getContext(), "Profile Update Failed!", Toast.LENGTH_SHORT).show();
                            mIMainActivity.showProgressDialog(false);
                        }
                    });

                break;
            }

            case R.id.menu_button: {
                PopupMenu popup = new PopupMenu(v.getContext(), v);
                popup.setOnMenuItemClickListener(this);
                popup.inflate(R.menu.profile_actions);
                popup.show();
                break;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TAKE_IMAGE_CODE && resultCode == ProfileActivity.this.getActivity().RESULT_OK) {
            if (resultCode == ProfileActivity.this.getActivity().RESULT_OK) {
                handleUpload(data.getData());
            }
        }
    }

    private void handleUpload(Uri uri) {
        mIMainActivity.showProgressDialog(true);
        final StorageReference reference = FirebaseStorage.getInstance().getReference("profileImages").child(mAuth.getCurrentUser().getUid()).child(uri.getLastPathSegment());

        reference.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(final UploadTask.TaskSnapshot taskSnapshot) {
                reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        mIMainActivity.showProgressDialog(false);
                        Glide.with(ProfileActivity.this).load(uri).into(mProfileImage);
                        Log.d("tag", "onSuccess: Uploaded Image URl is " + uri.toString());
                        setUserProfileUrl(uri);
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(ProfileActivity.this.getActivity(), "Update Failed!", Toast.LENGTH_SHORT).show();
                mIMainActivity.showProgressDialog(false);
            }
        });
    }

    private void setUserProfileUrl(final Uri uri) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setPhotoUri(uri)
                .build();

        mAuth.getCurrentUser().updateProfile(profileUpdates)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    mDatabaseRef.child("profilePicture").setValue(uri.toString());
                    Toast.makeText(ProfileActivity.this.getActivity(), "Successfully Updated Profile Image!", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(ProfileActivity.this.getActivity(), "Profile image update failed!", Toast.LENGTH_SHORT).show();
                }
            });

    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logout: {
                Snackbar snackbar = Snackbar.make(ProfileActivity.this.getActivity().findViewById(android.R.id.content), "Logging out...", Snackbar.LENGTH_SHORT);
                snackbar.setAnchorView(MainActivity.mMainButton);
                snackbar.setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE);
                snackbar.setCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        super.onDismissed(snackbar, event);

                        mAuth.signOut();
                        getActivity().finish();
                        startActivity(new Intent(getActivity(), LoginActivity.class));
                    }
                });
                snackbar.show();
                return true;
            }

            default:
                return false;
        }
    }
}
