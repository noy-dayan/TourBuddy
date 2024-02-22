package com.tourbuddy.tourbuddy.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.tourbuddy.tourbuddy.R;
import com.tourbuddy.tourbuddy.utils.AppUtils;

import java.util.Objects;

/**
 * ForgotPasswordActivity allows users to request a password reset link via email.
 */
public class ForgotPasswordActivity extends AppCompatActivity {

    ImageView btnBack;
    EditText inputEmail;
    Button btnResetPassword;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        mAuth = FirebaseAuth.getInstance();

        inputEmail = findViewById(R.id.forgotPasswordEmail);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        btnBack = findViewById(R.id.btnBack);

        btnResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = inputEmail.getText().toString().trim();

                if (email.isEmpty()) {
                    inputEmail.setError("Email is required");
                    inputEmail.requestFocus();
                    return;
                }

                // Send password reset email
                mAuth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(Task<Void> task) {
                        if (task.isSuccessful()) {
                            // Password reset email sent successfully
                            Toast.makeText(ForgotPasswordActivity.this, getString(R.string.password_reset_email_sent_please_check_your_email), Toast.LENGTH_SHORT).show();
                            // Switch back to MainActivity
                            AppUtils.switchActivity(ForgotPasswordActivity.this, MainActivity.class, "rtl");
                        } else {
                            // Failed to send password reset email
                            Toast.makeText(ForgotPasswordActivity.this, getString(R.string.failed_to_send_password_reset_email_please_try_again), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        // Handle back button click
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Switch back to MainActivity
                AppUtils.switchActivity(ForgotPasswordActivity.this, MainActivity.class, "rtl");
            }
        });
    }

    // Override the back button press to provide custom transition animation
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Switch back to MainActivity
        AppUtils.switchActivity(ForgotPasswordActivity.this, MainActivity.class, "rtl");
    }
}
