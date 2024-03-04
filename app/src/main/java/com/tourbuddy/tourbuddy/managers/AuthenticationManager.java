package com.tourbuddy.tourbuddy.managers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class AuthenticationManager {
    private FirebaseAuth mAuth;

    public AuthenticationManager() {
        mAuth = FirebaseAuth.getInstance();
    }

    // Sign in with email and password
    public void signInWithEmailAndPassword(String email, String password, Context context) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener((Activity) context, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Toast.makeText(context, "Authentication successful.",
                                    Toast.LENGTH_SHORT).show();
                            // Proceed with next action, e.g., navigating to main screen
                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(context, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // Sign up with email and password
    public void signUpWithEmailAndPassword(String email, String password, Context context) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener((Activity) context, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign up success, update UI with the signed-up user's information
                            Toast.makeText(context, "Registration successful.",
                                    Toast.LENGTH_SHORT).show();
                            // Proceed with next action, e.g., navigating to main screen
                        } else {
                            // If sign up fails, display a message to the user.
                            Toast.makeText(context, "Registration failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // Sign out
    public void signOut() {
        mAuth.signOut();
    }
}
