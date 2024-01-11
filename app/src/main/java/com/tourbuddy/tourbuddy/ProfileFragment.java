package com.tourbuddy.tourbuddy;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Locale;

public class ProfileFragment extends Fragment {

    private static final String PREFS_NAME = "ProfilePrefs";
    SharedPreferences prefs;

    ImageView profilePic;
    TextView username, bio, birthDate, gender, type;

    FirebaseAuth mAuth;
    FirebaseUser mUser;
    FirebaseFirestore db;
    StorageReference storageReference;

    SwipeRefreshLayout swipeRefreshLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        prefs = view.getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        showLoading(true);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefresh);

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();

        db = FirebaseFirestore.getInstance();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        profilePic = view.findViewById(R.id.profilePic);
        username = view.findViewById(R.id.username);
        bio = view.findViewById(R.id.bio);
        birthDate = view.findViewById(R.id.birthDate);
        gender = view.findViewById(R.id.gender);
        type = view.findViewById(R.id.type);

        // Attempt to load data from cache
        if (loadDataFromCache(view))
            // Data loaded from cache, no need to fetch from the server
            showLoading(false);
        else
            // Data not found in cache, fetch from the server
            loadDataFromFirebase(view);


        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                loadDataFromFirebase(view);
                swipeRefreshLayout.setRefreshing(false);
            });
        } else {
            Log.e("ProfileFragment", "SwipeRefreshLayout is null");
        }

        return view;
    }

    private void loadDataFromFirebase(View view) {
        mUser = mAuth.getCurrentUser();
        if (mUser != null) {
            String userId = mUser.getUid();

            // Reference to the Firestore document
            DocumentReference userDocumentRef = db.collection("users").document(userId);

            // Retrieve data from Firestore
            userDocumentRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    if (isAdded() && documentSnapshot.exists()) {
                        // Check if the profilePic field exists in the Firestore document
                        if (documentSnapshot.contains("username"))
                            username.setText(documentSnapshot.getString("username"));
                        if (documentSnapshot.contains("bio"))
                            bio.setText(documentSnapshot.getString("bio"));
                        if (documentSnapshot.contains("birthDate"))
                            birthDate.setText(documentSnapshot.getString("birthDate"));
                        if (documentSnapshot.contains("gender"))
                            gender.setText(documentSnapshot.getString("gender"));
                        if (documentSnapshot.contains("type"))
                            type.setText(documentSnapshot.getString("type"));

                    }

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e("DATABASE", "Error getting document", e);
                }
            });

            // Load the image into the ImageView using Glide
            storageReference.child("images/" + mUser.getUid() + "/profilePic").getDownloadUrl()
                    .addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            // Got the download URL for 'users/me/profile.png'
                            Glide.with(view)
                                    .load(uri)
                                    .apply(RequestOptions.circleCropTransform())
                                    .into(profilePic);
                            showLoading(false);
                            saveDataToCache(
                                    username.getText().toString(),
                                    bio.getText().toString(),
                                    birthDate.getText().toString(),
                                    gender.getText().toString(),
                                    type.getText().toString(),
                                    uri.toString() // Save profile picture URL
                            );
                        }

                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            showLoading(false);
                        }
                    });


        }
    }

    private boolean loadDataFromCache(View view) {
        // Check if data exists in cache
        if (prefs.contains("username") && prefs.contains("bio") && prefs.contains("birthDate")
                && prefs.contains("gender") && prefs.contains("type") && prefs.contains("profilePicUrl")) {

            // Load data from cache
            username.setText(prefs.getString("username", ""));
            bio.setText(prefs.getString("bio", ""));
            birthDate.setText(prefs.getString("birthDate", ""));
            gender.setText(prefs.getString("gender", ""));
            type.setText(prefs.getString("type", ""));

            // Load profile picture from cache (if available)
            String profilePicUrl = prefs.getString("profilePicUrl", "");
            if (!profilePicUrl.isEmpty()) {
                Glide.with(view)
                        .load(profilePicUrl)
                        .apply(RequestOptions.circleCropTransform())
                        .into(profilePic);
            }

            return true; // Data loaded from cache
        }

        return false; // Data not found in cache
    }

    private void saveDataToCache(String username, String bio, String birthDate, String gender, String type, String profilePicUrl) {
        if (isAdded()) {
            SharedPreferences.Editor editor = prefs.edit();

            // Save data to cache
            editor.putString("username", username);
            editor.putString("bio", bio);
            editor.putString("birthDate", birthDate);
            editor.putString("gender", gender);
            editor.putString("type", type);

            // Save profile picture URL to cache
            editor.putString("profilePicUrl", profilePicUrl);

            editor.apply();
        }
    }

    // Method to show/hide the loading screen
    private void showLoading(boolean show) {
        View view = getView();  // Get the view

        if (view != null) {
            View loadingOverlay = view.findViewById(R.id.loadingOverlay);
            View mainOverlay = view.findViewById(R.id.mainOverlay);

            if (show) {
                loadingOverlay.setVisibility(View.VISIBLE);
                mainOverlay.setVisibility(View.GONE);
            } else {
                loadingOverlay.setVisibility(View.GONE);
                mainOverlay.setVisibility(View.VISIBLE);
            }
        }
    }
}
