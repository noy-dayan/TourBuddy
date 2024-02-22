package com.tourbuddy.tourbuddy.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tourbuddy.tourbuddy.R;
import com.tourbuddy.tourbuddy.utils.AppUtils;

/**
 * Activity for user login, allowing users to login using email&password or google account.
 */
public class MainActivity extends AppCompatActivity {

    // Request code for Google Sign-In
    static final int RC_SIGN_IN = 9001;

    // UI elements
    TextView createNewAccount, forgotPassword;
    EditText inputEmail, inputPassword;
    Button btnLogin;
    ImageView btnGoogle;

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
        AppUtils.setAppLocale(MainActivity.this,"en");

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
        forgotPassword = findViewById(R.id.forgotPassword);

        // Initialize progress dialog
        progressDialog = new ProgressDialog(this);

        // Initialize Firebase instances
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Display progress dialog
        progressDialog.setMessage(getResources().getString(R.string.please_wait_while_logging_in));
        progressDialog.setTitle(getResources().getString(R.string.login));
        progressDialog.setCanceledOnTouchOutside(false);

        // Initialize GoogleSignInClient
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Set click listener for creating a new account
        createNewAccount.setOnClickListener(v -> AppUtils.switchActivity(MainActivity.this, RegisterActivity.class, "ltr"));

        // Set click listener for login button
        btnLogin.setOnClickListener(v -> LoginWithEmailAndPassword());

        // Set click listener for Google Sign-In button
        btnGoogle.setOnClickListener(v -> LoginWithGoogle());

        // Set click listener for forgot password
        forgotPassword.setOnClickListener(v -> AppUtils.switchActivity(MainActivity.this, ForgotPasswordActivity.class, "ltr"));
    }

    // Method to handle login with email and password
    private void LoginWithEmailAndPassword() {
        // Retrieve email and password from input fields
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
                                            AppUtils.switchActivity(MainActivity.this, UserHomeActivity.class, "ltr");

                                        } else {
                                            if (mUser.isEmailVerified()) {
                                                Log.d("DATABASE", "Document " + userId + " does not exist, and emails are verified, moving to user setup activity");
                                                AppUtils.switchActivity(MainActivity.this, UserSetupActivity.class, "ltr");

                                            } else {
                                                Log.d("DATABASE", "Document " + userId + " does not exist, and email is not verified, moving to user verification activity");
                                                AppUtils.switchActivity(MainActivity.this, UserVerificationActivity.class, "ltr");

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

    // Method to handle login with Google
    private void LoginWithGoogle(){
        // Sign out the current user (if any) before starting the Google sign-in process
        mGoogleSignInClient.signOut().addOnCompleteListener(MainActivity.this,
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        // Start the Google sign-in process
                        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                        startActivityForResult(signInIntent, RC_SIGN_IN);
                    }
                });
    }

    // Handle the result of Google Sign-In
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign-In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign-In failed, update UI accordingly
                Log.w("LOGIN", "Google sign in failed", e);
                // Optionally, display an error message to the user
            }
        }
    }

    // Method to authenticate with Firebase using Google credentials
    private void firebaseAuthWithGoogle(String idToken) {
        progressDialog.show();
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("LOGIN", "signInWithCredential:success");

                            // Get user information
                            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(MainActivity.this);
                            String userName = account.getDisplayName();
                            Uri userPhotoUri = account.getPhotoUrl();

                            // Inside your LoginWithEmailAndPassword method after getting the current user
                            mUser = mAuth.getCurrentUser();
                            String userId = mUser.getUid();

                            // Reference to the Firestore document
                            DocumentReference userDocumentRef = db.collection("users").document(userId);

                            // Check if the Firestore user document exists
                            userDocumentRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if (task.isSuccessful()) {
                                        DocumentSnapshot document = task.getResult();
                                        if (document.exists()) {
                                            // Document exists, user is authenticated, navigate to user profile activity
                                            AppUtils.switchActivity(MainActivity.this, UserHomeActivity.class, "ltr");
                                        } else {
                                            // Document doesn't exist, user needs to set up their profile
                                            Intent intent = new Intent(MainActivity.this, UserSetupActivity.class);
                                            intent.putExtra("username", userName);
                                            intent.putExtra("userProfilePicUri", userPhotoUri);
                                            AppUtils.switchActivity(MainActivity.this, intent, "ltr");
                                        }
                                    } else {
                                        // Error occurred while checking document existence
                                        Log.d("MainActivity", "Failed with: ", task.getException());
                                        Toast.makeText(MainActivity.this, "Failed to check user document", Toast.LENGTH_SHORT).show();
                                    }
                                    progressDialog.dismiss(); // Dismiss progress dialog after checking user document
                                }
                            });
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("LOGIN", "signInWithCredential:failure", task.getException());
                            progressDialog.dismiss();
                        }
                    }
                });
    }

    // Override the back button press to provide custom transition animation
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
}
