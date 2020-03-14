package com.example.ibato;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    FirebaseAuth mAuth;

    private ImageView appLogo;
    private TextInputEditText emailEditText, passwordEditText;
    private Button loginBtn, signUpBtn;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initialize();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mAuth.getCurrentUser() != null) {
            finish();
            switchToNextIntent(false);
        }
    }

    private void initialize() {
        // Firebase instance auth
        mAuth = FirebaseAuth.getInstance();

        // Components initialization
        appLogo = (ImageView) findViewById(R.id.app_logo);
        emailEditText = (TextInputEditText) findViewById(R.id.email_input);
        passwordEditText = (TextInputEditText) findViewById(R.id.password_input);
        loginBtn = (Button) findViewById(R.id.login);
        signUpBtn = (Button) findViewById(R.id.sign_up);
        progressBar = (ProgressBar) findViewById(R.id.loading);

        /* Action Listeners */
        loginBtn.setOnClickListener(this);
        signUpBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == loginBtn) handleLogin();
        else if (v == signUpBtn) handleSignUp();
    }

    private void handleLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            emailEditText.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Please enter a valid email");
            emailEditText.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            passwordEditText.setError("Password is required");
            passwordEditText.requestFocus();
            return;
        }

        if (password.length() < 6) {
            passwordEditText.setError("Minimum lenght of password should be 6");
            passwordEditText.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                progressBar.setVisibility(View.GONE);
                if (task.isSuccessful()) {
                    finish();
                    switchToNextIntent(true);
                } else {
                    Toast.makeText(getApplicationContext(), task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void handleSignUp() {
        finish();
        startActivity(new Intent(this, SignUpActivity.class));
    }

    private void switchToNextIntent(Boolean isLogin) {

        if (isLogin) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        } else {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
        }
    }

    private void loginAnimation() {
        ObjectAnimator animator = ObjectAnimator.ofFloat(appLogo, "y", 420f);
        animator.setDuration(500);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(animator);
        animatorSet.start();
    }
}
