package com.tourbuddy.tourbuddy;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;


public class UserProfileActivity extends AppCompatActivity {

    ImageView profilePic;
    TextView username, bio, birthDate, gender;

    FirebaseAuth mAuth;
    FirebaseUser mUser;
    FirebaseFirestore db;
    FirebaseStorage storage;
    StorageReference storageReference;


    SwipeRefreshLayout swipeRefreshLayout;
    BottomNavigationView bottomNavigationView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        swipeRefreshLayout = findViewById(R.id.swipeRefresh);

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        profilePic = findViewById(R.id.profilePic);
        username = findViewById(R.id.username);
        bio = findViewById(R.id.bio);
        birthDate = findViewById(R.id.birthDate);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        //gender = findViewById(R.id.gender);

        bottomNavigationView.setSelectedItemId(R.id.profile);


        loadProfileData();
        // Load the image into the ImageView using Glide
        storageReference.child("images/" + mUser.getUid() + "/profilePic").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                // Got the download URL for 'users/me/profile.png'
                Glide.with(UserProfileActivity.this)
                        .load(uri)
                        .apply(RequestOptions.circleCropTransform())
                        .into(profilePic);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
            }
        });
        //loadProfilePicture();

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    loadProfileData();
                    swipeRefreshLayout.setRefreshing(false);
                }
            });
        } else {
            Log.e("UserVerificationActivity", "SwipeRefreshLayout is null");
        }
    }

    private void loadProfileData() {
        mUser = mAuth.getCurrentUser();
        if (mUser != null) {
            String userId = mUser.getUid();

            // Reference to the Firestore document
            DocumentReference userDocumentRef = db.collection("users").document(userId);

            // Retrieve data from Firestore
            userDocumentRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    if (documentSnapshot.exists()) {
                        // Check if the profilePic field exists in the Firestore document
                        if (documentSnapshot.contains("username"))
                             username.setText(documentSnapshot.getString("username"));
                        if (documentSnapshot.contains("bio"))
                            bio.setText(documentSnapshot.getString("bio"));
                        if (documentSnapshot.contains("birthDate"))
                            birthDate.setText(documentSnapshot.getString("birthDate"));
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e("DATABASE", "Error getting document", e);
                }
            });
        }
    }




}