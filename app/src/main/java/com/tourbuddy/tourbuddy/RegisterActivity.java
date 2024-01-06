package com.tourbuddy.tourbuddy;

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
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;
import java.util.regex.Pattern;


public class RegisterActivity extends AppCompatActivity {

    TextView alreadyHaveAccount;
    EditText inputEmail, inputPassword, inputConfirmPassword;
    Button btnRegister;
    String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
    ProgressDialog progressDialog;

    FirebaseAuth mAuth;
    FirebaseUser mUser;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        alreadyHaveAccount = findViewById(R.id.alreadyHaveAccount);
        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        inputConfirmPassword = findViewById(R.id.inputConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);

        progressDialog = new ProgressDialog(this);

        mAuth = FirebaseAuth.getInstance();

        alreadyHaveAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);

            }
        });


        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PerformAuth();
            }
        });

    }

    private void PerformAuth() {
        String email = inputEmail.getText().toString();
        String password = inputPassword.getText().toString();
        String confirmPassword = inputConfirmPassword.getText().toString();

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
            progressDialog.setMessage("Please Wait While Registration...");
            progressDialog.setTitle("Registration");
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();

            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        mUser = mAuth.getCurrentUser();
                        if (mUser != null && !mUser.isEmailVerified()) {
                            mUser.sendEmailVerification().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    Log.d("EmailVerification", "Email verification sent successfully");
                                    sendUserToNextActivity(new Intent(RegisterActivity.this, UserVerificationActivity.class));
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


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    private void sendUserToNextActivity(Intent intent) {
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

    }

}