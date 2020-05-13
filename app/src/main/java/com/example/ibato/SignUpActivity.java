package com.example.ibato;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ibato.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import static com.example.ibato.Utils.Utils.getDatabase;

public class SignUpActivity extends AppCompatActivity implements View.OnClickListener {

    FirebaseAuth mAuth;
    DatabaseReference mDatabaseRef;

    public static final String TAG = "SignUpActivity";
    private ProgressBar progressBar;
    private TextInputEditText editFullName, editTextEmail, editTextPassword, editTextConfirmPassword;
    private Button loginBtn, signUpBtn;
    String userID;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        initialize();
    }

    private void initialize() {
        // Firebase instance auth
        mAuth = FirebaseAuth.getInstance();
        mDatabaseRef = getDatabase().getReference("users");

        // Components initialization
        loginBtn = (Button) findViewById(R.id.login);
        editFullName = (TextInputEditText) findViewById(R.id.account_name_input);
        editTextEmail = (TextInputEditText) findViewById(R.id.email_input);
        editTextPassword = (TextInputEditText) findViewById(R.id.password_input);
        editTextConfirmPassword = (TextInputEditText) findViewById(R.id.confirm_password_input);
        signUpBtn = (Button) findViewById(R.id.sign_up);
        progressBar = (ProgressBar) findViewById(R.id.loading);

        /* Action Listeners */
        loginBtn.setOnClickListener(this);
        signUpBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view == loginBtn) handleLogin();
        else if(view == signUpBtn) handleSignUp();
    }

    private void handleLogin() {
        finish();
        startActivity(new Intent(this, LoginActivity.class));
    }

    private void handleSignUp() {
        final String fullName = editFullName.getText().toString().trim();
        final String email = editTextEmail.getText().toString().trim();
        final String password = editTextPassword.getText().toString().trim();
        final String confirm_password = editTextConfirmPassword.getText().toString().trim();

        if (fullName.isEmpty()) {
            editFullName.setError("Name is required");
            editFullName.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            editTextEmail.setError("Email is required");
            editTextEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Please enter a valid email");
            editTextEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            editTextPassword.setError("Password is required");
            editTextPassword.requestFocus();
            return;
        }

        if (confirm_password.isEmpty()) {
            editTextConfirmPassword.setError("Confirm Password is required");
            editTextConfirmPassword.requestFocus();
            return;
        }

        if (!password.equals(confirm_password)) {
            editTextConfirmPassword.setError("Password doesn't match!");
            editTextConfirmPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            editTextPassword.setError("Minimum lenght of password should be 6");
            editTextPassword.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                progressBar.setVisibility(View.GONE);
                if (task.isSuccessful()) {
                    userID = mAuth.getCurrentUser().getUid();
                    User user = new User(fullName, "", "", null);
                    mDatabaseRef.child(userID).setValue(user);

                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setDisplayName(fullName)
                            .build();

                    mAuth.getCurrentUser().updateProfile(profileUpdates)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Log.d(TAG, "User profile updated.");
                                    }
                                }
                            });

                    finish();
                    startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                } else {

                    if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                        Toast.makeText(getApplicationContext(), "You are already registered", Toast.LENGTH_SHORT).show();

                    } else {
                        Toast.makeText(getApplicationContext(), task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }

                }
            }
        });
    }
}