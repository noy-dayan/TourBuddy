package com.tourbuddy.tourbuddy.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tourbuddy.tourbuddy.R;

import java.util.Locale;

/**
 * Activity for user login, allowing users to login using email&password, google or facebook.
 */
public class MainActivity extends AppCompatActivity {
    // UI elements
    TextView createNewAccount;
    EditText inputEmail, inputPassword;
    Button btnLogin;
    ImageView btnGoogle, btnFacebook;

    // Email pattern for validation
    String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";

    // Progress dialog for user feedback during login
    ProgressDialog progressDialog;

    // Firebase instances
    FirebaseFirestore db;
    FirebaseAuth mAuth;
    FirebaseUser mUser;

    GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the locale to English
        setAppLocale("en");

        // Set the app theme to night mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        // Set the content view
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        createNewAccount = findViewById(R.id.createNewAccount);
        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogle = findViewById(R.id.btnGoogle);
        btnFacebook = findViewById(R.id.btnFacebook);

        // Initialize progress dialog
        progressDialog = new ProgressDialog(this);

        // Initialize Firebase instances
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Set click listener for creating a new account
        createNewAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, RegisterActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });

        // Set click listener for login button
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LoginWithEmailAndPassword();
            }
        });
    }

    // Override the back button press to provide custom transition animation
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    // Method to handle login with email and password
    private void LoginWithEmailAndPassword() {
        String email = inputEmail.getText().toString();
        String password = inputPassword.getText().toString();

        // Validate email and password
        if (!email.matches(emailPattern)) {
            inputEmail.setError(getString(R.string.emailError));
            inputEmail.requestFocus();
        } else if (password.isEmpty() || password.length() < 8) {
            inputPassword.setError(getString(R.string.passwordError));
            inputPassword.requestFocus();
        } else {
            // Display progress dialog
            progressDialog.setMessage("Please Wait While Logging In...");
            progressDialog.setTitle("Login");
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();

            // Authenticate user with Firebase
            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        // Get current user
                        mUser = mAuth.getCurrentUser();

                        if (mUser != null) {
                            String userId = mUser.getUid();

                            // Reference to the Firestore document
                            DocumentReference userDocumentRef = db.collection("users").document(userId);

                            // Check if the Firestore document exists
                            userDocumentRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if (task.isSuccessful()) {
                                        DocumentSnapshot document = task.getResult();
                                        if (document.exists()) {
                                            Log.d("DATABASE", "Document " + userId + " exists and emails are verified, moving to user profile activity");
                                            sendUserToNextActivity(new Intent(MainActivity.this, UserHomeActivity.class));

                                        } else {
                                            if (mUser.isEmailVerified()) {
                                                Log.d("DATABASE", "Document " + userId + " does not exist, and emails are verified, moving to user setup activity");
                                                sendUserToNextActivity(new Intent(MainActivity.this, UserSetupActivity.class));

                                            } else {
                                                Log.d("DATABASE", "Document " + userId + " does not exist, and email is not verified, moving to user verification activity");
                                                sendUserToNextActivity(new Intent(MainActivity.this, UserVerificationActivity.class));

                                            }
                                        }
                                    } else {
                                        Log.d("DATABASE", "Failed with: ", task.getException());
                                        Toast.makeText(MainActivity.this, String.valueOf(task.getException()), Toast.LENGTH_SHORT).show();
                                    }
                                    progressDialog.dismiss();
                                }
                            });

                        }

                    } else {
                        progressDialog.dismiss();
                        if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            inputEmail.setError(getString(R.string.emailOrPasswordError));
                            inputEmail.requestFocus();
                        } else
                            Toast.makeText(MainActivity.this, String.valueOf(task.getException()), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    /**
     *  Method to transition to the next activity with clearing task and adding flags
     */
    private void sendUserToNextActivity(Intent intent) {
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    /**
     *  Method to set the app locale based on the language code.
     */
    private void setAppLocale(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration configuration = new Configuration();
        configuration.locale = locale;
        getBaseContext().getResources().updateConfiguration(configuration, getBaseContext().getResources().getDisplayMetrics());
    }
}
