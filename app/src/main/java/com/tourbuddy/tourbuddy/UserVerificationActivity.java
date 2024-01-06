package com.tourbuddy.tourbuddy;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class UserVerificationActivity extends AppCompatActivity {

    Button btnVerify, btnResendVerification;

    FirebaseAuth mAuth;
    FirebaseUser mUser;

    SwipeRefreshLayout swipeRefreshLayout;

    CountDownTimer resendVerificationTimer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_verification);

        btnVerify = findViewById(R.id.btnVerify);
        btnVerify.setEnabled(false);

        btnResendVerification = findViewById(R.id.btnResendVerification);

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();

        swipeRefreshLayout = findViewById(R.id.swipeRefresh);

        resendVerificationTimer();

        btnVerify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mUser.isEmailVerified())
                    sendUserToNextActivity(new Intent(UserVerificationActivity.this, UserSetupActivity.class));
                else
                    btnVerify.setError("Email is not verified");
            }
        });

        btnResendVerification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mUser.isEmailVerified()) {
                    mUser.sendEmailVerification().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Log.d("EmailVerification", "Email verification sent successfully");
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("EmailVerification", "Error while sending email verification", e);
                            btnResendVerification.setError("Error while sending email verification");
                            btnResendVerification.setEnabled(true);

                        }
                    });

                    resendVerificationTimer();

                }
            }
        });


        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    mUser.reload();

                    Log.e("refresh", String.valueOf(mUser.isEmailVerified()));
                    if (mUser.isEmailVerified())
                        btnVerify.setEnabled(true);

                    swipeRefreshLayout.setRefreshing(false); // Stop the refresh animation
                }
            });
        } else {
            Log.e("UserVerificationActivity", "SwipeRefreshLayout is null");
        }

    }


    private void sendUserToNextActivity(Intent intent) {
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void resendVerificationTimer(){
        String btnResendVerificationText = String.valueOf(btnResendVerification.getText());
        btnResendVerification.setEnabled(false);

        // Set the countdown timer for 2 minutes
        resendVerificationTimer = new CountDownTimer(2 * 60 * 1000, 1000) {
            @SuppressLint("DefaultLocale")
            public void onTick(long millisUntilFinished) {
                // Update the button text with the remaining time
                btnResendVerification.setText(String.format("%s (%d)", btnResendVerificationText, millisUntilFinished / 1000));
            }

            public void onFinish() {
                // Enable the button and reset the text
                btnResendVerification.setEnabled(true);
                btnResendVerification.setText(btnResendVerificationText);
            }
        }.start();
    }

}
