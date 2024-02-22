package com.tourbuddy.tourbuddy.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.tourbuddy.tourbuddy.R;
import com.tourbuddy.tourbuddy.utils.AppUtils;

import java.util.regex.Pattern;

/**
 * Activity for user register, allowing users to register using email&password.
 */
public class RegisterActivity extends AppCompatActivity {

    // UI elements
    TextView alreadyHaveAccount;
    EditText inputEmail, inputPassword, inputConfirmPassword;
    Button btnRegister;

    // Email pattern for validation
    String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";

    // Progress dialog for user feedback during registration
    ProgressDialog progressDialog;

    // Firebase instances
    FirebaseAuth mAuth;
    FirebaseUser mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize UI elements
        alreadyHaveAccount = findViewById(R.id.alreadyHaveAccount);
        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        inputConfirmPassword = findViewById(R.id.inputConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);

        // Initialize progress dialog
        progressDialog = new ProgressDialog(this);

        // Initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();

        // Set click listener for already have an account text
        alreadyHaveAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppUtils.switchActivity(RegisterActivity.this, MainActivity.class, "rtl");
            }
        });

        // Set click listener for register button
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PerformAuth();
            }
        });
    }

    // Method to perform user registration/authentication
    private void PerformAuth() {
        String email = inputEmail.getText().toString();
        String password = inputPassword.getText().toString();
        String confirmPassword = inputConfirmPassword.getText().toString();

        // Validate email, password, and confirm password
        if (!Pattern.matches(emailPattern, email)) {
            inputEmail.setError(getString(R.string.emailError));
            inputEmail.requestFocus();
        } else if (password.isEmpty() || password.length() < 8) {
            inputPassword.setError(getString(R.string.passwordError));
            inputPassword.requestFocus();
        } else if (!password.equals(confirmPassword)) {
            inputConfirmPassword.setError(getString(R.string.confirmPasswordError));
            inputConfirmPassword.requestFocus();
        } else {
            // Display progress dialog
            progressDialog.setMessage("Please Wait While Registration...");
            progressDialog.setTitle("Registration");
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();

            // Create a user with email and password
            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        // Get current user
                        mUser = mAuth.getCurrentUser();
                        if (mUser != null && !mUser.isEmailVerified()) {
                            // Send email verification
                            mUser.sendEmailVerification().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    Log.d("EmailVerification", "Email verification sent successfully");
                                    AppUtils.switchActivity(RegisterActivity.this, UserVerificationActivity.class, "ltr");
                                    progressDialog.dismiss();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.e("EmailVerification", "Error while sending email verification", e);
                                    inputEmail.setError("Error while sending email verification");
                                    inputEmail.requestFocus();
                                }
                            });
                        } else {
                            progressDialog.dismiss();
                            // Handle the case where the user is already verified or null
                        }
                    } else {
                        progressDialog.dismiss();
                        // Handle registration failure
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            inputEmail.setError(getString(R.string.emailError2));
                            inputEmail.requestFocus();
                        } else {
                            inputEmail.setError(getString(R.string.emailError));
                            inputEmail.requestFocus();
                        }
                    }
                }
            });
        }
    }

    // Override the back button press to provide custom transition animation
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        AppUtils.switchActivity(RegisterActivity.this, MainActivity.class, "rtl");
    }
}
